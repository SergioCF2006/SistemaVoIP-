package com.voip.model;

/**
 * Contiene los datos de la sesión de llamada activa entre el endpoint local y el remoto.
 */
public class CallSession {
    private final String localExtension;
    private final String remoteExtension;
    private final String remoteIp;
    private final int remoteSipPort;
    private final int remoteRtpPort;
    private final int localRtpPort;
    private final boolean isOutgoing;
    private final long startTimeMillis;

    public CallSession(String localExtension, String remoteExtension, String remoteIp,
                       int remoteSipPort, int remoteRtpPort, int localRtpPort, boolean isOutgoing) {
        this.localExtension = localExtension;
        this.remoteExtension = remoteExtension;
        this.remoteIp = remoteIp;
        this.remoteSipPort = remoteSipPort;
        this.remoteRtpPort = remoteRtpPort;
        this.localRtpPort = localRtpPort;
        this.isOutgoing = isOutgoing;
        this.startTimeMillis = System.currentTimeMillis();
    }

    public String getLocalExtension() {
        return localExtension;
    }

    public String getRemoteExtension() {
        return remoteExtension;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public int getRemoteSipPort() {
        return remoteSipPort;
    }

    public int getRemoteRtpPort() {
        return remoteRtpPort;
    }

    public int getLocalRtpPort() {
        return localRtpPort;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getDurationSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }
}
