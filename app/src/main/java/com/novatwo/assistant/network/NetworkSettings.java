
package com.novatwo.assistant.network;

import android.content.Context;
import android.content.SharedPreferences;

public final class NetworkSettings {

    private static final String PREFS_NAME = "nova_network_settings";

    private static final String KEY_WARN_MOBILE =
            "warn_mobile_data";

    private static final String KEY_ALLOW_AI_ON_MOBILE =
            "allow_ai_on_mobile";

    private static final String KEY_CONFIRM_LARGE_REQUESTS =
            "confirm_large_requests";

    private NetworkSettings() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean shouldWarnAboutMobileData(Context context) {
        return getPreferences(context).getBoolean(
                KEY_WARN_MOBILE,
                true
        );
    }

    public static void setWarnAboutMobileData(
            Context context,
            boolean enabled
    ) {
        getPreferences(context).edit()
                .putBoolean(KEY_WARN_MOBILE, enabled)
                .apply();
    }

    public static boolean isAIAllowedOnMobile(Context context) {
        return getPreferences(context).getBoolean(
                KEY_ALLOW_AI_ON_MOBILE,
                true
        );
    }

    public static void setAIAllowedOnMobile(
            Context context,
            boolean allowed
    ) {
        getPreferences(context).edit()
                .putBoolean(KEY_ALLOW_AI_ON_MOBILE, allowed)
                .apply();

        DataUsagePolicy.setMobileDataAllowed(context, allowed);
    }

    public static boolean shouldConfirmLargeRequests(Context context) {
        return getPreferences(context).getBoolean(
                KEY_CONFIRM_LARGE_REQUESTS,
                true
        );
    }

    public static void setConfirmLargeRequests(
            Context context,
            boolean enabled
    ) {
        getPreferences(context).edit()
                .putBoolean(KEY_CONFIRM_LARGE_REQUESTS, enabled)
                .apply();
    }

    public static void reset(Context context) {
        getPreferences(context).edit().clear().apply();

        DataUsagePolicy.setMobileDataAllowed(context, true);
    }
}
