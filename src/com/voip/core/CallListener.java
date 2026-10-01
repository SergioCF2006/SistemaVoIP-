package com.voip.core;

import com.voip.model.CallHistoryEntry;
import com.voip.model.CallState;

/**
 * Interfaz de escuchador de eventos del ciclo de vida de las llamadas.
 * Permite a la GUI y a otros componentes reaccionar a cambios de estado.
 */
public interface CallListener {
    /**
     * Notifica un cambio en el estado de la llamada actual.
     * @param newState Nuevo estado (DISPONIBLE, LLAMANDO, LLAMADA_ENTRANTE, EN_LLAMADA, FINALIZADA)
     * @param infoMensaje Descripción textual legible para la interfaz gráfica
     */
    void onStateChanged(CallState newState, String infoMensaje);

    /**
     * Notifica la recepción de una llamada entrante.
     * @param callerExtension Extensión que está llamando
     * @param callerIp IP del equipo llamante
     */
    void onIncomingCall(String callerExtension, String callerIp);

    /**
     * Notifica que se ha añadido un nuevo registro al historial de llamadas.
     * @param entry Registro del historial
     */
    void onHistoryAdded(CallHistoryEntry entry);

    /**
     * Notifica un mensaje o error para ser mostrado en la consola o UI.
     * @param message Mensaje descriptivo
     */
    void onLogMessage(String message);
}
