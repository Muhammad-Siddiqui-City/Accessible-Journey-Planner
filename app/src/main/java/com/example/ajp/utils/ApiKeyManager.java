package com.example.ajp.utils;

import com.example.ajp.BuildConfig;

/**
 * Central place for API keys. Add in Commit 3 with Retrofit and TflApi.
 * Values come from {@code local.properties} at build time via {@link BuildConfig} — never commit real keys.
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
