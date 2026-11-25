package com.novatwo.assistant;

import android.app.Activity;
import android.Manifest;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
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
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
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
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends FragmentActivity {

    private static final int AUDIO_PERMISSION = 100;
    private static final int BG = Color.rgb(3, 7, 13);
    private static final int PANEL = Color.rgb(8, 16, 25);
    private static final int GREEN = Color.rgb(0, 190, 255);
    private static final int VIOLET = Color.rgb(65, 205, 255);
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
    private EditText groqInput;
    private EditText grokInput;
    private Button callToggleButton;
    private Spinner providerSpinner;
    private Button listenButton;
    private Button sendButton;
    private LinearLayout settingsPanel;
    private LinearLayout chatHistory;
    private boolean ttsReady;
    private boolean requestInProgress;
    private boolean recognitionActive = false;
    private BroadcastReceiver voiceReceiver;
    private boolean voiceReceiverRegistered = false;
    private LinearLayout mainRoot;
    private OrbView orbView;
    private static final int MEDIA_PICK_REQUEST = 7110;
    private boolean biometricUnlocked = false;
    private final java.util.Set<String> sessionApprovedProviders = new java.util.HashSet<>();

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
        registerVoiceReceiver();
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

        TextView version = label("2.0  /  PERSONAL AI", 10, VIOLET, true);
        version.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(version);
        Button settingsToggle = new Button(this);
        settingsToggle.setText("⚙");
        settingsToggle.setContentDescription("Apri impostazioni NOVA");
        styleIconButton(settingsToggle, 23);
        settingsToggle.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams gearParams = new LinearLayout.LayoutParams(dp(48), dp(44));
        gearParams.leftMargin = dp(12);
        header.addView(settingsToggle, gearParams);
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

        orbView = new OrbView(this);
        LinearLayout.LayoutParams orbParams = new LinearLayout.LayoutParams(dp(248), dp(248));
        orbParams.gravity = Gravity.CENTER;
        orbParams.topMargin = dp(1);
        orbParams.bottomMargin = dp(1);
        root.addView(orbView, orbParams);

        status = label("PRONTA QUANDO VUOI", 10, GREEN, true);
        status.setGravity(Gravity.CENTER);
        status.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams statusParams = wrap();
        statusParams.topMargin = dp(2);
        statusParams.bottomMargin = dp(8);
        root.addView(status, statusParams);

        callToggleButton = new Button(this);
        updateContinuousVoiceButton();
        styleIconButton(callToggleButton, 30);
        callToggleButton.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams callParams = new LinearLayout.LayoutParams(dp(64), dp(58));
        callParams.gravity = Gravity.CENTER_HORIZONTAL;
        callParams.bottomMargin = dp(10);
        root.addView(callToggleButton, callParams);
        callToggleButton.setOnClickListener(v -> {
            if (continuousVoiceMode) {
                continuousVoiceMode = false;
                getSharedPreferences("nova_voice", MODE_PRIVATE).edit()
                        .putBoolean("continuous", false).putBoolean("session_active", false).apply();
                if (textToSpeech != null) textToSpeech.stop();
                stopVoiceSession();
                status.setText("CHIAMATA TERMINATA · MICROFONO RILASCIATO");
            } else {
                continuousVoiceMode = true;
                getSharedPreferences("nova_voice", MODE_PRIVATE).edit()
                        .putBoolean("continuous", true).apply();
                status.setText("APERTURA CHIAMATA...");
                startListening();
            }
            updateContinuousVoiceButton();
        });

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

        LinearLayout chatDialogContent = new LinearLayout(this);
        chatDialogContent.setOrientation(LinearLayout.VERTICAL);
        chatDialogContent.setPadding(dp(12), dp(12), dp(12), dp(12));
        chatDialogContent.setBackgroundColor(BG);
        ScrollView historyScroll = new ScrollView(this);
        historyScroll.setFillViewport(false);
        historyScroll.addView(chatCard);
        chatDialogContent.addView(historyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(330), 1));
        LinearLayout.LayoutParams composerDialogParams = matchWidth();
        composerDialogParams.topMargin = dp(10);
        chatDialogContent.addView(composer, composerDialogParams);

        Button chatToggle = new Button(this);
        chatToggle.setText("☰");
        chatToggle.setContentDescription("Apri conversazione");
        styleIconButton(chatToggle, 27);
        chatToggle.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams chatToggleParams = new LinearLayout.LayoutParams(dp(58), dp(54));
        chatToggleParams.gravity = Gravity.CENTER_HORIZONTAL;
        chatToggleParams.topMargin = dp(5);
        root.addView(chatToggle, chatToggleParams);
        chatToggle.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("NOVA · CONVERSAZIONE")
                    .setView(chatDialogContent)
                    .setNegativeButton("CHIUDI", null)
                    .create();
            dialog.setOnDismissListener(ignored -> {
                if (chatDialogContent.getParent() instanceof ViewGroup) {
                    ((ViewGroup) chatDialogContent.getParent()).removeView(chatDialogContent);
                }
            });
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });

        TextView providerLine = label("AI · GEMINI", 10, MUTED, false);
        providerLine.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams providerParams = wrap();
        providerParams.topMargin = dp(13);
        root.addView(providerLine, providerParams);

        settingsPanel = panel();
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setPadding(dp(15), dp(15), dp(15), dp(15));
        settingsPanel.setVisibility(View.VISIBLE);
        TextView settingsTitle = label("COLLEGA I TUOI SERVIZI AI", 13, Color.WHITE, true);
        settingsPanel.addView(settingsTitle, wrap());

        providerStatus = label("Provider: non configurato", 11, MUTED, false);
        providerStatus.setPadding(0, dp(6), 0, dp(8));
        settingsPanel.addView(providerStatus, matchWidth());

        providerSpinner = new Spinner(this);
        String[] providers = {"GEMINI · UNICO PROVIDER ATTIVO"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, providers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(adapter);
        settingsPanel.addView(providerSpinner, matchWidth());

        keyStatus = label("Le chiavi salvate sono protette sul dispositivo.", 10, MUTED, false);
        keyStatus.setPadding(0, dp(8), 0, dp(3));
        settingsPanel.addView(keyStatus, matchWidth());

        geminiInput = createKeyInput("Incolla qui la chiave API Gemini");
        settingsPanel.addView(geminiInput, matchWidth());

        TextView keyHint = label("NOVA usa soltanto Gemini. La chiave resta salvata in modo protetto sul telefono.", 10,
                MUTED, false);
        settingsPanel.addView(keyHint, matchWidth());

        Button saveButton = new Button(this);
        saveButton.setText("SALVA CONFIGURAZIONE");
        styleButton(saveButton, true);
        settingsPanel.addView(saveButton, fullButtonParams());
        saveButton.setOnClickListener(v -> saveAIConfiguration());

        Button resetProviderConsentButton = new Button(this);
        resetProviderConsentButton.setText("GESTISCI AUTORIZZAZIONI AI");
        styleButton(resetProviderConsentButton, false);
        settingsPanel.addView(resetProviderConsentButton, fullButtonParams());
        resetProviderConsentButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Autorizzazioni dei servizi AI")
                    .setMessage("Le autorizzazioni ricordate permettono a NOVA di inviare i normali messaggi al provider selezionato senza chiedere conferma ogni volta. Vuoi revocare tutte le autorizzazioni?")
                    .setNegativeButton("MANTIENI", null)
                    .setPositiveButton("REVOCA TUTTE", (d, w) -> {
                        getSharedPreferences("nova_provider_consent", MODE_PRIVATE).edit().clear().apply();
                        sessionApprovedProviders.clear();
                        Toast.makeText(this, "Autorizzazioni AI revocate.", Toast.LENGTH_SHORT).show();
                    }).show();
        });

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
            getSharedPreferences("nova_voice", MODE_PRIVATE).edit().putBoolean("continuous", continuousVoiceMode).apply();
            updateContinuousVoiceButton();
            if (!continuousVoiceMode) {
                stopVoiceSession();
                setControlsEnabled(true);
                status.setText("CHIAMATA NOVA TERMINATA");
            } else {
                status.setText("CHIAMATA NOVA ATTIVA");
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
        ScrollView settingsScroll = new ScrollView(this);
        settingsScroll.setFillViewport(false);
        settingsScroll.setBackgroundColor(BG);
        settingsScroll.addView(settingsPanel);
        settingsToggle.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("NOVA · IMPOSTAZIONI")
                    .setView(settingsScroll)
                    .setNegativeButton("CHIUDI", null)
                    .create();
            dialog.setOnDismissListener(ignored -> {
                if (settingsScroll.getParent() instanceof ViewGroup) {
                    ((ViewGroup) settingsScroll.getParent()).removeView(settingsScroll);
                }
            });
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }
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

    private void styleIconButton(Button button, float textSize) {
        button.setAllCaps(false);
        button.setTextSize(textSize);
        button.setTextColor(GREEN);
        button.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setElevation(0f);
        button.setStateListAnimator(null);
    }

    private void styleButton(Button button, boolean primary) {
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setLetterSpacing(0.035f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? Color.rgb(2, 12, 20) : Color.rgb(225, 246, 255));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(15));
        bg.setColor(primary ? Color.rgb(0, 177, 239) : Color.rgb(8, 25, 39));
        bg.setStroke(dp(primary ? 1.5f : 1.2f),
                primary ? Color.rgb(91, 224, 255) : Color.rgb(0, 145, 205));
        button.setBackground(bg);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setElevation(dp(primary ? 5 : 2));
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
            // NOVA in questa versione usa esclusivamente Gemini.
            // Non leggere le vecchie chiavi degli altri provider: un dato
            // cifrato non valido di un provider inattivo non deve bloccare Gemini.
            keyStore.setSelectedProvider("GEMINI");
            setSpinnerProvider("GEMINI");
            rebuildAIManager();
            updateKeyStatus();
        } catch (Exception e) {
            aiManager = new AIManager();
            status.setText("CONFIGURAZIONE GEMINI DA VERIFICARE");
            providerStatus.setText("NOVA non riesce a leggere la chiave Gemini salvata: " + safeMessage(e));
            if (keyStatus != null) keyStatus.setText("Chiave Gemini: lettura non riuscita");
        }
    }

    private void updateKeyStatus() throws Exception {
        boolean gemini = hasKey("GEMINI");
        keyStatus.setText("Chiave Gemini sul telefono: " + (gemini ? "SALVATA" : "NON SALVATA"));
    }

    private String yesNo(boolean value) {
        return value ? "✓" : "—";
    }

    private boolean hasKey(String provider) throws Exception {
        String key = keyStore.getKey(provider);
        return key != null && !key.trim().isEmpty();
    }

    private void setSpinnerProvider(String provider) {
        if (providerSpinner != null) providerSpinner.setSelection(0);
    }

    private String selectedProvider() {
        return "GEMINI";
    }

    private void saveAIConfiguration() {
        try {
            saveIfProvided("GEMINI", geminiInput);
            keyStore.setSelectedProvider("GEMINI");
            String savedKey = keyStore.getKey("GEMINI");
            if (savedKey == null || savedKey.trim().isEmpty()) {
                throw new IllegalStateException("La chiave Gemini non risulta salvata sul telefono.");
            }
            geminiInput.setText("");
            rebuildAIManager();
            updateKeyStatus();
            Toast.makeText(this, "Chiave Gemini salvata e riletta correttamente sul telefono.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Salvataggio Gemini non riuscito: " + safeMessage(e),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void saveIfProvided(String provider, EditText input) throws Exception {
        String value = input.getText().toString().trim();
        if (!value.isEmpty()) keyStore.saveKey(provider, value);
    }

    private void rebuildAIManager() throws Exception {
        // Legge soltanto la chiave Gemini: le altre chiavi già presenti
        // sul telefono restano intatte ma non partecipano alla configurazione.
        String geminiKey = keyStore.getKey("GEMINI");
        aiManager = AIProviderFactory.createManager(
                "GEMINI",
                null,
                null,
                geminiKey,
                null,
                null);
        AIProvider provider = aiManager.getProvider();
        String selected = "GEMINI";
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
                    keyStore.deleteKey("GROQ");
                    keyStore.deleteKey("GROK");
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
        Linkify.addLinks(line, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
        line.setLinksClickable(true);
        line.setMovementMethod(LinkMovementMethod.getInstance());
        chatHistory.addView(line, matchWidth());
        conversation = line;
    }

    private void updateContinuousVoiceButton() {
        if (continuousVoiceButton != null) {
            continuousVoiceButton.setText(continuousVoiceMode
                    ? "ASCOLTO CONTINUO: ATTIVO · TOCCA PER DISATTIVARE"
                    : "ATTIVA ASCOLTO CONTINUO");
        }
        if (callToggleButton != null) {
            callToggleButton.setText(continuousVoiceMode ? "✕" : "🎙");
            callToggleButton.setContentDescription(continuousVoiceMode ? "Chiudi chiamata" : "Apri chiamata");
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && textToSpeech != null) {
                int language = textToSpeech.setLanguage(Locale.forLanguageTag("it-IT"));
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
                if (ttsReady) {
                    textToSpeech.setSpeechRate(0.94f);
                    textToSpeech.setPitch(1.02f);
                    if (android.os.Build.VERSION.SDK_INT >= 21 && textToSpeech.getVoices() != null) {
                        android.speech.tts.Voice bestItalian = null;
                        for (android.speech.tts.Voice voice : textToSpeech.getVoices()) {
                            if (voice == null || !voice.getLocale().getLanguage().equals("it")) continue;
                            if (voice.isNetworkConnectionRequired()) continue;
                            if (bestItalian == null || voice.getQuality() > bestItalian.getQuality()) bestItalian = voice;
                        }
                        if (bestItalian != null) textToSpeech.setVoice(bestItalian);
                    }
                }
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {
                        runOnUiThread(() -> { if (orbView != null) orbView.setMode(OrbView.SPEAKING); });
                    }
                    @Override public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            if (orbView != null) orbView.setMode(continuousVoiceMode ? OrbView.LISTENING : OrbView.IDLE);
                            if (continuousVoiceMode && !requestInProgress) resumeVoiceSession();
                        });
                    }
                    @Override public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            if (orbView != null) orbView.setMode(OrbView.IDLE);
                            if (continuousVoiceMode && !requestInProgress && !recognitionActive) startListening();
                        });
                    }
                });
            }
        });
    }

    private void registerVoiceReceiver() {
        if (voiceReceiverRegistered) return;
        voiceReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (com.novatwo.assistant.voice.NovaVoiceService.ACTION_USER_SPEECH.equals(action)) {
                    if (orbView != null) orbView.setMode(OrbView.LISTENING);
                    if (textToSpeech != null && textToSpeech.isSpeaking()) textToSpeech.stop();
                    return;
                }
                if (com.novatwo.assistant.voice.NovaVoiceService.ACTION_TRANSCRIPT.equals(action)) {
                    String phrase = intent.getStringExtra(com.novatwo.assistant.voice.NovaVoiceService.EXTRA_TEXT);
                    if (phrase == null || phrase.trim().isEmpty()) { resumeVoiceSession(); return; }
                    String command = phrase.toLowerCase(Locale.ROOT);
                    if (command.contains("chiudi chiamata") || command.contains("termina chiamata")
                            || command.contains("smetti di ascoltare") || command.contains("disattiva ascolto")) {
                        continuousVoiceMode = false;
                        getSharedPreferences("nova_voice", MODE_PRIVATE).edit().putBoolean("continuous", false).putBoolean("session_active", false).apply();
                        updateContinuousVoiceButton();
                        stopVoiceSession();
                        addChatLine("assistant", "Va bene, chiudo la chiamata e rilascio il microfono.");
                        status.setText("CHIAMATA TERMINATA");
                        speak("Va bene, chiudo la chiamata.");
                        return;
                    }
                    if (messageInput != null) messageInput.setText(phrase);
                    sendMessage(phrase);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.novatwo.assistant.voice.NovaVoiceService.ACTION_TRANSCRIPT);
        filter.addAction(com.novatwo.assistant.voice.NovaVoiceService.ACTION_USER_SPEECH);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(voiceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(voiceReceiver, filter);
        voiceReceiverRegistered = true;
    }

    private void resumeVoiceSession() {
        if (!continuousVoiceMode) return;
        Intent intent = new Intent(this, com.novatwo.assistant.voice.NovaVoiceService.class);
        intent.setAction(com.novatwo.assistant.voice.NovaVoiceService.ACTION_RESUME);
        try { startService(intent); } catch (Exception ignored) { }
    }

    private void stopVoiceSession() {
        Intent intent = new Intent(this, com.novatwo.assistant.voice.NovaVoiceService.class);
        intent.setAction(com.novatwo.assistant.voice.NovaVoiceService.ACTION_STOP);
        try { startService(intent); } catch (Exception ignored) { }
        getSharedPreferences("nova_voice", MODE_PRIVATE).edit().putBoolean("session_active", false).apply();
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
                recognitionActive = true;
                if (orbView != null) orbView.setMode(OrbView.LISTENING);
                status.setText("TI STO ASCOLTANDO");
            }
            @Override public void onBeginningOfSpeech() {
                if (continuousVoiceMode && textToSpeech != null && textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
                }
                status.setText("TI STO ASCOLTANDO...");
            }
            @Override public void onRmsChanged(float value) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() {
                if (orbView != null) orbView.setMode(OrbView.THINKING);
                status.setText("STO PREPARANDO LA RISPOSTA");
            }
            @Override public void onError(int error) {
                recognitionActive = false;
                if (orbView != null) orbView.setMode(continuousVoiceMode ? OrbView.LISTENING : OrbView.IDLE);
                setControlsEnabled(true);
                if (continuousVoiceMode && !requestInProgress
                        && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    status.setText(error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                            || error == SpeechRecognizer.ERROR_NO_MATCH
                            ? "ASCOLTO ATTIVO · PUOI PARLARE"
                            : "RIPRISTINO DEL MICROFONO...");
                    if (mainRoot != null) {
                        long retryDelay = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1500 : 900;
                        mainRoot.postDelayed(() -> {
                            if (continuousVoiceMode && !requestInProgress && !recognitionActive) startListening();
                        }, retryDelay);
                    }
                } else {
                    status.setText("MICROFONO NON DISPONIBILE · CONTROLLA I PERMESSI");
                }
            }
            @Override public void onResults(Bundle results) {
                recognitionActive = false;
                if (orbView != null) orbView.setMode(OrbView.THINKING);
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    messageInput.setText(matches.get(0));
                    sendMessage(matches.get(0));
                } else {
                    setControlsEnabled(true);
                    status.setText("NON HO CAPITO · RIPROVA");
                    if (continuousVoiceMode && !requestInProgress) {
                        mainRoot.postDelayed(() -> startListening(), 600);
                    }
                }
            }
            @Override public void onPartialResults(Bundle results) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void startListening() {
        if (requestInProgress) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            continuousVoiceMode = false;
            getSharedPreferences("nova_voice", MODE_PRIVATE).edit().putBoolean("continuous", false).apply();
            updateContinuousVoiceButton();
            status.setText("RICONOSCIMENTO VOCALE NON DISPONIBILE SU QUESTO TELEFONO");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }
        Intent intent = new Intent(this, com.novatwo.assistant.voice.NovaVoiceService.class);
        intent.setAction(com.novatwo.assistant.voice.NovaVoiceService.ACTION_START);
        try {
            androidx.core.content.ContextCompat.startForegroundService(this, intent);
            getSharedPreferences("nova_voice", MODE_PRIVATE).edit().putBoolean("session_active", true).apply();
            if (orbView != null) orbView.setMode(OrbView.LISTENING);
            status.setText("CHIAMATA ATTIVA · NOVA TI ASCOLTA ANCHE IN BACKGROUND");
            setControlsEnabled(true);
        } catch (Exception e) {
            continuousVoiceMode = false;
            getSharedPreferences("nova_voice", MODE_PRIVATE).edit()
                    .putBoolean("continuous", false).putBoolean("session_active", false).apply();
            updateContinuousVoiceButton();
            status.setText("IMPOSSIBILE AVVIARE LA CHIAMATA VOCALE");
            Toast.makeText(this, "Controlla il permesso microfono e le notifiche.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendTypedMessage() {
        sendMessage(messageInput.getText().toString().trim());
    }

    private void sendMessage(String message) {
        AIProvider selectedForSearch = aiManager == null ? null : aiManager.getProvider();
        String normalizedMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);
        boolean deepSeekWebSearch = selectedForSearch != null
                && "deepseek".equalsIgnoreCase(selectedForSearch.getProviderName())
                && (normalizedMessage.startsWith("cerca su internet ")
                || normalizedMessage.startsWith("cerca sul web ")
                || normalizedMessage.startsWith("cerca online ")
                || normalizedMessage.startsWith("fai una ricerca web "));
        if (message != null && !deepSeekWebSearch && PhoneActionRouter.handle(this, message, result -> runOnUiThread(() -> {
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
        String providerConsentKey = active.getProviderName().toLowerCase(Locale.ROOT);
        String providerNameLower = active.getProviderName().toLowerCase(Locale.ROOT);
        boolean requiresPerRequestConsent = !providerNameLower.equals("groq");
        boolean providerAlreadyApproved = !requiresPerRequestConsent
                && getSharedPreferences("nova_provider_consent", MODE_PRIVATE)
                .getBoolean(providerConsentKey, false);
        if (providerAlreadyApproved) {
            sendConfirmedAIMessage(message, active);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confermi l'invio a " + active.getProviderName() + "?")
                .setMessage("La richiesta e il contesto della conversazione verranno inviati a " + active.getProviderName() + ". "
                        + (requiresPerRequestConsent
                        ? "ATTENZIONE: questo servizio API può addebitare i token o il consumo al tuo account, in base al piano e al credito disponibili. Questa singola richiesta partirà solo dopo la tua conferma. NOVA ti chiederà conferma ogni volta."
                        : "Confermando, NOVA ricorderà l'autorizzazione Groq. Verifica di usare il piano gratuito e non attivare crediti o pagamenti.")
                        + " NOVA non passa automaticamente ad altri provider.")
                .setNegativeButton("ANNULLA", (dialog, which) -> {
                    status.setText("INVIO ANNULLATO");
                    setControlsEnabled(true);
                    if (continuousVoiceMode) mainRoot.postDelayed(() -> startListening(), 500);
                })
                .setPositiveButton(requiresPerRequestConsent ? "AUTORIZZA SOLO QUESTA VOLTA" : "AUTORIZZA SEMPRE E INVIA", (dialog, which) -> {
                    if (!requiresPerRequestConsent) {
                        getSharedPreferences("nova_provider_consent", MODE_PRIVATE).edit()
                                .putBoolean(providerConsentKey, true).apply();
                        sessionApprovedProviders.add(providerConsentKey);
                    }
                    sendConfirmedAIMessage(message, active);
                })
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
        if (orbView != null) orbView.setMode(OrbView.THINKING);
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
                    if (orbView != null) orbView.setMode(OrbView.IDLE);
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
        if (!intimacy) return "Parla in italiano come una persona vera in una conversazione spontanea: tono caldo, presente, empatico e naturale. Usa frasi scorrevoli, varia il ritmo, evita formule da call center, elenchi non richiesti e ripetizioni. Sii diretta e colloquiale senza risultare artificiale. Non leggere emoji o simboli come parole. " + message;
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
                        startVoiceAfterUnlock();
                    }).show();
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        biometricUnlocked = true;
                        status.setText("ACCESSO PROTETTO · BENVENUTO");
                        startVoiceAfterUnlock();
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

    private void startVoiceAfterUnlock() {
        if (continuousVoiceMode && mainRoot != null) {
            mainRoot.postDelayed(() -> {
                if (!isFinishing() && continuousVoiceMode && !requestInProgress) startListening();
            }, 500);
        }
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

    private String cleanTextForSpeech(String text) {
        if (text == null) return "";
        StringBuilder cleaned = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            offset += Character.charCount(cp);
            int type = Character.getType(cp);
            boolean emoji = (cp >= 0x1F000 && cp <= 0x1FAFF)
                    || (cp >= 0x2600 && cp <= 0x27BF)
                    || (cp >= 0x1F1E6 && cp <= 0x1F1FF)
                    || cp == 0xFE0F || cp == 0xFE0E || cp == 0x200D
                    || cp == 0x20E3 || (cp >= 0xE0020 && cp <= 0xE007F);
            if (!emoji && type != Character.FORMAT) cleaned.appendCodePoint(cp);
        }
        return cleaned.toString()
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }

    private void speak(String message) {
        String spokenText = cleanTextForSpeech(message);
        if (ttsReady && textToSpeech != null && !spokenText.trim().isEmpty()) {
            if (orbView != null) orbView.setMode(OrbView.SPEAKING);
            textToSpeech.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "nova_response");
        } else {
            if (orbView != null) orbView.setMode(continuousVoiceMode ? OrbView.LISTENING : OrbView.IDLE);
            if (continuousVoiceMode && !requestInProgress) resumeVoiceSession();
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
                continuousVoiceMode = false;
                getSharedPreferences("nova_voice", MODE_PRIVATE).edit()
                        .putBoolean("continuous", false).putBoolean("session_active", false).apply();
                updateContinuousVoiceButton();
                status.setText("PERMESSO MICROFONO NON CONCESSO");
                setControlsEnabled(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        recognitionActive = false;
        if (voiceReceiverRegistered && voiceReceiver != null) {
            try { unregisterReceiver(voiceReceiver); } catch (Exception ignored) { }
            voiceReceiverRegistered = false;
        }
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
        static final int IDLE = 0, LISTENING = 1, SPEAKING = 2, THINKING = 3;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private android.animation.ValueAnimator animator;
        private float phase = 0f;
        private int mode = IDLE;

        OrbView(Activity context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(6200L);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(value -> {
                phase = (Float) value.getAnimatedValue() * (float) (Math.PI * 2.0);
                invalidate();
            });
            animator.start();
        }

        void setMode(int nextMode) {
            if (mode != nextMode) { mode = nextMode; invalidate(); }
        }

        private float wave(float offset, float speed) {
            return (float) Math.sin(phase * speed + offset);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w=getWidth(), h=getHeight();
            if(w<=0 || h<=0)return;
            float cx=w*0.5f, baseY=h*0.77f;
            float bob=(float)Math.sin(phase)*dp(mode==SPEAKING?5f:3f);
            float pulse=mode==SPEAKING?1f+0.045f*wave(0,3f):mode==LISTENING?1f+0.025f*wave(0,1.8f):mode==THINKING?1f+0.015f*wave(0,1.2f):1f+0.012f*wave(0,0.8f);
            float radius=Math.min(w*0.31f,h*0.285f)*pulse, cy=h*0.39f+bob;

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx,cy,radius*2f,new int[]{mode==SPEAKING?0x8840DFFF:0x6640CFFF,0x223080FF,0x00001020},null,Shader.TileMode.CLAMP));
            canvas.drawCircle(cx,cy,radius*2f,paint); paint.setShader(null);

            float energy=mode==SPEAKING?1.35f:mode==LISTENING?1.12f:1f;
            for(int i=0;i<5;i++){
                float travel=(phase*(0.22f+i*0.035f)+i*0.22f)%1f;
                float rx=radius*(0.82f+travel*1.05f)*energy;
                float ry=dp(5f+i*1.2f)*(0.78f+travel*0.35f);
                int alpha=Math.max(18,(int)(125*(1f-travel)*(mode==SPEAKING?1.25f:1f)));
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(i==0?2.1f:1.15f));
                paint.setColor((alpha<<24)|0x004DCFFF);
                canvas.drawOval(cx-rx,baseY-ry,cx+rx,baseY+ry,paint);
            }
            paint.setStrokeWidth(dp(1.4f)); paint.setColor(0x6650CFFF);
            canvas.drawOval(cx-radius*1.52f,baseY-dp(13),cx+radius*1.52f,baseY+dp(13),paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx-radius*0.34f,cy-radius*0.43f,radius*1.55f,
                    new int[]{0xAA557F9B,0x8820344B,0xCC050D18},new float[]{0f,0.48f,1f},Shader.TileMode.CLAMP));
            canvas.drawCircle(cx,cy,radius,paint); paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2f)); paint.setColor(0xCC8DEBFF);
            canvas.drawCircle(cx,cy,radius,paint);
            paint.setStrokeWidth(dp(5f)); paint.setColor(mode==SPEAKING?0xCC00E8FF:0x9950BFFF);
            canvas.drawArc(cx-radius+dp(3),cy-radius+dp(3),cx+radius-dp(3),cy+radius-dp(3),phase*57.2958f,92f,false,paint);

            canvas.save(); canvas.rotate((float)Math.sin(phase*0.55f)*13f+phase*8f,cx,cy);
            paint.setStrokeWidth(dp(1.15f)); paint.setColor(0x8873DFFF);
            canvas.drawOval(cx-radius*1.10f,cy-radius*0.35f,cx+radius*1.10f,cy+radius*0.35f,paint);
            canvas.restore();

            paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeWidth(dp(3.2f)); paint.setColor(0xCCFFFFFF);
            float glint=(float)Math.sin(phase*0.8f)*radius*0.12f;
            canvas.drawArc(cx-radius*0.86f+glint,cy-radius*0.86f,cx+radius*0.86f+glint,cy+radius*0.86f,218f,48f,false,paint);

            canvas.save(); canvas.rotate((float)Math.sin(phase*0.45f)*2.5f,cx,cy);
            paint.setStyle(Paint.Style.FILL); paint.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(radius*1.27f); paint.setColor(0xFF8AF2FF);
            paint.setShadowLayer(dp(mode==SPEAKING?23f:16f),0,0,mode==SPEAKING?0xFF00CFFF:0xCC00AFFF);
            canvas.drawText("N",cx,cy+radius*0.40f,paint); paint.clearShadowLayer(); canvas.restore();

            paint.setStyle(Paint.Style.FILL); paint.setColor(0xAAFFFFFF);
            float sparkleX=cx-radius*(0.42f+0.05f*wave(0.8f,0.7f));
            canvas.drawCircle(sparkleX,cy-radius*0.58f,dp(3.2f),paint);
            paint.setColor(0x6688DFFF); canvas.drawCircle(cx+radius*0.72f,cy-radius*0.12f,dp(1.8f),paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }

        @Override protected void onDetachedFromWindow() {
            if(animator!=null)animator.cancel();
            super.onDetachedFromWindow();
        }
    }
}
