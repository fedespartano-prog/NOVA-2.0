package com.novatwo.assistant.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AIManager {

    private final Map<String, AIProvider> providers = new LinkedHashMap<>();
    private String preferredProvider = "AUTO";

    public AIManager() {
    }

    public void setProvider(AIProvider provider) {
        providers.clear();
        if (provider != null) {
            providers.put(provider.getProviderName().toUpperCase(), provider);
        }
    }

    public void setProviders(List<AIProvider> availableProviders, String preference) {
        providers.clear();

        if (availableProviders != null) {
            for (AIProvider provider : availableProviders) {
                if (provider != null && provider.getProviderName() != null) {
                    providers.put(provider.getProviderName().toUpperCase(), provider);
                }
            }
        }

        setPreferredProvider(preference);
    }

    public void setPreferredProvider(String preference) {
        preferredProvider = preference == null || preference.trim().isEmpty()
                ? "AUTO" : preference.trim().toUpperCase();
    }

    public String getPreferredProvider() {
        return preferredProvider;
    }

    public AIProvider getProvider() {
        if (!"AUTO".equals(preferredProvider)) {
            return providers.get(preferredProvider);
        }

        for (AIProvider provider : providers.values()) {
            return provider;
        }
        return null;
    }

    public boolean isProviderAvailable() {
        return getProvider() != null;
    }

    public void sendMessage(String prompt, AIProvider.ResponseCallback callback) {
        if (callback == null) {
            return;
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError("Il messaggio non può essere vuoto.");
            return;
        }

        AIProvider provider = getProvider();
        if (provider == null) {
            callback.onError("Nessun provider configurato. Inserisci almeno una chiave API.");
            return;
        }

        // Una richiesta viene inviata a un solo provider: evita chiamate
        // duplicate e possibili addebiti multipli in caso di errore.
        try {
            provider.generateResponse(prompt.trim(), callback);
        } catch (Exception e) {
            callback.onError("Errore " + provider.getProviderName() + ": " + e.getMessage());
        }
    }
}
