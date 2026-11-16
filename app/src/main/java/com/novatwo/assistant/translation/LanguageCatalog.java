
package com.novatwo.assistant.translation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public final class LanguageCatalog {

    private static final Map<String, String> LANGUAGES;

    static {
        Map<String, String> languages = new LinkedHashMap<>();

        languages.put("it", "Italiano");
        languages.put("en", "Inglese");
        languages.put("es", "Spagnolo");
        languages.put("fr", "Francese");
        languages.put("de", "Tedesco");
        languages.put("pt", "Portoghese");
        languages.put("nl", "Olandese");
        languages.put("ru", "Russo");
        languages.put("uk", "Ucraino");
        languages.put("pl", "Polacco");
        languages.put("ro", "Rumeno");
        languages.put("el", "Greco");
        languages.put("tr", "Turco");
        languages.put("ar", "Arabo");
        languages.put("zh", "Cinese");
        languages.put("ja", "Giapponese");
        languages.put("ko", "Coreano");
        languages.put("hi", "Hindi");
        languages.put("bn", "Bengalese");
        languages.put("sv", "Svedese");
        languages.put("no", "Norvegese");
        languages.put("da", "Danese");
        languages.put("fi", "Finlandese");
        languages.put("cs", "Ceco");
        languages.put("sk", "Slovacco");
        languages.put("hu", "Ungherese");
        languages.put("he", "Ebraico");
        languages.put("id", "Indonesiano");
        languages.put("th", "Thailandese");
        languages.put("vi", "Vietnamita");

        LANGUAGES = Collections.unmodifiableMap(languages);
    }

    private LanguageCatalog() {
    }

    public static Map<String, String> getLanguages() {
        return LANGUAGES;
    }

    public static String getLanguageName(String code) {
        if (code == null) {
            return "Lingua sconosciuta";
        }

        String name = LANGUAGES.get(code.trim().toLowerCase());

        return name != null ? name : "Lingua non presente nel catalogo";
    }

    public static boolean containsLanguage(String code) {
        return code != null
                && LANGUAGES.containsKey(code.trim().toLowerCase());
    }
}
