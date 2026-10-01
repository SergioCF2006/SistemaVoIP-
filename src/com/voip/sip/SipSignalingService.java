package com.voip.sip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Servicio de Red UDP de Señalización SIP.
 * Escucha peticiones entrantes en el puerto SIP (5060 por defecto) y envía mensajes SIP.
 */
public class SipSignalingService {
    private DatagramSocket socket;
    private Thread listenThread;
    private volatile boolean running = false;
    private SipSignalingListener listener;
    private int localPort = 5060;

    public SipSignalingService(int localPort) {
        this.localPort = localPort;
    }

    public SipSignalingService() {
        this(5060);
    }

    public void setListener(SipSignalingListener listener) {
        this.listener = listener;
    }

    public synchronized void start() throws SocketException {
        if (running) return;

        socket = new DatagramSocket(localPort);
        running = true;

        listenThread = new Thread(this::listenLoop, "SIP-ListenThread-UDP-" + localPort);
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (listenThread != null) {
            listenThread.interrupt();
        }
    }

    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : localPort;
    }

    public void sendSipText(String sipText, String destIp, int destPort) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket SIP no está abierto");
        }
        byte[] data = sipText.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(destIp);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, destPort);
        socket.send(packet);
    }

    private void listenLoop() {
        byte[] buffer = new byte[4096];

        while (running && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                SipParser.ParsedMessage parsed = SipParser.parse(text);

                if (parsed != null && listener != null) {
                    dispatchMessage(parsed);
                }
            } catch (SocketException e) {
                if (!running) break; // Cierre normal al detener servicio
            } catch (Exception e) {
                System.err.println("Error procesando datagrama SIP: " + e.getMessage());
            }
        }
    }

    private void dispatchMessage(SipParser.ParsedMessage msg) {
        String methodOrStatus = msg.getMethodOrStatus().toUpperCase();

        if (methodOrStatus.startsWith("INVITE")) {
            listener.onInviteReceived(msg);
        } else if (methodOrStatus.contains("180") || methodOrStatus.contains("RINGING")) {
            listener.onRingingReceived(msg);
        } else if (methodOrStatus.contains("200") || methodOrStatus.contains("OK")) {
            listener.onOkReceived(msg);
        } else if (methodOrStatus.startsWith("ACK")) {
            listener.onAckReceived(msg);
        } else if (methodOrStatus.startsWith("BYE")) {
            listener.onByeReceived(msg);
        } else if (methodOrStatus.contains("486") || methodOrStatus.contains("BUSY")) {
            listener.onBusyReceived(msg);
        }
    }
}
