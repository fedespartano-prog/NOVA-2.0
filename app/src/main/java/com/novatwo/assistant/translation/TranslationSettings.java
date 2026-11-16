
package com.novatwo.assistant.translation;

import android.content.Context;
import android.content.SharedPreferences;

public final class TranslationSettings {

    private static final String PREFS_NAME = "nova_translation_options";

    private static final String KEY_AUTO_DETECT = "auto_detect";
    private static final String KEY_SHOW_ORIGINAL = "show_original";
    private static final String KEY_SAVE_HISTORY = "save_history";

    private final SharedPreferences preferences;

    public TranslationSettings(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Il contesto non può essere nullo."
            );
        }

        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setAutoDetectEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_AUTO_DETECT, enabled)
                .apply();
    }

    public boolean isAutoDetectEnabled() {
        return preferences.getBoolean(KEY_AUTO_DETECT, true);
    }

    public void setShowOriginalEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_SHOW_ORIGINAL, enabled)
                .apply();
    }

    public boolean isShowOriginalEnabled() {
        return preferences.getBoolean(KEY_SHOW_ORIGINAL, true);
    }

    public void setSaveHistoryEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_SAVE_HISTORY, enabled)
                .apply();
    }

    public boolean isSaveHistoryEnabled() {
        return preferences.getBoolean(KEY_SAVE_HISTORY, true);
    }

    public void resetSettings() {
        preferences.edit().clear().apply();
    }
}
