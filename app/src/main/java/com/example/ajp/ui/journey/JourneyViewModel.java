package com.example.ajp.ui.journey;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.ajp.api.JourneyPlace;
import com.example.ajp.api.Leg;
import com.example.ajp.utils.ApiKeyManager;
import com.example.ajp.utils.JourneyFetcher;
import com.example.ajp.utils.RouteMonitorPrefs;
import com.example.ajp.utils.RouteMonitorScheduler;
import com.example.ajp.utils.RouteOptimizer;
import com.example.ajp.utils.SettingsPrefs;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Plan Journey. Add in Commit 11; extend in 14 (coords, PlaceSearch fallback).
 * PURPOSE: Resolve from/to (TfL search or "lat,lon" or Geocoder), call TfL Journey API, map to RouteItem, RouteOptimizer.
 * WHY: ApiKeyManager.isTflKeyValid() before findRoutes to avoid wasted call; setSavedDestination for place-click from search.
 * ISSUES: Leg duration in seconds; Journey duration in minutes; use TimeFormatUtil for display.
 */
public class JourneyViewModel extends AndroidViewModel {

    private final MutableLiveData<List<RouteItem>> routes = new MutableLiveData<>(List.of());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> savedOrigin = new MutableLiveData<>("");
    private final MutableLiveData<String> savedDestination = new MutableLiveData<>("");
    private final MutableLiveData<String> routePreviewFrom = new MutableLiveData<>();
    private final MutableLiveData<String> routePreviewTo = new MutableLiveData<>();
    private final MutableLiveData<String> routePreviewSummary = new MutableLiveData<>();
    private final MutableLiveData<MapCoords> routePreviewMapCoords = new MutableLiveData<>();
    private RouteOptimizer.Strategy currentStrategy = RouteOptimizer.Strategy.FASTEST;

    public JourneyViewModel(@NonNull Application application) {
        super(application);
    }

    /** Start/end coordinates for map preview. */
    public static class MapCoords {
        public final double startLat, startLon, endLat, endLon;
        public MapCoords(double startLat, double startLon, double endLat, double endLon) {
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
        }
    }

