
package com.novatwo.assistant.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceInputManager {

    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private boolean listening = false;

    public interface VoiceCallback {
        void onResult(String text);
        void onError(String error);
        void onListeningStarted();
        void onListeningStopped();
    }

    public VoiceInputManager(Activity activity) {
        this.activity = activity;
    }

    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(activity);
    }

    public void startListening(VoiceCallback callback) {

        if (listening) {
            return;
        }

        if (activity.checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            callback.onError("Permesso microfono non autorizzato.");
            return;
        }

        if (!isAvailable()) {
            callback.onError(
                    "Riconoscimento vocale non disponibile."
            );
            return;
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer =
                        SpeechRecognizer.createSpeechRecognizer(
                                activity
                        );

                speechRecognizer.setRecognitionListener(
                        new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        listening = true;
                        callback.onListeningStarted();
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                        listening = false;
                        callback.onListeningStopped();
                    }

                    @Override
                    public void onError(int error) {
                        listening = false;
                        callback.onError(
                                "Errore riconoscimento vocale: "
                                        + error
                        );
                    }

                    @Override
                    public void onResults(Bundle results) {
                        listening = false;

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (matches != null && !matches.isEmpty()) {
                            callback.onResult(matches.get(0));
                        } else {
                            callback.onError(
                                    "Non ho capito la voce."
                            );
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle results) {
                    }

                    @Override
                    public void onEvent(
                            int eventType,
                            Bundle params
                    ) {
                    }
                });
            }

            Intent intent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.ITALIAN.toLanguageTag()
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
            );

            speechRecognizer.startListening(intent);

        } catch (Exception e) {
            listening = false;
            callback.onError(
                    "Impossibile avviare il microfono: "
                            + e.getMessage()
            );
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && listening) {
            speechRecognizer.stopListening();
            listening = false;
        }
    }

    public void shutdown() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
            listening = false;
        }
    }

    public boolean isListening() {
        return listening;
    }
}
