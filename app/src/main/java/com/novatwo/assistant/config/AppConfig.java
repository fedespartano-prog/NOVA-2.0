
package com.novatwo.assistant.config;

public final class AppConfig {

    private AppConfig() {
        // Impedisce la creazione di istanze.
    }

    public static final String APP_NAME = "NOVA 2.0";

    public static final String LOG_TAG = "NOVA";

    public static final String CORE_PACKAGE =
            "com.novatwo.assistant.core";

    public static final boolean DEBUG_MODE = false;

    public static final String AI_PROVIDER = "GEMINI";

    public static final boolean REQUIRE_PERMISSION_FOR_ACTIONS = true;

    public static final boolean LOCAL_MEMORY_ENABLED = true;

}
