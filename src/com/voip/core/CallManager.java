package com.voip.core;

import com.voip.model.*;
import com.voip.registry.SipLocationService;
import com.voip.rtp.RtpAudioEngine;
import com.voip.sip.SipMessage;
import com.voip.sip.SipParser;
import com.voip.sip.SipSignalingListener;
import com.voip.sip.SipSignalingService;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Controlador central del sistema VoIP y Administrador de la Máquina de Estados de Llamada.
 */
public class CallManager implements SipSignalingListener {
    private String localExtension = "101";
    private String localIp = "127.0.0.1";
    private int localSipPort = 5060;
    private int localRtpPort = 7000;

    private CallState currentState = CallState.DISPONIBLE;
    private CallSession currentSession;

    private final SipLocationService locationService;
    private SipSignalingService signalingService;
    private RtpAudioEngine audioEngine;
    private CallListener listener;

    private String pendingCallId;
    private SipParser.ParsedMessage incomingInvite;

    public CallManager(SipLocationService locationService) {
        this.locationService = locationService;
        this.localIp = detectLocalIp();
    }

    public void setListener(CallListener listener) {
        this.listener = listener;
    }

    public void configureLocalEndpoint(String extension, String ip, int sipPort, int rtpPort) {
        this.localExtension = extension;
        this.localIp = (ip != null && !ip.isBlank()) ? ip : detectLocalIp();
        this.localSipPort = sipPort;
        this.localRtpPort = rtpPort;
    }

    public synchronized void start() throws Exception {
        if (signalingService != null) {
            signalingService.stop();
        }
        signalingService = new SipSignalingService(localSipPort);
        signalingService.setListener(this);
        signalingService.start();

        audioEngine = new RtpAudioEngine(localRtpPort);
        log("Servicio VoIP iniciado en IP " + localIp + " [SIP: " + localSipPort + " | RTP: " + localRtpPort + "]");
    }

    public synchronized void stop() {
        if (audioEngine != null) {
            audioEngine.stopStreaming();
        }
        if (signalingService != null) {
            signalingService.stop();
        }
        setState(CallState.DISPONIBLE, "Sistema detenido");
    }

    /**
     * Inicia una llamada hacia la extensión destino.
     */
    public synchronized void makeCall(String targetExtension) {
        if (currentState != CallState.DISPONIBLE) {
            log("No se puede llamar: el sistema no está en estado Disponible");
            return;
        }

        SipContact contact = locationService.resolveExtension(targetExtension);
        if (contact == null) {
            log("Error: Extensión " + targetExtension + " no registrada en el servicio de ubicación");
            return;
        }

        pendingCallId = UUID.randomUUID().toString();
        currentSession = new CallSession(
            localExtension, targetExtension, contact.getIpAddress(),
            contact.getSipPort(), contact.getRtpPort(), localRtpPort, true
        );

        String inviteText = SipMessage.createInvite(
            localExtension, localIp, localSipPort,
            targetExtension, contact.getIpAddress(), contact.getSipPort(),
            localRtpPort, pendingCallId
        );

        try {
            signalingService.sendSipText(inviteText, contact.getIpAddress(), contact.getSipPort());
            setState(CallState.LLAMANDO, "Llamando a extensión " + targetExtension + "...");
            log("SIP INVITE enviado a " + contact.getIpAddress() + ":" + contact.getSipPort());
        } catch (IOException e) {
            log("Error al enviar SIP INVITE: " + e.getMessage());
            setState(CallState.DISPONIBLE, "Error de conexión");
        }
    }

    /**
     * Acepta una llamada entrante.
     */
    public synchronized void acceptCall() {
        if (currentState != CallState.LLAMADA_ENTRANTE || incomingInvite == null) {
            log("No hay llamada entrante activa para aceptar");
            return;
        }

        String okText = SipMessage.create200Ok(
            incomingInvite.getFromExtension(), incomingInvite.getFromIp(),
            localExtension, localIp, localRtpPort,
            incomingInvite.getCallId(), "INVITE"
        );

        try {
            signalingService.sendSipText(okText, incomingInvite.getFromIp(), 5060);
            
            // Iniciar flujo de audio RTP bidireccional
            audioEngine.startStreaming(incomingInvite.getFromIp(), incomingInvite.getRemoteRtpPort());
            
            setState(CallState.EN_LLAMADA, "En llamada con " + incomingInvite.getFromExtension());
            log("Llamada aceptada. Audio RTP activo con " + incomingInvite.getFromIp() + ":" + incomingInvite.getRemoteRtpPort());
        } catch (Exception e) {
            log("Error al aceptar llamada: " + e.getMessage());
        }
    }

