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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;
import retrofit2.Response;







/**
 * ViewModel that owns UI state for Stops.
 */
public class StopsViewModel extends ViewModel {

    private static final int QUOTA_PER_MODE = 2;

    private static final int MAX_STATION_TRAIN_ROWS = 5;

    private static final int MAX_LINE_IDS_FOR_DISRUPTIONS = 15;





    private static final String[] ELIZABETH_LINE_IDS_FOR_FALLBACK = {
            "elizabeth", "elizabeth-line"
    };

    private static final Map<String, String> ELIZABETH_RAIL_ALIASES = new HashMap<>();
    static {
        ELIZABETH_RAIL_ALIASES.put("HUBLST", "910GLIVST");
        ELIZABETH_RAIL_ALIASES.put("HUBBDS", "910GBONDST");
        ELIZABETH_RAIL_ALIASES.put("HUBTCR", "910GTOTCTRD");
        ELIZABETH_RAIL_ALIASES.put("940GZZLULVT", "910GLIVST");
        ELIZABETH_RAIL_ALIASES.put("940GZZLULDS", "910GLIVST");
        ELIZABETH_RAIL_ALIASES.put("940GZZLUTCR", "910GTOTCTRD");
        ELIZABETH_RAIL_ALIASES.put("940GZZLUBND", "910GBONDST");
    }

    private final MutableLiveData<List<StopItem>> stops = new MutableLiveData<>();
    private final MutableLiveData<List<LineStatus>> disruptions = new MutableLiveData<>();
    private final MutableLiveData<List<Arrival>> selectedStopArrivals = new MutableLiveData<>();

    private final MutableLiveData<List<Arrival>> tflArrivalsToStation = new MutableLiveData<>(Collections.emptyList());

    private final MutableLiveData<List<Arrival>> nationalRailDeparturesFromStation = new MutableLiveData<>(Collections.emptyList());

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






