package com.novatwo.assistant.actions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class PhoneActionRouter {
    public interface ResultCallback { void onResult(String message); }

    private static final int LOCATION_PERMISSION_REQUEST = 7104;
    private PhoneActionRouter() { }

    public static boolean handle(Activity activity, String raw, ResultCallback callback) {
        if (raw == null) return false;
        String command = raw.trim();
        String lower = command.toLowerCase(Locale.ITALIAN);

        if (starts(lower, "nota ", "scrivi una nota ", "scrivimi una nota ",
                "crea una nota ", "salva una nota ", "ricordami di ")) {
            String note = command;
            String[] prefixes = {"scrivimi una nota ", "scrivi una nota ", "crea una nota ",
                    "salva una nota ", "nota ", "ricordami di "};
            for (String prefix : prefixes) {
                if (lower.startsWith(prefix)) { note = command.substring(prefix.length()).trim(); break; }
            }
            if (note.isEmpty()) callback.onResult("Dimmi cosa devo scrivere nella nota.");
            else {
                new LocalNoteStore(activity).add(note);
                callback.onResult("Ho salvato questa nota sul telefono: " + note);
            }
            return true;
        }

        if (lower.contains("mostra le note") || lower.contains("leggi le note")
                || lower.equals("le mie note")) {
            callback.onResult(new LocalNoteStore(activity).latest(20));
            return true;
        }

        if (lower.contains("dove mi trovo") || lower.contains("dove sono")
                || lower.contains("la mia posizione")) {
            showCurrentLocation(activity, callback);
            return true;
        }

        if (lower.startsWith("portami a ") || lower.startsWith("naviga verso ")
                || lower.startsWith("indicami la strada per ") || lower.startsWith("vai a ")) {
            String destination = command.replaceFirst("(?i)^(portami a |naviga verso |indicami la strada per |vai a )", "").trim();
            if (destination.isEmpty()) callback.onResult("Dimmi la destinazione.");
            else openNavigation(activity, destination, callback);
            return true;
        }

        if (lower.startsWith("cosa c'è intorno") || lower.startsWith("cosa c'e intorno")
                || lower.contains("cosa c'è vicino") || lower.contains("cosa c'e vicino")) {
            openNearbySearch(activity, callback);
            return true;
        }

        if (lower.contains("meteo") || lower.contains("che tempo fa") || lower.contains("previsioni")) {
            openWeather(activity, command, callback);
            return true;
        }

        if (lower.startsWith("manda un messaggio whatsapp ") || lower.startsWith("scrivi su whatsapp ")
                || lower.startsWith("manda su whatsapp ")) {
            String text = command.replaceFirst("(?i)^(manda un messaggio whatsapp |scrivi su whatsapp |manda su whatsapp )", "").trim();
            shareTextToWhatsApp(activity, text, callback);
            return true;
        }

        if (lower.contains("invia una foto su whatsapp") || lower.contains("manda una foto su whatsapp")
                || lower.contains("condividi una foto su whatsapp")) {
            pickPhotoForWhatsApp(activity, callback);
            return true;
        }

        if (lower.startsWith("apri ")) {
            String requestedApp = command.substring(5).trim();
            if (requestedApp.isEmpty()) callback.onResult("Dimmi quale applicazione vuoi aprire.");
            else launchAnyInstalledApp(activity, requestedApp, callback);
            return true;
        }

        if (lower.startsWith("cerca nelle email ") || lower.startsWith("cerca email ")) {
            launchApp(activity, "com.google.android.gm", "Gmail", callback);
            callback.onResult("Ho aperto Gmail. Per cercare e leggere direttamente le email da NOVA serve collegare l'account tramite l'autorizzazione ufficiale Google; non userò né copierò la tua password.");
            return true;
        }

        if (lower.contains("attiva controllo schermo") || lower.contains("attiva accessibilità nova")) {
            activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            callback.onResult("Si sono aperte le impostazioni di Accessibilità. Cerca NOVA e attiva il servizio solo se vuoi consentirle di interagire con le schermate.");
            return true;
        }

        if (lower.equals("torna indietro") || lower.equals("vai alla home")
                || lower.equals("apri notifiche") || lower.equals("scorri schermata")
                || lower.startsWith("scrivi nel campo ")) {
            String action = lower.equals("torna indietro") ? "indietro"
                    : lower.equals("vai alla home") ? "home"
                    : lower.equals("apri notifiche") ? "notifiche"
                    : lower.equals("scorri schermata") ? "scorri"
                    : "scrivi " + command.substring("scrivi nel campo ".length());
            callback.onResult(NovaAccessibilityService.execute(action));
            return true;
        }

        if (lower.startsWith("tocca ")) {
            String label = command.substring(6).trim();
            new android.app.AlertDialog.Builder(activity)
                    .setTitle("Confermi il tocco?")
                    .setMessage("NOVA proverà a premere «" + label + "» nella schermata attiva. Se è un pulsante di invio o conferma, l'operazione potrebbe essere definitiva.")
                    .setNegativeButton("ANNULLA", (d,w) -> callback.onResult("Operazione annullata."))
                    .setPositiveButton("CONFERMA", (d,w) -> callback.onResult(NovaAccessibilityService.execute("tocca " + label)))
                    .show();
            return true;
        }

        if (lower.startsWith("apri impostazioni")) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            callback.onResult("Ho aperto le impostazioni Android.");
            return true;
        }
        return false;
    }

    private static boolean starts(String value, String... prefixes) {
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }

    private static void showCurrentLocation(Activity activity, ResultCallback callback) {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            callback.onResult("Per individuare la tua posizione, concedi a NOVA il permesso di posizione e ripeti la richiesta.");
            return;
        }
        try {
            LocationManager manager = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
            Location best = null;
            for (String provider : manager.getProviders(true)) {
                try {
                    Location candidate = manager.getLastKnownLocation(provider);
                    if (candidate != null && (best == null
                            || candidate.getTime() > best.getTime())) best = candidate;
                } catch (SecurityException ignored) { }
            }
            if (best == null) {
                callback.onResult("Non ho ancora una posizione recente. Attiva la posizione del telefono e riprova tra qualche secondo.");
                return;
            }
            String maps = "https://www.google.com/maps/search/?api=1&query="
                    + best.getLatitude() + "," + best.getLongitude();
            callback.onResult("La posizione disponibile è: " + best.getLatitude() + ", "
                    + best.getLongitude() + ". Apro la mappa.");
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(maps)));
        } catch (SecurityException e) {
            callback.onResult("Android non ha concesso l'accesso alla posizione.");
        } catch (Exception e) {
            callback.onResult("Non riesco a leggere la posizione in questo momento.");
        }
    }

    private static void openNavigation(Activity activity, String destination, ResultCallback callback) {
        try {
            String encoded = URLEncoder.encode(destination, StandardCharsets.UTF_8.name());
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=" + encoded));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(activity.getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + encoded));
            }
            activity.startActivity(intent);
            callback.onResult("Ho aperto il percorso verso " + destination + ". Controlla la destinazione prima di partire.");
        } catch (Exception e) { callback.onResult("Non sono riuscita ad aprire la navigazione."); }
    }

    private static void openNearbySearch(Activity activity, ResultCallback callback) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=luoghi+vicino+a+me"));
        activity.startActivity(intent);
        callback.onResult("Ho aperto Maps per cercare i luoghi vicini. Per risultati basati sul GPS, consenti la posizione anche a Maps.");
    }

    private static void openWeather(Activity activity, String request, ResultCallback callback) {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            callback.onResult("Concedi a NOVA il permesso di posizione e poi ripeti la richiesta meteo.");
            return;
        }
        try {
            LocationManager manager = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
            Location best = null;
            for (String provider : manager.getProviders(true)) {
                try {
                    Location candidate = manager.getLastKnownLocation(provider);
                    if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
                } catch (SecurityException ignored) { }
            }
            if (best == null) {
                callback.onResult("Non ho una posizione recente. Attiva il GPS, attendi qualche secondo e ripeti la richiesta.");
                return;
            }
            WeatherService.current(best.getLatitude(), best.getLongitude(), callback::onResult);
        } catch (Exception e) {
            callback.onResult("Non riesco a leggere la posizione per il meteo.");
        }
    }

    private static void shareTextToWhatsApp(Activity activity, String text, ResultCallback callback) {
        if (text.isEmpty()) { callback.onResult("Dimmi il testo da preparare per WhatsApp."); return; }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        send.setPackage("com.whatsapp");
        try {
            activity.startActivity(send);
            callback.onResult("Ho passato il testo a WhatsApp. Scegli il destinatario e conferma tu l'invio nell'app.");
        } catch (Exception e) {
            Intent chooser = new Intent(Intent.ACTION_SEND);
            chooser.setType("text/plain");
            chooser.putExtra(Intent.EXTRA_TEXT, text);
            activity.startActivity(Intent.createChooser(chooser, "Condividi con"));
            callback.onResult("WhatsApp non è disponibile direttamente: ho aperto il menu di condivisione. Conferma tu destinatario e invio.");
        }
    }

    private static void pickPhotoForWhatsApp(Activity activity, ResultCallback callback) {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick.setType("image/*");
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            activity.startActivityForResult(Intent.createChooser(pick, "Scegli la foto"), 7105);
            callback.onResult("Scegli la foto da condividere. Dopo la selezione potrai scegliere WhatsApp e confermare l'invio.");
        } catch (Exception e) { callback.onResult("Non riesco ad aprire la selezione delle foto."); }
    }


    private static void launchAnyInstalledApp(Activity activity, String requestedName, ResultCallback callback) {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = activity.getPackageManager().queryIntentActivities(query, 0);
        String wanted = requestedName.toLowerCase(Locale.ITALIAN).trim();
        ResolveInfo best = null;
        for (ResolveInfo info : apps) {
            CharSequence label = info.loadLabel(activity.getPackageManager());
            if (label != null && label.toString().toLowerCase(Locale.ITALIAN).equals(wanted)) {
                best = info;
                break;
            }
        }
        if (best == null) {
            for (ResolveInfo info : apps) {
                CharSequence label = info.loadLabel(activity.getPackageManager());
                if (label != null && label.toString().toLowerCase(Locale.ITALIAN).contains(wanted)) {
                    best = info;
                    break;
                }
            }
        }
        if (best == null) {
            callback.onResult("Non trovo un'app installata con il nome «" + requestedName
                    + "». Controlla il nome mostrato nella schermata del telefono.");
            return;
        }
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setClassName(best.activityInfo.packageName, best.activityInfo.name);
        try {
            activity.startActivity(launch);
            callback.onResult("Ho aperto " + best.loadLabel(activity.getPackageManager())
                    + ". Per le operazioni dentro l'app ti chiederò conferma quando necessario.");
        } catch (Exception e) {
            callback.onResult("Android non consente di aprire questa applicazione in questo momento.");
        }
    }

    private static void launchApp(Activity activity, String packageName, String label, ResultCallback callback) {
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) {
            callback.onResult(label + " non risulta installata. Posso comunque aprire un servizio compatibile dal browser.");
            return;
        }
        activity.startActivity(launch);
        callback.onResult("Ho aperto " + label + ".");
    }

    public static boolean isPhotoShareRequest(int requestCode) { return requestCode == 7105; }

    public static void shareSelectedPhoto(Activity activity, Uri uri) {
        if (uri == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/*");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(send, "Invia foto con"));
    }
}
