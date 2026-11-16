
package com.novatwo.assistant.translation;

import android.content.Context;
import android.content.SharedPreferences;

public final class TranslationPreferences {

    private static final String PREFS_NAME = "nova_translation_settings";
    private static final String KEY_TARGET_LANGUAGE = "target_language";

    private final SharedPreferences preferences;

    public TranslationPreferences(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Il contesto non può essere nullo."
            );
        }

        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setTargetLanguage(String languageCode) {
        if (!TranslationManager.isValidLanguageCode(languageCode)) {
            throw new IllegalArgumentException(
                    "Codice lingua non valido."
            );
        }

        preferences.edit()
                .putString(KEY_TARGET_LANGUAGE, languageCode.trim())
                .apply();
    }

    public String getTargetLanguage() {
        return preferences.getString(KEY_TARGET_LANGUAGE, "it");
    }

    public void resetTargetLanguage() {
        preferences.edit()
                .remove(KEY_TARGET_LANGUAGE)
                .apply();
    }
}
