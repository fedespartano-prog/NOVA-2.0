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

        String selected = preferredProvider == null
                ? "AUTO" : preferredProvider.trim().toUpperCase();
        List<AIProvider> availableProviders = new ArrayList<>();

        // AUTO è volutamente limitato a Gemini: è l'unico dei tre provider
        // qui integrati che offre un livello API gratuito utilizzabile senza
        // credito prepagato, entro quote/modelli/regione supportati.
        // Non passare automaticamente a DeepSeek o OpenAI: le loro API
        // possono richiedere credito o fatturazione separata.
        if ("AUTO".equals(selected)) {
            if (geminiKey != null && !geminiKey.trim().isEmpty()) {
                availableProviders.add(new GeminiProvider(geminiKey.trim()));
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
