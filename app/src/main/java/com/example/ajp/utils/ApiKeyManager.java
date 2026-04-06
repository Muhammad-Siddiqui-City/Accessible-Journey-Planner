package com.example.ajp.utils;

import com.example.ajp.BuildConfig;

/** Reads TfL and National Rail secrets from BuildConfig (from local.properties). */
public final class ApiKeyManager {

    private ApiKeyManager() { }

    public static boolean isTflKeyValid() {
        String k = BuildConfig.TFL_APP_KEY;
        return k != null && !k.trim().isEmpty() && !k.contains("YOUR_KEY");
    }

    public static boolean isRailTokenValid() {
        String t = BuildConfig.RAIL_ACCESS_TOKEN;
        return t != null && !t.trim().isEmpty() && !t.contains("YOUR_TOKEN");
    }

    public static String getTflKey() {
        return BuildConfig.TFL_APP_KEY != null ? BuildConfig.TFL_APP_KEY : "";
    }

    public static String getRailToken() {
        return BuildConfig.RAIL_ACCESS_TOKEN != null ? BuildConfig.RAIL_ACCESS_TOKEN : "";
    }
}

