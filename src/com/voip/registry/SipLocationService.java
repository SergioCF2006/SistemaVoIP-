package com.voip.registry;

import com.voip.model.SipContact;

/**
 * Interfaz que define la resolución de extensiones VoIP a ubicaciones de red (IP y puerto).
 * Permite desacoplar el origen de los contactos (Configuración IP Directa hoy, Servidor Registrar SIP mañana).
 */
public interface SipLocationService {
    /**
     * Busca la información de contacto de una extensión.
     * @param extension Número de extensión a resolver (ej. "102")
     * @return Objeto SipContact con la IP y puertos, o null si no se encuentra registrado.
     */
    SipContact resolveExtension(String extension);

    /**
     * Permite registrar o actualizar la ubicación de una extensión.
     * @param contact Información de contacto de la extensión.
     */
    void registerExtension(SipContact contact);
}
