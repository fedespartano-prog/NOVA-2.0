package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.GeminiProvider;
import com.novatwo.assistant.ai.providers.GroqProvider;

public final class AIProviderFactory {
    private AIProviderFactory() { }

    /** Build NOVA with exactly one explicitly selected provider; no paid fallback. */
    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey,
            String geminiKey,
            String groqKey,
            String grokKey) {

        AIManager manager = new AIManager();
        String selected = preferredProvider == null ? "" : preferredProvider.trim().toUpperCase();
        if ("GROQ".equals(selected)) {
            manager.setProvider(groqKey == null || groqKey.trim().isEmpty()
                    ? null : new GroqProvider(groqKey.trim()));
        } else if ("GEMINI".equals(selected)) {
            manager.setProvider(geminiKey == null || geminiKey.trim().isEmpty()
                    ? null : new GeminiProvider(geminiKey.trim(),
                            "gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite"));
        } else {
            manager.setProvider(null);
        }
        manager.setPreferredProvider(selected);
        return manager;
    }
}
