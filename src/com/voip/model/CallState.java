package com.voip.model;

/**
 * Representa los posibles estados de la máquina de estados de una llamada VoIP.
 */
public enum CallState {
    /**
     * El sistema está listo para realizar o recibir llamadas.
     */
    DISPONIBLE("Disponible"),

    /**
     * El usuario local inició una llamada y está esperando respuesta (SIP INVITE enviado).
     */
    LLAMANDO("Llamando..."),

    /**
     * Se ha recibido una llamada entrante (SIP INVITE recibido) y está timbrando.
     */
    LLAMADA_ENTRANTE("Llamada entrante"),

    /**
     * La llamada se ha establecido correctamente y el flujo de audio RTP está activo.
     */
    EN_LLAMADA("En llamada"),

    /**
     * La llamada ha concluido o ha sido rechazada/cancelada.
     */
    FINALIZADA("Finalizada");

    private final String description;

    CallState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
