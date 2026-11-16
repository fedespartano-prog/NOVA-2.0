
package com.novatwo.assistant.translation;

import java.util.Locale;

public final class TranslationManager {

    private TranslationManager() {
    }

    public static String getLanguageName(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return "Lingua sconosciuta";
        }

        Locale locale = Locale.forLanguageTag(languageCode.trim());

        String name = locale.getDisplayLanguage(Locale.ITALIAN);

        if (name == null || name.trim().isEmpty()) {
            return "Lingua sconosciuta";
        }

        return name;
    }

    public static boolean isValidLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return false;
        }

        Locale locale = Locale.forLanguageTag(languageCode.trim());

        return !locale.getLanguage().isEmpty();
    }

    public static String prepareTranslationPrompt(
            String text,
            String targetLanguage
    ) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        if (!isValidLanguageCode(targetLanguage)) {
            return "";
        }

        String languageName = getLanguageName(targetLanguage);

        return "Traduci il testo seguente in "
                + languageName
                + ". Rileva automaticamente la lingua originale. "
                + "Mantieni il significato e il tono. "
                + "Restituisci soltanto la traduzione.\n\n"
                + text;
    }
}
