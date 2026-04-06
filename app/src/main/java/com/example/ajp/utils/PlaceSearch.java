package com.example.ajp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import com.example.ajp.ui.nearby.StopItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared utility class for PlaceSearch.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public final class PlaceSearch {

    private static final double BBOX_SOUTH = 51.2;
    private static final double BBOX_WEST = -0.5;
    private static final double BBOX_NORTH = 51.7;
    private static final double BBOX_EAST = 0.3;
    private static final int MAX_RESULTS = 5;

    private static final String[] PLACE_BADGE = new String[] { "Place" };

    /** UK locale so address lines stay Latin/English (avoids RTL script in labels when the device is Arabic, etc.). */
    private static final Locale GEOCODER_LOCALE = Locale.UK;

    private PlaceSearch() { }

    /**
     * Geocodes in the London bbox; used to scan multiple results for journey resolution.
     */
    public static List<Address> geocodeInLondonBbox(Context context, String query, int maxResults) {
        if (context == null || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String q = query.trim();
        Geocoder geocoder = new Geocoder(context, GEOCODER_LOCALE);
        if (!geocoder.isPresent()) {
            return new ArrayList<>();
        }
        try {
            List<Address> addresses = geocoder.getFromLocationName(q, maxResults,
                    BBOX_SOUTH, BBOX_WEST, BBOX_NORTH, BBOX_EAST);
            return addresses != null ? addresses : new ArrayList<>();
        } catch (IOException ignored) {
            return new ArrayList<>();
        }
    }

    /**
     * First geocoded address in the Greater London bbox, or null if none (used for journey endpoints).
     */
    public static Address geocodeFirstAddress(Context context, String query) {
        List<Address> list = geocodeInLondonBbox(context, query, 1);
        if (list.isEmpty()) {
            return null;
        }
        Address a = list.get(0);
        return (a.hasLatitude() && a.hasLongitude()) ? a : null;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static List<StopItem> searchPlaces(Context context, String query) {
        if (context == null || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String q = query.trim();
        List<Address> addresses = geocodeInLondonBbox(context, q, MAX_RESULTS);
        List<StopItem> out = new ArrayList<>();
        if (addresses.isEmpty()) {
            return out;
        }
        for (Address addr : addresses) {
            if (addr.hasLatitude() && addr.hasLongitude()) {
                String id = addr.getLatitude() + "," + addr.getLongitude();
                String name = buildAddressLine(addr);
                out.add(new StopItem(id, name, 0, PLACE_BADGE, false, "", false));
            }
        }
        return out;
    }

    /** Builds a short label for journey UI (also used when resolving typed addresses). */
    public static String buildAddressLine(Address addr) {
        if (addr.getFeatureName() != null && !addr.getFeatureName().trim().isEmpty()) {
            String feature = addr.getFeatureName().trim();
            if (addr.getThoroughfare() != null && !addr.getThoroughfare().trim().isEmpty()
                    && !addr.getThoroughfare().trim().equalsIgnoreCase(feature)) {
                return feature + ", " + addr.getThoroughfare().trim();
            }
            return feature;
        }
        if (addr.getThoroughfare() != null && !addr.getThoroughfare().trim().isEmpty()) {
            return addr.getThoroughfare().trim();
        }
        if (addr.getAddressLine(0) != null && !addr.getAddressLine(0).trim().isEmpty()) {
            return addr.getAddressLine(0).trim();
        }
        return addr.getLatitude() + ", " + addr.getLongitude();
    }
}


