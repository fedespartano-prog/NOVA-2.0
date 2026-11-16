
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

    private APIKeyStore keyStore;
    private AIManager aiManager;

    private SpeechRecognizer recognizer;
    private TextToSpeech textToSpeech;

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
                int language = textToSpeech.setLanguage(Locale.ITALIAN);

                ttsReady =
                        language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        initializeSpeech();
    }

    private void createInterface() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(0xFF0A0A19);

        TextView title = createLabel("NOVA 2.0");
        title.setTextSize(34);
        title.setGravity(Gravity.CENTER);

        status = createLabel("NOVA pronta");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);

        recognizedText = createLabel("Premi il pulsante e parla");
        recognizedText.setGravity(Gravity.CENTER);

        listenButton = new Button(this);
        listenButton.setText("PARLA CON NOVA");
        listenButton.setOnClickListener(v -> startListening());

        TextView settings = createLabel(
                "CONFIGURAZIONE INTELLIGENZA ARTIFICIALE"
        );

        providerSpinner = new Spinner(this);

        String[] providers = {
                "OPENAI",
                "DEEPSEEK",
                "GEMINI"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
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
        layout.addView(status);
        layout.addView(recognizedText);
        layout.addView(listenButton);

        layout.addView(settings);
        layout.addView(createLabel("Provider preferito"));
        layout.addView(providerSpinner);

        layout.addView(openAIInput);
        layout.addView(deepSeekInput);
        layout.addView(geminiInput);
        layout.addView(saveButton);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private TextView createLabel(String value) {

        TextView label = new TextView(this);

        label.setText(value);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(16);
        label.setPadding(0, 15, 0, 15);

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

            String openAI = keyStore.getKey("OPENAI");
            String deepSeek = keyStore.getKey("DEEPSEEK");
            String gemini = keyStore.getKey("GEMINI");

            openAIInput.setText(openAI == null ? "" : openAI);
            deepSeekInput.setText(deepSeek == null ? "" : deepSeek);
            geminiInput.setText(gemini == null ? "" : gemini);

            rebuildAIManager();

        } catch (Exception e) {

            status.setText(
                    "Errore caricamento configurazione: "
                    + e.getMessage()
            );
        }
    }

    private void saveAIConfiguration() {

        try {

            String openAI = openAIInput.getText().toString().trim();
            String deepSeek = deepSeekInput.getText().toString().trim();
            String gemini = geminiInput.getText().toString().trim();

            if (!openAI.isEmpty()) {
                keyStore.saveKey("OPENAI", openAI);
            }

            if (!deepSeek.isEmpty()) {
                keyStore.saveKey("DEEPSEEK", deepSeek);
            }

            if (!gemini.isEmpty()) {
                keyStore.saveKey("GEMINI", gemini);
            }

            String selected = providerSpinner
                    .getSelectedItem()
                    .toString();

            keyStore.setSelectedProvider(selected);

            rebuildAIManager();

            Toast.makeText(
                    this,
                    "Configurazione salvata sul telefono.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            status.setText(
                    "Errore salvataggio: " + e.getMessage()
            );

            Toast.makeText(
                    this,
                    "Salvataggio non riuscito.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void rebuildAIManager() throws Exception {

        String openAI = keyStore.getKey("OPENAI");
        String deepSeek = keyStore.getKey("DEEPSEEK");
        String gemini = keyStore.getKey("GEMINI");

        String selected = keyStore.getSelectedProvider();

        aiManager = AIProviderFactory.createManager(
                selected,
                openAI,
                deepSeek,
                gemini
        );

        if (aiManager.isProviderAvailable()) {

            status.setText(
                    "NOVA pronta - "
                    + aiManager.getProvider().getProviderName()
            );

        } else {

            status.setText(
                    "Inserisci una chiave API valida."
            );
        }
    }

    private void initializeSpeech() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            status.setText(
                    "Riconoscimento vocale non disponibile."
            );

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
            public void onRmsChanged(float value) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("Invio all'intelligenza artificiale...");
            }

            @Override
            public void onError(int error) {
                status.setText("Riprova a parlare.");
                listenButton.setEnabled(true);
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                );

                if (matches != null && !matches.isEmpty()) {

                    String phrase = matches.get(0);

                    recognizedText.setText("Tu: " + phrase);

                    sendToAI(phrase);

                } else {

                    status.setText("Non ho capito.");
                    listenButton.setEnabled(true);
                }
            }

            @Override
            public void onPartialResults(Bundle results) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
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
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
        );

        listenButton.setEnabled(false);
        recognizer.startListening(intent);
    }

    private void sendToAI(String phrase) {

        if (aiManager == null || !aiManager.isProviderAvailable()) {

            status.setText("Nessuna AI configurata.");
            listenButton.setEnabled(true);
            return;
        }

        status.setText("NOVA sta pensando...");

        aiManager.sendMessage(
                phrase,
                new com.novatwo.assistant.ai.AIProvider.ResponseCallback() {

                    @Override
                    public void onSuccess(String response) {

                        runOnUiThread(() -> {

                            status.setText("NOVA ha risposto");

                            recognizedText.setText(
                                    "NOVA: " + response
                            );

                            speak(response);

                            listenButton.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> {

                            status.setText("Errore richiesta AI");

                            recognizedText.setText(error);

                            listenButton.setEnabled(true);
                        });
                    }
                }
        );
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
