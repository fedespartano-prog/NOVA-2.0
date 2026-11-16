
package com.novatwo.assistant.core;

import android.Manifest;

public final class Constants {

    private Constants() {
        // Classe di sole costanti.
    }

    public static final String APP_NAME = "NOVA 2.0";

    public static final String LOG_TAG = "NOVA";

    public static final String PACKAGE_NAME =
            "com.novatwo.assistant";

    // Permessi Android
    public static final String PERMISSION_INTERNET =
            Manifest.permission.INTERNET;

    public static final String PERMISSION_MICROPHONE =
            Manifest.permission.RECORD_AUDIO;

    // Codici di richiesta
    public static final int REQUEST_MICROPHONE = 1001;

    // Moduli principali
    public static final String MODULE_CORE = "CORE";
    public static final String MODULE_AI = "AI";
    public static final String MODULE_VOICE = "VOICE";
    public static final String MODULE_MEMORY = "MEMORY";

    // Impostazioni generali
    public static final boolean DEBUG = true;

}
