package com.novatwo.assistant.voice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.novatwo.assistant.MainActivity;
import com.novatwo.assistant.R;

import java.util.ArrayList;
import java.util.Locale;

public class NovaVoiceService extends Service {
    public static final String ACTION_START = "com.novatwo.assistant.voice.START";
    public static final String ACTION_RESUME = "com.novatwo.assistant.voice.RESUME";
    public static final String ACTION_STOP = "com.novatwo.assistant.voice.STOP";
    public static final String ACTION_TRANSCRIPT = "com.novatwo.assistant.voice.TRANSCRIPT";
    public static final String ACTION_USER_SPEECH = "com.novatwo.assistant.voice.USER_SPEECH";
    public static final String EXTRA_TEXT = "recognized_text";
    private static final String CHANNEL_ID = "nova_voice_session";
    private static final int NOTIFICATION_ID = 2042;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean active;
    private boolean listening;
    private boolean waitingForReply;
    private boolean destroyed;
    private boolean pausedForOtherApp;
    private AudioManager audioManager;
    private AudioManager.AudioRecordingCallback recordingCallback;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        registerRecordingMonitor();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSession();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            active = true;
            waitingForReply = false;
            startForegroundCompat();
            ensureRecognizer();
            beginListening();
        } else if (ACTION_RESUME.equals(action) && active) {
            waitingForReply = false;
            beginListening();
        }
        return START_STICKY;
    }

    private void startForegroundCompat() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 2042, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Intent stop = new Intent(this, NovaVoiceService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 2043, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(R.drawable.nova_ic_launcher)
                .setContentTitle("NOVA · chiamata attiva")
                .setContentText("NOVA resta in ascolto anche mentre usi altre app.")
                .setContentIntent(openPending)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "CHIUDI CHIAMATA", stopPending)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Chiamata NOVA", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notifica persistente della sessione vocale NOVA");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void registerRecordingMonitor() {
        if (Build.VERSION.SDK_INT < 29) return;
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) return;
        recordingCallback = new AudioManager.AudioRecordingCallback() {
            @Override public void onRecordingConfigChanged(java.util.List<AudioRecordingConfiguration> configs) {
                boolean externalRecorder = false;
                if (configs != null) {
                    for (AudioRecordingConfiguration config : configs) {
                        if (config == null) continue;
                        String packageName = config.getClientPackageName();
                        if (packageName == null || packageName.equals(getPackageName())) continue;
                        if (packageName.equals("com.google.android.googlequicksearchbox")
                                || packageName.equals("com.google.android.tts")
                                || packageName.equals("com.google.android.as")
                                || packageName.equals("com.android.speech")) continue;
                        externalRecorder = true;
                        break;
                    }
                }
                if (externalRecorder) pauseForExternalRecording();
                else resumeAfterExternalRecording();
            }
        };
        audioManager.registerAudioRecordingCallback(recordingCallback, handler);
    }

    private void pauseForExternalRecording() {
        if (!active || pausedForOtherApp) return;
        pausedForOtherApp = true;
        listening = false;
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (RuntimeException ignored) { }
        }
        updateNotification("Microfono occupato da un'altra app · NOVA riprenderà da sola");
    }

    private void resumeAfterExternalRecording() {
        if (!active || !pausedForOtherApp) return;
        pausedForOtherApp = false;
        updateNotification("NOVA è di nuovo in ascolto · chiamata attiva");
        handler.postDelayed(() -> {
            if (active && !pausedForOtherApp && !waitingForReply) beginListening();
        }, 900);
    }

    private void updateNotification(String message) {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 2042, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Intent stop = new Intent(this, NovaVoiceService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 2043, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        Notification notification = builder.setSmallIcon(R.drawable.nova_ic_launcher)
                .setContentTitle("NOVA · chiamata attiva")
                .setContentText(message).setContentIntent(openPending).setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "CHIUDI CHIAMATA", stopPending).build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(NOTIFICATION_ID, notification);
    }

    private void ensureRecognizer() {
        if (recognizer != null || !SpeechRecognizer.isRecognitionAvailable(this)) return;
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { listening = true; }
            @Override public void onBeginningOfSpeech() { sendEvent(ACTION_USER_SPEECH, null); }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { listening = false; }
            @Override public void onError(int error) {
                listening = false;
                if (active && !waitingForReply && !pausedForOtherApp && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    handler.postDelayed(() -> beginListening(), error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1400 : 850);
                }
            }
            @Override public void onResults(Bundle results) {
                listening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String phrase = matches.get(0);
                    waitingForReply = true;
                    sendEvent(ACTION_TRANSCRIPT, phrase);
                } else if (active && !waitingForReply && !pausedForOtherApp) {
                    handler.postDelayed(() -> beginListening(), 500);
                }
            }
            @Override public void onPartialResults(Bundle results) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void beginListening() {
        if (!active || destroyed || waitingForReply || pausedForOtherApp || listening) return;
        ensureRecognizer();
        if (recognizer == null) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        try {
            recognizer.startListening(intent);
            listening = true;
        } catch (RuntimeException error) {
            listening = false;
            handler.postDelayed(() -> beginListening(), 1200);
        }
    }

    private void sendEvent(String action, String phrase) {
        Intent event = new Intent(action);
        event.setPackage(getPackageName());
        if (phrase != null) event.putExtra(EXTRA_TEXT, phrase);
        sendBroadcast(event);
    }

    private void stopSession() {
        active = false;
        waitingForReply = false;
        pausedForOtherApp = false;
        handler.removeCallbacksAndMessages(null);
        if (audioManager != null && recordingCallback != null && Build.VERSION.SDK_INT >= 29) {
            audioManager.unregisterAudioRecordingCallback(recordingCallback);
            recordingCallback = null;
        }
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
        listening = false;
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        destroyed = true;
        active = false;
        handler.removeCallbacksAndMessages(null);
        if (audioManager != null && recordingCallback != null && Build.VERSION.SDK_INT >= 29) {
            audioManager.unregisterAudioRecordingCallback(recordingCallback);
            recordingCallback = null;
        }
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
