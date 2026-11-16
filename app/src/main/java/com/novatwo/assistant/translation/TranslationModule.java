
package com.novatwo.assistant.translation;

import android.content.Context;

import com.novatwo.assistant.ai.AIManager;

public final class TranslationModule {

    private final TranslationService translationService;

    public TranslationModule(Context context, AIManager aiManager) {

        if (context == null || aiManager == null) {
            throw new IllegalArgumentException(
                    "Context e AIManager sono obbligatori."
            );
        }

        TranslationProvider provider =
                new AITranslationProvider(aiManager);

        TranslationController controller =
                new TranslationController(provider);

        TranslationHistory history =
                new TranslationHistory(context);

        TranslationSettings settings =
                new TranslationSettings(context);

        translationService = new TranslationService(
                controller,
                history,
                settings
        );
    }

    public TranslationService getTranslationService() {
        return translationService;
    }
}
