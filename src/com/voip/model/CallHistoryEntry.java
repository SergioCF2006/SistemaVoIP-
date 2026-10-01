package com.voip.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa una entrada en el historial de llamadas registradas por la aplicación.
 */
public class CallHistoryEntry {
    public enum CallType {
        SALIENTE,
        ENTRANTE,
        PERDIDA
    }

    private final String remoteExtension;
    private final CallType type;
    private final LocalDateTime timestamp;
    private final long durationSeconds;

    public CallHistoryEntry(String remoteExtension, CallType type, long durationSeconds) {
        this.remoteExtension = remoteExtension;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.durationSeconds = durationSeconds;
    }

    public String getRemoteExtension() {
        return remoteExtension;
    }

    public CallType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        String typeIcon = switch (type) {
            case SALIENTE -> "↗ Saliente";
            case ENTRANTE -> "↙ Entrante";
            case PERDIDA -> "✕ Perdida";
        };
        if (type == CallType.PERDIDA) {
            return String.format("[%s] %s %s", getFormattedTime(), typeIcon, remoteExtension);
        }
        return String.format("[%s] %s %s (%ds)", getFormattedTime(), typeIcon, remoteExtension, durationSeconds);
    }
}
