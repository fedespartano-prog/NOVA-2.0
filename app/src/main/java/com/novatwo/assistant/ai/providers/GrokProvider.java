package com.novatwo.assistant.ai.providers;

import android.os.Handler;
import android.os.Looper;

import com.novatwo.assistant.ai.AIProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GrokProvider implements AIProvider {
    private static final String API_URL = "https://api.x.ai/v1/responses";
    private static final String MODEL = "grok-4.7";
    private final String apiKey;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GrokProvider(String apiKey) { this.apiKey = apiKey; }

    @Override public String getProviderName() { return "Grok (xAI)"; }

    @Override public void generateResponse(String prompt, ResponseCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Chiave API Grok mancante.");
            return;
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError("Messaggio vuoto.");
            return;
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(API_URL).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(120000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject request = new JSONObject();
                request.put("model", MODEL);
                request.put("input", prompt);
                request.put("store", false);
                request.put("max_output_tokens", 2048);
                request.put("temperature", 0.7);

                byte[] payload = request.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                    output.flush();
                }

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                StringBuilder body = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) body.append(line);
                    }
                }

                if (status < 200 || status >= 300) {
                    String detail = body.toString();
                    String error;
                    if (status == 401) error = "Chiave xAI/Grok non valida.";
                    else if (status == 402 || status == 403)
                        error = "xAI ha rifiutato la richiesta per credito, piano o autorizzazioni. L'API Grok è a pagamento: controlla il saldo e i limiti nel tuo account.";
                    else if (status == 429)
                        error = "Limite API Grok raggiunto. Nessun altro provider verrà usato automaticamente.";
                    else error = "Errore Grok HTTP " + status + ": " + detail;
                    postError(callback, error);
                    return;
                }

                JSONObject result = new JSONObject(body.toString());
                JSONArray output = result.optJSONArray("output");
                StringBuilder answer = new StringBuilder();
                if (output != null) {
                    for (int i = 0; i < output.length(); i++) {
                        JSONObject item = output.optJSONObject(i);
                        if (item == null || !"message".equals(item.optString("type"))) continue;
                        JSONArray parts = item.optJSONArray("content");
                        if (parts == null) continue;
                        for (int j = 0; j < parts.length(); j++) {
                            JSONObject part = parts.optJSONObject(j);
                            if (part != null && "output_text".equals(part.optString("type"))) {
                                String value = part.optString("text", "");
                                if (!value.isEmpty()) {
                                    if (answer.length() > 0) answer.append("\n");
                                    answer.append(value);
                                }
                            }
                        }
                    }
                }
                if (answer.length() == 0) {
                    postError(callback, "Grok non ha restituito testo. Stato: " + result.optString("status", "sconosciuto"));
                } else {
                    String finalAnswer = answer.toString().trim();
                    mainHandler.post(() -> callback.onSuccess(finalAnswer));
                }
            } catch (Exception e) {
                String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                postError(callback, "Connessione Grok non riuscita: " + detail);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void postError(ResponseCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
