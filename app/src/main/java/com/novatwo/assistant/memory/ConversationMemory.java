package com.novatwo.assistant.memory;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ConversationMemory {
    private static final String PREFS_NAME = "nova_local_memory";
    private static final String KEY_MESSAGES = "conversation_history";
    private static final int MAX_MESSAGES = 100;
    private static final String ENCRYPTED_PREFIX = "ENC:";
    private final SharedPreferences preferences;

    public ConversationMemory(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void saveMessage(String role, String content) {
        if (role == null || content == null || content.trim().isEmpty()) return;
        try {
            JSONArray messages = getMessages();
            JSONObject message = new JSONObject();
            message.put("role", role);
            message.put("content", content);
            message.put("timestamp", System.currentTimeMillis());
            messages.put(message);
            while (messages.length() > MAX_MESSAGES) {
                JSONArray updated = new JSONArray();
                for (int i = 1; i < messages.length(); i++) updated.put(messages.getJSONObject(i));
                messages = updated;
            }
            String encrypted = ENCRYPTED_PREFIX + LocalCipher.encrypt(messages.toString());
            preferences.edit().putString(KEY_MESSAGES, encrypted).apply();
        } catch (Exception ignored) {
            // In caso di errore crittografico non salvare la cronologia in chiaro.
        }
    }

    public synchronized JSONArray getMessages() {
        String saved = preferences.getString(KEY_MESSAGES, "[]");
        try {
            if (saved.startsWith(ENCRYPTED_PREFIX)) {
                return new JSONArray(LocalCipher.decrypt(saved.substring(ENCRYPTED_PREFIX.length())));
            }
            // Migrazione trasparente delle vecchie conversazioni salvate in chiaro.
            JSONArray legacy = new JSONArray(saved);
            if (legacy.length() > 0) {
                try {
                    preferences.edit().putString(KEY_MESSAGES,
                            ENCRYPTED_PREFIX + LocalCipher.encrypt(legacy.toString())).apply();
                } catch (Exception ignored) { }
            }
            return legacy;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public synchronized int getMessageCount() {
        return getMessages().length();
    }

    public synchronized void clearMemory() {
        preferences.edit().remove(KEY_MESSAGES).apply();
    }

    public synchronized boolean hasMemory() {
        return getMessageCount() > 0;
    }
}
