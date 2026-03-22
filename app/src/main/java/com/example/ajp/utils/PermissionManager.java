package com.example.ajp.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.Manifest;

/**
 * Location permission helper. Add in Commit 5 or with first feature that needs location.
 * PURPOSE: checkLocationPermission, askLocationPermission (via Fragment) for ACCESS_FINE_LOCATION.
 * WHY: Used by RouteDetailsActivity (GPS progress) and nearby/location-based features.
 * ISSUES: Singleton holds app context; request code must match Activity onRequestPermissionsResult.
 */
// AI Generated
// Built with Claude
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

    /** Returns true if ACCESS_FINE_LOCATION is granted. */
    public boolean checkLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context != null ? context : appContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Requests ACCESS_FINE_LOCATION via fragment. */
    public void askLocationPermission(Fragment fragment, int requestCode) {
        fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
    }

    /** Returns true if grantResults indicates permission granted. */
    public boolean isPermissionGranted(int[] grantResults) {
        return grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
