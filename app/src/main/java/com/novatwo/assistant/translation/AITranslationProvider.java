
package com.novatwo.assistant.translation;

import com.novatwo.assistant.ai.AIManager;
import com.novatwo.assistant.ai.AIProvider;

public class AITranslationProvider implements TranslationProvider {

    private final AIManager aiManager;

    public AITranslationProvider(AIManager aiManager) {
        if (aiManager == null) {
            throw new IllegalArgumentException(
                    "AIManager non può essere nullo."
            );
        }

        this.aiManager = aiManager;
    }

    @Override
    public void translate(
            String text,
            String targetLanguage,
            TranslationCallback callback
    ) {
        if (callback == null) {
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            callback.onError("Il testo da tradurre è vuoto.");
            return;
        }

        if (!TranslationManager.isValidLanguageCode(targetLanguage)) {
            callback.onError("Lingua di destinazione non valida.");
            return;
        }

        if (!aiManager.isProviderAvailable()) {
            callback.onError("Nessun provider AI disponibile.");
            return;
        }

        String prompt = TranslationManager.prepareTranslationPrompt(
                text,
                targetLanguage
        );

        aiManager.sendMessage(prompt, new AIProvider.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                callback.onSuccess(response);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
