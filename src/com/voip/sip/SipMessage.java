package com.voip.sip;

import java.util.UUID;

/**
 * Generador de mensajes SIP RFC 3261 en formato texto estándar.
 */
public class SipMessage {

    public enum Type {
        INVITE,
        RINGING_180,
        OK_200,
        ACK,
        BYE,
        BUSY_486
    }

    /**
     * Genera un mensaje SIP INVITE con cuerpo SDP que especifica el puerto de audio RTP.
     */
    public static String createInvite(String fromExt, String fromIp, int fromSipPort, 
                                      String toExt, String toIp, int toSipPort, 
                                      int localRtpPort, String callId) {
        String sdp = createSdpBody(fromExt, fromIp, localRtpPort);
        int sdpLength = sdp.getBytes().length;

        return String.format(
            "INVITE sip:%s@%s:%d SIP/2.0\r\n" +
            "Via: SIP/2.0/UDP %s:%d;branch=z9hG4bK%s\r\n" +
            "From: <sip:%s@%s>;tag=%s\r\n" +
            "To: <sip:%s@%s>\r\n" +
            "Call-ID: %s\r\n" +
            "CSeq: 1 INVITE\r\n" +
            "Contact: <sip:%s@%s:%d>\r\n" +
            "Content-Type: application/sdp\r\n" +
            "Content-Length: %d\r\n" +
            "\r\n" +
            "%s",
            toExt, toIp, toSipPort,
            fromIp, fromSipPort, UUID.randomUUID().toString().substring(0, 8),
            fromExt, fromIp, UUID.randomUUID().toString().substring(0, 8),
            toExt, toIp,
            callId,
            fromExt, fromIp, fromSipPort,
            sdpLength,
            sdp
        );
    }

    /**
     * Genera la respuesta SIP 180 Ringing.
     */
    public static String create180Ringing(String fromExt, String fromIp, String toExt, String toIp, String callId) {
        return String.format(
            "SIP/2.0 180 Ringing\r\n" +
            "From: <sip:%s@%s>\r\n" +
            "To: <sip:%s@%s>\r\n" +
            "Call-ID: %s\r\n" +
            "CSeq: 1 INVITE\r\n" +
            "Content-Length: 0\r\n" +
            "\r\n",
            fromExt, fromIp, toExt, toIp, callId
        );
    }

    /**
     * Genera la respuesta SIP 200 OK con cuerpo SDP confirmando el puerto de audio RTP.
     */
    public static String create200Ok(String fromExt, String fromIp, String toExt, String toIp, 
                                     int localRtpPort, String callId, String method) {
        if ("BYE".equalsIgnoreCase(method)) {
            return String.format(
                "SIP/2.0 200 OK\r\n" +
                "From: <sip:%s@%s>\r\n" +
                "To: <sip:%s@%s>\r\n" +
                "Call-ID: %s\r\n" +
                "CSeq: 2 BYE\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n",
                fromExt, fromIp, toExt, toIp, callId
            );
        }

        String sdp = createSdpBody(toExt, toIp, localRtpPort);
        int sdpLength = sdp.getBytes().length;

        return String.format(
            "SIP/2.0 200 OK\r\n" +
            "From: <sip:%s@%s>\r\n" +
            "To: <sip:%s@%s>\r\n" +
            "Call-ID: %s\r\n" +
            "CSeq: 1 INVITE\r\n" +
            "Content-Type: application/sdp\r\n" +
            "Content-Length: %d\r\n" +
            "\r\n" +
            "%s",
            fromExt, fromIp, toExt, toIp, callId, sdpLength, sdp
        );
    }

    /**
     * Genera la confirmación SIP ACK.
     */
    public static String createAck(String fromExt, String fromIp, String toExt, String toIp, String callId) {
        return String.format(
            "ACK sip:%s@%s SIP/2.0\r\n" +
            "From: <sip:%s@%s>\r\n" +
            "To: <sip:%s@%s>\r\n" +
            "Call-ID: %s\r\n" +
            "CSeq: 1 ACK\r\n" +
            "Content-Length: 0\r\n" +
            "\r\n",
            toExt, toIp, fromExt, fromIp, toExt, toIp, callId
        );
    }

    /**
     * Genera la petición SIP BYE para colgar la llamada.
     */
    public static String createBye(String fromExt, String fromIp, String toExt, String toIp, String callId) {
        return String.format(
            "BYE sip:%s@%s SIP/2.0\r\n" +
            "From: <sip:%s@%s>\r\n" +
            "To: <sip:%s@%s>\r\n" +
            "Call-ID: %s\r\n" +
            "CSeq: 2 BYE\r\n" +
            "Content-Length: 0\r\n" +
            "\r\n",
            toExt, toIp, fromExt, fromIp, toExt, toIp, callId
        );
    }

    /**
     * Genera la respuesta SIP 486 Busy Here (Rechazada / Ocupada).
     */
    public static String create486Busy(String fromExt, String fromIp, String toExt, String toIp, String callId) {
        return String.format(
            "SIP/2.0 486 Busy Here\r\n" +
            "From: <sip:%s@%s>\r\n" +
            "To: <sip:%s@%s>\r\n" +
            "Call-ID: %s\r\n" +
            "CSeq: 1 INVITE\r\n" +
            "Content-Length: 0\r\n" +
            "\r\n",
            fromExt, fromIp, toExt, toIp, callId
        );
    }

    /**
     * Genera el cuerpo SDP (Session Description Protocol) RFC 4566.
     */
    private static String createSdpBody(String ext, String ip, int rtpPort) {
        long sessionId = System.currentTimeMillis() / 1000;
        return String.format(
            "v=0\r\n" +
            "o=%s %d %d IN IP4 %s\r\n" +
            "s=VoIP Session\r\n" +
            "c=IN IP4 %s\r\n" +
            "t=0 0\r\n" +
            "m=audio %d RTP/AVP 0\r\n" +
            "a=rtpmap:0 L16/8000/1\r\n",
            ext, sessionId, sessionId, ip, ip, rtpPort
        );
    }
}
