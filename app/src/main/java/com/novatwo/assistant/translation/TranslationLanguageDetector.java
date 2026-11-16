
package com.novatwo.assistant.translation;

import java.util.Locale;

public final class TranslationLanguageDetector {

    private TranslationLanguageDetector() {
    }

    public static String getDefaultLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public static String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return "";
        }

        Locale locale = Locale.forLanguageTag(languageCode.trim());

        return locale.getLanguage().toLowerCase(Locale.ROOT);
    }

    public static boolean isSupportedLanguage(String languageCode) {
        String normalized = normalizeLanguageCode(languageCode);

        return LanguageCatalog.containsLanguage(normalized);
    }

    public static String getLanguageName(String languageCode) {
        String normalized = normalizeLanguageCode(languageCode);

        if (normalized.isEmpty()) {
            return "Lingua sconosciuta";
        }

        return LanguageCatalog.getLanguageName(normalized);
    }
}
