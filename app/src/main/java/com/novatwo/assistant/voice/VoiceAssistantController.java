
package com.novatwo.assistant.voice;

import android.app.Activity;

import com.novatwo.assistant.ai.AIManager;
import com.novatwo.assistant.ai.AIProvider;

public class VoiceAssistantController {

    public interface ConversationListener {
        void onListeningStarted();
        void onListeningStopped();
        void onUserMessage(String message);
        void onAssistantResponse(String response);
        void onError(String error);
    }

    private final VoiceInputManager voiceInput;
    private final VoiceManager voiceOutput;
    private final AIManager aiManager;

    private ConversationListener listener;
    private boolean processing = false;

    public VoiceAssistantController(
            Activity activity,
            AIManager aiManager
    ) {
        this.voiceInput = new VoiceInputManager(activity);
        this.voiceOutput = new VoiceManager(activity);
        this.aiManager = aiManager;
    }

    public void setListener(ConversationListener listener) {
        this.listener = listener;
    }

    public void startListening() {

        if (processing) {
            return;
        }

        voiceInput.startListening(
                new VoiceInputManager.VoiceCallback() {

            @Override
            public void onListeningStarted() {
                if (listener != null) {
                    listener.onListeningStarted();
                }
            }

            @Override
            public void onListeningStopped() {
                if (listener != null) {
                    listener.onListeningStopped();
                }
            }

            @Override
            public void onResult(String text) {
                handleUserMessage(text);
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError(error);
                }
            }
        });
    }

    private void handleUserMessage(String message) {

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        if (listener != null) {
            listener.onUserMessage(message);
        }

        if (aiManager == null ||
                !aiManager.isProviderAvailable()) {

            if (listener != null) {
                listener.onError(
                        "Nessun servizio AI configurato."
                );
            }

            return;
        }

        processing = true;

        aiManager.sendMessage(
                message,
                new AIProvider.ResponseCallback() {

            @Override
            public void onSuccess(String response) {
                processing = false;

                if (listener != null) {
                    listener.onAssistantResponse(response);
                }

                voiceOutput.speak(response);
            }

            @Override
            public void onError(String error) {
                processing = false;

                if (listener != null) {
                    listener.onError(error);
                }
            }
        });
    }

    public void stopListening() {
        voiceInput.stopListening();
    }

    public void stopSpeaking() {
        voiceOutput.stop();
    }

    public void shutdown() {
        voiceInput.shutdown();
        voiceOutput.shutdown();
        processing = false;
        listener = null;
    }
}
