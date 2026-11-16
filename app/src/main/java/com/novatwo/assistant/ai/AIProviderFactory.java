
package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.OpenAIProvider;
import com.novatwo.assistant.ai.providers.DeepSeekProvider;
import com.novatwo.assistant.ai.providers.GeminiProvider;

public final class AIProviderFactory {

    private AIProviderFactory() {
    }

    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey,
            String geminiKey) {

        AIProvider openAI = null;
        AIProvider deepSeek = null;
        AIProvider gemini = null;

        if (openAIKey != null && !openAIKey.trim().isEmpty()) {
            openAI = new OpenAIProvider(openAIKey);
        }

        if (deepSeekKey != null && !deepSeekKey.trim().isEmpty()) {
            deepSeek = new DeepSeekProvider(deepSeekKey);
        }

        if (geminiKey != null && !geminiKey.trim().isEmpty()) {
            gemini = new GeminiProvider(geminiKey);
        }

        AIProvider selected = AIProviderRouter.selectProvider(
                preferredProvider, openAI, deepSeek, gemini
        );

        AIManager manager = new AIManager();
        manager.setProvider(selected);
        return manager;
    }
}
