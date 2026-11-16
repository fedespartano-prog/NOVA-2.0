
package com.novatwo.assistant.core;

import android.content.Context;
import android.util.Log;

public class Core {

    private static final String TAG = "NOVA_CORE";

    private final Context context;
    private boolean initialized = false;

    public Core(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        Log.i(TAG, "NOVA 2.0 CORE inizializzato");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Context getContext() {
        return context;
    }

    public void shutdown() {
        initialized = false;
        Log.i(TAG, "NOVA CORE arrestato");
    }
}
