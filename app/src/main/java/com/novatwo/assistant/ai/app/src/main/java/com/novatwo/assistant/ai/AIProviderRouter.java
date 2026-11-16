
package com.novatwo.assistant.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AIProviderRouter {

    private AIProviderRouter() {
    }

    public static AIProvider selectProvider(
            String preferredProvider,
            AIProvider openAI,
            AIProvider deepSeek,
            AIProvider gemini) {

        Map<String, AIProvider> providers =
                new LinkedHashMap<>();

        providers.put("OPENAI", openAI);
        providers.put("DEEPSEEK", deepSeek);
        providers.put("GEMINI", gemini);

        if (preferredProvider != null) {
            AIProvider preferred = providers.get(
                    preferredProvider.trim().toUpperCase()
            );

            if (preferred != null) {
                return preferred;
            }
        }

        for (AIProvider provider : providers.values()) {
            if (provider != null) {
                return provider;
            }
        }

        return null;
    }
}
