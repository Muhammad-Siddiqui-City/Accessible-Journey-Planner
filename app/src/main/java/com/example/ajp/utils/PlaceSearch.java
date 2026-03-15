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
 * Geocoder-based place search. Add in Commit 14.
 * PURPOSE: Search by place name (e.g. "McDonalds") within London bbox; return as StopItem with id "lat,lon".
 * WHY: TfL only has stations; Geocoder gives addresses/POIs; "lat,lon" used as journey from/to in JourneyViewModel.
 * ISSUES: Geocoder can be slow or absent on device; StopsViewModel skips TfL/NR when stopId contains ",".
 */
public final class PlaceSearch {

    /** London bounding box: south-west (51.2, -0.5) to north-east (51.7, 0.3). */
    private static final double BBOX_SOUTH = 51.2;
    private static final double BBOX_WEST = -0.5;
    private static final double BBOX_NORTH = 51.7;
    private static final double BBOX_EAST = 0.3;
    private static final int MAX_RESULTS = 5;

    private static final String[] PLACE_BADGE = new String[] { "Place" };

    private PlaceSearch() { }

    /**
     * Search for places by name (e.g. "McDonalds", "gym"). Returns up to 5 results as StopItem
     * with id = "lat,lon" so they can be used as journey from/to coordinates.
     */
    public static List<StopItem> searchPlaces(Context context, String query) {
        if (context == null || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String q = query.trim();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        if (!geocoder.isPresent()) {
            return new ArrayList<>();
        }
        try {
            List<Address> addresses = geocoder.getFromLocationName(q, MAX_RESULTS,
                    BBOX_SOUTH, BBOX_WEST, BBOX_NORTH, BBOX_EAST);
            List<StopItem> out = new ArrayList<>();
            if (addresses == null) return out;
            for (Address addr : addresses) {
                if (addr.hasLatitude() && addr.hasLongitude()) {
                    String id = addr.getLatitude() + "," + addr.getLongitude();
                    String name = buildAddressLine(addr);
                    out.add(new StopItem(id, name, 0, PLACE_BADGE, false, "", false));
                }
            }
            return out;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static String buildAddressLine(Address addr) {
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
