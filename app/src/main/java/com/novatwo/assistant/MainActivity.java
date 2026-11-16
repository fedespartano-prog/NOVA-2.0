
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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.novatwo.assistant.ai.AIManager;
import com.novatwo.assistant.ai.AIProviderFactory;
import com.novatwo.assistant.config.APIKeyStore;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 100;

    private SpeechRecognizer recognizer;
    private TextToSpeech textToSpeech;
    private APIKeyStore keyStore;
    private AIManager aiManager;

    private TextView status;
    private TextView recognizedText;
    private Button listenButton;

    private EditText openAIInput;
    private EditText deepSeekInput;
    private EditText geminiInput;
    private Spinner providerSpinner;

    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyStore = new APIKeyStore(this);

        createInterface();
        loadAIConfiguration();

        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int languageResult =
                        textToSpeech.setLanguage(Locale.ITALIAN);

                ttsReady =
                        languageResult != TextToSpeech.LANG_MISSING_DATA
                        && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText(
                    "Riconoscimento vocale non disponibile."
            );
            listenButton.setEnabled(false);
            return;
        }

        recognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(
                new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("NOVA ti sta ascoltando...");
            }

            @Override
            public void onBeginningOfSpeech() {
                status.setText("Ti sto ascoltando...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                status.setText("Invio all'intelligenza artificiale...");
            }

            @Override
            public void onError(int error) {
                status.setText(
                        "Non ho riconosciuto la voce. Riprova."
                );
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
                    recognizedText.setText("Tu: " + phrase);

                    sendToAI(phrase);
                } else {
                    status.setText("Non ho capito. Riprova.");
                    listenButton.setEnabled(true);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        listenButton.setOnClickListener(v -> startListening());
    }

    private void createInterface() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
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
        status.setPadding(0, 30, 0, 20);

        recognizedText = new TextView(this);
        recognizedText.setText("Premi il pulsante e parla");
        recognizedText.setTextColor(0xFFFFFFFF);
        recognizedText.setTextSize(16);
        recognizedText.setGravity(Gravity.CENTER);
        recognizedText.setPadding(0, 20, 0, 25);

        listenButton = new Button(this);
        listenButton.setText("PARLA CON NOVA");

        TextView settingsTitle = new TextView(this);
        settingsTitle.setText("CONFIGURAZIONE INTELLIGENZA ARTIFICIALE");
        settingsTitle.setTextColor(0xFFFFFFFF);
        settingsTitle.setTextSize(17);
        settingsTitle.setPadding(0, 35, 0, 15);

        TextView providerLabel = createLabel("Provider preferito");
        providerSpinner = new Spinner(this);

        String[] providers = {
                "OPENAI",
                "DEEPSEEK",
                "GEMINI"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        providers
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        providerSpinner.setAdapter(adapter);

        openAIInput = createKeyInput("Chiave API OpenAI");
        deepSeekInput = createKeyInput("Chiave API DeepSeek");
        geminiInput = createKeyInput("Chiave API Gemini");

        Button saveButton = new Button(this);
        saveButton.setText("SALVA CONFIGURAZIONE AI");
        saveButton.setOnClickListener(v -> saveAIConfiguration());

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(status);
        layout.addView(recognizedText);
        layout.addView(listenButton);

        layout.addView(settingsTitle);
        layout.addView(providerLabel);
        layout.addView(providerSpinner);
        layout.addView(openAIInput);
        layout.addView(deepSeekInput);
        layout.addView(geminiInput);
        layout.addView(saveButton);

        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(15);
        label.setPadding(0, 12, 0, 5);
        return label;
    }

    private EditText createKeyInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        input.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        return input;
    }

    private void loadAIConfiguration() {

        try {
            String selected = keyStore.getSelectedProvider();

            String[] providers = {
                    "OPENAI",
                    "DEEPSEEK",
                    "GEMINI"
            };

            for (int i = 0; i < providers.length; i++) {
                if (providers[i].equalsIgnoreCase(selected)) {
                    providerSpinner.setSelection(i);
                    break;
                }
            }

            rebuildAIManager();

        } catch (Exception e) {
            status.setText(
                    "Errore nel caricamento della configurazione."
            );
        }
    }

    private void saveAIConfiguration() {

        try {
            if (!openAIInput.getText().toString().trim().isEmpty()) {
                keyStore.saveKey(
                        "OPENAI",
                        openAIInput.getText().toString()
                );
            }

            if (!deepSeekInput.getText().toString().trim().isEmpty()) {
                keyStore.saveKey(
                        "DEEPSEEK",
                        deepSeekInput.getText().toString()
                );
            }

            if (!geminiInput.getText().toString().trim().isEmpty()) {
                keyStore.saveKey(
                        "GEMINI",
                        geminiInput.getText().toString()
                );
            }

            String selected =
                    providerSpinner.getSelectedItem().toString();

            keyStore.setSelectedProvider(selected);

            rebuildAIManager();

            openAIInput.setText("");
            deepSeekInput.setText("");
            geminiInput.setText("");

            Toast.makeText(
                    this,
                    "Configurazione salvata sul telefono.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            status.setText(
                    "Errore nel salvataggio delle chiavi API."
            );
        }
    }

    private void rebuildAIManager() throws Exception {

        String openAIKey = keyStore.getKey("OPENAI");
        String deepSeekKey = keyStore.getKey("DEEPSEEK");
        String geminiKey = keyStore.getKey("GEMINI");

        String selected = keyStore.getSelectedProvider();

        aiManager = AIProviderFactory.createManager(
                selected,
                openAIKey,
                deepSeekKey,
                geminiKey
        );

        if (aiManager.isProviderAvailable()) {
            status.setText(
                    "NOVA pronta - "
                    + aiManager.getProvider().getProviderName()
            );
        } else {
            status.setText(
                    "Configura una chiave API per iniziare."
            );
        }
    }

    private void sendToAI(String phrase) {

        if (aiManager == null || !aiManager.isProviderAvailable()) {
            status.setText(
                    "Nessuna AI configurata. Inserisci una chiave API."
            );
            listenButton.setEnabled(true);
            return;
        }

        status.setText("NOVA sta pensando...");

        aiManager.sendMessage(
                phrase,
                new com.novatwo.assistant.ai.AIProvider.ResponseCallback() {

            @Override
            public void onSuccess(String response) {
                status.setText("NOVA ha risposto");
                recognizedText.setText("NOVA: " + response);
                speak(response);
                listenButton.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                status.setText("Errore durante la richiesta AI");
                recognizedText.setText(error);
                speak("Si è verificato un errore.");
                listenButton.setEnabled(true);
            }
        });
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

        if (recognizer == null) {
            return;
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
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == AUDIO_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startListening();

        } else {
            status.setText("Permesso microfono non concesso.");
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
