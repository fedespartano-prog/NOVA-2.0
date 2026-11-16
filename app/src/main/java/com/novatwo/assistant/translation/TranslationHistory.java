
package com.novatwo.assistant.translation;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class TranslationHistory {

    private static final String PREFS_NAME = "nova_translation_history";
    private static final String KEY_HISTORY = "translations";
    private static final int MAX_ITEMS = 50;

    private final SharedPreferences preferences;

    public TranslationHistory(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Il contesto non può essere nullo."
            );
        }

        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void saveTranslation(
            String originalText,
            String translatedText,
            String targetLanguage
    ) {
        if (originalText == null || originalText.trim().isEmpty()
                || translatedText == null || translatedText.trim().isEmpty()
                || !TranslationManager.isValidLanguageCode(targetLanguage)) {
            return;
        }

        try {
            JSONArray history = getHistory();

            JSONObject item = new JSONObject();
            item.put("original", originalText);
            item.put("translated", translatedText);
            item.put("language", targetLanguage);
            item.put("timestamp", System.currentTimeMillis());

            history.put(item);

            JSONArray updated = new JSONArray();
            int start = Math.max(0, history.length() - MAX_ITEMS);

            for (int i = start; i < history.length(); i++) {
                updated.put(history.getJSONObject(i));
            }

            preferences.edit()
                    .putString(KEY_HISTORY, updated.toString())
                    .apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public synchronized JSONArray getHistory() {
        String saved = preferences.getString(KEY_HISTORY, "[]");

        try {
            return new JSONArray(saved);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public synchronized int getHistoryCount() {
        return getHistory().length();
    }

    public synchronized void clearHistory() {
        preferences.edit().remove(KEY_HISTORY).apply();
    }
}
