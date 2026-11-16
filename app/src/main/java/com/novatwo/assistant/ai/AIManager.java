
package com.novatwo.assistant.ai;

public class AIManager {

    private AIProvider activeProvider;

    public AIManager() {
        activeProvider = null;
    }

    public void setProvider(AIProvider provider) {
        this.activeProvider = provider;
    }

    public AIProvider getProvider() {
        return activeProvider;
    }

    public boolean isProviderAvailable() {
        return activeProvider != null;
    }

    public void sendMessage(
            String prompt,
            AIProvider.ResponseCallback callback
    ) {
        if (activeProvider == null) {
            callback.onError(
                    "Nessun provider AI configurato."
            );
            return;
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError(
                    "Il messaggio non può essere vuoto."
            );
            return;
        }

        activeProvider.generateResponse(
                prompt,
                callback
        );
    }
}
