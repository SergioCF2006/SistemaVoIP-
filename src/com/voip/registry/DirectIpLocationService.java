package com.voip.registry;

import com.voip.model.SipContact;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de resolución de extensiones basada en mapa local (IP directa).
 * Utilizada para la Fase 1 (PC a PC en red local).
 */
public class DirectIpLocationService implements SipLocationService {
    private final Map<String, SipContact> registry = new ConcurrentHashMap<>();

    @Override
    public SipContact resolveExtension(String extension) {
        return registry.get(extension);
    }

    @Override
    public void registerExtension(SipContact contact) {
        if (contact != null && contact.getExtension() != null) {
            registry.put(contact.getExtension(), contact);
        }
    }
}
