
package com.novatwo.assistant.translation;

public final class TranslationResult {

    private final String originalText;
    private final String translatedText;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final long timestamp;

    public TranslationResult(
            String originalText,
            String translatedText,
            String sourceLanguage,
            String targetLanguage
    ) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isValid() {
        return originalText != null
                && !originalText.trim().isEmpty()
                && translatedText != null
                && !translatedText.trim().isEmpty();
    }
}
