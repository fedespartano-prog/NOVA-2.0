
package com.novatwo.assistant.ai;

public final class AIProviderRouter {

    private AIProviderRouter() {
        // Classe di utilità.
    }

    public static AIProvider selectProvider(
            String preferredProvider,
            AIProvider openAI,
            AIProvider deepSeek
    ) {

        if (preferredProvider == null) {
            return firstAvailable(openAI, deepSeek);
        }

        String preference =
                preferredProvider.trim().toUpperCase();

        if (preference.equals("OPENAI")) {
            return firstAvailable(openAI, deepSeek);
        }

        if (preference.equals("DEEPSEEK")) {
            return firstAvailable(deepSeek, openAI);
        }

        return firstAvailable(openAI, deepSeek);
    }

    private static AIProvider firstAvailable(
            AIProvider first,
            AIProvider second
    ) {

        if (first != null) {
            return first;
        }

        return second;
    }
}
