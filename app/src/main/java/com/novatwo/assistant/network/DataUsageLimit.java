
package com.novatwo.assistant.network;

import android.content.Context;
import android.content.SharedPreferences;

public final class DataUsageLimit {

    private static final String PREFS_NAME = "nova_data_limit";
    private static final String KEY_LIMIT_MB = "limit_mb";
    private static final String KEY_LIMIT_ENABLED = "limit_enabled";

    private static final long DEFAULT_LIMIT_MB = 100;

    private DataUsageLimit() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setLimitMB(Context context, long megabytes) {
        if (megabytes < 1) {
            return;
        }

        getPreferences(context).edit()
                .putLong(KEY_LIMIT_MB, megabytes)
                .apply();
    }

    public static long getLimitMB(Context context) {
        return getPreferences(context).getLong(
                KEY_LIMIT_MB,
                DEFAULT_LIMIT_MB
        );
    }

    public static void setLimitEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_LIMIT_ENABLED, enabled)
                .apply();
    }

    public static boolean isLimitEnabled(Context context) {
        return getPreferences(context).getBoolean(
                KEY_LIMIT_ENABLED,
                false
        );
    }

    public static boolean isLimitReached(Context context) {
        if (!isLimitEnabled(context)) {
            return false;
        }

        long usedBytes = DataUsageMonitor.getTotalBytes(context);
        long limitBytes = getLimitMB(context) * 1024L * 1024L;

        return usedBytes >= limitBytes;
    }

    public static void reset(Context context) {
        getPreferences(context).edit().clear().apply();
    }
}
