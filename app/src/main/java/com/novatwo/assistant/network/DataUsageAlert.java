
package com.novatwo.assistant.network;

import android.content.Context;

public final class DataUsageAlert {

    private DataUsageAlert() {
    }

    public static boolean shouldShowWarning(Context context) {
        return DataUsageLimit.isLimitEnabled(context)
                && DataUsageLimit.isLimitReached(context);
    }

    public static String getWarningMessage(Context context) {

        if (!DataUsageLimit.isLimitEnabled(context)) {
            return "";
        }

        if (DataUsageLimit.isLimitReached(context)) {
            return "Attenzione: NOVA ha raggiunto il limite di consumo dati impostato.";
        }

        long usedMB = DataUsageMonitor.getTotalBytes(context)
                / (1024L * 1024L);

        long limitMB = DataUsageLimit.getLimitMB(context);

        return "Consumo dati NOVA: "
                + usedMB + " MB su "
                + limitMB + " MB.";
    }

    public static boolean canContinueUsingAI(Context context) {
        if (!DataUsageLimit.isLimitEnabled(context)) {
            return true;
        }

        return !DataUsageLimit.isLimitReached(context);
    }
}
