
package com.novatwo.assistant.network;

import android.content.Context;
import android.content.SharedPreferences;

public final class DataUsageMonitor {

    private static final String PREFS_NAME = "nova_data_monitor";
    private static final String KEY_REQUEST_COUNT = "request_count";
    private static final String KEY_SENT_BYTES = "sent_bytes";
    private static final String KEY_RECEIVED_BYTES = "received_bytes";

    private DataUsageMonitor() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void recordRequest(
            Context context,
            long sentBytes,
            long receivedBytes
    ) {
        if (sentBytes < 0 || receivedBytes < 0) {
            return;
        }

        SharedPreferences preferences = getPreferences(context);

        preferences.edit()
                .putInt(
                        KEY_REQUEST_COUNT,
                        preferences.getInt(KEY_REQUEST_COUNT, 0) + 1
                )
                .putLong(
                        KEY_SENT_BYTES,
                        preferences.getLong(KEY_SENT_BYTES, 0) + sentBytes
                )
                .putLong(
                        KEY_RECEIVED_BYTES,
                        preferences.getLong(KEY_RECEIVED_BYTES, 0) + receivedBytes
                )
                .apply();
    }

    public static int getRequestCount(Context context) {
        return getPreferences(context).getInt(KEY_REQUEST_COUNT, 0);
    }

    public static long getSentBytes(Context context) {
        return getPreferences(context).getLong(KEY_SENT_BYTES, 0);
    }

    public static long getReceivedBytes(Context context) {
        return getPreferences(context).getLong(KEY_RECEIVED_BYTES, 0);
    }

    public static long getTotalBytes(Context context) {
        return getSentBytes(context) + getReceivedBytes(context);
    }

    public static double getTotalMegabytes(Context context) {
        return getTotalBytes(context) / (1024.0 * 1024.0);
    }

    public static void reset(Context context) {
        getPreferences(context).edit().clear().apply();
    }
}
