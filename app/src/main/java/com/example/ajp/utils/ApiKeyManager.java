package com.example.ajp.utils;

/**
 * Central place for API keys. Add in Commit 3 with Retrofit and TflApi.
 * Validates before use so the app can show a friendly error instead of crashing when keys
 * are missing (e.g. when running without local.properties).
 */
public final class ApiKeyManager {

    /* --- BLOCK: Key constants ---
     * PURPOSE: Hold TfL API key and National Rail OpenLDBWS token in one place.
     * WHY: Easy to replace for production or leave as placeholder; avoid hardcoding in
     *      multiple files. Check isTflKeyValid() / isRailTokenValid() before calling APIs.
     * ISSUES: Keys in source are visible; for production use buildConfig or env.
     */
    private static final String TFL_KEY = "1de2b729dbee4c0f9a4b5230e2030468";
    private static final String RAIL_TOKEN = "5d4bfb7e-5ed4-4f6d-afc9-4d32de042094";

    private ApiKeyManager() { }

    /* --- BLOCK: Validation and getters ---
     * PURPOSE: Let callers check key presence and get key for interceptors/requests.
     * WHY: isTflKeyValid/isRailTokenValid avoid 403/401 when key is empty or "YOUR_KEY";
     *      getTflKey/getRailToken return non-null string for Retrofit/OkHttp.
     * ISSUES: None.
     */
    public static boolean isTflKeyValid() {
        return TFL_KEY != null && !TFL_KEY.isEmpty() && !TFL_KEY.contains("YOUR_KEY");
    }

    public static boolean isRailTokenValid() {
        return RAIL_TOKEN != null && !RAIL_TOKEN.isEmpty() && !RAIL_TOKEN.contains("YOUR_TOKEN");
    }

    public static String getTflKey() {
        return TFL_KEY != null ? TFL_KEY : "";
    }

    public static String getRailToken() {
        return RAIL_TOKEN != null ? RAIL_TOKEN : "";
    }
}