    public void loadNearestStops(double lat, double lon) {
        cachedLat = lat;
        cachedLon = lon;
        errorMessage.setValue(null);
        loading.setValue(true);
        disruptions.setValue(Collections.emptyList());

        new Thread(() -> {
            try {
                TflApi api = RetrofitClient.getApi();


                List<StopItem> busItems = new ArrayList<>();
                Response<StopPointResponse> busesResp = api.getNearbyBuses(lat, lon).execute();
                if (busesResp.isSuccessful() && busesResp.body() != null && busesResp.body().getStopPoints() != null) {
                    for (StopPoint sp : busesResp.body().getStopPoints()) {
                        busItems.add(mapToStopItem(sp, false));
                    }
                }


                List<StopItem> top2Tube = new ArrayList<>();
                List<StopItem> top2Overground = new ArrayList<>();
                List<StopItem> top2Elizabeth = new ArrayList<>();
                List<StopItem> top2Dlr = new ArrayList<>();
                List<StopItem> top2NationalRail = new ArrayList<>();
                bucketAndSelectTrainStations(api, lat, lon, top2Tube, top2Overground, top2Elizabeth, top2Dlr, top2NationalRail);


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


                Iterator<StopItem> iterator = merged.iterator();
                while (iterator.hasNext()) {
                    StopItem stop = iterator.next();
                    if (stop.getLineCodes() == null || stop.getLineCodes().length == 0) {
                        System.out.println("REMOVING GHOST STOP: " + stop.getName());
                        iterator.remove();
                    }
                }


                Collections.sort(merged, STOPS_COMPARATOR);

                stops.postValue(merged);


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


    private static String lineNameToApiId(String name) {
        String n = name.toLowerCase()
                .replace(" line", "")
                .replace(" & ", "-")
                .replace(" and ", "-")
                .replace(' ', '-');
        return n.replaceAll("[^a-z0-9\\-]", "");
    }


    private static void addUpToQuota(List<StopItem> merged, Set<String> addedIds, List<StopItem> source) {
        for (StopItem s : source) {
            if (!hasLines(s)) continue;
            if (addedIds.contains(s.getStopId())) continue;
            merged.add(s);
            addedIds.add(s.getStopId());
        }
    }


    private static boolean hasLines(StopItem s) {
        return s.getLineCodes() != null && s.getLineCodes().length > 0;
    }





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


    private static void addTopN(List<StopItem> out, List<StopPoint> bucket, int n) {
        for (int i = 0; i < Math.min(n, bucket.size()); i++) {
            out.add(mapToStopItem(bucket.get(i), true));
        }
    }





    private static final Comparator<StopItem> STOPS_COMPARATOR = (s1, s2) -> {
        int p1 = getPriority(s1);
        int p2 = getPriority(s2);
        if (p1 != p2) {
            return Integer.compare(p1, p2);
        }
        return Double.compare(s1.getDistance(), s2.getDistance());
    };





    private static int getPriority(StopItem stop) {
        if (containsLine(stop, "South Western Railway")) return 2;
        if (containsTubeLine(stop)) return 3;
        if (!stop.isStation()) return 1;
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





    public static List<StopItem> getHomeHighlights(List<StopItem> nearestStops) {
        List<StopItem> out = new ArrayList<>(2);
        if (nearestStops == null || nearestStops.isEmpty()) return out;

        StopItem station1 = null;
        StopItem station2 = null;
        StopItem fallback1 = null;
        StopItem fallback2 = null;

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





    public void searchStopsByName(android.content.Context context, String query) {
        String q = query != null ? query.trim() : "";
        if (q.isEmpty()) {
            searchResults.postValue(Collections.emptyList());
            return;
        }
        searchLoading.postValue(true);
        searchResults.postValue(Collections.emptyList());
        new Thread(() -> {
            try {
                List<StopItem> list = new ArrayList<>();
                TflApi api = RetrofitClient.getApi();
                Response<TflSearchResponse> resp = api.searchStops(q).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    for (MatchedStop m : resp.body().getMatches()) {
                        list.addAll(mapMatchedStopToStopItems(api, m));
                    }
                    list = deduplicateSearchResultsById(list);
                    list = enrichGenericSearchResults(api, list);
                }
                searchResults.postValue(list);
            } catch (Exception e) {
                searchResults.postValue(Collections.emptyList());
            } finally {
                searchLoading.postValue(false);
            }
        }).start();
    }


    private static List<StopItem> deduplicateSearchResultsById(List<StopItem> list) {
        if (list == null || list.isEmpty()) return list;
        Set<String> seen = new HashSet<>();
        List<StopItem> out = new ArrayList<>();
        for (StopItem s : list) {
            String id = s.getStopId() != null ? s.getStopId().trim() : "";
            if (!id.isEmpty() && seen.add(id)) out.add(s);
        }
        return out;
    }


    private static List<StopItem> mapMatchedStopToStopItems(TflApi api, MatchedStop m) {
        if (m == null) return Collections.emptyList();
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
        if (!containsBusMode(modes)) {
            String[] resolvedCodes = lineCodes;
            StopPoint detailed = fetchStopPointSafely(api, m.getId());
            if (detailed != null) {
                resolvedCodes = stopPointLinesToCodes(api, detailed, lineCodes, m);
            }
            return Collections.singletonList(
                    new StopItem(m.getId(), m.getName(), 0, resolvedCodes, false, "", isStation));
        }
        return mapBusMatchedStopToItems(api, m, lineCodes, isStation);
    }

    private static List<StopItem> mapBusMatchedStopToItems(TflApi api, MatchedStop m, String[] fallbackLineCodes, boolean isStation) {
        List<StopItem> out = new ArrayList<>();
        String stopId = m.getId();
        if (api == null || stopId == null || stopId.trim().isEmpty()) {
            out.add(new StopItem(stopId, m.getName(), 0, fallbackLineCodes, false, "", isStation));
            return out;
        }
        try {
            StopPoint parent = fetchStopPointSafely(api, stopId.trim());
            if (parent != null) {
                String parentLetter = parent.getStopLetter() != null ? parent.getStopLetter().trim() : "";
                if (!parentLetter.isEmpty()) {
                    out.add(new StopItem(
                            m.getId(),
                            m.getName(),
                            0,
                            stopPointLinesToCodes(api, parent, fallbackLineCodes, m),
                            false,
                            parentLetter,
                            isStation));
                    return out;
                }
                for (StopPoint child : parent.getChildren()) {
                    if (child == null) continue;
                    StopPoint detailedChild = child;
                    String childId = child.getNaptanId();
                    if (childId != null && !childId.trim().isEmpty()) {
                        StopPoint fetched = fetchStopPointSafely(api, childId.trim());
                        if (fetched != null) detailedChild = fetched;
                    }
                    String letter = detailedChild.getStopLetter() != null ? detailedChild.getStopLetter().trim() : "";
                    if (letter.isEmpty()) continue;
                    String childName = detailedChild.getCommonName() != null && !detailedChild.getCommonName().trim().isEmpty()
                            ? detailedChild.getCommonName().trim()
                            : m.getName();
                    out.add(new StopItem(
                            detailedChild.getNaptanId(),
                            childName,
                            0,
                            stopPointLinesToCodes(api, detailedChild, fallbackLineCodes, m),
                            false,
                            letter,
                            isStation));
                }
            }
        } catch (Exception ignored) {
        }
        if (!out.isEmpty()) return out;
        out.add(new StopItem(m.getId(), m.getName(), 0, fallbackLineCodes, false, "", isStation));
        return out;
    }

    private static StopPoint fetchStopPointSafely(TflApi api, String stopId) {
        if (api == null || stopId == null || stopId.trim().isEmpty()) return null;
        try {
            Response<StopPoint> resp = api.getStopPoint(stopId.trim()).execute();
            if (!resp.isSuccessful() || resp.body() == null) return null;
            return resp.body();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String[] stopPointLinesToCodes(StopPoint stopPoint, String[] fallbackCodes) {
        return stopPointLinesToCodes(null, stopPoint, fallbackCodes, null);
    }

    private static String[] stopPointLinesToCodes(TflApi api, StopPoint stopPoint, String[] fallbackCodes, MatchedStop matchedStop) {
        if (stopPoint == null || stopPoint.getLines() == null || stopPoint.getLines().isEmpty()) {
            String[] fallback = fallbackCodes != null ? fallbackCodes : new String[0];
            return loadBusLinesFromArrivalsIfNeeded(api, stopPoint, fallback, matchedStop);
        }
        int n = Math.min(4, stopPoint.getLines().size());
        String[] out = new String[n];
        for (int i = 0; i < n; i++) {
            String lineName = stopPoint.getLines().get(i) != null ? stopPoint.getLines().get(i).getName() : "";
            out[i] = lineName != null && !lineName.isEmpty() ? lineName : "";
        }
        return loadBusLinesFromArrivalsIfNeeded(api, stopPoint, out, matchedStop);
    }

    private static String[] loadBusLinesFromArrivalsIfNeeded(TflApi api, StopPoint stopPoint, String[] currentCodes, MatchedStop matchedStop) {
        String[] codes = currentCodes != null ? currentCodes : new String[0];
        if (stopPoint == null || api == null) return codes;
        if (!isGenericModesOnly(codes)) return codes;
        String stopId = stopPoint.getNaptanId();
        if (stopId == null || stopId.trim().isEmpty()) return codes;
        try {
            Set<String> busLines = new LinkedHashSet<>();
            collectBusLinesFromArrivals(api, stopId.trim(), busLines);
            if (busLines.isEmpty()) {
                // Some parent stop points do not expose arrivals directly. Try children in the same payload first.
                for (StopPoint child : stopPoint.getChildren()) {
                    if (child == null || child.getNaptanId() == null || child.getNaptanId().trim().isEmpty()) continue;
                    collectBusLinesFromArrivals(api, child.getNaptanId().trim(), busLines);
                    if (busLines.size() >= 4) break;
                }
            }
            if (busLines.isEmpty()) {
                // Final fallback: refetch stop point details and query any returned children.
                Response<StopPoint> detailResp = api.getStopPoint(stopId.trim()).execute();
                if (detailResp.isSuccessful() && detailResp.body() != null) {
                    for (StopPoint child : detailResp.body().getChildren()) {
                        if (child == null || child.getNaptanId() == null || child.getNaptanId().trim().isEmpty()) continue;
                        collectBusLinesFromArrivals(api, child.getNaptanId().trim(), busLines);
                        if (busLines.size() >= 4) break;
                    }
                }
            }
            if (busLines.isEmpty() && matchedStop != null) {
                // Final fallback for search matches where parent/children have no direct line metadata.
                // Query nearby bus stop points around the match and use best-name-match (or nearest) route lines.
                Response<StopPointResponse> nearbyResp = api.getNearbyBuses(matchedStop.getLat(), matchedStop.getLon()).execute();
                if (nearbyResp.isSuccessful() && nearbyResp.body() != null && nearbyResp.body().getStopPoints() != null) {
                    List<StopPoint> nearbyStops = nearbyResp.body().getStopPoints();
                    String target = matchedStop.getName() != null ? matchedStop.getName().trim() : "";
                    StopPoint bestByName = null;
                    for (StopPoint nearby : nearbyStops) {
                        if (nearby == null) continue;
                        if (isLikelySameStopName(target, nearby.getCommonName())) {
                            bestByName = nearby;
                            break;
                        }
                    }
                    StopPoint source = bestByName != null
                            ? bestByName
                            : (!nearbyStops.isEmpty() ? nearbyStops.get(0) : null);
                    if (source != null && source.getLines() != null) {
                        for (int i = 0; i < source.getLines().size() && busLines.size() < 4; i++) {
                            String ln = source.getLines().get(i) != null ? source.getLines().get(i).getName() : "";
                            if (ln != null && !ln.trim().isEmpty()) busLines.add(ln.trim());
                        }
                    }
                }
            }
            if (busLines.isEmpty()) return codes;
            return busLines.toArray(new String[0]);
        } catch (Exception ignored) {
            return codes;
        }
    }

    private static void collectBusLinesFromArrivals(TflApi api, String stopId, Set<String> out) {
        if (api == null || stopId == null || stopId.isEmpty() || out == null || out.size() >= 4) return;
        try {
            Response<List<ArrivalPrediction>> resp = api.getArrivals(stopId).execute();
            if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) return;
            for (ArrivalPrediction p : resp.body()) {
                if (p == null) continue;
                String mode = p.getModeName() != null ? p.getModeName().toLowerCase(Locale.UK) : "";
                String line = p.getLineName() != null ? p.getLineName().trim() : "";
                if (!mode.contains("bus") || line.isEmpty()) continue;
                out.add(line);
                if (out.size() >= 4) return;
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isGenericModesOnly(String[] codes) {
        if (codes == null || codes.length == 0) return true;
        if (codes.length > 4) return false;
        for (String code : codes) {
            String c = code != null ? code.trim().toLowerCase(Locale.UK) : "";
            if (c.isEmpty()) continue;
            if ("bus".equals(c) || "tube".equals(c) || "tram".equals(c) || "dlr".equals(c)
                    || "national rail".equals(c) || "london overground".equals(c)
                    || "elizabeth line".equals(c)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static List<StopItem> enrichGenericSearchResults(TflApi api, List<StopItem> list) {
        if (api == null || list == null || list.isEmpty()) return list;
        List<StopItem> out = new ArrayList<>(list.size());
        for (StopItem s : list) {
            if (s == null || !isGenericModesOnly(s.getLineCodes())) {
                out.add(s);
                continue;
            }
            String[] resolved = resolveLineCodesForGenericStop(api, s);
            out.add(new StopItem(
                    s.getStopId(),
                    s.getName(),
                    s.getDistance(),
                    resolved,
                    s.isStepFree(),
                    s.getStopLetter(),
                    s.isStation()
            ));
        }
        return out;
    }

    private static String[] resolveLineCodesForGenericStop(TflApi api, StopItem item) {
        String[] fallback = item != null && item.getLineCodes() != null ? item.getLineCodes() : new String[0];
        if (api == null || item == null) return fallback;

        String stopId = item.getStopId() != null ? item.getStopId().trim() : "";
        if (!stopId.isEmpty() && !stopId.contains(",")) {
            StopPoint byId = fetchStopPointSafely(api, stopId);
            if (byId != null) {
                String[] byIdCodes = stopPointLinesToCodes(api, byId, fallback, null);
                if (!isGenericModesOnly(byIdCodes)) return byIdCodes;
            }
        }

        String name = item.getName() != null ? item.getName().trim() : "";
        if (name.isEmpty()) return fallback;
        try {
            Response<TflSearchResponse> byNameResp = api.searchStops(name).execute();
            if (!byNameResp.isSuccessful() || byNameResp.body() == null || byNameResp.body().getMatches() == null) {
                return fallback;
            }
            for (MatchedStop candidate : byNameResp.body().getMatches()) {
                if (candidate == null || !containsBusMode(candidate.getModes())) continue;
                if (!isLikelySameStopName(name, candidate.getName())) continue;
                StopPoint point = fetchStopPointSafely(api, candidate.getId());
                if (point == null) continue;
                String[] candidateCodes = stopPointLinesToCodes(api, point, fallback, candidate);
                if (!isGenericModesOnly(candidateCodes)) return candidateCodes;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static boolean isLikelySameStopName(String searchName, String candidateName) {
        String a = normalizeStopName(searchName);
        String b = normalizeStopName(candidateName);
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.equals(b)) return true;
        if (a.contains(b) || b.contains(a)) return true;
        String[] aTokens = a.split(" ");
        String[] bTokens = b.split(" ");
        if (aTokens.length == 0 || bTokens.length == 0) return false;
        int matches = 0;
        for (String token : aTokens) {
            if (token.isEmpty()) continue;
            if (Arrays.asList(bTokens).contains(token)) matches++;
        }
        return matches >= Math.min(2, aTokens.length);
    }

    private static String normalizeStopName(String raw) {
        if (raw == null) return "";
        String n = raw.toLowerCase(Locale.UK);
        n = n.replaceAll("\\(stop\\s+[a-z0-9]+\\)", " ");
        n = n.replace("stn", "station");
        n = n.replace("&", " ");
        n = n.replace("/", " ");
        n = n.replaceAll("[^a-z0-9 ]", " ");
        n = n.replaceAll("\\b(stop|station|underground|bus|road|rd|street|st)\\b", " ");
        n = n.replaceAll("\\s+", " ").trim();
        return n;
    }

    private static boolean containsBusMode(List<String> modes) {
        if (modes == null || modes.isEmpty()) return false;
        for (String mode : modes) {
            if (mode != null && mode.toLowerCase(Locale.UK).contains("bus")) return true;
        }
        return false;
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






    public void loadTrainsToAndFrom(String stopId) {
        loadTflArrivalsOnly(stopId);
        loadNationalRailDeparturesOnly(stopId);
    }


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
                list = dropArrivalsUnderOneMinuteForStationTrains(list);
                tflArrivalsToStation.postValue(truncateToTopN(list, MAX_STATION_TRAIN_ROWS));
            } catch (Exception e) {
                tflArrivalsToStation.postValue(Collections.emptyList());
            }
        }).start();
    }


    private void loadNationalRailDeparturesOnly(String stopId) {
        if (stopId == null || stopId.isEmpty()) {
            nationalRailDeparturesFromStation.postValue(Collections.emptyList());
            return;
        }
        final String sid = stopId;
        new Thread(() -> {
            try {
                if (!ApiKeyManager.isRailTokenValid()) {
                    postTflFromStationTrainsFallback(sid);
                    return;
                }

                TflApi api = RetrofitClient.getApi();
                String crs = resolveNationalRailCrs(api, sid);

                if (crs == null) {
                    postTflFromStationTrainsFallback(sid);
                    return;
                }

                NationalRailApi railApi = new NationalRailApi();
                railApi.getDepartureBoard(crs, null, new NationalRailApi.DepartureBoardCallback() {
                    @Override
                    public void onDepartures(List<Arrival> arrivals) {
                        List<Arrival> filtered = filterAndSortNationalRailArrivals(arrivals);
                        filtered = dropArrivalsUnderOneMinuteForStationTrains(filtered);
                        if (!filtered.isEmpty()) {
                            nationalRailDeparturesFromStation.postValue(truncateToTopN(filtered, MAX_STATION_TRAIN_ROWS));
                        } else if (arrivals != null && !arrivals.isEmpty()) {

                            nationalRailDeparturesFromStation.postValue(truncateToTopN(
                                    filterAndSortNationalRailArrivals(arrivals), MAX_STATION_TRAIN_ROWS));
                        } else {
                            postTflFromStationTrainsFallback(sid);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        postTflFromStationTrainsFallback(sid);
                    }
                });
            } catch (Exception e) {
                postTflFromStationTrainsFallback(sid);
            }
        }).start();
    }





    private void postTflFromStationTrainsFallback(String stopId) {
        new Thread(() -> {
            try {
                if (!ApiKeyManager.isTflKeyValid()) {
                    nationalRailDeparturesFromStation.postValue(Collections.emptyList());
                    return;
                }
                TflApi api = RetrofitClient.getApi();
                List<String> stopIdsToTry = new ArrayList<>();
                addUniqueStopId(stopIdsToTry, stopId);
                appendElizabethRailAliases(stopId, stopIdsToTry);
                try {
                    Response<StopPoint> spR = api.getStopPoint(stopId).execute();
                    if (spR.isSuccessful() && spR.body() != null) {
                        for (StopPoint ch : spR.body().getChildren()) {
                            if (ch == null) continue;
                            String nid = ch.getNaptanId();
                            addUniqueStopId(stopIdsToTry, nid);
                            appendElizabethRailAliases(nid, stopIdsToTry);
                        }
                    }
                } catch (Exception ignored) {

                }

                List<ArrivalPrediction> preds = new ArrayList<>();
                outerEliz:
                for (String tryId : stopIdsToTry) {
                    if (tryId == null || tryId.isEmpty()) continue;
                    for (String lineId : ELIZABETH_LINE_IDS_FOR_FALLBACK) {
                        try {
                            Response<List<ArrivalPrediction>> eliz = api.getLineArrivals(lineId, tryId).execute();
                            if (eliz.isSuccessful() && eliz.body() != null && !eliz.body().isEmpty()) {
                                preds.addAll(eliz.body());
                                break outerEliz;
                            }
                        } catch (Exception ignored) {

                        }
                    }
                }
                if (preds.isEmpty()) {
                    for (String tryId : stopIdsToTry) {
                        List<ArrivalPrediction> all = fetchArrivalsFromApi(api, tryId);
                        for (ArrivalPrediction p : all) {
                            String m = p.getModeName();
                            if (m == null) continue;
                            String ml = m.toLowerCase(Locale.UK);
                            if (ml.contains("elizabeth") || ml.contains("national-rail") || ml.contains("overground")) {
                                preds.add(p);
                            }
                        }
                        if (!preds.isEmpty()) break;
                    }
                }
                if (preds.isEmpty()) {
                    for (String tryId : stopIdsToTry) {
                        List<ArrivalPrediction> all = fetchArrivalsFromApi(api, tryId);
                        if (!all.isEmpty()) {
                            preds.addAll(all);
                            break;
                        }
                    }
                }
                List<Arrival> list = new ArrayList<>();
                for (ArrivalPrediction p : preds) {
                    list.add(new Arrival(p.getLineName(), p.getDestinationName(), p.getPlatformName(), p.getTimeToStation(), p.getModeName()));
                }
                list = filterAndSortArrivals(list);
                list = dropArrivalsUnderOneMinuteForStationTrains(list);
                if (list.isEmpty() && !preds.isEmpty()) {
                    list = new ArrayList<>();
                    for (ArrivalPrediction p : preds) {
                        list.add(new Arrival(p.getLineName(), p.getDestinationName(), p.getPlatformName(), p.getTimeToStation(), p.getModeName()));
                    }
                    list = filterAndSortNationalRailArrivals(list);
                    list = truncateToTopN(list, MAX_STATION_TRAIN_ROWS);
                } else {
                    list = truncateToTopN(list, MAX_STATION_TRAIN_ROWS);
                }
                nationalRailDeparturesFromStation.postValue(list);
            } catch (Exception e) {
                nationalRailDeparturesFromStation.postValue(Collections.emptyList());
            }
        }).start();
    }

    private static void addUniqueStopId(List<String> list, String id) {
        if (list == null || id == null || id.isEmpty()) return;
        if (!list.contains(id)) list.add(id);
    }


    private static void appendElizabethRailAliases(String stopId, List<String> out) {
        if (stopId == null || out == null) return;
        String u = stopId.toUpperCase(Locale.UK).trim();
        String rail = ELIZABETH_RAIL_ALIASES.get(u);
        if (rail != null) addUniqueStopId(out, rail);
        for (Map.Entry<String, String> e : ELIZABETH_RAIL_ALIASES.entrySet()) {
            if (e.getKey().startsWith("940") && u.contains(e.getKey())) {
                addUniqueStopId(out, e.getValue());
            }
        }
    }





    private static String resolveNationalRailCrs(TflApi api, String stopId) throws java.io.IOException {
        String crs = CrsLookup.getCrs(stopId);
        if (crs != null) return crs;

        Response<StopPoint> spResp = api.getStopPoint(stopId).execute();
        if (!spResp.isSuccessful() || spResp.body() == null) return null;
        return findCrsInStopTree(spResp.body(), 6);
    }


    private static String findCrsInStopTree(StopPoint sp, int depth) {
        if (sp == null || depth < 0) return null;
        String crs = CrsLookup.getCrs(sp.getNaptanId());
        if (crs != null) return crs;
        crs = sp.getCrsFromAdditionalProperties();
        if (crs != null) return crs;
        crs = CrsLookup.getCrsFromName(sp.getCommonName());
        if (crs != null) return crs;
        for (StopPoint child : sp.getChildren()) {
            crs = findCrsInStopTree(child, depth - 1);
            if (crs != null) return crs;
        }
        return null;
    }

    private List<ArrivalPrediction> fetchArrivalsFromApi(TflApi api, String stopId) throws java.io.IOException {
        Response<List<ArrivalPrediction>> resp = api.getArrivals(stopId).execute();
        if (resp.isSuccessful() && resp.body() != null) {
            return resp.body();
        }
        return Collections.emptyList();
    }


    private List<ArrivalPrediction> fetchLineArrivalsFallback(TflApi api, String stopId) {
        String[] nationalRailLines = {"south-western-railway", "southern", "southeastern", "thameslink", "great-western-railway", "greater-anglia", "c2c"};
        for (String lineId : nationalRailLines) {
            try {
                Response<List<ArrivalPrediction>> resp = api.getLineArrivals(lineId, stopId).execute();
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    return resp.body();
                }
            } catch (Exception ignored) {

            }
        }
        return Collections.emptyList();
    }


    private static List<Arrival> filterAndSortArrivals(List<Arrival> list) {
        if (list == null) return Collections.emptyList();
        List<Arrival> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Arrival a : list) {
            int sec = a.getTimeToStationSeconds();
            if (sec < 60) continue;
            if (sec > 3600) continue;
            String key = (a.getDestinationName() != null ? a.getDestinationName() : "") + "|" + (sec / 60);
            if (!seen.add(key)) continue;
            out.add(a);
        }
        Collections.sort(out, (a, b) -> Integer.compare(a.getTimeToStationSeconds(), b.getTimeToStationSeconds()));
        return out;
    }





    private static List<Arrival> filterAndSortNationalRailArrivals(List<Arrival> list) {
        if (list == null) return Collections.emptyList();
        List<Arrival> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
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
        Collections.sort(out, (a1, b1) -> Integer.compare(a1.getTimeToStationSeconds(), b1.getTimeToStationSeconds()));
        return out;
    }




    private static List<Arrival> dropArrivalsUnderOneMinuteForStationTrains(List<Arrival> list) {
        if (list == null) return Collections.emptyList();
        List<Arrival> out = new ArrayList<>();
        for (Arrival a : list) {
            if (a == null) continue;
            if (a.getTimeToStationSeconds() < 60) continue;
            out.add(a);
        }
        return out;
    }


    private static <T> List<T> truncateToTopN(List<T> list, int n) {
        if (list == null) return Collections.emptyList();
        if (n <= 0) return Collections.emptyList();
        if (list.size() <= n) return list;
        return new ArrayList<>(list.subList(0, n));
    }


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


    private boolean checkIsNationalRailNoData(TflApi api, String stopId) {
        try {
            Response<StopPoint> resp = api.getStopPoint(stopId).execute();
            return resp.isSuccessful() && resp.body() != null && resp.body().hasNationalRailMode();
        } catch (Exception e) {
            return false;
        }
    }


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

