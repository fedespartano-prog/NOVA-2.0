
package com.novatwo.assistant.network;

public final class NetworkRetryPolicy {

    private static final int MAX_RETRIES = 2;

    private NetworkRetryPolicy() {
    }

    public static int getMaximumRetries() {
        return MAX_RETRIES;
    }

    public static boolean shouldRetry(
            int currentAttempt,
            Throwable error
    ) {
        if (currentAttempt >= MAX_RETRIES) {
            return false;
        }

        return NetworkErrorHandler.isConnectionError(error);
    }

    public static long getRetryDelayMillis(int attempt) {

        if (attempt <= 0) {
            return 1000L;
        }

        if (attempt == 1) {
            return 2000L;
        }

        return 4000L;
    }

    public static String getRetryDescription() {
        return "NOVA può riprovare al massimo due volte "
                + "in caso di errore di connessione.";
    }
}
