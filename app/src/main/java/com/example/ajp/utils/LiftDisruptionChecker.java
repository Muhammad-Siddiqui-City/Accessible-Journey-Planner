package com.example.ajp.utils;

import android.content.Context;
import com.example.ajp.api.Disruption;
import com.example.ajp.api.StopPoint;
import com.example.ajp.api.TflApi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit2.Response;

/**
 * Utility to check stations for lift disruptions and lift availability.
 * PURPOSE: Check if stations in routes have lift issues (disruptions or no lifts).
 * WHY: Enables deprioritizing routes with step-free access issues when step-free routing is enabled.
 * ISSUES: API calls may fail; cache prevents duplicate calls but may return stale results within a search session.
 */
public class LiftDisruptionChecker {

    private final Context appContext;

    // Cache to prevent duplicate API calls for the same station in a single search
    private final Map<String, Boolean> stationIssueCache = new HashMap<>();
    private final Gson gson = new Gson();

    /** Constructor for use with simulated disruption testing (pass application context). */
    public LiftDisruptionChecker(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
    }

    /**
     * Checks if a station has lift issues (disruptions or no lifts).
     * Consults simulated disrupted stop IDs from RouteMonitorPrefs first (for testing), then API.
     * Uses caching to avoid duplicate API calls for the same station.
     *
     * @param originalStopId The station NaPTAN ID or TfL stop ID
     * @param api The TfL API client
     * @return true if station has lift issues, false otherwise (or on error)
     */
    public boolean hasLiftIssues(String originalStopId, TflApi api) {
        android.util.Log.d("LiftChecker", "hasLiftIssues called with ID: " + originalStopId);
        if (originalStopId == null || originalStopId.isEmpty()) {
            android.util.Log.d("LiftChecker", "hasLiftIssues: ID is null or empty, returning false");
            return false;
        }

        // Read any simulated disrupted IDs or names from prefs once so we can use them
        // both for exact ID matches and later name matches after we fetch StopPoint details.
        java.util.Set<String> simulated = null;
        // Simulated disrupted stops (for testing lift unavailable / station closed)
        if (appContext != null) {
            simulated = RouteMonitorPrefs.get(appContext).getSimulatedDisruptedStopIds();
            if (simulated != null && !simulated.isEmpty()) {
                String idTrimmed = originalStopId.trim();
                for (String s : simulated) {
                    if (s == null) continue;
                    String token = s.trim();
                    if (token.isEmpty()) continue;
                    if (idTrimmed.equalsIgnoreCase(token)) {
                        android.util.Log.d("LiftChecker", "hasLiftIssues: Simulated disruption by ID for " + originalStopId);
                        return true;
                    }
                }
            }
        }

        // Use original platform ID for API calls (TfL API doesn't recognize hub IDs like HUBTWG)
        // The API requires the actual platform ID (e.g., 940GZZDLTWG) not the hub ID
        String stopId = originalStopId;
        android.util.Log.d("LiftChecker", "hasLiftIssues: Using original ID " + stopId + " for API calls");
        
        // Return cached result if we've already checked this station
        if (stationIssueCache.containsKey(stopId)) {
            boolean cached = stationIssueCache.get(stopId);
            android.util.Log.d("LiftChecker", "hasLiftIssues: Using cached result for " + stopId + " = " + cached);
            return cached;
        }
        
        android.util.Log.d("LiftChecker", "hasLiftIssues: No cache, checking API for " + stopId);

        try {
            // 1. Check active disruptions
            android.util.Log.d("LiftChecker", "hasLiftIssues: Calling API getStopPointDisruptions for " + stopId);
            Response<List<Object>> disruptionResp = api.getStopPointDisruptions(stopId).execute();
            android.util.Log.d("LiftChecker", "hasLiftIssues: Disruption API response - successful=" + disruptionResp.isSuccessful() + ", code=" + disruptionResp.code() + ", hasBody=" + (disruptionResp.body() != null));
            if (!disruptionResp.isSuccessful()) {
                android.util.Log.w("LiftChecker", "hasLiftIssues: Disruption API failed with code " + disruptionResp.code() + ", message: " + disruptionResp.message());
                if (disruptionResp.errorBody() != null) {
                    try {
                        String errorBody = disruptionResp.errorBody().string();
                        android.util.Log.w("LiftChecker", "hasLiftIssues: Error body: " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.w("LiftChecker", "hasLiftIssues: Could not read error body", e);
                    }
                }
            }
            if (disruptionResp.isSuccessful() && disruptionResp.body() != null) {
                // Log raw response for debugging
                String rawJson = gson.toJson(disruptionResp.body());
                android.util.Log.d("LiftChecker", "hasLiftIssues: Raw disruption response for " + stopId + ": " + rawJson);
                
                // Retrofit returns List<LinkedTreeMap> for List<Object>, convert it to List<Disruption>
                Type listType = new TypeToken<List<Disruption>>(){}.getType();
                List<Disruption> disruptions = gson.fromJson(rawJson, listType);
                
                android.util.Log.d("LiftChecker", "hasLiftIssues: Parsed " + (disruptions != null ? disruptions.size() : 0) + " disruptions");
                if (disruptions != null && !disruptions.isEmpty()) {
                    for (Disruption d : disruptions) {
                        String text = "";
                        if (d.getDescription() != null) text += " " + d.getDescription().toLowerCase();
                        if (d.getAdditionalInfo() != null) text += " " + d.getAdditionalInfo().toLowerCase();
                        if (d.getType() != null) text += " " + d.getType().toLowerCase();
                        android.util.Log.d("LiftChecker", "hasLiftIssues: Checking disruption - description: " + d.getDescription() + ", additionalInfo: " + d.getAdditionalInfo() + ", type: " + d.getType());
                        
                        // Check for lift-related keywords
                        if (text.contains("lift") || text.contains("escalator") || 
                            text.contains("step-free") || text.contains("no access") || 
                            text.contains("out of order") || text.contains("step free") ||
                            text.contains("closure") || text.contains("closed")) {
                            
                            android.util.Log.d("LiftChecker", "hasLiftIssues: Found lift-related disruption!");
                            stationIssueCache.put(stopId, true);
                            return true;
                        }
                    }
                }
            }

            // 2. Fallback: Check if the station inherently has no lifts
            android.util.Log.d("LiftChecker", "hasLiftIssues: Checking lift count API for " + stopId);
            Response<StopPoint> stopPointResp = api.getStopPoint(stopId).execute();
            android.util.Log.d("LiftChecker", "hasLiftIssues: Lift count API response - successful=" + stopPointResp.isSuccessful() + ", code=" + stopPointResp.code() + ", hasBody=" + (stopPointResp.body() != null));
            if (!stopPointResp.isSuccessful()) {
                android.util.Log.w("LiftChecker", "hasLiftIssues: Lift count API failed with code " + stopPointResp.code() + ", message: " + stopPointResp.message());
                if (stopPointResp.errorBody() != null) {
                    try {
                        String errorBody = stopPointResp.errorBody().string();
                        android.util.Log.w("LiftChecker", "hasLiftIssues: Error body: " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.w("LiftChecker", "hasLiftIssues: Could not read error body", e);
                    }
                }
            }
            if (stopPointResp.isSuccessful() && stopPointResp.body() != null) {
                StopPoint stopPoint = stopPointResp.body();
                // If user entered station/bus stop names instead of IDs for simulation,
                // treat a name match as simulated disruption.
                if (simulated != null && isSimulatedByName(stopPoint, simulated)) {
                    stationIssueCache.put(stopId, true);
                    return true;
                }
                boolean hasNoLifts = stopPoint.hasNoLifts();
                android.util.Log.d("LiftChecker", "hasLiftIssues: Station " + stopId + " hasNoLifts=" + hasNoLifts);
                if (hasNoLifts) {
                    stationIssueCache.put(stopId, true);
                    return true;
                }
            }
            
            // No issues found
            android.util.Log.d("LiftChecker", "hasLiftIssues: No issues found for " + stopId + ", returning false");
            stationIssueCache.put(stopId, false);
            return false;

        } catch (Exception e) {
            // Print the exact reason it failed!
            android.util.Log.e("LiftCheckError", "Crash in checker for station " + stopId + ": " + e.getMessage(), e);
            e.printStackTrace();
            // Cache false result to avoid repeated failed calls
            stationIssueCache.put(stopId, false);
            return false;
        }
    }
    
    /**
     * Normalizes station IDs by converting platform-specific IDs to hub IDs.
     * TfL disruption data is often stored under hub IDs (HUB...) rather than platform IDs (940GZZ...).
     * 
     * @param id The original station ID
     * @return The normalized ID (hub ID if conversion possible, otherwise original)
     */
    private String normalizeStationId(String id) {
        if (id == null) return null;
        
        // If it's a standard Tube/DLR NaPTAN (e.g., 940GZZDLTWG)
        if (id.startsWith("940GZZ")) {
            // For Tower Gateway (940GZZDLTWG -> HUBTWG)
            if (id.equals("940GZZDLTWG")) {
                android.util.Log.d("JourneyVM", "CONVERTED ID: 940GZZDLTWG to HUBTWG");
                return "HUBTWG";
            }
            
            // For other cases, try to extract the station suffix
            // Most platform IDs follow pattern: 940GZZ[LINE][STATION]
            // We can try HUB + last 3 characters as a fallback
            if (id.length() > 9) {
                String shortCode = id.substring(id.length() - 3);
                String normalized = "HUB" + shortCode;
                android.util.Log.d("JourneyVM", "CONVERTED ID: " + id + " to " + normalized);
                return normalized;
            }
        }
        
        // Return the original if we don't know how to convert it safely
        return id;
    }

    /**
     * Checks if a StopPoint's common name matches any simulated token (station/bus stop name or ID).
     * Allows users to type names like "Brixton" instead of full NaPTAN IDs in settings.
     */
    private boolean isSimulatedByName(StopPoint stopPoint, java.util.Set<String> simulated) {
        if (stopPoint == null || simulated == null || simulated.isEmpty()) return false;
        String commonName = stopPoint.getCommonName();
        if (commonName == null || commonName.trim().isEmpty()) return false;
        String station = commonName.trim().toLowerCase();
        for (String s : simulated) {
            if (s == null) continue;
            String token = s.trim().toLowerCase();
            if (token.isEmpty()) continue;
            if (station.contains(token) || token.contains(station)) {
                android.util.Log.d("LiftChecker", "hasLiftIssues: Simulated disruption by name match: token=" + s + ", station=" + commonName);
                return true;
            }
        }
        return false;
    }
}
