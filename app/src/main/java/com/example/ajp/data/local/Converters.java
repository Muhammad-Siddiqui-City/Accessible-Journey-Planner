package com.example.ajp.data.local;

import androidx.room.TypeConverter;
import com.example.ajp.ui.journey.RouteItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Room TypeConverters. Add in Commit 4 with AppDatabase.
 * Converts RouteItem to/from JSON for stored routes (complex object in one column).
 */
public class Converters {

    private static final Gson GSON = new Gson();

    /* --- BLOCK: RouteItem → JSON ---
     * PURPOSE: Persist RouteItem in a single text column.
     * WHY: Gson handles nested objects; Room cannot store arbitrary objects without a converter.
     * ISSUES: RouteItem must be serialisable (no Android-only types that Gson can't handle).
     */
    @TypeConverter
    public static String fromRouteItem(RouteItem route) {
        if (route == null) return null;
        return GSON.toJson(route);
    }

    /* --- BLOCK: JSON → RouteItem ---
     * PURPOSE: Read stored route back into RouteItem.
     * WHY: TypeToken<RouteItem>() for correct generic type with Gson.
     * ISSUES: Returns null for null/empty string to match Room contract.
     */
    @TypeConverter
    public static RouteItem toRouteItem(String json) {
        if (json == null || json.isEmpty()) return null;
        return GSON.fromJson(json, new TypeToken<RouteItem>() {}.getType());
    }
}
