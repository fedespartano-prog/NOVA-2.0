package com.novatwo.assistant;

import android.app.Activity;
import android.Manifest;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import com.novatwo.assistant.actions.PhoneActionRouter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends FragmentActivity {

    private static final int AUDIO_PERMISSION = 100;
    private static final int BG = Color.rgb(5, 7, 11);
    private static final int PANEL = Color.rgb(15, 19, 25);
    private static final int GREEN = Color.rgb(0, 255, 164);
    private static final int MUTED = Color.rgb(139, 151, 163);

    private APIKeyStore keyStore;
    private ConversationMemory memory;
    private AIManager aiManager;
    private SpeechRecognizer recognizer;
    private TextToSpeech textToSpeech;
    private Button continuousVoiceButton;
    private boolean continuousVoiceMode = false;

    private TextView status;
    private TextView conversation;
    private TextView providerStatus;
    private TextView keyStatus;
    private EditText messageInput;
    private EditText deepSeekInput;
    private EditText openAIInput;
    private EditText geminiInput;
    private Spinner providerSpinner;
    private Button listenButton;
    private Button sendButton;
    private LinearLayout settingsPanel;
    private LinearLayout chatHistory;
    private boolean ttsReady;
    private boolean requestInProgress;
    private LinearLayout mainRoot;
    private static final int MEDIA_PICK_REQUEST = 7110;
    private boolean biometricUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        keyStore = new APIKeyStore(this);
        memory = new ConversationMemory(this);
        continuousVoiceMode = getSharedPreferences("nova_voice", MODE_PRIVATE)
                .getBoolean("continuous", false);

        createInterface();
        loadAIConfiguration();
        restoreConversation();
        initializeTextToSpeech();
        initializeSpeech();
        applyAppearance();
        authenticateApp();
    }

    private void createInterface() {
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        mainRoot = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(28));
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView brand = label("N O V A", 19, Color.WHITE, true);
        brand.setLetterSpacing(0.16f);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView version = label("2.0  /  PERSONAL AI", 10, MUTED, true);
        version.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(version);
        root.addView(header, matchWidth());

        TextView intro = label("IL TUO ASSISTENTE", 10, GREEN, true);
        intro.setLetterSpacing(0.22f);
        intro.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams introParams = wrap();
        introParams.topMargin = dp(22);
        root.addView(intro, introParams);

        TextView headline = label("Sono qui.", 29, Color.WHITE, false);
        headline.setGravity(Gravity.CENTER);
        root.addView(headline, wrap());

        OrbView orb = new OrbView(this);
        LinearLayout.LayoutParams orbParams = new LinearLayout.LayoutParams(dp(224), dp(224));
        orbParams.gravity = Gravity.CENTER;
        orbParams.topMargin = dp(5);
        orbParams.bottomMargin = dp(2);
        root.addView(orb, orbParams);

        status = label("PRONTA QUANDO VUOI", 10, GREEN, true);
        status.setGravity(Gravity.CENTER);
        status.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams statusParams = wrap();
        statusParams.topMargin = dp(2);
        statusParams.bottomMargin = dp(18);
        root.addView(status, statusParams);

        LinearLayout chatCard = panel();
        chatCard.setOrientation(LinearLayout.VERTICAL);
        chatCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout chatHeader = new LinearLayout(this);
        chatHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView chatTitle = label("CONVERSAZIONE", 10, MUTED, true);
        chatTitle.setLetterSpacing(0.13f);
        chatHeader.addView(chatTitle, new LinearLayout.LayoutParams(0, dp(24), 1));
        TextView localTag = label("MEMORIA LOCALE", 9, GREEN, true);
        chatHeader.addView(localTag);
        chatCard.addView(chatHeader);

        chatHistory = new LinearLayout(this);
        chatHistory.setOrientation(LinearLayout.VERTICAL);
        TextView welcome = label("Ciao, sono NOVA. Puoi parlarmi oppure scrivermi qui sotto.", 14,
                Color.rgb(220, 226, 232), false);
        welcome.setPadding(0, dp(8), 0, dp(6));
        chatHistory.addView(welcome, matchWidth());
        chatCard.addView(chatHistory, matchWidth());
        conversation = welcome;
        LinearLayout.LayoutParams chatParams = matchWidth();
        chatParams.bottomMargin = dp(14);
        root.addView(chatCard, chatParams);

        LinearLayout composer = panel();
        composer.setOrientation(LinearLayout.VERTICAL);
        composer.setPadding(dp(12), dp(8), dp(12), dp(10));

        messageInput = new EditText(this);
        messageInput.setTextColor(Color.WHITE);
        messageInput.setHintTextColor(Color.rgb(112, 124, 137));
        messageInput.setHint("Scrivi a NOVA...");
        messageInput.setTextSize(15);
        messageInput.setSingleLine(false);
        messageInput.setMinLines(1);
        messageInput.setMaxLines(4);
        messageInput.setBackgroundColor(Color.TRANSPARENT);
        messageInput.setPadding(dp(5), dp(7), dp(5), dp(7));
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        composer.addView(messageInput, matchWidth());

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        listenButton = new Button(this);
        listenButton.setText("🎙  PARLA");
        styleButton(listenButton, false);
        actions.addView(listenButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        listenButton.setOnClickListener(v -> startListening());

        sendButton = new Button(this);
        sendButton.setText("INVIA  ↗");
        styleButton(sendButton, true);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        sendParams.leftMargin = dp(9);
        actions.addView(sendButton, sendParams);
        sendButton.setOnClickListener(v -> sendTypedMessage());
        composer.addView(actions, matchWidth());
        root.addView(composer, matchWidth());

        TextView providerLine = label("AI  ·  DeepSeek  /  OpenAI  /  Gemini", 10, MUTED, false);
        providerLine.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams providerParams = wrap();
        providerParams.topMargin = dp(13);
        root.addView(providerLine, providerParams);

        Button settingsToggle = new Button(this);
        settingsToggle.setText("IMPOSTAZIONI  ⚙");
        styleButton(settingsToggle, false);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        toggleParams.topMargin = dp(10);
        root.addView(settingsToggle, toggleParams);

        settingsPanel = panel();
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setPadding(dp(15), dp(15), dp(15), dp(15));
        settingsPanel.setVisibility(View.GONE);
        TextView settingsTitle = label("COLLEGA I TUOI SERVIZI AI", 13, Color.WHITE, true);
        settingsPanel.addView(settingsTitle, wrap());

        providerStatus = label("Provider: non configurato", 11, MUTED, false);
        providerStatus.setPadding(0, dp(6), 0, dp(8));
        settingsPanel.addView(providerStatus, matchWidth());

        providerSpinner = new Spinner(this);
        String[] providers = {"AUTO · DeepSeek, OpenAI, Gemini",
                "DEEPSEEK", "OPENAI", "GEMINI"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, providers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(adapter);
        settingsPanel.addView(providerSpinner, matchWidth());

        keyStatus = label("Le chiavi salvate sono protette sul dispositivo.", 10, MUTED, false);
        keyStatus.setPadding(0, dp(8), 0, dp(3));
        settingsPanel.addView(keyStatus, matchWidth());

        deepSeekInput = createKeyInput("Nuova chiave API DeepSeek");
        openAIInput = createKeyInput("Nuova chiave API OpenAI");
        geminiInput = createKeyInput("Nuova chiave API Gemini");
        settingsPanel.addView(deepSeekInput, matchWidth());
        settingsPanel.addView(openAIInput, matchWidth());
        settingsPanel.addView(geminiInput, matchWidth());

        TextView keyHint = label("Inserisci solo le chiavi nuove. I campi vuoti mantengono quelle già salvate.", 10,
                MUTED, false);
        settingsPanel.addView(keyHint, matchWidth());

        Button saveButton = new Button(this);
        saveButton.setText("SALVA CONFIGURAZIONE");
        styleButton(saveButton, true);
        settingsPanel.addView(saveButton, fullButtonParams());
        saveButton.setOnClickListener(v -> saveAIConfiguration());

        Button deleteKeysButton = new Button(this);
        deleteKeysButton.setText("ELIMINA TUTTE LE CHIAVI");
        styleButton(deleteKeysButton, false);
        settingsPanel.addView(deleteKeysButton, fullButtonParams());
        deleteKeysButton.setOnClickListener(v -> confirmDeleteKeys());

        Button appearanceButton = new Button(this);
        appearanceButton.setText("PERSONALIZZA ASPETTO E SFONDO");
        styleButton(appearanceButton, false);
        settingsPanel.addView(appearanceButton, fullButtonParams());
        appearanceButton.setOnClickListener(v -> chooseAppearance());


        Button personalityButton = new Button(this);
        personalityButton.setText("PERSONALITÀ E MODALITÀ INTIMITÀ");
        styleButton(personalityButton, false);
        settingsPanel.addView(personalityButton, fullButtonParams());
        personalityButton.setOnClickListener(v -> choosePersonality());

        TextView voiceSecurityInfo = label("RISPOSTE VOCALI: NOVA legge le risposte in italiano usando la voce installata sul telefono. La disponibilità e la qualità dipendono dal motore vocale Android.", 10, MUTED, false);
        voiceSecurityInfo.setPadding(0, dp(8), 0, dp(4));
        settingsPanel.addView(voiceSecurityInfo, matchWidth());

        continuousVoiceButton = new Button(this);
        updateContinuousVoiceButton();
        styleButton(continuousVoiceButton, false);
        settingsPanel.addView(continuousVoiceButton, fullButtonParams());
        continuousVoiceButton.setOnClickListener(v -> {
            continuousVoiceMode = !continuousVoiceMode;
            getSharedPreferences("nova_voice", MODE_PRIVATE).edit()
                    .putBoolean("continuous", continuousVoiceMode).apply();
            updateContinuousVoiceButton();
            if (!continuousVoiceMode && recognizer != null) {
                recognizer.cancel();
                setControlsEnabled(true);
                status.setText("ASCOLTO CONTINUO DISATTIVATO");
            } else if (continuousVoiceMode) {
                status.setText("ASCOLTO CONTINUO ATTIVO");
                startListening();
            }
        });

        Button googleServicesButton = new Button(this);
        googleServicesButton.setText("COLLEGA GMAIL · DRIVE · CALENDARIO");
        styleButton(googleServicesButton, false);
        settingsPanel.addView(googleServicesButton, fullButtonParams());
        googleServicesButton.setOnClickListener(v -> startActivity(new Intent(this,
                com.novatwo.assistant.integrations.GoogleServicesActivity.class)));

        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("COLLEGA E CONTROLLA LE APP");
        styleButton(accessibilityButton, false);
        settingsPanel.addView(accessibilityButton, fullButtonParams());
        accessibilityButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Controllo delle app")
                .setMessage("Se attivi il servizio Accessibilità di NOVA, potrai chiederle di leggere il testo visibile, premere elementi e scrivere nei campi delle app che stai usando. Ogni lettura, tocco o scrittura avviata da NOVA richiede una conferma. Android e le singole app possono limitare alcune operazioni. Il contenuto dello schermo non viene registrato né inviato automaticamente.")
                .setNegativeButton("NON ORA", null)
                .setPositiveButton("APRI IMPOSTAZIONI", (dialog, which) ->
                        startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .show());

        Button mediaButton = new Button(this);
        mediaButton.setText("STUDIO CREAZIONE IMMAGINI AI");
        styleButton(mediaButton, false);
        settingsPanel.addView(mediaButton, fullButtonParams());
        mediaButton.setOnClickListener(v -> startActivity(new Intent(this, com.novatwo.assistant.media.MediaStudioActivity.class)));

        Button analyzeMediaButton = new Button(this);
        analyzeMediaButton.setText("ANALIZZA FOTO O VIDEO CON L'AI");
        styleButton(analyzeMediaButton, false);
        settingsPanel.addView(analyzeMediaButton, fullButtonParams());
        analyzeMediaButton.setOnClickListener(v -> startActivity(new Intent(this,
                com.novatwo.assistant.media.MediaAnalysisActivity.class)));

        Button clearMemoryButton = new Button(this);
        clearMemoryButton.setText("CANCELLA MEMORIA LOCALE");
        styleButton(clearMemoryButton, false);
        settingsPanel.addView(clearMemoryButton, fullButtonParams());
        clearMemoryButton.setOnClickListener(v -> confirmClearMemory());

        TextView privacy = label("Privacy: chiavi e cronologia restano sul telefono. I messaggi vengono inviati al servizio AI scelto per generare le risposte. Le API possono avere costi secondo il piano del provider. NOVA non effettua chiamate a pagamento senza una tua richiesta.", 10,
                MUTED, false);
        privacy.setPadding(0, dp(10), 0, 0);
        settingsPanel.addView(privacy, matchWidth());
        LinearLayout.LayoutParams settingsParams = matchWidth();
        settingsParams.topMargin = dp(10);
        root.addView(settingsPanel, settingsParams);

        settingsToggle.setOnClickListener(v -> {
            settingsPanel.setVisibility(settingsPanel.getVisibility() == View.VISIBLE
                    ? View.GONE : View.VISIBLE);
            settingsToggle.setText(settingsPanel.getVisibility() == View.VISIBLE
                    ? "CHIUDI IMPOSTAZIONI  ×" : "IMPOSTAZIONI  ⚙");
        });

        page.addView(root);
        setContentView(page);
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.rgb(30, 39, 47));
        box.setBackground(bg);
        return box;
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private void styleButton(Button button, boolean primary) {
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? BG : Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(primary ? GREEN : Color.rgb(25, 31, 39));
        if (!primary) bg.setStroke(dp(1), Color.rgb(48, 59, 67));
        button.setBackground(bg);
        button.setPadding(dp(10), 0, dp(10), 0);
    }

    private EditText createKeyInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(MUTED);
        input.setTextSize(13);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return input;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        p.topMargin = dp(8);
        return p;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void loadAIConfiguration() {
        try {
            setSpinnerProvider(keyStore.getSelectedProvider());
            rebuildAIManager();
            updateKeyStatus();
        } catch (Exception e) {
            status.setText("CONFIGURAZIONE DA COMPLETARE");
            providerStatus.setText("Errore nel caricamento delle chiavi: " + safeMessage(e));
        }
    }

    private void updateKeyStatus() throws Exception {
        boolean deep = hasKey("DEEPSEEK");
        boolean open = hasKey("OPENAI");
        boolean gemini = hasKey("GEMINI");
        keyStatus.setText("Chiavi presenti: DeepSeek " + yesNo(deep)
                + "  ·  OpenAI " + yesNo(open) + "  ·  Gemini " + yesNo(gemini));
    }

    private String yesNo(boolean value) {
        return value ? "✓" : "—";
    }

    private boolean hasKey(String provider) throws Exception {
        String key = keyStore.getKey(provider);
        return key != null && !key.trim().isEmpty();
    }

    private void setSpinnerProvider(String provider) {
        String wanted = provider == null ? "AUTO" : provider.toUpperCase(Locale.ROOT);
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
            saveIfProvided("DEEPSEEK", deepSeekInput);
            saveIfProvided("OPENAI", openAIInput);
            saveIfProvided("GEMINI", geminiInput);
            keyStore.setSelectedProvider(selectedProvider());
            deepSeekInput.setText("");
            openAIInput.setText("");
            geminiInput.setText("");
            rebuildAIManager();
            updateKeyStatus();
            Toast.makeText(this, "Configurazione salvata sul dispositivo.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Salvataggio non riuscito: " + safeMessage(e),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void saveIfProvided(String provider, EditText input) throws Exception {
        String value = input.getText().toString().trim();
        if (!value.isEmpty()) keyStore.saveKey(provider, value);
    }

    private void rebuildAIManager() throws Exception {
        aiManager = AIProviderFactory.createManager(
                keyStore.getSelectedProvider(),
                keyStore.getKey("OPENAI"),
                keyStore.getKey("DEEPSEEK"),
                keyStore.getKey("GEMINI"));
        AIProvider provider = aiManager.getProvider();
        String selected = keyStore.getSelectedProvider();
        if (provider == null) {
            status.setText("PRONTA · CONFIGURA UN SERVIZIO AI");
            providerStatus.setText("Nessun provider disponibile per la scelta " + selected + ".");
        } else {
            status.setText("PRONTA · " + provider.getProviderName().toUpperCase(Locale.ROOT));
            providerStatus.setText("Provider attivo: " + provider.getProviderName()
                    + "  ·  modalità " + selected);
        }
    }

    private void confirmDeleteKeys() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminare le chiavi API?")
                .setMessage("Le chiavi salvate sul telefono verranno cancellate. Questa operazione non è reversibile.")
                .setNegativeButton("ANNULLA", null)
                .setPositiveButton("ELIMINA", (dialog, which) -> {
                    keyStore.deleteKey("DEEPSEEK");
                    keyStore.deleteKey("OPENAI");
                    keyStore.deleteKey("GEMINI");
                    keyStore.setSelectedProvider("AUTO");
                    try {
                        setSpinnerProvider("AUTO");
                        rebuildAIManager();
                        updateKeyStatus();
                        Toast.makeText(this, "Chiavi eliminate.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        status.setText("Errore configurazione.");
                    }
                }).show();
    }

    private void confirmClearMemory() {
        new AlertDialog.Builder(this)
                .setTitle("Cancellare la memoria?")
                .setMessage("La cronologia della conversazione e tutte le note locali di NOVA verranno eliminate dal telefono.")
                .setNegativeButton("ANNULLA", null)
                .setPositiveButton("CANCELLA", (dialog, which) -> {
                    memory.clearMemory();
                    new com.novatwo.assistant.actions.LocalNoteStore(this).clear();
                    chatHistory.removeAllViews();
                    TextView empty = label("Memoria cancellata. Ricominciamo da qui.", 14,
                            Color.rgb(220, 226, 232), false);
                    chatHistory.addView(empty, matchWidth());
                    conversation = empty;
                }).show();
    }

    private void restoreConversation() {
        JSONArray messages = memory.getMessages();
        chatHistory.removeAllViews();
        if (messages.length() == 0) {
            TextView welcome = label("Ciao, sono NOVA. Puoi parlarmi oppure scrivermi qui sotto.",
                    14, Color.rgb(220, 226, 232), false);
            chatHistory.addView(welcome, matchWidth());
            conversation = welcome;
            return;
        }
        int start = Math.max(0, messages.length() - 8);
        for (int i = start; i < messages.length(); i++) {
            JSONObject item = messages.optJSONObject(i);
            if (item == null) continue;
            String role = item.optString("role", "user");
            String body = item.optString("content", "");
            addChatLine(role, body);
        }
    }

    private void addChatLine(String role, String body) {
        boolean assistant = "assistant".equals(role);
        TextView line = label((assistant ? "NOVA  ·  " : "TU  ·  ") + body,
                14, assistant ? Color.rgb(226, 235, 233) : Color.rgb(178, 192, 202), false);
        line.setPadding(0, dp(8), 0, dp(8));
        chatHistory.addView(line, matchWidth());
        conversation = line;
    }

    private void updateContinuousVoiceButton() {
        if (continuousVoiceButton != null) {
            continuousVoiceButton.setText(continuousVoiceMode
                    ? "ASCOLTO CONTINUO: ATTIVO · TOCCA PER DISATTIVARE"
                    : "ATTIVA ASCOLTO CONTINUO");
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && textToSpeech != null) {
                int language = textToSpeech.setLanguage(Locale.ITALIAN);
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { }
                    @Override public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            if (continuousVoiceMode && !requestInProgress) startListening();
                        });
                    }
                    @Override public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            if (continuousVoiceMode && !requestInProgress) startListening();
                        });
                    }
                });
            }
        });
    }

    private void initializeSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            listenButton.setEnabled(false);
            listenButton.setText("VOCE NON DISPONIBILE");
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                status.setText("TI STO ASCOLTANDO");
            }
            @Override public void onBeginningOfSpeech() {
                status.setText("TI STO ASCOLTANDO...");
            }
            @Override public void onRmsChanged(float value) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() {
                status.setText("STO PREPARANDO LA RISPOSTA");
            }
            @Override public void onError(int error) {
                setControlsEnabled(true);
                if (continuousVoiceMode && !requestInProgress
                        && (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        || error == SpeechRecognizer.ERROR_NO_MATCH)) {
                    status.setText("ASCOLTO ATTIVO · PUOI PARLARE");
                    if (mainRoot != null) {
                        mainRoot.postDelayed(() -> {
                            if (continuousVoiceMode && !requestInProgress) startListening();
                        }, 700);
                    }
                } else {
                    status.setText("NON HO RICEVUTO LA VOCE · RIPROVA");
                }
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    messageInput.setText(matches.get(0));
                    sendMessage(matches.get(0));
                } else {
                    setControlsEnabled(true);
                    status.setText("NON HO CAPITO · RIPROVA");
                }
            }
            @Override public void onPartialResults(Bundle results) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void startListening() {
        if (requestInProgress) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }
        if (recognizer == null) {
            status.setText("RICONOSCIMENTO VOCALE NON DISPONIBILE");
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        setControlsEnabled(false);
        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            setControlsEnabled(true);
            status.setText("MICROFONO NON DISPONIBILE");
        }
    }

    private void sendTypedMessage() {
        sendMessage(messageInput.getText().toString().trim());
    }

    private void sendMessage(String message) {
        if (message != null && PhoneActionRouter.handle(this, message, result -> runOnUiThread(() -> {
            addChatLine("assistant", result);
            status.setText("OPERAZIONE PRONTA");
            speak(result);
            setControlsEnabled(true);
        }))) {
            addChatLine("user", message);
            memory.saveMessage("user", message);
            messageInput.setText("");
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            status.setText("SCRIVI O PRONUNCIA UN MESSAGGIO");
            setControlsEnabled(true);
            return;
        }
        if (aiManager == null || !aiManager.isProviderAvailable()) {
            status.setText("CONFIGURA UNA CHIAVE API NELLE IMPOSTAZIONI");
            setControlsEnabled(true);
            return;
        }
        AIProvider active = aiManager.getProvider();
        new AlertDialog.Builder(this)
                .setTitle("Confermi l'invio a " + active.getProviderName() + "?")
                .setMessage("La richiesta e gli ultimi messaggi della conversazione verranno inviati al provider AI scelto. Le API possono comportare costi o consumare eventuali crediti, in base al tuo account e al piano attivo. NOVA non può verificare da qui se la tua quota è gratuita. Nessuna richiesta partirà senza questa conferma.")
                .setNegativeButton("ANNULLA", (dialog, which) -> {
                    status.setText("INVIO ANNULLATO");
                    setControlsEnabled(true);
                })
                .setPositiveButton("CONFERMO E INVIA", (dialog, which) -> sendConfirmedAIMessage(message, active))
                .show();
    }

    private String buildConversationPrompt(String currentMessage) {
        JSONArray history = memory.getMessages();
        StringBuilder context = new StringBuilder();
        context.append("Rispondi sempre in italiano, tenendo conto della conversazione recente. ");
        context.append("Non inventare azioni sul telefono che non hai realmente eseguito.\n\n");
        int start = Math.max(0, history.length() - 9);
        for (int i = start; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            if (item == null) continue;
            String role = item.optString("role", "user");
            String content = item.optString("content", "");
            if (content.trim().isEmpty()) continue;
            if (i == history.length() - 1 && content.equals(currentMessage)) continue;
            context.append("assistant".equals(role) ? "NOVA: " : "Utente: ");
            context.append(content).append('\n');
        }
        context.append("Richiesta attuale dell'utente: ").append(currentMessage);
        return context.toString();
    }

    private void sendConfirmedAIMessage(String message, AIProvider active) {
        requestInProgress = true;
        setControlsEnabled(false);
        addChatLine("user", message);
        memory.saveMessage("user", message);
        status.setText("NOVA STA PENSANDO · " + active.getProviderName().toUpperCase(Locale.ROOT));
        aiManager.sendMessage(applyPersonality(buildConversationPrompt(message)), new AIProvider.ResponseCallback() {
            @Override public void onSuccess(String response) {
                runOnUiThread(() -> {
                    requestInProgress = false;
                    addChatLine("assistant", response);
                    memory.saveMessage("assistant", response);
                    status.setText("RISPOSTA PRONTA");
                    speak(response);
                    setControlsEnabled(true);
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    requestInProgress = false;
                    addChatLine("assistant", "Non sono riuscita a completare la richiesta. " + error);
                    status.setText("RICHIESTA NON RIUSCITA");
                    setControlsEnabled(true);
                });
            }
        });
    }


    private void choosePersonality() {
        final String[] modes = {"Assistente quotidiana", "Intimità · dolce e sensuale"};
        new AlertDialog.Builder(this).setTitle("Personalità di NOVA")
                .setSingleChoiceItems(modes,
                        getSharedPreferences("nova_personality", MODE_PRIVATE)
                                .getBoolean("intimacy", false) ? 1 : 0,
                        (dialog, which) -> {
                            getSharedPreferences("nova_personality", MODE_PRIVATE).edit()
                                    .putBoolean("intimacy", which == 1).apply();
                            Toast.makeText(this, which == 1 ? "Modalità Intimità attivata"
                                    : "Modalità quotidiana attivata", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }).setNegativeButton("CHIUDI", null).show();
    }

    private String applyPersonality(String message) {
        boolean intimacy = getSharedPreferences("nova_personality", MODE_PRIVATE)
                .getBoolean("intimacy", false);
        if (!intimacy) return "Rispondi in italiano con personalità calda, gentile, naturale e rispettosa. " + message;
        return "PERSONALITÀ NOVA - MODALITÀ INTIMITÀ: parla in italiano con tono dolce, affettuoso, romantico, complice e sensuale. Puoi flirtare e affrontare conversazioni adulte consensuali senza giudicare. Mantieni chiari i confini tra simulazione e realtà: sei un'assistente virtuale e non provi sensazioni fisiche. Non dichiarare di avere un corpo o esperienze fisiche reali. " + message;
    }

    private void authenticateApp() {
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        BiometricManager manager = BiometricManager.from(this);
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // La protezione biometrica è consigliata, ma non deve rendere l'app inutilizzabile
            // sui dispositivi che non dispongono di sensore o credenziale configurata.
            new AlertDialog.Builder(this).setTitle("Protezione dispositivo non configurata")
                    .setMessage("Puoi usare NOVA anche senza biometria. Per proteggere l'accesso, configura un PIN, una sequenza o un'impronta nelle impostazioni Android.")
                    .setCancelable(false)
                    .setNegativeButton("IMPOSTAZIONI", (d, w) ->
                            startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)))
                    .setPositiveButton("CONTINUA", (d, w) -> {
                        biometricUnlocked = true;
                        status.setText("NOVA PRONTA · PROTEZIONE DISPOSITIVO NON ATTIVA");
                    }).show();
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        biometricUnlocked = true;
                        status.setText("ACCESSO PROTETTO · BENVENUTO");
                    }
                    @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
                        biometricUnlocked = false;
                        Toast.makeText(MainActivity.this, "Accesso non autorizzato", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onAuthenticationFailed() { biometricUnlocked = false; }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sblocca NOVA 2.0")
                .setSubtitle("Usa impronta digitale o credenziale del dispositivo")
                .setAllowedAuthenticators(authenticators).build();
        prompt.authenticate(info);
    }

    private void chooseAppearance() {
        final String[] names = {"Nero profondo", "Blu notte", "Verde scuro", "Viola notte", "Grigio antracite"};
        final int[] colors = {Color.rgb(5,7,11), Color.rgb(5,12,28), Color.rgb(4,20,17),
                Color.rgb(17,8,25), Color.rgb(24,26,30)};
        new AlertDialog.Builder(this).setTitle("Scegli lo sfondo di NOVA")
                .setItems(names, (dialog, which) -> {
                    getSharedPreferences("nova_appearance", MODE_PRIVATE).edit()
                            .putInt("background", colors[which]).apply();
                    applyAppearance();
                }).setNegativeButton("ANNULLA", null).show();
    }

    private void applyAppearance() {
        int color = getSharedPreferences("nova_appearance", MODE_PRIVATE)
                .getInt("background", BG);
        if (mainRoot != null) {
            mainRoot.setBackgroundColor(color);
            View page = (View) mainRoot.getParent();
            if (page != null) page.setBackgroundColor(color);
        }
        getWindow().setStatusBarColor(color);
        getWindow().setNavigationBarColor(color);
    }

    private void chooseMedia() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(pick, "Scegli foto o video"), MEDIA_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7105 && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            PhoneActionRouter.shareSelectedPhoto(this, data.getData());
            return;
        }
        if (requestCode == MEDIA_PICK_REQUEST && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            Uri uri = data.getData();
            Intent share = new Intent(Intent.ACTION_SEND);
            String type = getContentResolver().getType(uri);
            share.setType(type == null ? "*/*" : type);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Condividi foto o video"));
            addChatLine("assistant", "Ho aperto la condivisione del file selezionato. Scegli l'app e conferma tu.");
        }
    }

    private void setControlsEnabled(boolean enabled) {
        if (sendButton != null) sendButton.setEnabled(enabled);
        if (listenButton != null) listenButton.setEnabled(enabled);
    }

    private void speak(String message) {
        if (ttsReady && textToSpeech != null && message != null
                && !message.trim().isEmpty()) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "nova_response");
        } else if (continuousVoiceMode && !requestInProgress) {
            startListening();
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                status.setText("PERMESSO MICROFONO NON CONCESSO");
                setControlsEnabled(true);
            }
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

    private final class OrbView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float rotation = 0f;
        private android.animation.ValueAnimator animator;

        OrbView(Activity context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            animator = android.animation.ValueAnimator.ofFloat(0f, 360f);
            animator.setDuration(24000);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(value -> {
                rotation = (Float) value.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.39f;

            paint.setShader(new RadialGradient(cx, cy, radius * 1.55f,
                    new int[]{0x5530FFD0, 0x2220BFAF, 0x0010A0A0},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius * 1.55f, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.2f));
            paint.setColor(0x6639F5C2);
            canvas.drawCircle(cx, cy, radius * 1.13f, paint);
            paint.setColor(0x3339F5C2);
            canvas.drawCircle(cx, cy, radius * 1.22f, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx - radius * 0.32f, cy - radius * 0.38f,
                    radius * 1.55f,
                    new int[]{0xFF344C50, 0xFF17242B, 0xFF080D13},
                    new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.4f));
            paint.setColor(0xAA91FFE0);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(0x4476EED0);
            paint.setStrokeWidth(dp(5));
            canvas.drawArc(cx - radius + dp(5), cy - radius + dp(5),
                    cx + radius - dp(5), cy + radius - dp(5),
                    rotation, 105, false, paint);

            canvas.save();
            canvas.rotate(rotation * 0.12f, cx, cy);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(radius * 1.18f);
            paint.setColor(GREEN);
            paint.setShadowLayer(dp(18), 0, 0, 0xCC00FFAA);
            canvas.drawText("N", cx, cy + radius * 0.39f, paint);
            paint.clearShadowLayer();
            canvas.restore();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x66FFFFFF);
            canvas.drawCircle(cx - radius * 0.37f, cy - radius * 0.48f, dp(5), paint);
            canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.56f, dp(2), paint);
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            super.onDetachedFromWindow();
        }
    }
}