    /**
     * Rechaza una llamada entrante.
     */
    public synchronized void rejectCall() {
        if (currentState != CallState.LLAMADA_ENTRANTE || incomingInvite == null) {
            return;
        }

        String busyText = SipMessage.create486Busy(
            incomingInvite.getFromExtension(), incomingInvite.getFromIp(),
            localExtension, localIp, incomingInvite.getCallId()
        );

        try {
            signalingService.sendSipText(busyText, incomingInvite.getFromIp(), 5060);
            log("Llamada de " + incomingInvite.getFromExtension() + " rechazada");
            recordHistory(incomingInvite.getFromExtension(), CallHistoryEntry.CallType.PERDIDA, 0);
        } catch (IOException e) {
            log("Error enviando SIP 486 Busy: " + e.getMessage());
        } finally {
            incomingInvite = null;
            setState(CallState.DISPONIBLE, "Llamada rechazada");
        }
    }

    /**
     * Cuelga la llamada activa o cancela la llamada en curso.
     */
    public synchronized void hangUp() {
        if (currentSession == null && incomingInvite == null) {
            setState(CallState.DISPONIBLE, "Disponible");
            return;
        }

        String remoteIp = currentSession != null ? currentSession.getRemoteIp() : 
                         (incomingInvite != null ? incomingInvite.getFromIp() : "127.0.0.1");
        String remoteExt = currentSession != null ? currentSession.getRemoteExtension() : 
                          (incomingInvite != null ? incomingInvite.getFromExtension() : "desconocido");
        String callId = currentSession != null ? pendingCallId : 
                       (incomingInvite != null ? incomingInvite.getCallId() : "");

        long duration = currentSession != null ? currentSession.getDurationSeconds() : 0;
        boolean wasOutgoing = currentSession != null && currentSession.isOutgoing();

        String byeText = SipMessage.createBye(localExtension, localIp, remoteExt, remoteIp, callId);
        try {
            signalingService.sendSipText(byeText, remoteIp, 5060);
            log("SIP BYE enviado a " + remoteIp);
        } catch (Exception ignored) {}

        if (audioEngine != null) {
            audioEngine.stopStreaming();
        }

        if (currentSession != null) {
            recordHistory(remoteExt, wasOutgoing ? CallHistoryEntry.CallType.SALIENTE : CallHistoryEntry.CallType.ENTRANTE, duration);
        }

        currentSession = null;
        incomingInvite = null;
        pendingCallId = null;

        setState(CallState.FINALIZADA, "Llamada finalizada");
        
        // Volver a disponible tras 1 segundo
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            setState(CallState.DISPONIBLE, "Disponible");
        }).start();
    }

    // --- Implementación de Callbacks SipSignalingListener ---

    @Override
    public synchronized void onInviteReceived(SipParser.ParsedMessage msg) {
        log("SIP INVITE recibido de Ext. " + msg.getFromExtension() + " (" + msg.getFromIp() + ")");

        if (currentState != CallState.DISPONIBLE) {
            // Responder 486 Busy si ya estamos en otra llamada
            String busyText = SipMessage.create486Busy(msg.getFromExtension(), msg.getFromIp(), localExtension, localIp, msg.getCallId());
            try {
                signalingService.sendSipText(busyText, msg.getFromIp(), 5060);
            } catch (IOException ignored) {}
            return;
        }

        incomingInvite = msg;
        currentSession = new CallSession(
            localExtension, msg.getFromExtension(), msg.getFromIp(),
            5060, msg.getRemoteRtpPort(), localRtpPort, false
        );

        // Responder 180 Ringing
        String ringingText = SipMessage.create180Ringing(msg.getFromExtension(), msg.getFromIp(), localExtension, localIp, msg.getCallId());
        try {
            signalingService.sendSipText(ringingText, msg.getFromIp(), 5060);
        } catch (IOException ignored) {}

        setState(CallState.LLAMADA_ENTRANTE, "Llamada entrante de Extensión " + msg.getFromExtension());
        if (listener != null) {
            listener.onIncomingCall(msg.getFromExtension(), msg.getFromIp());
        }
    }

    @Override
    public synchronized void onRingingReceived(SipParser.ParsedMessage msg) {
        log("SIP 180 Ringing recibido de " + msg.getFromExtension());
        setState(CallState.LLAMANDO, "Timbrando en extensión " + msg.getFromExtension() + "...");
    }

    @Override
    public synchronized void onOkReceived(SipParser.ParsedMessage msg) {
        log("SIP 200 OK recibido de " + msg.getFromExtension());
        if (currentState == CallState.LLAMANDO && currentSession != null) {
            String targetIp = currentSession.getRemoteIp();
            // Enviar ACK a la IP remota del destino
            String ackText = SipMessage.createAck(localExtension, localIp, msg.getFromExtension(), targetIp, msg.getCallId());
            try {
                signalingService.sendSipText(ackText, targetIp, 5060);
            } catch (IOException ignored) {}

            // Iniciar audio RTP hacia la IP remota del destino
            try {
                audioEngine.startStreaming(targetIp, msg.getRemoteRtpPort());
                setState(CallState.EN_LLAMADA, "En llamada con " + msg.getFromExtension());
            } catch (Exception e) {
                log("Error al iniciar audio RTP: " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized void onAckReceived(SipParser.ParsedMessage msg) {
        log("SIP ACK recibido de " + msg.getFromExtension());
    }

    @Override
    public synchronized void onByeReceived(SipParser.ParsedMessage msg) {
        log("SIP BYE recibido de " + msg.getFromExtension());
        
        // Responder 200 OK
        String okText = SipMessage.create200Ok(msg.getFromExtension(), msg.getFromIp(), localExtension, localIp, 0, msg.getCallId(), "BYE");
        try {
            signalingService.sendSipText(okText, msg.getFromIp(), 5060);
        } catch (IOException ignored) {}

        if (audioEngine != null) {
            audioEngine.stopStreaming();
        }

        long duration = currentSession != null ? currentSession.getDurationSeconds() : 0;
        recordHistory(msg.getFromExtension(), CallHistoryEntry.CallType.ENTRANTE, duration);

        currentSession = null;
        incomingInvite = null;

        setState(CallState.FINALIZADA, "Llamada finalizada por el otro extremo");
        
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            setState(CallState.DISPONIBLE, "Disponible");
        }).start();
    }

    @Override
    public synchronized void onBusyReceived(SipParser.ParsedMessage msg) {
        log("SIP 486 Busy (Ocupado/Rechazado) recibido de " + msg.getFromExtension());
        recordHistory(msg.getFromExtension(), CallHistoryEntry.CallType.PERDIDA, 0);
        currentSession = null;
        setState(CallState.FINALIZADA, "Llamada rechazada / ocupado");
        
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            setState(CallState.DISPONIBLE, "Disponible");
        }).start();
    }

    // --- Auxiliares ---

    private void setState(CallState state, String message) {
        this.currentState = state;
        if (listener != null) {
            listener.onStateChanged(state, message);
        }
    }

    private void recordHistory(String extension, CallHistoryEntry.CallType type, long duration) {
        CallHistoryEntry entry = new CallHistoryEntry(extension, type, duration);
        if (listener != null) {
            listener.onHistoryAdded(entry);
        }
    }

    private void log(String message) {
        System.out.println("[CallManager] " + message);
        if (listener != null) {
            listener.onLogMessage(message);
        }
    }

    private String detectLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public CallState getCurrentState() { return currentState; }
    public String getLocalExtension() { return localExtension; }
    public String getLocalIp() { return localIp; }
}
