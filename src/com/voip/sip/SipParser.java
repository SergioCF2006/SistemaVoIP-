package com.voip.sip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analizador sintáctico para parsear mensajes SIP RFC 3261 y cuerpos SDP.
 */
public class SipParser {

    public static class ParsedMessage {
        private final String rawContent;
        private final String methodOrStatus;
        private final String fromExtension;
        private final String fromIp;
        private final String toExtension;
        private final String toIp;
        private final String callId;
        private final int remoteRtpPort;
        private final boolean isRequest;

        public ParsedMessage(String rawContent, String methodOrStatus, String fromExtension, 
                             String fromIp, String toExtension, String toIp, 
                             String callId, int remoteRtpPort, boolean isRequest) {
            this.rawContent = rawContent;
            this.methodOrStatus = methodOrStatus;
            this.fromExtension = fromExtension;
            this.fromIp = fromIp;
            this.toExtension = toExtension;
            this.toIp = toIp;
            this.callId = callId;
            this.remoteRtpPort = remoteRtpPort;
            this.isRequest = isRequest;
        }

        public String getRawContent() { return rawContent; }
        public String getMethodOrStatus() { return methodOrStatus; }
        public String getFromExtension() { return fromExtension; }
        public String getFromIp() { return fromIp; }
        public String getToExtension() { return toExtension; }
        public String getToIp() { return toIp; }
        public String getCallId() { return callId; }
        public int getRemoteRtpPort() { return remoteRtpPort; }
        public boolean isRequest() { return isRequest; }

        @Override
        public String toString() {
            return String.format("[%s] From: %s@%s -> To: %s@%s (CallID: %s, RTP Port: %d)",
                methodOrStatus, fromExtension, fromIp, toExtension, toIp, callId, remoteRtpPort);
        }
    }

    private static final Pattern FIRST_LINE_PATTERN = Pattern.compile("^([A-Z]+|SIP/2\\.0 \\d{3} [A-Za-z ]+)");
    private static final Pattern FROM_PATTERN = Pattern.compile("From:\\s*<sip:([^@]+)@([^>:;]+)");
    private static final Pattern TO_PATTERN = Pattern.compile("To:\\s*<sip:([^@]+)@([^>:;]+)");
    private static final Pattern CALL_ID_PATTERN = Pattern.compile("Call-ID:\\s*(.+)");
    private static final Pattern SDP_PORT_PATTERN = Pattern.compile("m=audio\\s+(\\d+)\\s+RTP/AVP");

    public static ParsedMessage parse(String sipText) {
        if (sipText == null || sipText.isBlank()) {
            return null;
        }

        String[] lines = sipText.split("\r?\n");
        if (lines.length == 0) return null;

        String firstLine = lines[0].trim();
        boolean isRequest = firstLine.startsWith("INVITE") || firstLine.startsWith("ACK") || firstLine.startsWith("BYE");
        
        String methodOrStatus = firstLine;
        if (firstLine.startsWith("SIP/2.0")) {
            methodOrStatus = firstLine.substring("SIP/2.0 ".length()).trim();
        } else {
            String[] parts = firstLine.split(" ");
            if (parts.length > 0) {
                methodOrStatus = parts[0];
            }
        }

        String fromExt = extractPattern(FROM_PATTERN, sipText, 1, "desconocido");
        String fromIp = extractPattern(FROM_PATTERN, sipText, 2, "127.0.0.1");
        String toExt = extractPattern(TO_PATTERN, sipText, 1, "desconocido");
        String toIp = extractPattern(TO_PATTERN, sipText, 2, "127.0.0.1");
        String callId = extractPattern(CALL_ID_PATTERN, sipText, 1, "").trim();

        int rtpPort = 7000;
        String portStr = extractPattern(SDP_PORT_PATTERN, sipText, 1, null);
        if (portStr != null) {
            try {
                rtpPort = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        return new ParsedMessage(sipText, methodOrStatus, fromExt, fromIp, toExt, toIp, callId, rtpPort, isRequest);
    }

    private static String extractPattern(Pattern pattern, String text, int groupIndex, String defaultValue) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(groupIndex);
        }
        return defaultValue;
    }
}
