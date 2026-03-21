package com.example.ajp.ui.nearby;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ajp.api.ArrivalPrediction;
import com.example.ajp.api.LineStatus;
import com.example.ajp.api.RetrofitClient;
import com.example.ajp.api.StatusDetail;
import com.example.ajp.api.MatchedStop;
import com.example.ajp.api.StopPoint;
import com.example.ajp.api.StopPointResponse;
import com.example.ajp.api.NationalRailApi;
import com.example.ajp.utils.ApiKeyManager;
import com.example.ajp.api.TflApi;
import com.example.ajp.api.TflSearchResponse;
import com.example.ajp.ui.arrivals.Arrival;
import com.example.ajp.utils.CrsLookup;
import com.example.ajp.utils.PlaceSearch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import retrofit2.Response;

/**
 * ViewModel for nearby stops and arrivals. Add in Commit 8; extend in 9 (NR), 10 (filter), 14 (place search).
 * PURPOSE: Load buses (300m) + top 2 per mode (3.5km), line status, arrivals; National Rail fallback; search (TfL + PlaceSearch).
 * WHY: filterAndSortArrivals removes <60s, >60min, duplicates; tryNationalRailFallback when TfL empty; skip TfL/NR when stopId contains "," (place).
 * ISSUES: Check ApiKeyManager.isRailTokenValid() before NR call; CrsLookup for NaPTAN→CRS.
 */
public class StopsViewModel extends ViewModel {

    private static final int QUOTA_PER_MODE = 2;
    private static final int MAX_TRAIN_TIMES = 5;

    private static final int MAX_LINE_IDS_FOR_DISRUPTIONS = 15;

    private final MutableLiveData<List<StopItem>> stops = new MutableLiveData<>();
    private final MutableLiveData<List<LineStatus>> disruptions = new MutableLiveData<>();
    private final MutableLiveData<List<Arrival>> selectedStopArrivals = new MutableLiveData<>();
    /** Trains to the chosen station from TfL (StopPoint/{id}/Arrivals without National Rail fallback). */
    private final MutableLiveData<List<Arrival>> tflArrivalsToStation = new MutableLiveData<>(Collections.emptyList());
    /** National Rail departures from the chosen station (OpenLDBWS GetDepartureBoard). */
    private final MutableLiveData<List<Arrival>> nationalRailDeparturesFromStation = new MutableLiveData<>(Collections.emptyList());
    /** True when arrivals are empty because stop is National Rail and TfL does not provide that data. */
    private final MutableLiveData<Boolean> nationalRailNoDataHint = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<StopItem>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> searchLoading = new MutableLiveData<>(false);
    private double cachedLat = Double.NaN;
    private double cachedLon = Double.NaN;

