
package com.novatwo.assistant.network;

import android.content.Context;

public final class InternetAccessManager {

    private InternetAccessManager() {
    }

    public static boolean hasInternet(Context context) {
        return NetworkConnectionManager.isConnected(context);
    }

    public static boolean canAccessAI(Context context) {
        return hasInternet(context)
                && NetworkSettings.isAIAllowedOnMobile(context)
                || hasInternet(context)
                && !NetworkConnectionManager.isUsingMobileData(context);
    }

    public static boolean isMobileConnection(Context context) {
        return NetworkConnectionManager.isUsingMobileData(context);
    }

    public static boolean isWifiConnection(Context context) {
        return NetworkConnectionManager.isUsingWifi(context);
    }

    public static String getConnectionDescription(Context context) {
        return NetworkStatus.getStatusMessage(context);
    }

    public static boolean requiresDataWarning(Context context) {
        return NetworkSettings.shouldWarnAboutMobileData(context)
                && isMobileConnection(context);
    }
}
