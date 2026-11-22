package com.novatwo.assistant.actions;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class LocalNoteStore {
    private static final String PREFS = "nova_local_notes";
    private static final String KEY = "notes";
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
            preferences.edit().putString(KEY, notes.toString()).apply();
        } catch (JSONException ignored) { }
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
        try { return new JSONArray(preferences.getString(KEY, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
}
