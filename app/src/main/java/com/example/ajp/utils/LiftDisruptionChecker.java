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

/** TfL disruption + stop metadata to flag possible lift/access issues at a NaPTAN id. */
public class LiftDisruptionChecker {

    private final Context appContext;

    private final Map<String, Boolean> stationIssueCache = new HashMap<>();
    private final Gson gson = new Gson();

    public LiftDisruptionChecker(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
    }

    public boolean hasLiftIssues(String originalStopId, TflApi api) {
        android.util.Log.d("LiftChecker", "hasLiftIssues called with ID: " + originalStopId);
        if (originalStopId == null || originalStopId.isEmpty()) {
            android.util.Log.d("LiftChecker", "hasLiftIssues: ID is null or empty, returning false");
            return false;
        }

        java.util.Set<String> simulated = null;

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

        String stopId = originalStopId;
        android.util.Log.d("LiftChecker", "hasLiftIssues: Using original ID " + stopId + " for API calls");

        if (stationIssueCache.containsKey(stopId)) {
            boolean cached = stationIssueCache.get(stopId);
            android.util.Log.d("LiftChecker", "hasLiftIssues: Using cached result for " + stopId + " = " + cached);
            return cached;
        }

        android.util.Log.d("LiftChecker", "hasLiftIssues: No cache, checking API for " + stopId);

        try {

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

                String rawJson = gson.toJson(disruptionResp.body());
                android.util.Log.d("LiftChecker", "hasLiftIssues: Raw disruption response for " + stopId + ": " + rawJson);

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

            android.util.Log.d("LiftChecker", "hasLiftIssues: No issues found for " + stopId + ", returning false");
            stationIssueCache.put(stopId, false);
            return false;

        } catch (Exception e) {

            android.util.Log.e("LiftCheckError", "Crash in checker for station " + stopId + ": " + e.getMessage(), e);
            e.printStackTrace();

            stationIssueCache.put(stopId, false);
            return false;
        }
    }

    private String normalizeStationId(String id) {
        if (id == null) return null;

        if (id.startsWith("940GZZ")) {

            if (id.equals("940GZZDLTWG")) {
                android.util.Log.d("JourneyVM", "CONVERTED ID: 940GZZDLTWG to HUBTWG");
                return "HUBTWG";
            }

            if (id.length() > 9) {
                String shortCode = id.substring(id.length() - 3);
                String normalized = "HUB" + shortCode;
                android.util.Log.d("JourneyVM", "CONVERTED ID: " + id + " to " + normalized);
                return normalized;
            }
        }

        return id;
    }

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


