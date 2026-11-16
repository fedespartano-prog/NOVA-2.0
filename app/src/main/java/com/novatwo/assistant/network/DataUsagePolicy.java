
package com.novatwo.assistant.network;

import android.content.Context;
import android.content.SharedPreferences;

public final class DataUsagePolicy {

    private static final String PREFS_NAME = "nova_data_settings";
    private static final String KEY_MOBILE_DATA_ALLOWED =
            "mobile_data_allowed";

    private static final int MAX_MOBILE_PROMPT_CHARACTERS = 4000;
    private static final int MAX_WIFI_PROMPT_CHARACTERS = 12000;

    private DataUsagePolicy() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isMobileDataAllowed(Context context) {
        return getPreferences(context).getBoolean(
                KEY_MOBILE_DATA_ALLOWED,
                true
        );
    }

    public static void setMobileDataAllowed(
            Context context,
            boolean allowed
    ) {
        getPreferences(context)
                .edit()
                .putBoolean(KEY_MOBILE_DATA_ALLOWED, allowed)
                .apply();
    }

    public static boolean canUseAI(Context context) {
        if (!NetworkConnectionManager.isConnected(context)) {
            return false;
        }

        if (NetworkConnectionManager.isUsingMobileData(context)) {
            return isMobileDataAllowed(context);
        }

        return true;
    }

    public static boolean shouldWarnAboutData(Context context) {
        return NetworkConnectionManager.isConnectionMetered(context);
    }

    public static int getMaxPromptCharacters(Context context) {
        if (NetworkConnectionManager.isConnectionMetered(context)) {
            return MAX_MOBILE_PROMPT_CHARACTERS;
        }

        return MAX_WIFI_PROMPT_CHARACTERS;
    }

    public static String preparePrompt(
            Context context,
            String prompt
    ) {
        if (prompt == null) {
            return "";
        }

        String cleaned = prompt.trim();

        int maximum = getMaxPromptCharacters(context);

        if (cleaned.length() > maximum) {
            cleaned = cleaned.substring(0, maximum);
        }

        return cleaned;
    }
}
