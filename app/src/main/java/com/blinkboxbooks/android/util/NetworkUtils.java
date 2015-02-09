// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Simple helper class for network status
 */
public class NetworkUtils {

    private static BroadcastReceiver sNetworkStateReceiver;

    /**
     * Check the internet connectivity
     *
     * @return true if the device has internet connectivity
     */
    public static boolean hasInternetConnectivity(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnected());
    }

    /**
     * Is using a wifi connection
     *
     * @return true if the device is currently connected with wifi
     */
    public static boolean isConnectedWithWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return (wifiInfo != null && wifiInfo.isConnected());
    }

    /**
     * Unregister a network status Observer to track the network connection.
     *
     * @param context The Context in which the observer is attached.
     */
    public static void unRegisterNetworkStatusObserver(Context context) {
        context.unregisterReceiver(sNetworkStateReceiver);
    }
}