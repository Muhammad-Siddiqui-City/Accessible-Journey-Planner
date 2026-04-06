package com.example.ajp.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Schedules/cancels route monitor work and runs a one-off check for settings tests. */
public final class RouteMonitorScheduler {

    private static final String UNIQUE_WORK_NAME = "ajp_route_monitor";

    private RouteMonitorScheduler() {}

    // Coordinates background execution timing and guards against duplicate work.
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

        }
        return false;
    }
}

