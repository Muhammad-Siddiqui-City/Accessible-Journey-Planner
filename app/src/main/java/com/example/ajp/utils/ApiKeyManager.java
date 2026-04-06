package com.example.ajp.utils;

import com.example.ajp.BuildConfig;

/**
 * Shared utility class for ApiKeyManager.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

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


