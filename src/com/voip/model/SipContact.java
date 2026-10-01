package com.voip.model;

/**
 * Representa la información de contacto de un endpoint VoIP (Extensión, IP y Puertos).
 */
public class SipContact {
    private final String extension;
    private final String ipAddress;
    private final int sipPort;
    private final int rtpPort;

    public SipContact(String extension, String ipAddress, int sipPort, int rtpPort) {
        this.extension = extension;
        this.ipAddress = ipAddress;
        this.sipPort = sipPort;
        this.rtpPort = rtpPort;
    }

    public SipContact(String extension, String ipAddress) {
        this(extension, ipAddress, 5060, 7000);
    }

    public String getExtension() {
        return extension;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getSipPort() {
        return sipPort;
    }

    public int getRtpPort() {
        return rtpPort;
    }

    @Override
    public String toString() {
        return String.format("Ext: %s (%s:%d | RTP:%d)", extension, ipAddress, sipPort, rtpPort);
    }
}
