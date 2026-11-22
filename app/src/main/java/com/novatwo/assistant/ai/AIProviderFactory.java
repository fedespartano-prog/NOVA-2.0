package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.DeepSeekProvider;
import com.novatwo.assistant.ai.providers.GeminiProvider;
import com.novatwo.assistant.ai.providers.OpenAIProvider;

import java.util.ArrayList;
import java.util.List;

public final class AIProviderFactory {

    private AIProviderFactory() {
    }

    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey,
            String geminiKey) {

        List<AIProvider> availableProviders = new ArrayList<>();

        // AUTO usa il primo provider configurato, nell'ordine DeepSeek,
        // OpenAI, Gemini. Non invia la stessa richiesta a più servizi.
        if (deepSeekKey != null && !deepSeekKey.trim().isEmpty()) {
            availableProviders.add(new DeepSeekProvider(deepSeekKey.trim()));
        }

        if (openAIKey != null && !openAIKey.trim().isEmpty()) {
            availableProviders.add(new OpenAIProvider(openAIKey.trim()));
        }

        if (geminiKey != null && !geminiKey.trim().isEmpty()) {
            availableProviders.add(new GeminiProvider(geminiKey.trim()));
        }

        AIManager manager = new AIManager();
        manager.setProviders(availableProviders, preferredProvider);
        return manager;
    }
}
