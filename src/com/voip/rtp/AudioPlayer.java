package com.voip.rtp;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Hilo de recepción de paquetes RTP UDP que desempaqueta las tramas PCM de 16 bits
 * y las reproduce en tiempo real en los parlantes/auriculares.
 */
public class AudioPlayer implements Runnable {
    private final DatagramSocket socket;
    private volatile boolean running = false;
    private SourceDataLine sourceLine;

    public AudioPlayer(DatagramSocket socket) {
        this.socket = socket;
    }

    public void start() {
        running = true;
        Thread thread = new Thread(this, "RTP-AudioPlayer-Rx");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (sourceLine != null) {
            sourceLine.stop();
            sourceLine.close();
        }
    }

    @Override
    public void run() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, AudioRecorder.AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Parlantes no soportan el formato PCM 8000Hz 16-bit mono");
                return;
            }

            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(AudioRecorder.AUDIO_FORMAT, AudioRecorder.FRAME_SIZE * 8);
            sourceLine.start();

            byte[] buffer = new byte[RtpHeader.HEADER_SIZE + AudioRecorder.FRAME_SIZE + 64];

            while (running && !socket.isClosed() && sourceLine.isOpen()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                int length = packet.getLength();
                if (length > RtpHeader.HEADER_SIZE) {
                    RtpHeader header = RtpHeader.parse(buffer, length);
                    if (header != null) {
                        int payloadLength = length - RtpHeader.HEADER_SIZE;
                        // Escribir payload PCM directamente a los altavoces
                        sourceLine.write(buffer, RtpHeader.HEADER_SIZE, payloadLength);
                    }
                }
            }
        } catch (SocketException e) {
            if (!running) return; // Cierre normal al detener llamada
        } catch (Exception e) {
            if (running) {
                System.err.println("Error en hilo de recepción/reproducción de audio RTP: " + e.getMessage());
            }
        } finally {
            if (sourceLine != null && sourceLine.isOpen()) {
                sourceLine.close();
            }
        }
    }
}
