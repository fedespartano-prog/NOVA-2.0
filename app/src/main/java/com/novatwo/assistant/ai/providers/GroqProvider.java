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

public final class GroqProvider implements AIProvider {

    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-120b";

    private final String apiKey;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GroqProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "Groq";
    }

    @Override
    public void generateResponse(String prompt, ResponseCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Chiave API Groq mancante.");
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
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(45000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);

                JSONArray messages = new JSONArray();
                messages.put(userMessage);

                JSONObject request = new JSONObject();
                request.put("model", MODEL);
                request.put("messages", messages);
                request.put("max_tokens", 2048);
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

                if (status >= 200 && status < 300) {
                    JSONObject result = new JSONObject(body.toString());
                    String answer = result.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .optString("content", "");
                    if (answer.trim().isEmpty()) {
                        postError(callback, "Groq ha restituito una risposta vuota.");
                    } else {
                        mainHandler.post(() -> callback.onSuccess(answer.trim()));
                    }
                } else {
                    String message;
                    if (status == 401) {
                        message = "Chiave Groq non valida o non autorizzata. Controllala nella Console Groq.";
                    } else if (status == 429) {
                        message = "Limite gratuito Groq raggiunto. Attendi il ripristino della quota; NOVA non passerà a un piano a pagamento.";
                    } else if (status == 402 || status == 403) {
                        message = "Groq ha rifiutato la richiesta per piano, permessi o fatturazione. Non aggiungere un metodo di pagamento: verifica che l'account sia sul piano Free.";
                    } else {
                        message = "Errore Groq HTTP " + status + ": " + body;
                    }
                    postError(callback, message);
                }
            } catch (Exception e) {
                String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                postError(callback, "Connessione Groq non riuscita: " + detail);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void postError(ResponseCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
