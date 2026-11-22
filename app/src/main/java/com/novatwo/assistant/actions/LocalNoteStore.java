package com.novatwo.assistant.actions;

import android.content.Context;
import android.content.SharedPreferences;

import com.novatwo.assistant.memory.LocalCipher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class LocalNoteStore {
    private static final String PREFS = "nova_local_notes";
    private static final String KEY = "notes";
    private static final String ENCRYPTED_PREFIX = "ENC:";
    private final SharedPreferences preferences;

    public LocalNoteStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void add(String text) {
        if (text == null || text.trim().isEmpty()) return;
        JSONArray notes = read();
        JSONObject note = new JSONObject();
        try {
            note.put("text", text.trim());
            note.put("created_at", System.currentTimeMillis());
            notes.put(note);
            while (notes.length() > 300) {
                JSONArray trimmed = new JSONArray();
                for (int i = 1; i < notes.length(); i++) trimmed.put(notes.opt(i));
                notes = trimmed;
            }
            preferences.edit().putString(KEY,
                    ENCRYPTED_PREFIX + LocalCipher.encrypt(notes.toString())).apply();
        } catch (Exception ignored) {
            // Le note non vengono salvate in chiaro se la cifratura non è disponibile.
        }
    }

    public synchronized String latest(int limit) {
        JSONArray notes = read();
        if (notes.length() == 0) return "Non hai ancora salvato note.";
        StringBuilder out = new StringBuilder("Le tue ultime note:\n");
        int start = Math.max(0, notes.length() - Math.max(1, limit));
        for (int i = notes.length() - 1; i >= start; i--) {
            JSONObject note = notes.optJSONObject(i);
            if (note != null) out.append("• ").append(note.optString("text")).append('\n');
        }
        return out.toString().trim();
    }

    public synchronized void clear() {
        preferences.edit().remove(KEY).apply();
    }

    private JSONArray read() {
        String saved = preferences.getString(KEY, "[]");
        try {
            if (saved.startsWith(ENCRYPTED_PREFIX)) {
                return new JSONArray(LocalCipher.decrypt(saved.substring(ENCRYPTED_PREFIX.length())));
            }
            // Migrazione trasparente delle note precedenti.
            JSONArray legacy = new JSONArray(saved);
            if (legacy.length() > 0) {
                try {
                    preferences.edit().putString(KEY,
                            ENCRYPTED_PREFIX + LocalCipher.encrypt(legacy.toString())).apply();
                } catch (Exception ignored) { }
            }
            return legacy;
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
