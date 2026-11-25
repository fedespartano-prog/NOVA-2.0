
package com.novatwo.assistant.translation;

public interface TranslationProvider {

    void translate(
            String text,
            String targetLanguage,
            TranslationCallback callback
    );

    interface TranslationCallback {

        void onSuccess(String translatedText);

        void onError(String error);
    }
}
