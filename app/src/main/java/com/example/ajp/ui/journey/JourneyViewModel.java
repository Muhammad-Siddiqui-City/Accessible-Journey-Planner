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

/** Runs JourneyFetcher on a worker thread; holds routes, preview labels, sort strategy. */
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

    public void setOptimizationStrategy(RouteOptimizer.Strategy strategy) {
        currentStrategy = strategy;
        reSortRoutes();
    }

    public void reSortRoutes() {
        List<RouteItem> current = routes.getValue();
        if (current != null && !current.isEmpty()) {
            // Sort a copy so observers get a new list instance.
            List<RouteItem> copy = new ArrayList<>(current);
            boolean avoidCrowded = SettingsPrefs.get(getApplication()).isAvoidCrowded();
            RouteOptimizer.sortRoutes(copy, currentStrategy, avoidCrowded);
            routes.postValue(copy);
        }
    }

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
                            Leg lastLeg = legs.get(legs.size() - 1);
                            int transfers = firstRoute.getTransfersCount();
                            String summary = TimeFormatUtil.formatMinutesToHourMin(firstRoute.getDurationMinutesInt()) + " • "
                                    + transfers + (transfers == 1 ? " transfer" : " transfers");

                            // Prefer resolved route labels (e.g. geocoder) so preview matches actual pins.
                            String pf = firstRoute.getFromStation();
                            String pt = firstRoute.getToStation();
                            routePreviewFrom.postValue(pf != null && !pf.trim().isEmpty() ? pf.trim() : fromInput);
                            routePreviewTo.postValue(pt != null && !pt.trim().isEmpty() ? pt.trim() : toInput);
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

}

