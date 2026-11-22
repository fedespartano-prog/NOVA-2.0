package com.novatwo.assistant.actions;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public final class WeatherService {
    public interface Callback { void onResult(String result); }
    private WeatherService() {}
    public static void current(double latitude, double longitude, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String address = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&timezone=auto",
                    latitude, longitude);
                connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/json");
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line; while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
                double temp = current.getDouble("temperature_2m");
                double feels = current.getDouble("apparent_temperature");
                int humidity = current.getInt("relative_humidity_2m");
                double wind = current.getDouble("wind_speed_10m");
                double rain = current.getDouble("precipitation");
                int code = current.getInt("weather_code");
                String description = describe(code);
                String result = String.format(Locale.ITALIAN,
                    "Meteo attuale nella tua posizione: %s. Temperatura %.1f °C, percepita %.1f °C, umidità %d%%, vento %.1f km/h, precipitazioni %.1f mm.",
                    description, temp, feels, humidity, wind, rain);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
            } catch (Exception e) {
                String message = "Non riesco a recuperare il meteo adesso. Controlla la connessione e riprova.";
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(message));
            } finally { if (connection != null) connection.disconnect(); }
        }).start();
    }
    private static String describe(int code) {
        if (code == 0) return "sereno";
        if (code <= 3) return "poco nuvoloso o coperto";
        if (code == 45 || code == 48) return "nebbia";
        if (code >= 51 && code <= 57) return "pioviggine";
        if (code >= 61 && code <= 67) return "pioggia";
        if (code >= 71 && code <= 77) return "neve";
        if (code >= 80 && code <= 82) return "rovesci";
        if (code >= 85 && code <= 86) return "rovesci di neve";
        if (code >= 95) return "temporale";
        return "condizioni variabili";
    }
}