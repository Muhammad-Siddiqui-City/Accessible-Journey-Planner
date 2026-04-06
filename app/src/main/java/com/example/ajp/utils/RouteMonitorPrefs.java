package com.example.ajp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared utility class for RouteMonitorPrefs.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public final class RouteMonitorPrefs {

    private static final String PREFS_NAME = "ajp_route_monitor";
    private static final String KEY_LAST_FROM = "last_from";
    private static final String KEY_LAST_TO = "last_to";
    private static final String KEY_LAST_TIME = "last_time";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_LAST_SIGNATURE = "last_signature";
    private static final String KEY_SIMULATED_STOPS = "simulated_stop_ids";

    private final SharedPreferences prefs;

    private RouteMonitorPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Returns data from local state or derives a value needed by callers.
    public static RouteMonitorPrefs get(Context context) {
        return new RouteMonitorPrefs(context);
    }

    public void setLastSearch(String from, String to, String timeHHmm, String dateyyyyMMdd) {
        prefs.edit()
                .putString(KEY_LAST_FROM, from != null ? from : "")
                .putString(KEY_LAST_TO, to != null ? to : "")
                .putString(KEY_LAST_TIME, timeHHmm != null ? timeHHmm : "")
                .putString(KEY_LAST_DATE, dateyyyyMMdd != null ? dateyyyyMMdd : "")
                .apply();
    }

    public String getLastFrom() {
        return prefs.getString(KEY_LAST_FROM, "");
    }

    public String getLastTo() {
        return prefs.getString(KEY_LAST_TO, "");
    }

    public String getLastTime() {
        return prefs.getString(KEY_LAST_TIME, "");
    }

    public String getLastDate() {
        return prefs.getString(KEY_LAST_DATE, "");
    }

    public void setLastSignature(String signature) {
        prefs.edit().putString(KEY_LAST_SIGNATURE, signature != null ? signature : "").apply();
    }

    public String getLastSignature() {
        return prefs.getString(KEY_LAST_SIGNATURE, "");
    }

    public Set<String> getSimulatedDisruptedStopIds() {
        Set<String> raw = prefs.getStringSet(KEY_SIMULATED_STOPS, null);
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        return new HashSet<>(raw);
    }

    public void setSimulatedDisruptedStopIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            prefs.edit().remove(KEY_SIMULATED_STOPS).apply();
        } else {
            prefs.edit().putStringSet(KEY_SIMULATED_STOPS, new HashSet<>(ids)).apply();
        }
    }
}


