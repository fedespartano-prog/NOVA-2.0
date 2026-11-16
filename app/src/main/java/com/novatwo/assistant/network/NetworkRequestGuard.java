
package com.novatwo.assistant.network;

import android.content.Context;

public final class NetworkRequestGuard {

    public enum Decision {
        ALLOWED,
        NO_CONNECTION,
        MOBILE_DATA_BLOCKED,
        EMPTY_REQUEST,
        REQUEST_TOO_LARGE
    }

    private NetworkRequestGuard() {
    }

    public static Decision check(
            Context context,
            String prompt
    ) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return Decision.EMPTY_REQUEST;
        }

        if (!NetworkConnectionManager.isConnected(context)) {
            return Decision.NO_CONNECTION;
        }

        if (NetworkConnectionManager.isUsingMobileData(context)
                && !NetworkSettings.isAIAllowedOnMobile(context)) {
            return Decision.MOBILE_DATA_BLOCKED;
        }

        int maximum = DataUsagePolicy.getMaxPromptCharacters(context);

        if (prompt.trim().length() > maximum) {
            return Decision.REQUEST_TOO_LARGE;
        }

        return Decision.ALLOWED;
    }

    public static String getMessage(Decision decision) {
        switch (decision) {
            case ALLOWED:
                return "Richiesta autorizzata.";

            case NO_CONNECTION:
                return "Controlla la connessione Internet.";

            case MOBILE_DATA_BLOCKED:
                return "Uso dei dati mobili disattivato nelle impostazioni di NOVA.";

            case EMPTY_REQUEST:
                return "Scrivi o pronuncia un messaggio.";

            case REQUEST_TOO_LARGE:
                return "Messaggio troppo lungo per le impostazioni attuali.";

            default:
                return "Richiesta non autorizzata.";
        }
    }
}
