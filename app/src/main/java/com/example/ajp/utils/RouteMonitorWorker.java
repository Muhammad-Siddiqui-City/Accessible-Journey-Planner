package com.example.ajp.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Shared utility class for RouteMonitorWorker.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public class RouteMonitorWorker extends Worker {

    public RouteMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public Result doWork() {
        RouteMonitorScheduler.runCheckSync(getApplicationContext());
        return Result.success();
    }
}


