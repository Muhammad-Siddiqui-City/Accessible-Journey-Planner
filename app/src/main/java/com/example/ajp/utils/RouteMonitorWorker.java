package com.example.ajp.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Periodic job: compares latest journey options to the saved signature (TfL may change routes).
 */
public class RouteMonitorWorker extends Worker {

    public RouteMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        RouteMonitorScheduler.runCheckSync(getApplicationContext());
        return Result.success();
    }
}
