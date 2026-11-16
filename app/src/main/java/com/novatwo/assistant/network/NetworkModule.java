
package com.novatwo.assistant.network;

import android.content.Context;

public final class NetworkModule {

    private final Context context;
    private final DataUsageController dataController;

    public NetworkModule(Context context) {
        this.context = context.getApplicationContext();
        this.dataController = new DataUsageController(this.context);
    }

    public boolean isInternetAvailable() {
        return NetworkConnectionManager.isConnected(context);
    }

    public boolean isUsingMobileData() {
        return NetworkConnectionManager.isUsingMobileData(context);
    }

    public boolean isUsingWifi() {
        return NetworkConnectionManager.isUsingWifi(context);
    }

    public boolean canSendAIRequest(String prompt) {
        return dataController.canSendAIRequest(prompt);
    }

    public String prepareAIRequest(String prompt) {
        if (!canSendAIRequest(prompt)) {
            return null;
        }

        return NetworkRequestManager.prepareRequest(
                context,
                prompt
        );
    }

    public String getConnectionStatus() {
        return NetworkStatus.getStatusMessage(context);
    }

    public String getDataUsageReport() {
        return dataController.getUsageReport();
    }

    public void setMobileDataAllowed(boolean allowed) {
        NetworkSettings.setAIAllowedOnMobile(context, allowed);
    }

    public void setDataLimit(long megabytes) {
        DataUsageLimit.setLimitMB(context, megabytes);
    }

    public void enableDataLimit(boolean enabled) {
        DataUsageLimit.setLimitEnabled(context, enabled);
    }

    public void resetDataUsage() {
        dataController.resetUsageCounters();
    }
}
