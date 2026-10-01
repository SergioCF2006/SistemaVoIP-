package com.voip.rtp;

import java.nio.ByteBuffer;

/**
 * Cabecera estándar RTP RFC 3550 (12 bytes).
 */
public class RtpHeader {
    public static final int HEADER_SIZE = 12;

    private final int sequenceNumber;
    private final long timestamp;
    private final long ssrc;
    private final int payloadType;

    public RtpHeader(int sequenceNumber, long timestamp, long ssrc, int payloadType) {
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.ssrc = ssrc;
        this.payloadType = payloadType;
    }

    public RtpHeader(int sequenceNumber, long timestamp, long ssrc) {
        this(sequenceNumber, timestamp, ssrc, 0); // Type 0 = Audio PCM
    }

    /**
     * Serializa la cabecera RTP de 12 bytes en un arreglo de bytes.
     */
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        // Byte 0: Version=2 (10), P=0, X=0, CC=0 -> 0x80
        buffer.put((byte) 0x80);
        // Byte 1: M=0, PayloadType=0 -> 0x00
        buffer.put((byte) (payloadType & 0x7F));
        // Bytes 2-3: Sequence Number
        buffer.putShort((short) (sequenceNumber & 0xFFFF));
        // Bytes 4-7: Timestamp
        buffer.putInt((int) (timestamp & 0xFFFFFFFFL));
        // Bytes 8-11: SSRC
        buffer.putInt((int) (ssrc & 0xFFFFFFFFL));

        return buffer.array();
    }

    /**
     * Parse un arreglo de bytes para extraer información de la cabecera RTP.
     */
    public static RtpHeader parse(byte[] data, int length) {
        if (length < HEADER_SIZE) return null;
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
        byte b0 = buffer.get();
        byte b1 = buffer.get();
        int payloadType = b1 & 0x7F;
        int seqNum = buffer.getShort() & 0xFFFF;
        long timestamp = buffer.getInt() & 0xFFFFFFFFL;
        long ssrc = buffer.getInt() & 0xFFFFFFFFL;

        return new RtpHeader(seqNum, timestamp, ssrc, payloadType);
    }

    public int getSequenceNumber() { return sequenceNumber; }
    public long getTimestamp() { return timestamp; }
    public long getSsrc() { return ssrc; }
    public int getPayloadType() { return payloadType; }
}
