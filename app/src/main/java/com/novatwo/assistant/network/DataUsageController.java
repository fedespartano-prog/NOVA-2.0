
package com.novatwo.assistant.network;

import android.content.Context;

public final class DataUsageController {

    private final Context context;

    public DataUsageController(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean canSendAIRequest(String prompt) {

        if (!NetworkRequestManager.canSendRequest(context, prompt)) {
            return false;
        }

        if (!DataUsageAlert.canContinueUsingAI(context)) {
            return false;
        }

        return true;
    }

    public String getBlockingReason(String prompt) {

        if (!NetworkRequestManager.canSendRequest(context, prompt)) {
            return NetworkRequestManager.getBlockingReason(
                    context,
                    prompt
            );
        }

        if (!DataUsageAlert.canContinueUsingAI(context)) {
            return DataUsageAlert.getWarningMessage(context);
        }

        return "Richiesta autorizzata.";
    }

    public String getUsageReport() {
        return DataUsageReport.generate(context);
    }

    public void resetUsageCounters() {
        DataUsageMonitor.reset(context);
    }
}
