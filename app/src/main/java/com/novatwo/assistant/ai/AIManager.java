
package com.novatwo.assistant.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AIManager {

    private final Map<String, AIProvider> providers =
            new LinkedHashMap<>();

    private String preferredProvider = "AUTO";

    public AIManager() {
    }

    public void setProvider(AIProvider provider) {
        providers.clear();

        if (provider != null && isFreeProvider(provider)) {
            providers.put(
                    provider.getProviderName().toUpperCase(),
                    provider
            );
        }
    }

    public void setProviders(
            List<AIProvider> availableProviders,
            String preference
    ) {
        providers.clear();

        if (availableProviders != null) {
            for (AIProvider provider : availableProviders) {
                if (provider != null && isFreeProvider(provider)) {
                    providers.put(
                            provider.getProviderName().toUpperCase(),
                            provider
                    );
                }
            }
        }

        setPreferredProvider(preference);
    }

    public void setPreferredProvider(String preference) {
        if (preference == null || preference.trim().isEmpty()) {
            preferredProvider = "AUTO";
        } else {
            preferredProvider = preference.trim().toUpperCase();
        }
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

    private boolean isFreeProvider(AIProvider provider) {
        String name = provider.getProviderName();

        if (name == null) {
            return false;
        }

        // Per ora è ammesso soltanto Gemini API
        // nella modalità gratuita verificata.
        return name.toUpperCase().contains("GEMINI");
    }

    public void sendMessage(
            String prompt,
            AIProvider.ResponseCallback callback
    ) {
        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError("Il messaggio non può essere vuoto.");
            return;
        }

        List<AIProvider> candidates = new ArrayList<>();

        if ("AUTO".equals(preferredProvider)) {
            candidates.addAll(providers.values());
        } else {
            AIProvider selected = providers.get(preferredProvider);

            if (selected != null) {
                candidates.add(selected);
            }
        }

        if (candidates.isEmpty()) {
            callback.onError(
                    "Nessun provider AI gratuito disponibile o configurato."
            );
            return;
        }

        tryProvider(prompt, callback, candidates, 0);
    }

    private void tryProvider(
            String prompt,
            AIProvider.ResponseCallback callback,
            List<AIProvider> candidates,
            int index
    ) {
        if (index >= candidates.size()) {
            callback.onError(
                    "Nessuna AI gratuita disponibile al momento."
            );
            return;
        }

        AIProvider provider = candidates.get(index);

        provider.generateResponse(
                prompt,
                new AIProvider.ResponseCallback() {

                    @Override
                    public void onSuccess(String response) {
                        callback.onSuccess(response);
                    }

                    @Override
                    public void onError(String error) {
                        tryProvider(
                                prompt,
                                callback,
                                candidates,
                                index + 1
                        );
                    }
                }
        );
    }
}
