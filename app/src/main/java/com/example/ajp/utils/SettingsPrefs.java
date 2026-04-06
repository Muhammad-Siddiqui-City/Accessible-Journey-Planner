package com.example.ajp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Shared utility class for SettingsPrefs.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public class SettingsPrefs {

    private static final String PREFS_NAME = "ajp_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_HIGH_CONTRAST = "high_contrast";
    private static final String KEY_LARGE_TEXT = "large_text";
    private static final String KEY_TTS = "text_to_speech";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DISRUPTION_ALERTS = "disruption_alerts";
    private static final String KEY_CROWDING_UPDATES = "crowding_updates";
    private static final String KEY_AVOID_CROWDED = "avoid_crowded";

    public static final String LANG_EN_GB = "en_GB";
    public static final String LANG_ES = "es";
    public static final String LANG_ZH = "zh";
    public static final String LANG_AR = "ar";

    private final SharedPreferences prefs;

    public SettingsPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Returns data from local state or derives a value needed by callers.
    public static SettingsPrefs get(Context context) {
        return new SettingsPrefs(context);
    }

    public boolean isDarkMode() { return prefs.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean on) { prefs.edit().putBoolean(KEY_DARK_MODE, on).apply(); }

    public boolean isHighContrast() { return prefs.getBoolean(KEY_HIGH_CONTRAST, false); }
    public void setHighContrast(boolean on) { prefs.edit().putBoolean(KEY_HIGH_CONTRAST, on).apply(); }

    public boolean isLargeText() { return prefs.getBoolean(KEY_LARGE_TEXT, false); }
    public void setLargeText(boolean on) { prefs.edit().putBoolean(KEY_LARGE_TEXT, on).apply(); }

    public boolean isTtsEnabled() { return prefs.getBoolean(KEY_TTS, false); }
    public void setTtsEnabled(boolean on) { prefs.edit().putBoolean(KEY_TTS, on).apply(); }

    public boolean isDisruptionAlerts() { return prefs.getBoolean(KEY_DISRUPTION_ALERTS, false); }
    public void setDisruptionAlerts(boolean on) { prefs.edit().putBoolean(KEY_DISRUPTION_ALERTS, on).apply(); }

    public boolean isCrowdingUpdates() { return prefs.getBoolean(KEY_CROWDING_UPDATES, false); }
    public void setCrowdingUpdates(boolean on) { prefs.edit().putBoolean(KEY_CROWDING_UPDATES, on).apply(); }

    public boolean isAvoidCrowded() { return prefs.getBoolean(KEY_AVOID_CROWDED, false); }
    public void setAvoidCrowded(boolean on) { prefs.edit().putBoolean(KEY_AVOID_CROWDED, on).apply(); }

    public String getLanguage() {
        String v = prefs.getString(KEY_LANGUAGE, LANG_EN_GB);
        return v != null ? v.trim() : LANG_EN_GB;
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang != null ? lang.trim() : LANG_EN_GB).commit();
    }
}


