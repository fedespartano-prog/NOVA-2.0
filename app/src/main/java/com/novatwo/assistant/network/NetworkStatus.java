
package com.novatwo.assistant.network;

import android.content.Context;

public final class NetworkStatus {

    public enum Status {
        CONNECTED_WIFI,
        CONNECTED_MOBILE,
        CONNECTED_OTHER,
        NO_CONNECTION,
        MOBILE_DATA_DISABLED
    }

    private NetworkStatus() {
    }

    public static Status getStatus(Context context) {

        if (!NetworkConnectionManager.isConnected(context)) {
            return Status.NO_CONNECTION;
        }

        if (NetworkConnectionManager.isUsingWifi(context)) {
            return Status.CONNECTED_WIFI;
        }

        if (NetworkConnectionManager.isUsingMobileData(context)) {

            if (!NetworkSettings.isAIAllowedOnMobile(context)) {
                return Status.MOBILE_DATA_DISABLED;
            }

            return Status.CONNECTED_MOBILE;
        }

        return Status.CONNECTED_OTHER;
    }

    public static String getStatusMessage(Context context) {

        switch (getStatus(context)) {

            case CONNECTED_WIFI:
                return "NOVA è collegata tramite Wi-Fi.";

            case CONNECTED_MOBILE:
                return "NOVA sta usando i dati mobili. Attenzione ai giga.";

            case CONNECTED_OTHER:
                return "NOVA è collegata a Internet.";

            case MOBILE_DATA_DISABLED:
                return "L'utilizzo dell'intelligenza artificiale tramite dati mobili è disattivato.";

            case NO_CONNECTION:
            default:
                return "Nessuna connessione Internet disponibile.";
        }
    }
}
