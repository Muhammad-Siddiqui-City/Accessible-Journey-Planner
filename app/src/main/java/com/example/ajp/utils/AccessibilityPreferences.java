package com.example.ajp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Accessibility profile for journey planning. Add in Commit 5.
 * PURPOSE: Step-free, walking speed (slow/average/fast), max walking minutes; passed to TfL Journey API.
 * WHY: JourneyViewModel reads these for accessibilityPreference and maxWalkingMinutes; Settings UI edits them.
 * ISSUES: getMaxWalkingMinutes clamped 5–60 in setter; TfL expects "slow"/"average"/"fast".
 */
public class AccessibilityPreferences {

    private static final String PREFS_NAME = "ajp_accessibility";
    private static final String KEY_STEP_FREE = "step_free";
    private static final String KEY_WALKING_SPEED = "walking_speed";
    private static final String KEY_MAX_WALKING_MINUTES = "max_walking_minutes";

    /** Values for TfL API walkingSpeed parameter. */
    public static final String SPEED_SLOW = "slow";
    public static final String SPEED_AVERAGE = "average";
    public static final String SPEED_FAST = "fast";

    private static final int DEFAULT_MAX_WALKING_MINUTES = 15;

    private final SharedPreferences prefs;

    public AccessibilityPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static AccessibilityPreferences get(Context context) {
        return new AccessibilityPreferences(context);
    }

    public boolean isStepFree() {
        return prefs.getBoolean(KEY_STEP_FREE, false);
    }

    public void setStepFree(boolean enabled) {
        prefs.edit().putBoolean(KEY_STEP_FREE, enabled).apply();
    }

    /** Returns "slow", "average", or "fast" for the TfL API. */
    public String getWalkingSpeed() {
        String v = prefs.getString(KEY_WALKING_SPEED, SPEED_AVERAGE);
        if (SPEED_SLOW.equals(v) || SPEED_FAST.equals(v)) return v;
        return SPEED_AVERAGE;
    }

    public void setWalkingSpeed(String speed) {
        prefs.edit().putString(KEY_WALKING_SPEED, speed != null ? speed : SPEED_AVERAGE).apply();
    }

    public int getMaxWalkingMinutes() {
        return prefs.getInt(KEY_MAX_WALKING_MINUTES, DEFAULT_MAX_WALKING_MINUTES);
    }

    public void setMaxWalkingMinutes(int minutes) {
        int clamped = Math.max(5, Math.min(60, minutes));
        prefs.edit().putInt(KEY_MAX_WALKING_MINUTES, clamped).apply();
    }
}
