
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

    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

    private final String apiKey;

    private final Handler mainHandler =
        new Handler(Looper.getMainLooper());

    public GeminiProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    @Override
    public void generateResponse(
            String prompt,
            ResponseCallback callback) {

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

                connection = (HttpURLConnection)
                    url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                    "x-goog-api-key", apiKey.trim()
                );

                connection.setRequestProperty(
                    "Content-Type", "application/json"
                );

                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject content = new JSONObject();
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject request = new JSONObject();
                request.put("contents", contents);

                byte[] data = request.toString()
                    .getBytes(StandardCharsets.UTF_8);

                try (OutputStream output =
                         connection.getOutputStream()) {
                    output.write(data);
                }

                int status = connection.getResponseCode();

                InputStream stream =
                    status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                if (stream == null) {
                    throw new Exception(
                        "Risposta vuota dal server Gemini."
                    );
                }

                StringBuilder response =
                    new StringBuilder();

                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(
                            stream,
                            StandardCharsets.UTF_8))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                if (status >= 200 && status < 300) {

                    JSONObject result =
                        new JSONObject(response.toString());

                    String answer = result
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                    mainHandler.post(
                        () -> callback.onSuccess(answer)
                    );

                } else {

                    String error = response.toString();

                    mainHandler.post(
                        () -> callback.onError(
                            "Errore Gemini HTTP "
                            + status + ": " + error
                        )
                    );
                }

            } catch (Exception e) {

                String error = e.getMessage();

                mainHandler.post(
                    () -> callback.onError(
                        "Errore Gemini: " + error
                    )
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}
