package com.voip.rtp;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * Hilo de captura de micrófono que empaqueta muestras PCM lineal de 16 bits
 * con cabeceras RTP RFC 3550 y las transmite vía UDP al puerto del endpoint remoto.
 */
public class AudioRecorder implements Runnable {
    private final String remoteIp;
    private final int remoteRtpPort;
    private final DatagramSocket socket;
    private volatile boolean running = false;
    private TargetDataLine targetLine;

    // Configuración de audio: PCM 8000 Hz, 16 bits, Mono
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        8000.0f,  // Frecuencia de muestreo (8000 Hz)
        16,       // Bits por muestra (16 bits)
        1,        // Canales (Mono)
        true,     // Con signo (PCM_SIGNED)
        false     // Little-endian
    );

    // 160 muestras * 2 bytes/muestra = 320 bytes (20 ms de voz)
    public static final int SAMPLES_PER_FRAME = 160;
    public static final int FRAME_SIZE = SAMPLES_PER_FRAME * 2; // 320 bytes

    public AudioRecorder(String remoteIp, int remoteRtpPort, DatagramSocket socket) {
        this.remoteIp = remoteIp;
        this.remoteRtpPort = remoteRtpPort;
        this.socket = socket;
    }

    public void start() {
        running = true;
        Thread thread = new Thread(this, "RTP-AudioRecorder-Tx");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
    }

    @Override
    public void run() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Micrófono no soporta el formato PCM 8000Hz 16-bit mono");
                return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(AUDIO_FORMAT, FRAME_SIZE * 4);
            targetLine.start();

            InetAddress destinationAddress = InetAddress.getByName(remoteIp);
            byte[] audioBuffer = new byte[FRAME_SIZE];
            byte[] packetBuffer = new byte[RtpHeader.HEADER_SIZE + FRAME_SIZE];

            int seqNum = 0;
            long timestamp = 0;
            long ssrc = new Random().nextInt(1000000);

            while (running && targetLine.isOpen()) {
                int bytesRead = targetLine.read(audioBuffer, 0, FRAME_SIZE);
                if (bytesRead > 0) {
                    RtpHeader header = new RtpHeader(seqNum, timestamp, ssrc);
                    byte[] headerBytes = header.toByteArray();

                    // Copiar cabecera RTP (12 bytes)
                    System.arraycopy(headerBytes, 0, packetBuffer, 0, RtpHeader.HEADER_SIZE);
                    // Copiar payload de audio PCM (bytesRead bytes)
                    System.arraycopy(audioBuffer, 0, packetBuffer, RtpHeader.HEADER_SIZE, bytesRead);

                    DatagramPacket packet = new DatagramPacket(
                        packetBuffer, 
                        RtpHeader.HEADER_SIZE + bytesRead, 
                        destinationAddress, 
                        remoteRtpPort
                    );

                    socket.send(packet);

                    seqNum = (seqNum + 1) & 0xFFFF;
                    timestamp += SAMPLES_PER_FRAME;
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("Error en hilo de transmisión de audio RTP: " + e.getMessage());
            }
        } finally {
            if (targetLine != null && targetLine.isOpen()) {
                targetLine.close();
            }
        }
    }
}
