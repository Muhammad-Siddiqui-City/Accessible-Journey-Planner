package com.example.ajp.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/**
 * Schedules periodic re-check of the last journey against TfL (WorkManager).
 * WHY: User rules prefer WorkManager for background work; manual test uses {@link #runCheckSync(Context)}.
 */
public final class RouteMonitorScheduler {

    private static final String UNIQUE_WORK_NAME = "ajp_route_monitor";

    private RouteMonitorScheduler() {}

    /** Enqueue ~15-minute periodic work when a search completes (idempotent). */
    public static void schedule(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                        RouteMonitorWorker.class,
                        15,
                        TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                work);
    }

    /**
     * Re-fetch routes for the last saved search; returns true if the signature changed (e.g. disruption).
     */
    public static boolean runCheckSync(@NonNull Context context) {
        RouteMonitorPrefs prefs = RouteMonitorPrefs.get(context);
        String from = prefs.getLastFrom();
        String to = prefs.getLastTo();
        String time = prefs.getLastTime();
        String date = prefs.getLastDate();
        if (from.isEmpty() || to.isEmpty() || time.isEmpty() || date.isEmpty()) {
            return false;
        }
        String oldSig = prefs.getLastSignature();
        try {
            JourneyFetcher fetcher = new JourneyFetcher(context);
            JourneyFetcher.FetchResult result = fetcher.fetch(from, to, time, date);
            if (!result.isSuccess() || result.getRoutes().isEmpty()) {
                return false;
            }
            String newSig = JourneyFetcher.buildSignature(result.getRoutes());
            if (!newSig.equals(oldSig)) {
                prefs.setLastSignature(newSig);
                return true;
            }
        } catch (Exception ignored) {
            // Graceful fallback: no change reported
        }
        return false;
    }
}
