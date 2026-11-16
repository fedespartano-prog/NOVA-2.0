
package com.novatwo.assistant.ai;

import java.util.ArrayList;
import java.util.List;

import com.novatwo.assistant.ai.providers.GeminiProvider;

public final class AIProviderFactory {

    private AIProviderFactory() {
    }

    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey,
            String geminiKey) {

        List<AIProvider> availableProviders = new ArrayList<>();

        /*
         * MODALITÀ GRATUITA SICURA:
         *
         * Per ora viene attivato esclusivamente Gemini API.
         * OpenAI e DeepSeek API non vengono inizializzati
         * e quindi non possono ricevere richieste.
         *
         * Non inserire mai le chiavi API nel repository GitHub.
         */

        if (geminiKey != null && !geminiKey.trim().isEmpty()) {
            availableProviders.add(
                    new GeminiProvider(geminiKey.trim())
            );
        }

        AIManager manager = new AIManager();

        manager.setProviders(
                availableProviders,
                preferredProvider
        );

        return manager;
    }
}
