package com.novatwo.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
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
import com.novatwo.assistant.ai.AIProvider;
import com.novatwo.assistant.ai.AIProviderFactory;
import com.novatwo.assistant.config.APIKeyStore;
import com.novatwo.assistant.memory.ConversationMemory;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 100;

    private APIKeyStore keyStore;
    private ConversationMemory memory;
    private AIManager aiManager;
    private SpeechRecognizer recognizer;
    private TextToSpeech textToSpeech;

    private TextView status;
    private TextView conversation;
    private EditText messageInput;
    private EditText deepSeekInput;
    private EditText openAIInput;
    private EditText geminiInput;
    private Spinner providerSpinner;
    private Button listenButton;
    private Button sendButton;
    private boolean ttsReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyStore = new APIKeyStore(this);
        memory = new ConversationMemory(this);

        createInterface();
        loadAIConfiguration();
        initializeTextToSpeech();
        initializeSpeech();
    }

    private void createInterface() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(28, 24, 28, 32);
        layout.setBackgroundColor(0xFF090B12);

        TextView title = createLabel("NOVA 2.0");
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = createLabel("Assistente pronta");
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        conversation = createLabel("Ciao! Sono NOVA. Scrivimi oppure premi il microfono.");
        conversation.setTextSize(16);
        layout.addView(conversation);

        messageInput = new EditText(this);
        messageInput.setHint("Scrivi un messaggio...");
        messageInput.setTextColor(0xFFFFFFFF);
        messageInput.setHintTextColor(0xFFAAAAAA);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageInput.setMinLines(2);
        messageInput.setGravity(Gravity.TOP);
        layout.addView(messageInput, matchWidth());

        sendButton = new Button(this);
        sendButton.setText("INVIA MESSAGGIO");
        sendButton.setOnClickListener(v -> sendTypedMessage());
        layout.addView(sendButton, matchWidth());

        listenButton = new Button(this);
        listenButton.setText("🎤 PARLA CON NOVA");
        listenButton.setOnClickListener(v -> startListening());
        layout.addView(listenButton, matchWidth());

        TextView configTitle = createLabel("CONFIGURA I SERVIZI AI");
        configTitle.setTextSize(19);
        layout.addView(configTitle);

        providerSpinner = new Spinner(this);
        String[] providers = {"AUTO (DeepSeek, poi OpenAI, poi Gemini)",
                "DEEPSEEK", "OPENAI", "GEMINI"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, providers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(adapter);
        layout.addView(providerSpinner, matchWidth());

        deepSeekInput = createKeyInput("Chiave API DeepSeek");
        openAIInput = createKeyInput("Chiave API OpenAI");
        geminiInput = createKeyInput("Chiave API Gemini");
        layout.addView(deepSeekInput, matchWidth());
        layout.addView(openAIInput, matchWidth());
        layout.addView(geminiInput, matchWidth());

        Button saveButton = new Button(this);
        saveButton.setText("SALVA LE CHIAVI SUL TELEFONO");
        saveButton.setOnClickListener(v -> saveAIConfiguration());
        layout.addView(saveButton, matchWidth());

        TextView privacy = createLabel("Le chiavi vengono cifrate e conservate sul telefono. "
                + "Le richieste vengono inviate soltanto al provider selezionato. "
                + "Le API possono comportare costi secondo il tuo piano: verifica "
                + "tariffe e limiti del provider prima dell'uso.");
        privacy.setTextSize(12);
        layout.addView(privacy);

        Button clearMemoryButton = new Button(this);
        clearMemoryButton.setText("CANCELLA MEMORIA LOCALE");
        clearMemoryButton.setOnClickListener(v -> {
            memory.clearMemory();
            Toast.makeText(this, "Memoria locale cancellata.", Toast.LENGTH_SHORT).show();
        });
        layout.addView(clearMemoryButton, matchWidth());

        scroll.addView(layout);
        setContentView(scroll);
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView createLabel(String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(16);
        label.setPadding(0, 12, 0, 12);
        return label;
    }

    private EditText createKeyInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFFAAAAAA);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return input;
    }

    private void loadAIConfiguration() {
        try {
            setSpinnerProvider(keyStore.getSelectedProvider());
            setInputValue(deepSeekInput, keyStore.getKey("DEEPSEEK"));
            setInputValue(openAIInput, keyStore.getKey("OPENAI"));
            setInputValue(geminiInput, keyStore.getKey("GEMINI"));
            rebuildAIManager();
        } catch (Exception e) {
            status.setText("Configurazione da completare: " + safeMessage(e));
        }
    }

    private void setInputValue(EditText input, String value) {
        input.setText(value == null ? "" : value);
    }

    private void setSpinnerProvider(String provider) {
        String wanted = provider == null ? "AUTO" : provider.toUpperCase();
        int position = 0;
        if ("DEEPSEEK".equals(wanted)) position = 1;
        if ("OPENAI".equals(wanted)) position = 2;
        if ("GEMINI".equals(wanted)) position = 3;
        providerSpinner.setSelection(position);
    }

    private String selectedProvider() {
        int position = providerSpinner.getSelectedItemPosition();
        if (position == 1) return "DEEPSEEK";
        if (position == 2) return "OPENAI";
        if (position == 3) return "GEMINI";
        return "AUTO";
    }

    private void saveAIConfiguration() {
        try {
            saveOrDeleteKey("DEEPSEEK", deepSeekInput);
            saveOrDeleteKey("OPENAI", openAIInput);
            saveOrDeleteKey("GEMINI", geminiInput);
            keyStore.setSelectedProvider(selectedProvider());
            rebuildAIManager();
            Toast.makeText(this, "Configurazione salvata in modo cifrato sul telefono.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("Salvataggio non riuscito: " + safeMessage(e));
        }
    }

    private void saveOrDeleteKey(String provider, EditText input) throws Exception {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            keyStore.deleteKey(provider);
        } else {
            keyStore.saveKey(provider, value);
        }
    }

    private void rebuildAIManager() throws Exception {
        aiManager = AIProviderFactory.createManager(
                keyStore.getSelectedProvider(),
                keyStore.getKey("OPENAI"),
                keyStore.getKey("DEEPSEEK"),
                keyStore.getKey("GEMINI"));
        AIProvider provider = aiManager.getProvider();
        status.setText(provider == null
                ? "Inserisci almeno una chiave API per iniziare."
                : "Pronta · provider selezionato: " + provider.getProviderName());
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && textToSpeech != null) {
                int language = textToSpeech.setLanguage(Locale.ITALIAN);
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    private void initializeSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Riconoscimento vocale non disponibile su questo dispositivo.");
            listenButton.setEnabled(false);
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                status.setText("NOVA ti sta ascoltando...");
            }
            @Override public void onBeginningOfSpeech() {
                status.setText("Ti sto ascoltando...");
            }
            @Override public void onRmsChanged(float value) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() {
                status.setText("Sto elaborando la richiesta...");
            }
            @Override public void onError(int error) {
                status.setText("Non ho ricevuto la voce. Premi e riprova.");
                listenButton.setEnabled(true);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                listenButton.setEnabled(true);
                if (matches != null && !matches.isEmpty()) {
                    messageInput.setText(matches.get(0));
                    sendMessage(matches.get(0));
                } else {
                    status.setText("Non ho capito. Riprova.");
                }
            }
            @Override public void onPartialResults(Bundle results) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION);
            return;
        }
        if (recognizer == null) {
            status.setText("Riconoscimento vocale non disponibile.");
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        listenButton.setEnabled(false);
        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            listenButton.setEnabled(true);
            status.setText("Impossibile avviare il microfono: " + safeMessage(e));
        }
    }

    private void sendTypedMessage() {
        String message = messageInput.getText().toString().trim();
        sendMessage(message);
    }

    private void sendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            status.setText("Scrivi o pronuncia prima un messaggio.");
            return;
        }
        if (aiManager == null || !aiManager.isProviderAvailable()) {
            status.setText("Configura e salva almeno una chiave API.");
            return;
        }

        sendButton.setEnabled(false);
        listenButton.setEnabled(false);
        conversation.setText("Tu: " + message);
        status.setText("NOVA sta contattando " + aiManager.getProvider().getProviderName() + "...");
        memory.saveMessage("user", message);

        aiManager.sendMessage(message, new AIProvider.ResponseCallback() {
            @Override public void onSuccess(String response) {
                runOnUiThread(() -> {
                    conversation.setText("NOVA: " + response);
                    status.setText("Risposta ricevuta.");
                    memory.saveMessage("assistant", response);
                    speak(response);
                    setControlsEnabled(true);
                });
            }

            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    conversation.setText(error);
                    status.setText("Richiesta non riuscita.");
                    setControlsEnabled(true);
                });
            }
        });
    }

    private void setControlsEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
        listenButton.setEnabled(enabled);
    }

    private void speak(String message) {
        if (ttsReady && textToSpeech != null && message != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "nova_response");
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else if (requestCode == AUDIO_PERMISSION) {
            status.setText("Permesso microfono non concesso.");
            listenButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.cancel();
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
