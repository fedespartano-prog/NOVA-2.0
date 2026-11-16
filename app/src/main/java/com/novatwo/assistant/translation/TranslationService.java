
package com.novatwo.assistant.translation;

public final class TranslationService {

    public interface ResultCallback {
        void onSuccess(TranslationResult result);
        void onError(String error);
    }

    private final TranslationController controller;
    private final TranslationHistory history;
    private final TranslationSettings settings;

    public TranslationService(
            TranslationController controller,
            TranslationHistory history,
            TranslationSettings settings
    ) {
        if (controller == null || history == null || settings == null) {
            throw new IllegalArgumentException(
                    "I componenti della traduzione non possono essere nulli."
            );
        }

        this.controller = controller;
        this.history = history;
        this.settings = settings;
    }

    public void translate(
            String originalText,
            String targetLanguage,
            ResultCallback callback
    ) {
        if (callback == null) {
            return;
        }

        if (originalText == null || originalText.trim().isEmpty()) {
            callback.onError("Inserisci un testo da tradurre.");
            return;
        }

        controller.translateText(
                originalText,
                targetLanguage,
                new TranslationProvider.TranslationCallback() {

                    @Override
                    public void onSuccess(String translatedText) {
                        TranslationResult result = new TranslationResult(
                                originalText,
                                translatedText,
                                "auto",
                                targetLanguage
                        );

                        if (settings.isSaveHistoryEnabled()) {
                            history.saveTranslation(
                                    originalText,
                                    translatedText,
                                    targetLanguage
                            );
                        }

                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                }
        );
    }

    public void clearHistory() {
        history.clearHistory();
    }

    public int getHistoryCount() {
        return history.getHistoryCount();
    }
}
