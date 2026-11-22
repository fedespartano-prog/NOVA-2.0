package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.DeepSeekProvider;
import com.novatwo.assistant.ai.providers.GeminiProvider;
import com.novatwo.assistant.ai.providers.GroqProvider;
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
            String geminiKey,
            String groqKey) {

        String selected = preferredProvider == null
                ? "AUTO" : preferredProvider.trim().toUpperCase();
        List<AIProvider> availableProviders = new ArrayList<>();

        // AUTO usa esclusivamente Groq. Non effettua fallback automatici:
        // se la quota Free è terminata, la richiesta fallisce senza costi
        // e senza inviare dati a un altro provider.
        if ("AUTO".equals(selected) || "GROQ".equals(selected)) {
            if (groqKey != null && !groqKey.trim().isEmpty()) {
                availableProviders.add(new GroqProvider(groqKey.trim()));
            }
        } else if ("GEMINI".equals(selected)) {
            if (geminiKey != null && !geminiKey.trim().isEmpty()) {
                availableProviders.add(new GeminiProvider(geminiKey.trim()));
            }
        } else if ("DEEPSEEK".equals(selected)) {
            if (deepSeekKey != null && !deepSeekKey.trim().isEmpty()) {
                availableProviders.add(new DeepSeekProvider(deepSeekKey.trim()));
            }
        } else if ("OPENAI".equals(selected)) {
            if (openAIKey != null && !openAIKey.trim().isEmpty()) {
                availableProviders.add(new OpenAIProvider(openAIKey.trim()));
            }
        }

        AIManager manager = new AIManager();
        manager.setProviders(availableProviders, "AUTO");
        return manager;
    }
}
