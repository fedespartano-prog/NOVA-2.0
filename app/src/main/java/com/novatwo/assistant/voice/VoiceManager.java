
package com.novatwo.assistant.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public class VoiceManager implements TextToSpeech.OnInitListener {

    private final Context context;
    private TextToSpeech textToSpeech;
    private boolean ready = false;

    public VoiceManager(Context context) {
        this.context = context.getApplicationContext();
        textToSpeech = new TextToSpeech(
                this.context,
                this
        );
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(
                    Locale.ITALIAN
            );

            ready = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    public void speak(String message) {
        if (ready && message != null
                && !message.trim().isEmpty()) {
            textToSpeech.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "NOVA_SPEECH"
            );
        }
    }

    public boolean isReady() {
        return ready;
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            ready = false;
        }
    }
}