    public LiveData<List<StopItem>> getStops() { return stops; }
    public boolean hasCachedLocation() { return !Double.isNaN(cachedLat) && !Double.isNaN(cachedLon); }
    public double getCachedLat() { return cachedLat; }
    public double getCachedLon() { return cachedLon; }
    public LiveData<List<LineStatus>> getDisruptions() { return disruptions; }
    public LiveData<List<Arrival>> getSelectedStopArrivals() { return selectedStopArrivals; }
    public LiveData<List<Arrival>> getTflArrivalsToStation() { return tflArrivalsToStation; }
    public LiveData<List<Arrival>> getNationalRailDeparturesFromStation() { return nationalRailDeparturesFromStation; }
    public LiveData<Boolean> getNationalRailNoDataHint() { return nationalRailNoDataHint; }
    public LiveData<List<StopItem>> getNearestStops() { return stops; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getIsLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<StopItem>> getSearchResults() { return searchResults; }
    public LiveData<Boolean> getSearchLoading() { return searchLoading; }

    /**
     * 1. Fetch buses (all). 2. Fetch trains (3.5km), bucket by line names: Tube, Overground, Elizabeth, DLR, National Rail.
     * 3. Take top 2 from each bucket. 4. Merge: all buses + top 2 tube + top 2 overground + top 2 elizabeth + top 2 dlr + top 2 rail (dedupe by stopId).
     * 5. Sort final list by distance for "best of everything" order.
     */
    public void loadNearestStops(double lat, double lon) {
        cachedLat = lat;
        cachedLon = lon;
        errorMessage.setValue(null);
        loading.setValue(true);
        disruptions.setValue(Collections.emptyList());

        new Thread(() -> {
            try {
                TflApi api = RetrofitClient.getApi();

                // 1. Buses – keep all (radius 300m)
                List<StopItem> busItems = new ArrayList<>();
                Response<StopPointResponse> busesResp = api.getNearbyBuses(lat, lon).execute();
                if (busesResp.isSuccessful() && busesResp.body() != null && busesResp.body().getStopPoints() != null) {
                    for (StopPoint sp : busesResp.body().getStopPoints()) {
                        busItems.add(mapToStopItem(sp, false));  // bus = low priority
                    }
                }

                // 2. Trains (3.5km): categorize into 5 buckets, take top 2 from each
                List<StopItem> top2Tube = new ArrayList<>();
                List<StopItem> top2Overground = new ArrayList<>();
                List<StopItem> top2Elizabeth = new ArrayList<>();
                List<StopItem> top2Dlr = new ArrayList<>();
                List<StopItem> top2NationalRail = new ArrayList<>();
                bucketAndSelectTrainStations(api, lat, lon, top2Tube, top2Overground, top2Elizabeth, top2Dlr, top2NationalRail);

                // 3. Construct final list: only stops with valid lines (skip ghost stops entirely)
                Set<String> addedIds = new HashSet<>();
                List<StopItem> merged = new ArrayList<>();
                for (StopItem s : busItems) {
                    if (!hasLines(s)) continue;
                    merged.add(s);
                    addedIds.add(s.getStopId());
                }
                addUpToQuota(merged, addedIds, top2Tube);
                addUpToQuota(merged, addedIds, top2Overground);
                addUpToQuota(merged, addedIds, top2Elizabeth);
                addUpToQuota(merged, addedIds, top2Dlr);
                addUpToQuota(merged, addedIds, top2NationalRail);

                // Sanity check: remove any ghost stops that slipped through (null/empty lines)
                Iterator<StopItem> iterator = merged.iterator();
                while (iterator.hasNext()) {
                    StopItem stop = iterator.next();
                    if (stop.getLineCodes() == null || stop.getLineCodes().length == 0) {
                        System.out.println("REMOVING GHOST STOP: " + stop.getName());
                        iterator.remove();
                    }
                }

                // 4. Weighted priority sort: 1=Bus, 2=SWR, 3=Tube, 4=Other; then by distance (no ghost – filtered above)
                Collections.sort(merged, STOPS_COMPARATOR);

                stops.postValue(merged);

                // 5. Fetch disruptions for lines serving nearby stops (contextually relevant)
                loadNearbyDisruptions(api, merged);
            } catch (Exception e) {
                errorMessage.postValue(e.getMessage() != null ? e.getMessage() : "Failed to load stops");
                stops.postValue(Collections.emptyList());
                disruptions.postValue(Collections.emptyList());
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    /**
     * Extract unique line ids from nearby stops, call getLineStatus, filter to lines with severity != 10 (Good Service),
     * sort by severity (worse first), post to disruptions.
     */
    private void loadNearbyDisruptions(TflApi api, List<StopItem> nearbyStops) {
        if (nearbyStops == null || nearbyStops.isEmpty()) {
            disruptions.postValue(Collections.emptyList());
            return;
        }
        try {
            Set<String> lineIds = new HashSet<>();
            for (StopItem s : nearbyStops) {
                String[] codes = s.getLineCodes();
                if (codes == null) continue;
                for (String name : codes) {
                    if (name == null || name.trim().isEmpty()) continue;
                    String id = lineNameToApiId(name.trim());
                    if (!id.isEmpty()) lineIds.add(id);
                }
            }
            if (lineIds.isEmpty()) {
                disruptions.postValue(Collections.emptyList());
                return;
            }
            List<String> idList = new ArrayList<>(lineIds);
            if (idList.size() > MAX_LINE_IDS_FOR_DISRUPTIONS) {
                idList = idList.subList(0, MAX_LINE_IDS_FOR_DISRUPTIONS);
            }
            String commaIds = String.join(",", idList);

            Response<List<LineStatus>> resp = api.getLineStatus(commaIds).execute();
            List<LineStatus> all = resp.isSuccessful() && resp.body() != null ? resp.body() : Collections.<LineStatus>emptyList();
            List<LineStatus> disrupted = new ArrayList<>();
            for (LineStatus line : all) {
                if (line.getLineStatuses() == null) continue;
                boolean hasDisruption = false;
                for (StatusDetail d : line.getLineStatuses()) {
                    if (d.getStatusSeverity() != 10) {
                        hasDisruption = true;
                        break;
                    }
                }
                if (hasDisruption) disrupted.add(line);
            }
            Collections.sort(disrupted, (a, b) -> {
                int sevA = minSeverity(a);
                int sevB = minSeverity(b);
                return Integer.compare(sevA, sevB);
            });
            disruptions.postValue(disrupted);
        } catch (Exception e) {
            disruptions.postValue(Collections.emptyList());
        }
    }

    private static int minSeverity(LineStatus line) {
        if (line.getLineStatuses() == null || line.getLineStatuses().isEmpty()) return 10;
        int min = 10;
        for (StatusDetail d : line.getLineStatuses()) {
            if (d.getStatusSeverity() < min) min = d.getStatusSeverity();
        }
        return min;
    }

    /** Convert display name (e.g. "District line", "South Western Railway") to TfL API line id (e.g. "district", "south-western-railway"). */
    private static String lineNameToApiId(String name) {
        String n = name.toLowerCase()
                .replace(" line", "")
                .replace(" & ", "-")
                .replace(" and ", "-")
                .replace(' ', '-');
        return n.replaceAll("[^a-z0-9\\-]", "");
    }

    /** Add up to QUOTA_PER_MODE items from source into merged; skip duplicates and ghost stops (no lines). */
    private static void addUpToQuota(List<StopItem> merged, Set<String> addedIds, List<StopItem> source) {
        for (StopItem s : source) {
            if (!hasLines(s)) continue;
            if (addedIds.contains(s.getStopId())) continue;
            merged.add(s);
            addedIds.add(s.getStopId());
        }
    }

    /** True if stop has at least one line (active service); false for ghost stops. */
    private static boolean hasLines(StopItem s) {
        return s.getLineCodes() != null && s.getLineCodes().length > 0;
    }

    /**
     * Fetches getNearbyTrains (3.5km). Buckets by line names: tube, overground, elizabeth, dlr, nationalRail.
     * Sorts each bucket by distance and fills the output lists with top 2 from each.
     */
    private void bucketAndSelectTrainStations(TflApi api, double lat, double lon,
                                              List<StopItem> top2TubeOut, List<StopItem> top2OvergroundOut,
                                              List<StopItem> top2ElizabethOut, List<StopItem> top2DlrOut,
                                              List<StopItem> top2NationalRailOut) throws java.io.IOException {
        Response<StopPointResponse> trainsResp = api.getNearbyTrains(lat, lon).execute();
        if (!trainsResp.isSuccessful() || trainsResp.body() == null || trainsResp.body().getStopPoints() == null) {
            return;
        }
        List<StopPoint> raw = trainsResp.body().getStopPoints();

        List<StopPoint> tubeStations = new ArrayList<>();
        List<StopPoint> overgroundStations = new ArrayList<>();
        List<StopPoint> elizabethStations = new ArrayList<>();
        List<StopPoint> dlrStations = new ArrayList<>();
        List<StopPoint> nationalRailStations = new ArrayList<>();

        for (StopPoint sp : raw) {
            boolean inTube = false, inOverground = false, inElizabeth = false, inDlr = false;
            if (sp.getLines() != null) {
                for (com.example.ajp.api.LineIdentifier line : sp.getLines()) {
                    String name = line.getName();
                    if (name == null) continue;
                    String n = name.toLowerCase();
                    if (n.contains("bakerloo") || n.contains("central") || n.contains("district") || n.contains("jubilee")
                            || n.contains("northern") || n.contains("piccadilly") || n.contains("victoria")
                            || n.contains("waterloo & city") || n.contains("circle") || n.contains("metropolitan")
                            || n.contains("hammersmith & city")) {
                        inTube = true;
                    }
                    if (n.contains("london overground") || n.contains("overground")) inOverground = true;
                    if (n.contains("elizabeth line") || n.contains("elizabeth")) inElizabeth = true;
                    if (n.contains("dlr")) inDlr = true;
                }
            }
            if (inTube) tubeStations.add(sp);
            if (inOverground) overgroundStations.add(sp);
            if (inElizabeth) elizabethStations.add(sp);
            if (inDlr) dlrStations.add(sp);
            if (!inTube && !inOverground && !inElizabeth && !inDlr) nationalRailStations.add(sp);
        }

        Comparator<StopPoint> byDistance = Comparator.comparingDouble(StopPoint::getDistance);
        Collections.sort(tubeStations, byDistance);
        Collections.sort(overgroundStations, byDistance);
        Collections.sort(elizabethStations, byDistance);
        Collections.sort(dlrStations, byDistance);
        Collections.sort(nationalRailStations, byDistance);

        addTopN(top2TubeOut, tubeStations, QUOTA_PER_MODE);
        addTopN(top2OvergroundOut, overgroundStations, QUOTA_PER_MODE);
        addTopN(top2ElizabethOut, elizabethStations, QUOTA_PER_MODE);
        addTopN(top2DlrOut, dlrStations, QUOTA_PER_MODE);
        addTopN(top2NationalRailOut, nationalRailStations, QUOTA_PER_MODE);
    }

    /** Map top N StopPoints (by distance) to StopItems and add to out list. Train buckets use isStation=true. */
    private static void addTopN(List<StopItem> out, List<StopPoint> bucket, int n) {
        for (int i = 0; i < Math.min(n, bucket.size()); i++) {
            out.add(mapToStopItem(bucket.get(i), true));  // station = high priority
        }
    }

    /**
     * Weighted priority sort: 1=Bus, 2=SWR, 3=Tube, 4=Other (Overground, DLR, Elizabeth, etc.). Then by distance ascending.
     * Ghost stops are excluded before merge, so no Score 5.
     */
    private static final Comparator<StopItem> STOPS_COMPARATOR = (s1, s2) -> {
        int p1 = getPriority(s1);
        int p2 = getPriority(s2);
        if (p1 != p2) {
            return Integer.compare(p1, p2);
        }
        return Double.compare(s1.getDistance(), s2.getDistance());
    };

    /**
     * Priority score. Lower = higher in list. Only stops with lines reach here (ghost stops filtered before merge).
     * 1 = Working bus, 2 = SWR, 3 = Tube, 4 = Everything else (Overground, DLR, Elizabeth, other rail).
     */
    private static int getPriority(StopItem stop) {
        if (containsLine(stop, "South Western Railway")) return 2;
        if (containsTubeLine(stop)) return 3;
        if (!stop.isStation()) return 1;  // bus
        return 4;
    }

    private static boolean containsLine(StopItem stop, String lineSubstring) {
        String[] codes = stop.getLineCodes();
        if (codes == null) return false;
        String key = lineSubstring.toLowerCase();
        for (String name : codes) {
            if (name != null && name.toLowerCase().contains(key)) return true;
        }
        return false;
    }

    private static boolean containsTubeLine(StopItem stop) {
        String[] codes = stop.getLineCodes();
        if (codes == null) return false;
        for (String name : codes) {
            if (name == null) continue;
            String n = name.toLowerCase();
            if (n.contains("district") || n.contains("piccadilly") || n.contains("victoria") || n.contains("northern")
                    || n.contains("central") || n.contains("bakerloo") || n.contains("jubilee") || n.contains("metropolitan")
                    || n.contains("circle") || n.contains("hammersmith") || n.contains("waterloo & city")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Home screen highlights: Station 1 = first SWR; Station 2 = first Tube. Fallback: next closest station (Rail/Tube) if a slot is missing.
     * Returns a list of 0, 1, or 2 StopItems (nearestStops is already sorted by priority then distance).
     */
    public static List<StopItem> getHomeHighlights(List<StopItem> nearestStops) {
        List<StopItem> out = new ArrayList<>(2);
        if (nearestStops == null || nearestStops.isEmpty()) return out;

        StopItem station1 = null;  // SWR preferred
        StopItem station2 = null;  // Tube preferred
        StopItem fallback1 = null;  // first station (any)
        StopItem fallback2 = null;  // second station (any)

        for (StopItem s : nearestStops) {
            if (station1 == null && containsLine(s, "South Western Railway")) {
                station1 = s;
            }
            if (station2 == null && containsTubeLine(s)) {
                station2 = s;
            }
            if (s.isStation()) {
                if (fallback1 == null) fallback1 = s;
                else if (fallback2 == null && !s.getStopId().equals(fallback1.getStopId())) fallback2 = s;
            }
        }

        if (station1 == null) station1 = fallback1;
        if (station2 == null) station2 = (fallback2 != null ? fallback2 : (station1 != null && fallback1 != null && !fallback1.getStopId().equals(station1.getStopId()) ? fallback1 : null));

        if (station1 != null) out.add(station1);
        if (station2 != null && (station1 == null || !station2.getStopId().equals(station1.getStopId()))) out.add(station2);
        return out;
    }

    /**
     * Search stops/stations and places by name. Combines TfL station search with Geocoder place search.
     * Pass application context (e.g. context.getApplicationContext()) to avoid leaking Activity.
     */
    public void searchStopsByName(android.content.Context context, String query) {
        String q = query != null ? query.trim() : "";
        if (q.isEmpty()) {
            searchResults.postValue(Collections.emptyList());
            return;
        }
        searchLoading.postValue(true);
        searchResults.postValue(Collections.emptyList());
        android.content.Context appContext = context != null ? context.getApplicationContext() : null;
        new Thread(() -> {
            try {
                List<StopItem> list = new ArrayList<>();
                TflApi api = RetrofitClient.getApi();
                Response<TflSearchResponse> resp = api.searchStops(q).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    for (MatchedStop m : resp.body().getMatches()) {
                        list.add(mapMatchedStopToStopItem(m));
                    }
                    list = deduplicateSearchResultsByName(list);
                }
                List<StopItem> places = (appContext != null) ? PlaceSearch.searchPlaces(appContext, q) : Collections.emptyList();
                list.addAll(places);
                searchResults.postValue(list);
            } catch (Exception e) {
                searchResults.postValue(Collections.emptyList());
            } finally {
                searchLoading.postValue(false);
            }
        }).start();
    }

    /** Keep first occurrence of each name so search results don't repeat "King's Cross Station" etc. */
    private static List<StopItem> deduplicateSearchResultsByName(List<StopItem> list) {
        if (list == null || list.isEmpty()) return list;
        Set<String> seen = new HashSet<>();
        List<StopItem> out = new ArrayList<>();
        for (StopItem s : list) {
            String name = s.getName() != null ? s.getName().trim() : "";
            if (!name.isEmpty() && seen.add(name)) out.add(s);
        }
        return out;
    }

    /** Map TfL search API match (id, name, modes) to StopItem for the list. */
    private static StopItem mapMatchedStopToStopItem(MatchedStop m) {
        List<String> modes = m.getModes();
        String[] lineCodes = new String[modes.size()];
        for (int i = 0; i < modes.size(); i++) {
            lineCodes[i] = modeToDisplayName(modes.get(i));
        }
        boolean isStation = false;
        for (String mode : modes) {
            if (mode != null && (mode.contains("tube") || mode.contains("rail") || mode.contains("dlr") || mode.contains("overground") || mode.contains("elizabeth") || mode.contains("metro"))) {
                isStation = true;
                break;
            }
        }
        return new StopItem(m.getId(), m.getName(), 0, lineCodes, false, "", isStation);
    }

    private static String modeToDisplayName(String mode) {
        if (mode == null || mode.isEmpty()) return "";
        String m = mode.toLowerCase();
        if (m.contains("tube")) return "Tube";
        if (m.contains("national-rail") || m.contains("national_rail")) return "National Rail";
        if (m.contains("dlr")) return "DLR";
        if (m.contains("overground")) return "London Overground";
        if (m.contains("elizabeth")) return "Elizabeth line";
        if (m.contains("bus")) return "Bus";
        if (m.contains("tram")) return "Tram";
        return mode.substring(0, 1).toUpperCase() + (mode.length() > 1 ? mode.substring(1).toLowerCase() : "");
    }

    /**
     * Fetch real TfL arrivals for the given stop (Naptan ID) and post to getSelectedStopArrivals().
     * For National Rail (e.g. Barnes SWR), StopPoint/Arrivals often returns empty. We try:
     * 1) Line/Arrivals for south-western-railway (and other operators)
     * 2) Arrivals for each platform child (parent 910GBARNES has no arrivals; child 9100BARNES0 does)
     */
    public void loadArrivals(String stopId) {
        if (stopId == null || stopId.isEmpty()) {
            selectedStopArrivals.postValue(Collections.emptyList());
            return;
        }
        nationalRailNoDataHint.postValue(false);
        new Thread(() -> {
            try {
                TflApi api = RetrofitClient.getApi();
                List<ArrivalPrediction> predictions = fetchArrivalsFromApi(api, stopId);

                if (predictions.isEmpty()) {
                    predictions = fetchLineArrivalsFallback(api, stopId);
                }
                if (predictions.isEmpty()) {
                    predictions = fetchChildPlatformArrivals(api, stopId);
                }

                if (predictions.isEmpty()) {
                    if (stopId != null && stopId.contains(",")) {
                        nationalRailNoDataHint.postValue(false);
                        selectedStopArrivals.postValue(Collections.emptyList());
                        return;
                    }
                    String crs = CrsLookup.getCrs(stopId);
                    android.util.Log.d("DEBUG_SWR", "Checking Leg: stopId=" + stopId + " -> CRS from id: " + crs);
                    if (crs == null) {
                        boolean isNationalRail = checkIsNationalRailNoData(api, stopId);
                        android.util.Log.d("DEBUG_SWR", "isNationalRail=" + isNationalRail);
                        if (isNationalRail) {
                            Response<StopPoint> spResp = api.getStopPoint(stopId).execute();
                            if (spResp.isSuccessful() && spResp.body() != null) {
                                crs = CrsLookup.getCrsFromName(spResp.body().getCommonName());
                                android.util.Log.d("DEBUG_SWR", "CRS from name (" + spResp.body().getCommonName() + "): " + crs);
                            }
                        }
                    }
                    if (crs != null) {
                        if (ApiKeyManager.isRailTokenValid()) {
                            android.util.Log.d("DEBUG_SWR", "Calling National Rail API for CRS: " + crs);
                            tryNationalRailFallback(crs, stopId);
                        } else {
                            android.util.Log.d("ApiKeyManager", "Skipping Rail Check: Token missing");
                            nationalRailNoDataHint.postValue(true);
                        }
                        return;
                    }
                    boolean isNationalRail = checkIsNationalRailNoData(api, stopId);
                    nationalRailNoDataHint.postValue(isNationalRail);
                } else {
                    nationalRailNoDataHint.postValue(false);
                }

                List<Arrival> list = new ArrayList<>();
                for (ArrivalPrediction p : predictions) {
                    list.add(new Arrival(p.getLineName(), p.getDestinationName(), p.getPlatformName(), p.getTimeToStation(), p.getModeName()));
                }
                list = filterAndSortArrivals(list);
                selectedStopArrivals.postValue(list);
            } catch (Exception e) {
                nationalRailNoDataHint.postValue(false);
                selectedStopArrivals.postValue(Collections.emptyList());
            }
        }).start();
    }

    /**
     * Load both sides of the "trains to and from this station" screen:
     * - "to station" uses TfL arrivals only (StopPoint/{id}/Arrivals + existing fallbacks), no National Rail fallback
     * - "from station" uses National Rail OpenLDBWS GetDepartureBoard when CRS mapping exists
     */
    public void loadTrainsToAndFrom(String stopId) {
        loadTflArrivalsOnly(stopId);
        loadNationalRailDeparturesOnly(stopId);
    }

    /** TfL arrivals only (no National Rail fallback), suitable for the "to this station" list. */
    private void loadTflArrivalsOnly(String stopId) {
        if (stopId == null || stopId.isEmpty()) {
            tflArrivalsToStation.postValue(Collections.emptyList());
            return;
        }
        new Thread(() -> {
            try {
                TflApi api = RetrofitClient.getApi();

                List<ArrivalPrediction> predictions = fetchArrivalsFromApi(api, stopId);
                if (predictions.isEmpty()) predictions = fetchLineArrivalsFallback(api, stopId);
                if (predictions.isEmpty()) predictions = fetchChildPlatformArrivals(api, stopId);

                List<Arrival> list = new ArrayList<>();
                for (ArrivalPrediction p : predictions) {
                    list.add(new Arrival(p.getLineName(), p.getDestinationName(), p.getPlatformName(), p.getTimeToStation(), p.getModeName()));
                }

                list = filterAndSortArrivals(list);
                tflArrivalsToStation.postValue(truncateToTopN(list, MAX_TRAIN_TIMES));
            } catch (Exception e) {
                tflArrivalsToStation.postValue(Collections.emptyList());
            }
        }).start();
    }

    /** National Rail departures only, suitable for the "from this station" list. */
    private void loadNationalRailDeparturesOnly(String stopId) {
        if (stopId == null || stopId.isEmpty()) {
            nationalRailDeparturesFromStation.postValue(Collections.emptyList());
            return;
        }
        new Thread(() -> {
            try {
                if (!ApiKeyManager.isRailTokenValid()) {
                    nationalRailDeparturesFromStation.postValue(Collections.emptyList());
                    return;
                }

                TflApi api = RetrofitClient.getApi();
                String crs = CrsLookup.getCrs(stopId);

                if (crs == null) {
                    // Fallback: map from StopPoint common name when CRS isn't present in the static ID map.
                    Response<StopPoint> spResp = api.getStopPoint(stopId).execute();
                    if (spResp.isSuccessful() && spResp.body() != null) {
                        crs = CrsLookup.getCrsFromName(spResp.body().getCommonName());
                    }
                }

                if (crs == null) {
                    nationalRailDeparturesFromStation.postValue(Collections.emptyList());
                    return;
                }

                NationalRailApi railApi = new NationalRailApi();
                railApi.getDepartureBoard(crs, null, new NationalRailApi.DepartureBoardCallback() {
                    @Override
                    public void onDepartures(List<Arrival> arrivals) {
                        // National Rail parsing sometimes returns "due now" (0 seconds) and can also
                        // include departures slightly beyond the TfL time window. Use a relaxed filter
                        // so the UI shows results instead of staying empty.
                        List<Arrival> filtered = filterAndSortNationalRailArrivals(arrivals);
                        nationalRailDeparturesFromStation.postValue(truncateToTopN(filtered, MAX_TRAIN_TIMES));
                    }

                    @Override
                    public void onError(String message) {
                        nationalRailDeparturesFromStation.postValue(Collections.emptyList());
                    }
                });
            } catch (Exception e) {
                nationalRailDeparturesFromStation.postValue(Collections.emptyList());
            }
        }).start();
    }

    private List<ArrivalPrediction> fetchArrivalsFromApi(TflApi api, String stopId) throws java.io.IOException {
        Response<List<ArrivalPrediction>> resp = api.getArrivals(stopId).execute();
        if (resp.isSuccessful() && resp.body() != null) {
            return resp.body();
        }
        return Collections.emptyList();
    }

    /** Try Line/Arrivals for National Rail when StopPoint/Arrivals returns empty. */
    private List<ArrivalPrediction> fetchLineArrivalsFallback(TflApi api, String stopId) {
        String[] nationalRailLines = {"south-western-railway", "southern", "southeastern", "thameslink", "great-western-railway", "greater-anglia", "c2c"};
        for (String lineId : nationalRailLines) {
            try {
                Response<List<ArrivalPrediction>> resp = api.getLineArrivals(lineId, stopId).execute();
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    return resp.body();
                }
            } catch (Exception ignored) {
                // Try next line
            }
        }
        return Collections.emptyList();
    }

    /** Filter arrivals: remove 0 min, duplicates, and entries > 60 min. */
    private static List<Arrival> filterAndSortArrivals(List<Arrival> list) {
        if (list == null) return Collections.emptyList();
        List<Arrival> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Arrival a : list) {
            int sec = a.getTimeToStationSeconds();
            if (sec < 60) continue;           // Remove 0 min / due now
            if (sec > 3600) continue;         // Only show <= 60 min
            String key = (a.getDestinationName() != null ? a.getDestinationName() : "") + "|" + (sec / 60);
            if (!seen.add(key)) continue;     // Remove duplicates (same dest + same minute)
            out.add(a);
        }
        Collections.sort(out, (a, b) -> Integer.compare(a.getTimeToStationSeconds(), b.getTimeToStationSeconds()));
        return out;
    }

    /**
     * Relaxed filter for National Rail departures.
     * PURPOSE: Show results even when the API returns "due now" (0 seconds) or times outside
     * the TfL-only 60 minute window.
     */
    private static List<Arrival> filterAndSortNationalRailArrivals(List<Arrival> list) {
        if (list == null) return Collections.emptyList();
        List<Arrival> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Allow up to 4 hours for national rail, and keep due-now (0 seconds) entries.
        int maxSeconds = 4 * 60 * 60;
        for (Arrival a : list) {
            int sec = a.getTimeToStationSeconds();
            if (sec < 0) continue;
            if (sec > maxSeconds) continue;

            String dest = a.getDestinationName() != null ? a.getDestinationName() : "";
            int minuteBucket = sec / 60;
            String key = dest + "|" + minuteBucket;
            if (!seen.add(key)) continue;
            out.add(a);
        }
        Collections.sort(out, (a, b) -> Integer.compare(a.getTimeToStationSeconds(), b.getTimeToStationSeconds()));
        return out;
    }

    /** Truncate list to top N items (keeps earlier sort order). */
    private static <T> List<T> truncateToTopN(List<T> list, int n) {
        if (list == null) return Collections.emptyList();
        if (n <= 0) return Collections.emptyList();
        if (list.size() <= n) return list;
        return new ArrayList<>(list.subList(0, n));
    }

    /** When TfL returns no data for a National Rail station, try OpenLDBWS API. */
    private void tryNationalRailFallback(String crs, String stopId) {
        NationalRailApi railApi = new NationalRailApi();
        railApi.getDepartureBoard(crs, null, new NationalRailApi.DepartureBoardCallback() {
            @Override
            public void onDepartures(List<Arrival> arrivals) {
                nationalRailNoDataHint.postValue(false);
                List<Arrival> filtered = filterAndSortArrivals(arrivals);
                selectedStopArrivals.postValue(filtered);
            }

            @Override
            public void onError(String message) {
                nationalRailNoDataHint.postValue(true);
                selectedStopArrivals.postValue(Collections.emptyList());
            }
        });
    }

    /** Check if stop is National Rail (TfL does not provide arrivals for National Rail). */
    private boolean checkIsNationalRailNoData(TflApi api, String stopId) {
        try {
            Response<StopPoint> resp = api.getStopPoint(stopId).execute();
            return resp.isSuccessful() && resp.body() != null && resp.body().hasNationalRailMode();
        } catch (Exception e) {
            return false;
        }
    }

    /** For National Rail, parent stop often has no arrivals; platform children do. Try each child. */
    private List<ArrivalPrediction> fetchChildPlatformArrivals(TflApi api, String stopId) {
        try {
            Response<StopPoint> resp = api.getStopPoint(stopId).execute();
            if (!resp.isSuccessful() || resp.body() == null) return Collections.emptyList();

            StopPoint parent = resp.body();
            List<ArrivalPrediction> merged = new ArrayList<>();
            Set<String> tried = new HashSet<>();

            for (StopPoint child : parent.getChildren()) {
                String childId = child.getNaptanId();
                if (childId == null || tried.contains(childId)) continue;
                tried.add(childId);

                List<ArrivalPrediction> childArrivals = fetchArrivalsFromApi(api, childId);
                merged.addAll(childArrivals);
                if (childArrivals.isEmpty()) {
                    List<ArrivalPrediction> lineArr = fetchLineArrivalsFallback(api, childId);
                    merged.addAll(lineArr);
                }
            }
            return merged;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static StopItem mapToStopItem(StopPoint sp, boolean isStation) {
        String[] codes = new String[0];
        if (sp.getLines() != null && !sp.getLines().isEmpty()) {
            int n = Math.min(4, sp.getLines().size());
            codes = new String[n];
            for (int i = 0; i < n; i++) {
                codes[i] = sp.getLines().get(i).getName();
            }
        }
        return new StopItem(sp.getNaptanId(), sp.getCommonName(), sp.getDistance(), codes, false, sp.getStopLetter(), isStation);
    }
}
