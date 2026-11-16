
package com.novatwo.assistant.network;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;

public final class NetworkErrorHandler {

    private NetworkErrorHandler() {
    }

    public static String getReadableMessage(Throwable error) {

        if (error == null) {
            return "Si è verificato un errore sconosciuto.";
        }

        if (error instanceof UnknownHostException) {
            return "Nessuna connessione Internet disponibile.";
        }

        if (error instanceof SocketTimeoutException) {
            return "La connessione è troppo lenta. Riprova tra poco.";
        }

        if (error instanceof IOException) {
            return "Errore durante la comunicazione con il servizio AI.";
        }

        return "NOVA non è riuscita a completare la richiesta.";
    }

    public static boolean isConnectionError(Throwable error) {

        return error instanceof UnknownHostException
                || error instanceof SocketTimeoutException
                || error instanceof IOException;
    }

    public static String getRetryMessage() {
        return "Controlla la connessione e riprova.";
    }
}
