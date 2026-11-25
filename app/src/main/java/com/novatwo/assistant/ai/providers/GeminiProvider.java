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

public class GeminiProvider implements AIProvider {

    // Gemini 3.8 Flash: modello attuale con disponibilità nel livello gratuito.
    // La quota effettiva dipende dal progetto Google e dai limiti applicati.
    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

    private final String apiKey;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GeminiProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    @Override
    public void generateResponse(String prompt, ResponseCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Chiave API Gemini mancante.");
            return;
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError("Messaggio vuoto.");
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(45000);
                connection.setDoOutput(true);
                connection.setRequestProperty("x-goog-api-key", apiKey.trim());
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject part = new JSONObject();
                part.put("text", prompt);
                JSONArray parts = new JSONArray().put(part);
                JSONObject content = new JSONObject();
                content.put("parts", parts);
                JSONArray contents = new JSONArray().put(content);
                JSONObject request = new JSONObject();
                request.put("contents", contents);
                request.put("generationConfig", new JSONObject().put("maxOutputTokens", 2048));

                byte[] data = request.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                }

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                StringBuilder response = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                    }
                }

                if (status < 200 || status >= 300) {
                    String detail = response.toString();
                    final String message;
                    if (status == 404) {
                        message = "Gemini non trova o non abilita il modello richiesto per questo progetto (HTTP 404). Controllare il modello disponibile nel progetto Google AI Studio. Dettaglio: " + detail;
                    } else if (status == 429) {
                        message = "Limite o quota Gemini raggiunti (HTTP 429). NOVA non riproverà automaticamente e non passerà a servizi a pagamento. Attendi il ripristino della quota gratuita. Dettaglio: " + detail;
                    } else if (status == 401 || status == 403) {
                        message = "Google ha rifiutato la chiave o l'accesso al progetto Gemini (HTTP " + status + "). Verifica autorizzazioni e progetto associato. Dettaglio: " + detail;
                    } else {
                        message = "Errore Gemini HTTP " + status + ": " + detail;
                    }
                    mainHandler.post(() -> callback.onError(message));
                    return;
                }

                JSONObject result = new JSONObject(response.toString());
                JSONArray candidates = result.optJSONArray("candidates");
                if (candidates == null || candidates.length() == 0) {
                    JSONObject feedback = result.optJSONObject("promptFeedback");
                    String reason = feedback == null ? "" : feedback.optString("blockReason", "");
                    String detail = reason.isEmpty()
                            ? "Google non ha restituito una risposta. Dettaglio: " + response
                            : "Richiesta bloccata da Gemini: " + reason;
                    mainHandler.post(() -> callback.onError(detail));
                    return;
                }

                JSONObject candidate = candidates.optJSONObject(0);
                JSONObject candidateContent = candidate == null
                        ? null : candidate.optJSONObject("content");
                JSONArray answerParts = candidateContent == null
                        ? null : candidateContent.optJSONArray("parts");
                StringBuilder answer = new StringBuilder();
                if (answerParts != null) {
                    for (int i = 0; i < answerParts.length(); i++) {
                        JSONObject answerPart = answerParts.optJSONObject(i);
                        if (answerPart != null) {
                            String piece = answerPart.optString("text", "");
                            if (!piece.isEmpty()) {
                                if (answer.length() > 0) answer.append("\n");
                                answer.append(piece);
                            }
                        }
                    }
                }
                if (answer.length() == 0) {
                    String finishReason = candidate == null ? "" : candidate.optString("finishReason", "");
                    String detail = "Gemini ha restituito una risposta senza testo"
                            + (finishReason.isEmpty() ? "." : " (motivo: " + finishReason + ").");
                    mainHandler.post(() -> callback.onError(detail));
                    return;
                }
                String finalAnswer = answer.toString();
                mainHandler.post(() -> callback.onSuccess(finalAnswer));

            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                mainHandler.post(() -> callback.onError("Errore Gemini: " + error));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}
