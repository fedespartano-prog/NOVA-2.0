package com.novatwo.assistant.ai;

import java.util.ArrayList;
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
        if (callback == null) return;
        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError("Il messaggio non può essere vuoto.");
            return;
        }

        List<AIProvider> ordered = new ArrayList<>();
        if (!"AUTO".equals(preferredProvider)) {
            AIProvider selected = providers.get(preferredProvider);
            if (selected != null) ordered.add(selected);
            if (selected != null && selected.getProviderName().startsWith("Gemini")) {
                for (AIProvider candidate : providers.values()) {
                    if (candidate != selected
                            && candidate.getProviderName().startsWith("Gemini")) {
                        ordered.add(candidate);
                    }
                }
            }
        } else {
            ordered.addAll(providers.values());
        }

        if (ordered.isEmpty()) {
            callback.onError("Nessun provider configurato. Inserisci almeno una chiave API.");
            return;
        }
        sendWithGeminiFallback(ordered, 0, prompt.trim(), callback);
    }

    private void sendWithGeminiFallback(
            List<AIProvider> ordered,
            int index,
            String prompt,
            AIProvider.ResponseCallback callback) {
        AIProvider provider = ordered.get(index);
        try {
            provider.generateResponse(prompt, new AIProvider.ResponseCallback() {
                @Override public void onSuccess(String response) {
                    callback.onSuccess(response);
                }

                @Override public void onError(String error) {
                    boolean isGemini = provider.getProviderName().startsWith("Gemini");
                    boolean hasNextGemini = index + 1 < ordered.size()
                            && ordered.get(index + 1).getProviderName().startsWith("Gemini");
                    boolean retryableModelError = error != null
                            && (error.contains("HTTP 429")
                            || error.contains("HTTP 404")
                            || error.contains("HTTP 503"));
                    if (isGemini && hasNextGemini && retryableModelError) {
                        sendWithGeminiFallback(ordered, index + 1, prompt, callback);
                    } else {
                        callback.onError(error);
                    }
                }
            });
        } catch (Exception e) {
            String error = "Errore " + provider.getProviderName() + ": " + e.getMessage();
            boolean isGemini = provider.getProviderName().startsWith("Gemini");
            boolean hasNextGemini = index + 1 < ordered.size()
                    && ordered.get(index + 1).getProviderName().startsWith("Gemini");
            if (isGemini && hasNextGemini) {
                sendWithGeminiFallback(ordered, index + 1, prompt, callback);
            } else {
                callback.onError(error);
            }
        }
    }

}
