
package com.novatwo.assistant.network;

import android.content.Context;
import java.util.Locale;

public final class NetworkInfo {

    private NetworkInfo() {
    }

    public static String getConnectionType(Context context) {

        if (!NetworkConnectionManager.isConnected(context)) {
            return "Nessuna connessione";
        }

        if (NetworkConnectionManager.isUsingWifi(context)) {
            return "Wi-Fi";
        }

        if (NetworkConnectionManager.isUsingMobileData(context)) {
            return "Dati mobili";
        }

        return "Altra connessione";
    }

    public static String getDataUsage(Context context) {

        double megabytes =
                DataUsageMonitor.getTotalMegabytes(context);

        return String.format(
                Locale.ITALIAN,
                "%.2f MB",
                megabytes
        );
    }

    public static String getNetworkSummary(Context context) {

        return "Connessione: "
                + getConnectionType(context)
                + "\nRichieste AI: "
                + DataUsageMonitor.getRequestCount(context)
                + "\nDati registrati: "
                + getDataUsage(context);
    }

    public static boolean isMetered(Context context) {
        return NetworkConnectionManager.isConnectionMetered(context);
    }
}
