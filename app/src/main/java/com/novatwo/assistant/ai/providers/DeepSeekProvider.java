
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

public class DeepSeekProvider implements AIProvider {

    private static final String API_URL =
            "https://api.deepseek.com/chat/completions";

    private static final String MODEL = "deepseek-flash";

    private final String apiKey;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public DeepSeekProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "DeepSeek";
    }

    @Override
    public void generateResponse(
            String prompt,
            ResponseCallback callback
    ) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Chiave API DeepSeek mancante.");
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
                        "Authorization",
                        "Bearer " + apiKey
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);

                JSONArray messages = new JSONArray();
                messages.put(message);

                JSONObject request = new JSONObject();
                request.put("model", MODEL);
                request.put("thinking", new JSONObject().put("type", "disabled"));
                request.put("max_tokens", 2048);
                request.put("safety_check", new JSONObject().put("level", "high"));
                String lowerPrompt = prompt.toLowerCase(java.util.Locale.ROOT);
                if (lowerPrompt.contains("cerca su internet")
                        || lowerPrompt.contains("cerca sul web")
                        || lowerPrompt.contains("cerca online")
                        || lowerPrompt.contains("fai una ricerca web")) {
                    JSONArray tools = new JSONArray();
                    tools.put(new JSONObject()
                            .put("type", "deepseek_web_search")
                            .put("name", "deepseek_web_search"));
                    tools.put(new JSONObject()
                            .put("type", "deepseek_read_url")
                            .put("name", "deepseek_read_url"));
                    request.put("tools", tools);
                    request.put("tool_choice", "auto");
                }
                request.put("messages", messages);

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

                StringBuilder response = new StringBuilder();

                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             stream,
                                             StandardCharsets.UTF_8
                                     ))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                if (status >= 200 && status < 300) {

                    JSONObject result =
                            new JSONObject(response.toString());

                    String answer = result
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    mainHandler.post(
                            () -> callback.onSuccess(answer)
                    );

                } else {

                    String error = response.toString();

                    mainHandler.post(
                            () -> callback.onError(
                                    "Errore DeepSeek HTTP "
                                            + status + ": " + error
                            )
                    );
                }

            } catch (Exception e) {

                String error = e.getMessage();

                mainHandler.post(
                        () -> callback.onError(
                                "Errore di connessione: " + error
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
