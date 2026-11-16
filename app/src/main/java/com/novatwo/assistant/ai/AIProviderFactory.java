
package com.novatwo.assistant.ai;

import com.novatwo.assistant.ai.providers.OpenAIProvider;
import com.novatwo.assistant.ai.providers.DeepSeekProvider;

public final class AIProviderFactory {

    private AIProviderFactory() {
        // Impedisce la creazione di istanze.
    }

    public static AIManager createManager(
            String preferredProvider,
            String openAIKey,
            String deepSeekKey
    ) {

        AIProvider openAI = null;
        AIProvider deepSeek = null;

        if (openAIKey != null &&
                !openAIKey.trim().isEmpty()) {
            openAI = new OpenAIProvider(openAIKey);
        }

        if (deepSeekKey != null &&
                !deepSeekKey.trim().isEmpty()) {
            deepSeek = new DeepSeekProvider(deepSeekKey);
        }

        AIProvider selected =
                AIProviderRouter.selectProvider(
                        preferredProvider,
                        openAI,
                        deepSeek
                );

        AIManager manager = new AIManager();
        manager.setProvider(selected);

        return manager;
    }
}
