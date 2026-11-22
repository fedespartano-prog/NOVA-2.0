package com.novatwo.assistant.integrations;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GoogleServicesActivity extends Activity {
    private static final int REQUEST_AUTH = 8201;
    private TextView status;
    private String pendingService = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(5,7,11));
        getWindow().setNavigationBarColor(Color.rgb(5,7,11));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,24,28,28);
        root.setBackgroundColor(Color.rgb(5,7,11));
        TextView title = new TextView(this);
        title.setText("NOVA · SERVIZI GOOGLE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Collegamento Google ufficiale con consenso sul telefono. NOVA richiede separatamente l'accesso in sola lettura a Gmail, Drive e Calendar. Puoi revocare l'accesso. Le API devono essere abilitate nel progetto Google e l'app Android registrata con il suo certificato.");
        info.setTextColor(0xFFB0BEC5);
        info.setPadding(0,16,0,16);
        root.addView(info);
        status = new TextView(this);
        status.setText("Nessun servizio Google richiesto in questa sessione.");
        status.setTextColor(0xFF00FFA4);
        status.setPadding(0,8,0,14);
        root.addView(status);
        addAction(root,"COLLEGA E LEGGI GMAIL", "GMAIL", Arrays.asList(new Scope("https://www.googleapis.com/auth/gmail.readonly")));
        addAction(root,"COLLEGA E LEGGI FILE DRIVE", "DRIVE", Arrays.asList(new Scope("https://www.googleapis.com/auth/drive.metadata.readonly")));
        addAction(root,"COLLEGA E LEGGI CALENDARIO", "CALENDAR", Arrays.asList(new Scope("https://www.googleapis.com/auth/calendar.readonly")));
        Button revoke = new Button(this);
        revoke.setText("REVOCA ACCESSO GOOGLE A NOVA");
        root.addView(revoke, new LinearLayout.LayoutParams(-1,-2));
        revoke.setOnClickListener(v -> {
            status.setText("Revoca dell'accesso in corso...");
            Identity.getAuthorizationClient(this).revokeAccess(RevokeAccessRequest.builder().build())
                    .addOnSuccessListener(unused -> status.setText("Accesso Google revocato."))
                    .addOnFailureListener(error -> status.setText("Revoca non riuscita: " + safe(error.getMessage())));
        });
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addAction(LinearLayout root, String label, String service, List<Scope> scopes) {
        Button button = new Button(this);
        button.setText(label);
        root.addView(button, new LinearLayout.LayoutParams(-1,-2));
        button.setOnClickListener(v -> authorize(service, scopes));
    }

    private void authorize(String service, List<Scope> scopes) {
        pendingService = service;
        status.setText("Apertura del consenso Google per " + service + "...");
        AuthorizationRequest request = AuthorizationRequest.builder().setRequestedScopes(scopes).build();
        Identity.getAuthorizationClient(this).authorize(request)
                .addOnSuccessListener(this::handleAuthorizationResult)
                .addOnFailureListener(error -> status.setText("Autorizzazione Google non disponibile: "
                        + safe(error.getMessage()) + ". Verifica API abilitate, account Google e registrazione Android dell'app."));
    }

    private void handleAuthorizationResult(AuthorizationResult result) {
        if (result.hasResolution()) {
            PendingIntent pending = result.getPendingIntent();
            try {
                startIntentSenderForResult(pending.getIntentSender(), REQUEST_AUTH, null, 0, 0, 0);
            } catch (Exception e) {
                status.setText("Non riesco ad aprire il consenso Google: " + safe(e.getMessage()));
            }
            return;
        }
        String token = result.getAccessToken();
        if (token == null || token.isEmpty()) {
            status.setText("Google non ha restituito un token di accesso.");
            return;
        }
        fetchService(pendingService, token);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AUTH) return;
        if (resultCode != RESULT_OK || data == null) {
            status.setText("Collegamento Google annullato.");
            return;
        }
        try {
            handleAuthorizationResult(Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data));
        } catch (Exception e) {
            status.setText("Consenso Google non completato: " + safe(e.getMessage()));
        }
    }

    private void fetchService(String service, String token) {
        status.setText("Accesso autorizzato. Recupero dati " + service + "...");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint;
                if ("GMAIL".equals(service)) {
                    endpoint = "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=10";
                } else if ("DRIVE".equals(service)) {
                    endpoint = "https://www.googleapis.com/drive/v3/files?pageSize=20&orderBy=modifiedTime%20desc&fields=files(id,name,mimeType,modifiedTime)";
                } else {
                    endpoint = "https://www.googleapis.com/calendar/v3/calendars/primary/events?maxResults=15";
                }
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Accept", "application/json");
                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                if (code < 200 || code >= 300) throw new Exception("Google API HTTP " + code + ": " + body);
                String summary = summarize(service, new JSONObject(body.toString()), token);
                runOnUiThread(() -> status.setText(summary));
            } catch (Exception e) {
                String message = "Errore " + service + ": " + safe(e.getMessage());
                runOnUiThread(() -> status.setText(message));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String summarize(String service, JSONObject json, String token) throws Exception {
        StringBuilder out = new StringBuilder();
        if ("GMAIL".equals(service)) {
            JSONArray items = json.optJSONArray("messages");
            int count = items == null ? 0 : items.length();
            out.append("GMAIL · ").append(json.optInt("resultSizeEstimate", count)).append(" messaggi stimati. Ultimi ID:\n");
            for (int i=0; i<count; i++) {
                JSONObject detail = fetchGmailMetadata(items.getJSONObject(i).optString("id"), token);
                JSONObject payload = detail.optJSONObject("payload");
                JSONArray headers = payload == null ? null : payload.optJSONArray("headers");
                String subject = "(senza oggetto)";
                String from = "";
                if (headers != null) for (int h=0; h<headers.length(); h++) {
                    JSONObject header = headers.getJSONObject(h);
                    if ("subject".equalsIgnoreCase(header.optString("name"))) subject = header.optString("value");
                    if ("from".equalsIgnoreCase(header.optString("name"))) from = header.optString("value");
                }
                out.append("• ").append(subject).append(from.isEmpty() ? "" : " · " + from).append('\n');
            }
        } else if ("DRIVE".equals(service)) {
            JSONArray files = json.optJSONArray("files");
            int count = files == null ? 0 : files.length();
            out.append("GOOGLE DRIVE · ").append(count).append(" file recenti:\n");
            for (int i=0; i<count; i++) {
                JSONObject item = files.getJSONObject(i);
                out.append("• ").append(item.optString("name")).append(" · ").append(item.optString("mimeType")).append('\n');
            }
        } else {
            JSONArray events = json.optJSONArray("items");
            int count = events == null ? 0 : events.length();
            out.append("GOOGLE CALENDAR · ").append(count).append(" eventi:\n");
            for (int i=0; i<count; i++) {
                JSONObject item = events.getJSONObject(i);
                JSONObject start = item.optJSONObject("start");
                String when = start == null ? "" : start.optString("dateTime", start.optString("date", ""));
                out.append("• ").append(item.optString("summary", "(senza titolo)")).append(" · ").append(when).append('\n');
            }
        }
        return out.toString().trim();
    }

    private JSONObject fetchGmailMetadata(String id, String token) throws Exception {
        HttpURLConnection connection = null;
        try {
            String endpoint = "https://gmail.googleapis.com/gmail/v1/users/me/messages/"
                    + id + "?format=metadata&metadataHeaders=Subject&metadataHeaders=From";
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            if (code < 200 || code >= 300) throw new Exception("Gmail HTTP " + code);
            return new JSONObject(body.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "errore sconosciuto" : value;
    }
}
