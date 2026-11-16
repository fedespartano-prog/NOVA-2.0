
package com.novatwo.assistant.network;

import android.content.Context;

public final class NetworkRequestManager {

    private NetworkRequestManager() {
    }

    public static boolean canSendRequest(
            Context context,
            String prompt
    ) {
        NetworkRequestGuard.Decision decision =
                NetworkRequestGuard.check(context, prompt);

        return decision == NetworkRequestGuard.Decision.ALLOWED;
    }

    public static String getBlockingReason(
            Context context,
            String prompt
    ) {
        NetworkRequestGuard.Decision decision =
                NetworkRequestGuard.check(context, prompt);

        return NetworkRequestGuard.getMessage(decision);
    }

    public static String prepareRequest(
            Context context,
            String prompt
    ) {
        if (!canSendRequest(context, prompt)) {
            return null;
        }

        return RequestSizeManager.prepareRequest(context, prompt);
    }

    public static boolean requiresMobileWarning(Context context) {
        return NetworkConnectionManager.isUsingMobileData(context)
                && NetworkSettings.shouldWarnAboutMobileData(context);
    }
}
