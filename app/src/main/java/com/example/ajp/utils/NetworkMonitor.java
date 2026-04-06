package com.example.ajp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.NonNull;

/**
 * Shared utility class for NetworkMonitor.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public class NetworkMonitor {

    private final ConnectivityManager connectivityManager;
    private volatile boolean isOnline = true;

    public NetworkMonitor(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        refreshOnlineState();
    }

    public boolean isOnline() {
        return isOnline;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void refreshOnlineState() {
        if (connectivityManager == null) {
            isOnline = false;
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                isOnline = false;
                return;
            }
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            isOnline = caps != null
                    && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            isOnline = netInfo != null && netInfo.isConnected();
        }
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void registerCallback(@NonNull OnConnectivityChangedListener listener) {
        if (connectivityManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                // Handles a focused part of this feature flow and keeps related logic encapsulated.
                public void onAvailable(@NonNull Network network) {
                    isOnline = true;
                    listener.onConnectivityChanged(true);
                }

                @Override
                // Handles a focused part of this feature flow and keeps related logic encapsulated.
                public void onLost(@NonNull Network network) {
                    refreshOnlineState();
                    listener.onConnectivityChanged(isOnline);
                }
            });
        }
    }

    public interface OnConnectivityChangedListener {
        void onConnectivityChanged(boolean isOnline);
    }
}


