
package com.novatwo.assistant.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public final class NetworkConnectionManager {

    private NetworkConnectionManager() {
    }

    private static ConnectivityManager getManager(Context context) {
        return (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager manager = getManager(context);

        if (manager == null) {
            return false;
        }

        Network network = manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);

        return capabilities != null
                && capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                && capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                );
    }

    public static boolean isUsingMobileData(Context context) {
        ConnectivityManager manager = getManager(context);

        if (manager == null) {
            return false;
        }

        Network network = manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);

        return capabilities != null
                && capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                );
    }

    public static boolean isUsingWifi(Context context) {
        ConnectivityManager manager = getManager(context);

        if (manager == null) {
            return false;
        }

        Network network = manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);

        return capabilities != null
                && capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                );
    }

    public static boolean isConnectionMetered(Context context) {
        ConnectivityManager manager = getManager(context);

        return manager != null
                && manager.isActiveNetworkMetered();
    }
}
