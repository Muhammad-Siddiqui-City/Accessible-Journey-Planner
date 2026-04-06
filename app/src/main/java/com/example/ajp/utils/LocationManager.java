package com.example.ajp.utils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

/** Single-flight fused location reads for Home / Journey current-location. */
public class LocationManager {

    private static final double FALLBACK_LAT = 51.5072;
    private static final double FALLBACK_LON = -0.1276;
    private static final long TIMEOUT_MS = 5000;

    private static volatile LocationManager instance;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context appContext;
    private final PermissionManager permissionManager;

    private LocationManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);
        this.permissionManager = PermissionManager.getInstance(appContext);
    }

    public static LocationManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LocationManager.class) {
                if (instance == null) {
                    instance = new LocationManager(context);
                }
            }
        }
        return instance;
    }

    public interface LocationCallback {
        void onLocationReceived(double lat, double lon);
        void onLocationFailed();
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (!permissionManager.checkLocationPermission(appContext)) {
            callback.onLocationFailed();
            return;
        }

        final boolean[] delivered = { false };
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (delivered[0]) return;
            delivered[0] = true;
            Toast.makeText(appContext, "⚠️ GPS Timeout! Using Default London.", Toast.LENGTH_LONG).show();
            callback.onLocationReceived(FALLBACK_LAT, FALLBACK_LON);
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        Task<Location> task = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null);
        task.addOnSuccessListener(location -> {
            if (location == null || delivered[0]) return;
            delivered[0] = true;
            timeoutHandler.removeCallbacks(timeoutRunnable);
            callback.onLocationReceived(location.getLatitude(), location.getLongitude());
        });
        task.addOnFailureListener(e -> {
            if (delivered[0]) return;
            delivered[0] = true;
            timeoutHandler.removeCallbacks(timeoutRunnable);
            callback.onLocationReceived(FALLBACK_LAT, FALLBACK_LON);
        });
    }
}

