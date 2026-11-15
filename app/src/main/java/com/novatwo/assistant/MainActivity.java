
package com.novatwo.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 100;
    private SpeechRecognizer recognizer;
    private TextToSpeech textToSpeech;
    private TextView status;
    private TextView recognizedText;
    private Button listenButton;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int languageResult =
                    textToSpeech.setLanguage(Locale.ITALIAN);
                ttsReady = languageResult !=
                    TextToSpeech.LANG_MISSING_DATA &&
                    languageResult !=
                    TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Riconoscimento vocale non disponibile");
            listenButton.setEnabled(false);
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("NOVA ti sta ascoltando...");
            }

            @Override
            public void onBeginningOfSpeech() {
                status.setText("Ti sto ascoltando...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("Elaborazione...");
            }

            @Override
            public void onError(int error) {
                status.setText("Non ho riconosciuto la voce. Riprova.");
                listenButton.setEnabled(true);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (matches != null && !matches.isEmpty()) {
                    String phrase = matches.get(0);
                    recognizedText.setText(phrase);

                    status.setText("Voce riconosciuta");
                    speak("Ho capito: " + phrase);
                }

                listenButton.setEnabled(true);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        listenButton.setOnClickListener(v -> startListening());
    }

    private void createInterface() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(0xFF0A0A19);

        TextView title = new TextView(this);
        title.setText("NOVA 2.0");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(34);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Personal AI Assistant");
        subtitle.setTextColor(0xFFCCCCCC);
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("NOVA pronta");
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 40, 0, 20);

        recognizedText = new TextView(this);
        recognizedText.setText("Premi il pulsante e parla");
        recognizedText.setTextColor(0xFFFFFFFF);
        recognizedText.setTextSize(16);
        recognizedText.setGravity(Gravity.CENTER);
        recognizedText.setPadding(0, 20, 0, 30);

        listenButton = new Button(this);
        listenButton.setText("PARLA CON NOVA");

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(status);
        layout.addView(recognizedText);
        layout.addView(listenButton);

        setContentView(layout);
    }

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION
            );
            return;
        }

        if (recognizer == null) return;

        Intent intent = new Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "it-IT"
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            "it-IT"
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS,
            1
        );

        listenButton.setEnabled(false);
        recognizer.startListening(intent);
    }

    private void speak(String message) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "nova_response"
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == AUDIO_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            status.setText("Permesso microfono non concesso");
            listenButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
    }
}
