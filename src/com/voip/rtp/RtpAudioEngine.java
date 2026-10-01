package com.voip.rtp;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Motor de audio RTP que gestiona el socket UDP de audio y los hilos de transmisión y recepción.
 */
public class RtpAudioEngine {
    private final int localRtpPort;
    private DatagramSocket rtpSocket;
    private AudioRecorder recorder;
    private AudioPlayer player;
    private volatile boolean isStreaming = false;

    public RtpAudioEngine(int localRtpPort) {
        this.localRtpPort = localRtpPort;
    }

    public synchronized void startStreaming(String remoteIp, int remoteRtpPort) throws SocketException {
        if (isStreaming) return;

        rtpSocket = new DatagramSocket(localRtpPort);
        
        player = new AudioPlayer(rtpSocket);
        recorder = new AudioRecorder(remoteIp, remoteRtpPort, rtpSocket);

        player.start();
        recorder.start();
        
        isStreaming = true;
    }

    public synchronized void stopStreaming() {
        if (!isStreaming) return;

        isStreaming = false;

        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }

        if (player != null) {
            player.stop();
            player = null;
        }

        if (rtpSocket != null && !rtpSocket.isClosed()) {
            rtpSocket.close();
            rtpSocket = null;
        }
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public int getLocalRtpPort() {
        return localRtpPort;
    }
}
