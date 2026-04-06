package com.example.ajp.utils;

// AI Generated
// Built with Claude
// Lovable.dev reference

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.Manifest;

/**
 * Shared utility class for PermissionManager.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public class PermissionManager {

    private static volatile PermissionManager instance;
    private final Context appContext;

    private PermissionManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static PermissionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PermissionManager.class) {
                if (instance == null) {
                    instance = new PermissionManager(context);
                }
            }
        }
        return instance;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public boolean checkLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context != null ? context : appContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void askLocationPermission(Fragment fragment, int requestCode) {
        fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
    }

    public boolean isPermissionGranted(int[] grantResults) {
        return grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}


