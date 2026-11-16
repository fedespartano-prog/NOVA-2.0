
package com.novatwo.assistant.network;

import android.content.Context;
import android.widget.Toast;

public final class NetworkNotification {

    private NetworkNotification() {
    }

    public static void showConnectionStatus(Context context) {
        show(context, NetworkStatus.getStatusMessage(context));
    }

    public static void showMobileDataWarning(Context context) {
        if (NetworkSettings.shouldWarnAboutMobileData(context)
                && NetworkConnectionManager.isUsingMobileData(context)) {
            show(context,
                    "Attenzione: NOVA sta utilizzando i tuoi giga mobili.");
        }
    }

    public static void showNoConnection(Context context) {
        if (!NetworkConnectionManager.isConnected(context)) {
            show(context, "Internet non disponibile. Controlla la connessione.");
        }
    }

    public static void show(Context context, String message) {
        if (context == null || message == null || message.trim().isEmpty()) {
            return;
        }

        Toast.makeText(
                context.getApplicationContext(),
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
