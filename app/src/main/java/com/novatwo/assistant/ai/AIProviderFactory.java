package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.GeminiProvider;

public final class AIProviderFactory {
    private AIProviderFactory() { }

    /**
     * Build NOVA in Gemini-only test mode.
     * Other API keys are deliberately ignored so no request can be sent
     * to OpenAI, DeepSeek, Groq, or Grok during this test.
     */
    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey,
            String geminiKey,
            String groqKey,
            String grokKey) {

        AIManager manager = new AIManager();
        if (geminiKey == null || geminiKey.trim().isEmpty()) {
            manager.setProvider(null);
            return manager;
        }

        manager.setProvider(new GeminiProvider(
                geminiKey.trim(),
                "gemini-3.1-flash-lite",
                "Gemini 3.1 Flash-Lite · gratuito"
        ));
        manager.setPreferredProvider("AUTO");
        return manager;
    }
}
