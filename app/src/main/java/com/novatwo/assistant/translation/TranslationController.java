
package com.novatwo.assistant.translation;

public final class TranslationController {

    private final TranslationProvider translationProvider;

    public TranslationController(TranslationProvider translationProvider) {
        if (translationProvider == null) {
            throw new IllegalArgumentException(
                    "TranslationProvider non può essere nullo."
            );
        }

        this.translationProvider = translationProvider;
    }

    public void translateText(
            String text,
            String targetLanguage,
            TranslationProvider.TranslationCallback callback
    ) {
        if (callback == null) {
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            callback.onError("Inserisci il testo da tradurre.");
            return;
        }

        if (!TranslationManager.isValidLanguageCode(targetLanguage)) {
            callback.onError("Seleziona una lingua valida.");
            return;
        }

        translationProvider.translate(
                text,
                targetLanguage,
                callback
        );
    }
}
