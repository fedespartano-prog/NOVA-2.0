package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.DeepSeekProvider;
import com.novatwo.assistant.ai.providers.GeminiProvider;
import com.novatwo.assistant.ai.providers.GroqProvider;
import com.novatwo.assistant.ai.providers.GrokProvider;
import com.novatwo.assistant.ai.providers.OpenAIProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AIProviderFactory {
    private AIProviderFactory() { }

    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey,
            String geminiKey,
            String groqKey,
            String grokKey) {

        String selected = preferredProvider == null
                ? "AUTO" : preferredProvider.trim().toUpperCase(Locale.ROOT);
        List<AIProvider> availableProviders = new ArrayList<>();

        if ("AUTO".equals(selected)) {
            // Ordine richiesto: DeepSeek prima, poi Groq, poi Gemini.
            // AUTO sceglie un solo provider configurato e non effettua
            // fallback automatici che potrebbero inviare dati o creare costi.
            if (hasKey(deepSeekKey)) availableProviders.add(new DeepSeekProvider(deepSeekKey.trim()));
            else if (hasKey(groqKey)) availableProviders.add(new GroqProvider(groqKey.trim()));
            else if (hasKey(geminiKey)) availableProviders.add(new GeminiProvider(geminiKey.trim()));
        } else if ("DEEPSEEK".equals(selected) && hasKey(deepSeekKey)) {
            availableProviders.add(new DeepSeekProvider(deepSeekKey.trim()));
        } else if ("GROQ".equals(selected) && hasKey(groqKey)) {
            availableProviders.add(new GroqProvider(groqKey.trim()));
        } else if ("GEMINI".equals(selected) && hasKey(geminiKey)) {
            availableProviders.add(new GeminiProvider(geminiKey.trim()));
        } else if ("OPENAI".equals(selected) && hasKey(openAIKey)) {
            availableProviders.add(new OpenAIProvider(openAIKey.trim()));
        } else if ("GROK".equals(selected) && hasKey(grokKey)) {
            availableProviders.add(new GrokProvider(grokKey.trim()));
        }

        AIManager manager = new AIManager();
        manager.setProviders(availableProviders, "AUTO");
        return manager;
    }

    private static boolean hasKey(String key) {
        return key != null && !key.trim().isEmpty();
    }
}