    public LiveData<List<RouteItem>> getRoutes() { return routes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getRoutePreviewFrom() { return routePreviewFrom; }
    public LiveData<String> getRoutePreviewTo() { return routePreviewTo; }
    public LiveData<String> getRoutePreviewSummary() { return routePreviewSummary; }
    public LiveData<MapCoords> getRoutePreviewMapCoords() { return routePreviewMapCoords; }
    public LiveData<String> getSavedOrigin() { return savedOrigin; }
    public LiveData<String> getSavedDestination() { return savedDestination; }

    public void setSavedOrigin(String s) { savedOrigin.setValue(s != null ? s : ""); }
    public void setSavedDestination(String s) { savedDestination.setValue(s != null ? s : ""); }

    /** Change sort strategy and re-sort existing routes. When "Avoid crowds" is on, crowded routes move to the bottom. */
    public void setOptimizationStrategy(RouteOptimizer.Strategy strategy) {
        currentStrategy = strategy;
        reSortRoutes();
    }

    /** Re-sorts current routes using current strategy and "Avoid crowds" preference (e.g. after toggling that switch). */
    public void reSortRoutes() {
        List<RouteItem> current = routes.getValue();
        if (current != null && !current.isEmpty()) {
            List<RouteItem> copy = new ArrayList<>(current);
            boolean avoidCrowded = SettingsPrefs.get(getApplication()).isAvoidCrowded();
            RouteOptimizer.sortRoutes(copy, currentStrategy, avoidCrowded);
            routes.postValue(copy);
        }
    }

    /**
     * Find routes: uses JourneyFetcher then posts results, updates preview, persists last search and schedules background monitor.
     */
    public void findRoutes(String from, String to, String timeHHmm, String dateyyyyMMdd) {
        String fromTrimmed = from != null ? from.trim() : "";
        String toTrimmed = to != null ? to.trim() : "";
        if (fromTrimmed.isEmpty() || toTrimmed.isEmpty()) {
            errorMessage.postValue("Please enter origin and destination");
            return;
        }
        if (!ApiKeyManager.isTflKeyValid()) {
            errorMessage.postValue("API Configuration Error: TfL Key is missing. Please add it to ApiKeyManager.java.");
            return;
        }
        isLoading.postValue(true);
        errorMessage.postValue(null);
        routes.postValue(List.of());
        routePreviewFrom.postValue(null);
        routePreviewTo.postValue(null);
        routePreviewSummary.postValue(null);
        routePreviewMapCoords.postValue(null);

        final String fromInput = fromTrimmed;
        final String toInput = toTrimmed;
        new Thread(() -> {
            try {
                JourneyFetcher fetcher = new JourneyFetcher(getApplication());
                JourneyFetcher.FetchResult result = fetcher.fetch(fromInput, toInput, timeHHmm, dateyyyyMMdd);

                if (result.isSuccess()) {
                    List<RouteItem> items = new ArrayList<>(result.getRoutes());
                    boolean avoidCrowded = SettingsPrefs.get(getApplication()).isAvoidCrowded();
                    RouteOptimizer.sortRoutes(items, currentStrategy, avoidCrowded);
                    routes.postValue(items);

                    if (!items.isEmpty()) {
                        RouteItem firstRoute = items.get(0);
                        List<Leg> legs = firstRoute.getLegs();
                        if (legs != null && !legs.isEmpty()) {
                            // Prefer user's typed origin when API returns "Destination" (e.g. walk leg)
                            String rawFrom = legs.get(0).getDeparturePoint() != null
                                    ? sanitizeStationName(legs.get(0).getDeparturePoint().getCommonName()) : "";
                            String fromName = "Destination".equals(rawFrom) ? fromInput : (rawFrom.isEmpty() ? fromInput : rawFrom);
                            Leg lastLeg = legs.get(legs.size() - 1);
                            // Use user's typed destination when API returns "Destination" (walk leg) or when it's a UK postcode
                            String rawTo = lastLeg.getArrivalPoint() != null
                                    ? sanitizeStationName(lastLeg.getArrivalPoint().getCommonName()) : "";
                            String toName = "Destination".equals(rawTo) || rawTo.isEmpty()
                                    ? toInput.trim()
                                    : (looksLikeUkPostcode(toInput) ? toInput.trim() : rawTo);
                            int transfers = firstRoute.getTransfersCount();
                            String summary = TimeFormatUtil.formatMinutesToHourMin(firstRoute.getDurationMinutesInt()) + " • "
                                    + transfers + (transfers == 1 ? " transfer" : " transfers");
                            routePreviewFrom.postValue(fromName);
                            routePreviewTo.postValue(toName);
                            routePreviewSummary.postValue(summary);
                            JourneyPlace dep = legs.get(0).getDeparturePoint();
                            JourneyPlace arr = lastLeg.getArrivalPoint();
                            if (dep != null && arr != null) {
                                routePreviewMapCoords.postValue(new MapCoords(
                                        dep.getLat(), dep.getLon(), arr.getLat(), arr.getLon()));
                            }
                        }
                    }

                    RouteMonitorPrefs prefs = RouteMonitorPrefs.get(getApplication());
                    prefs.setLastSearch(fromInput, toInput, timeHHmm, dateyyyyMMdd);
                    prefs.setLastSignature(JourneyFetcher.buildSignature(items));
                    RouteMonitorScheduler.schedule(getApplication());
                } else {
                    errorMessage.postValue(result.getError() != null ? result.getError() : "Failed to load routes");
                }
            } catch (Exception e) {
                errorMessage.postValue(e.getMessage() != null ? e.getMessage() : "Failed to load routes");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    /** True if the string looks like a UK postcode (e.g. sw15 5le, SW15 5LE). */
    private static boolean looksLikeUkPostcode(String s) {
        if (s == null || s.length() < 5) return false;
        String t = s.trim().toUpperCase().replaceAll("\\s+", " ");
        return t.matches("[A-Z]{1,2}[0-9][0-9A-Z]?\\s*[0-9][A-Z]{2}");
    }

    /**
     * Sanitizes station/place names returned by TfL API.
     * Replaces debug text like "walk inside building" with user-friendly names.
     */
    private static String sanitizeStationName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) return "";
        String cleaned = rawName.trim();
        if (cleaned.toLowerCase().contains("walk inside building")) return "Destination";
        return cleaned;
    }
}
