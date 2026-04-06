package com.example.ajp.utils;

import android.content.Context;
import android.location.Address;
import com.example.ajp.R;
import com.example.ajp.api.InstructionRef;
import com.example.ajp.api.Journey;
import com.example.ajp.api.JourneyPlace;
import com.example.ajp.api.JourneyResponse;
import com.example.ajp.api.Leg;
import com.example.ajp.api.MatchedStop;
import com.example.ajp.api.RetrofitClient;
import com.example.ajp.api.RouteOptionRef;
import com.example.ajp.api.StopPoint;
import com.example.ajp.api.StopPointResponse;
import com.example.ajp.api.TflApi;
import com.example.ajp.api.TflSearchResponse;
import com.example.ajp.ui.journey.RouteItem;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import retrofit2.Response;

/** Resolves from/to, calls TfL journey API, maps legs to RouteItems, optional lift checks. */
public final class JourneyFetcher {

    private static final Pattern COORDS_PATTERN = Pattern.compile(
            "^\\s*-?\\d+(?:\\.\\d+)?\\s*,\\s*-?\\d+(?:\\.\\d+)?\\s*$");

    private static final double EARTH_RADIUS_M = 6371000.0;

    private static final Set<String> QUERY_STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "and", "for", "near", "bus", "train", "tube", "stop", "station", "rail",
            "underground", "dlr", "line", "at", "to", "of", "in", "on", "a", "an", "or"));

    private final Context appContext;

    public JourneyFetcher(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
    }

    public static String buildSignature(List<RouteItem> routes) {
        if (routes == null || routes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < routes.size(); i++) {
            RouteItem r = routes.get(i);
            // Keep this stable for background change detection between polling runs.
            sb.append(i).append(':').append(r.getRouteId()).append(':').append(r.getDurationMinutes()).append(';');
        }
        return sb.toString();
    }

    public FetchResult fetch(String fromInput, String toInput, String timeHHmm, String dateyyyyMMdd) {
        if (appContext == null) {
            return FetchResult.fail("No context");
        }
        try {
            return executeFetch(fromInput, toInput, timeHHmm, dateyyyyMMdd);
        } catch (IOException e) {
            if (isRetryableTimeout(e)) {
                try {
                    Thread.sleep(450);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return FetchResult.fail(e.getMessage() != null ? e.getMessage() : "Network error");
                }
                try {
                    return executeFetch(fromInput, toInput, timeHHmm, dateyyyyMMdd);
                } catch (IOException e2) {
                    return FetchResult.fail(e2.getMessage() != null ? e2.getMessage() : "Network error");
                } catch (Exception e2) {
                    return FetchResult.fail(e2.getMessage() != null ? e2.getMessage() : "Failed to plan journey");
                }
            }
            return FetchResult.fail(e.getMessage() != null ? e.getMessage() : "Network error");
        } catch (Exception e) {
            return FetchResult.fail(e.getMessage() != null ? e.getMessage() : "Failed to plan journey");
        }
    }

    /** One full plan attempt; IOException from Retrofit/OkHttp (timeouts, connection errors). */
    private FetchResult executeFetch(String fromInput, String toInput, String timeHHmm, String dateyyyyMMdd)
            throws IOException {
        TflApi api = RetrofitClient.getApi();
        AccessibilityPreferences acc = AccessibilityPreferences.get(appContext);
        // Resolve from and to concurrently (each may hit TfL + geocoder + nearby stops).
        ResolvedEndpoint fromEp;
        ResolvedEndpoint toEp;
        ExecutorService endpointsPool = Executors.newFixedThreadPool(2);
        try {
            Future<ResolvedEndpoint> fromFuture = endpointsPool.submit(() -> {
                try {
                    return resolveEndpoint(api, fromInput, acc, false);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            Future<ResolvedEndpoint> toFuture = endpointsPool.submit(() -> {
                try {
                    return resolveEndpoint(api, toInput, acc, true);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            try {
                fromEp = fromFuture.get();
                toEp = toFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted");
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof UncheckedIOException) {
                    throw ((UncheckedIOException) c).getCause();
                }
                if (c instanceof IOException) {
                    throw (IOException) c;
                }
                if (c instanceof Error) {
                    throw (Error) c;
                }
                throw new IOException(c != null ? c.getMessage() : "Failed to resolve endpoint");
            }
        } finally {
            endpointsPool.shutdown();
        }
        String walkingSpeed = acc.getWalkingSpeed();
        int maxWalk = acc.getMaxWalkingMinutes();
        String accessibility = acc.isStepFree() ? "noSolidStairs" : null;

        Response<JourneyResponse> response = requestJourneys(
                api, fromEp, toEp, timeHHmm, dateyyyyMMdd, walkingSpeed, maxWalk, accessibility, null);

        if (!response.isSuccessful()) {
            return FetchResult.fail("Journey request failed (" + response.code() + ")");
        }
        JourneyResponse body = response.body();
        if (body == null || body.getJourneys().isEmpty()) {
            return FetchResult.fail("No routes found");
        }
        List<Journey> mergedJourneys = new ArrayList<>(body.getJourneys());

        // Step-free requests can occasionally miss bus-only options in the default response.
        if (!hasBusJourney(mergedJourneys)) {
            try {
                Response<JourneyResponse> busOnlyResponse = requestJourneys(
                        api, fromEp, toEp, timeHHmm, dateyyyyMMdd, walkingSpeed, maxWalk, accessibility, "bus");
                if (busOnlyResponse.isSuccessful()
                        && busOnlyResponse.body() != null
                        && busOnlyResponse.body().getJourneys() != null) {
                    mergedJourneys.addAll(busOnlyResponse.body().getJourneys());
                }
            } catch (IOException ignored) {
                // Keep primary results even if bus supplement fails.
            }
        }

        boolean stepFree = acc.isStepFree();
        LiftDisruptionChecker liftChecker = stepFree ? new LiftDisruptionChecker(appContext) : null;
        String userFrom = fromInput != null ? fromInput.trim() : "";
        String userTo = toInput != null ? toInput.trim() : "";
        List<RouteItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int idx = 0;
        for (Journey j : mergedJourneys) {
            RouteItem item = mapJourneyToRouteItem(j, idx++, liftChecker, api, userFrom, userTo,
                    fromEp, toEp, walkingSpeed, maxWalk);
            if (item != null) {
                String sig = item.getDepartureTime() + "|" + item.getArrivalTime() + "|" + item.getDurationMinutes()
                        + "|" + item.getRouteSummary();
                if (seen.add(sig)) {
                    items.add(item);
                }
            }
        }
        if (items.isEmpty()) {
            return FetchResult.fail("No routes found");
        }
        return FetchResult.ok(items);
    }

    private static boolean isRetryableTimeout(IOException e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
            String m = t.getMessage();
            if (m != null) {
                String lower = m.toLowerCase(Locale.UK);
                if (lower.contains("timeout") || lower.contains("timed out")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static Response<JourneyResponse> requestJourneys(TflApi api, ResolvedEndpoint fromEp, ResolvedEndpoint toEp,
            String timeHHmm, String dateyyyyMMdd, String walkingSpeed, int maxWalk, String accessibility, String mode)
            throws IOException {
        return api.getJourneyResults(
                fromEp.journeyPath,
                toEp.journeyPath,
                timeHHmm,
                dateyyyyMMdd,
                "Departing",
                walkingSpeed,
                maxWalk,
                accessibility,
                mode
        ).execute();
    }

    /** True when nearest stop is a bus-style stop (not tube/rail/DLR/etc.) so the first step says "bus stop". */
    private static boolean isStreetLevelConnectorStop(StopPoint s) {
        if (s == null) return true;
        if (s.isTubeStation() || s.isRailStation()) return false;
        for (String m : s.getModes()) {
            if (m == null) continue;
            String low = m.toLowerCase(Locale.UK);
            if (low.contains("dlr") || low.contains("tram") || low.contains("elizabeth") || low.contains("overground")) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasBusJourney(List<Journey> journeys) {
        if (journeys == null || journeys.isEmpty()) return false;
        for (Journey j : journeys) {
            if (j == null || j.getLegs() == null) continue;
            for (Leg leg : j.getLegs()) {
                if (leg != null && leg.getMode() != null) {
                    String n = leg.getMode().getName();
                    if (n != null && n.toLowerCase(Locale.UK).contains("bus")) return true;
                }
            }
        }
        return false;
    }

    /**
     * TfL stop search first; if that fails, geocode in London and snap to the nearest bus/rail stop.
     * Brand + location text is split for both endpoints; the brand is used for the POI verification warning only.
     * Split queries skip the origin TfL name shortcut so we geocode and add a place-to-stop walk from the pin.
     */
    private ResolvedEndpoint resolveEndpoint(TflApi api, String input, AccessibilityPreferences acc,
            boolean isDestination) throws IOException {
        String t = input != null ? input.trim() : "";
        if (t.isEmpty()) {
            throw new IOException("Empty place");
        }
        BrandLocationSplit bl = BrandLocationSplit.analyze(t);
        String resolveQuery = bl.isSplit() ? bl.getLocationQuery() : t;
        String brandPhrase = bl.isSplit() ? bl.getBrandDisplay() : null;
        String locationLabel = bl.isSplit() ? bl.getLocationQuery() : null;

        if (COORDS_PATTERN.matcher(t).matches()) {
            return ResolvedEndpoint.direct(t.replaceAll("\\s+", ""));
        }

        Response<TflSearchResponse> resp = api.searchStops(resolveQuery).execute();
        if (resp.isSuccessful() && resp.body() != null && !resp.body().getMatches().isEmpty()) {
            MatchedStop m = resp.body().getMatches().get(0);
            // "waterloo" → all words match the stop name, journey ends at the station.
            // Destination: never shortcut here — TfL's first hit is often a roadside stop while the map pin is the
            // real place; we always geocode and add a final "Walk to [your text]" from the nearest stop to that pin.
            if (allSignificantQueryTokensAppearInStopName(resolveQuery, m.getName())) {
                // Split queries need geocoding + place anchor for a correct pin, even when from matches a stop name.
                if (!isDestination && !bl.isSplit()) {
                    return ResolvedEndpoint.direct(m.getLat() + "," + m.getLon());
                }
            }
        }
        Address addr = PlaceSearch.geocodeFirstAddress(appContext, resolveQuery);
        if (addr == null) {
            throw new IOException(appContext.getString(R.string.could_not_find_place, resolveQuery));
        }
        double placeLat = addr.getLatitude();
        double placeLon = addr.getLongitude();
        String placeLabel = PlaceSearch.buildAddressLine(addr);
        if (placeLabel == null || placeLabel.isEmpty()) {
            placeLabel = resolveQuery;
        }
        StopPoint nearest = findNearestTransitStop(api, placeLat, placeLon);
        if (nearest == null) {
            throw new IOException(appContext.getString(R.string.no_transit_near_place, resolveQuery));
        }
        nearest = ensureStopCoordinates(api, nearest);
        String path = journeyPathForStop(nearest);
        PlaceAnchor anchor = new PlaceAnchor(placeLat, placeLon, placeLabel, nearest);
        return new ResolvedEndpoint(path, anchor, brandPhrase, locationLabel);
    }

    /**
     * Detects brand + place in either order (e.g. McDonald's elderfield place, elderfield place McDonald's,
     * Barclays Canary Wharf, Canary Wharf Barclays). Brand text is kept for the verification warning.
     */
    private static final class BrandLocationSplit {
        private static final Set<String> STREET_PLACE_SUFFIXES = new HashSet<>(Arrays.asList(
                "place", "road", "street", "lane", "avenue", "close", "court", "drive", "way", "square",
                "gardens", "park", "wharf", "hill", "mews", "terrace", "crescent", "grove", "yard", "row",
                "end", "rise", "gate", "walk", "bridge", "common", "green"));

        private static final Set<String> NO_SPLIT_SECOND = new HashSet<>(Arrays.asList(
                "station", "railway", "underground", "central", "circle", "district", "line", "loop"));

        /** Last token is usually geography/infrastructure, not a shop/bank name (e.g. "Canary Wharf Barclays"). */
        private static final Set<String> TRAILING_BRAND_LAST_BLOCKLIST = new HashSet<>(Arrays.asList(
                "station", "airport", "international", "junction", "interchange", "national", "north", "south",
                "east", "west", "central", "express", "pier", "terminal", "line", "bridge", "gate", "circus",
                "underground", "overground", "common", "green", "hill", "park", "wood", "town", "city",
                "quay", "market", "hall", "house", "centre", "center", "village", "county", "college",
                "university", "hospital", "museum", "gallery", "wharf", "dock", "docks", "basin", "marina",
                "plaza", "gardens", "heath", "field", "fields", "copse", "rise", "end", "change", "exchange",
                "arcade", "mall", "heights", "tower", "towers", "point", "cross", "royal", "estate", "works"));

        final boolean split;
        final String locationQuery;
        final String brandDisplay;

        private BrandLocationSplit(boolean split, String locationQuery, String brandDisplay) {
            this.split = split;
            this.locationQuery = locationQuery;
            this.brandDisplay = brandDisplay;
        }

        static BrandLocationSplit none() {
            return new BrandLocationSplit(false, null, null);
        }

        static BrandLocationSplit analyze(String input) {
            if (input == null) return none();
            String trimmed = input.trim();
            if (trimmed.isEmpty()) return none();
            String[] raw = trimmed.split("\\s+");
            if (raw.length < 2) return none();

            String lastNorm = normToken(raw[raw.length - 1]);
            if (raw.length >= 3 && STREET_PLACE_SUFFIXES.contains(lastNorm)) {
                String location = String.join(" ", Arrays.copyOfRange(raw, raw.length - 2, raw.length));
                String brand = String.join(" ", Arrays.copyOfRange(raw, 0, raw.length - 2));
                if (brand.isEmpty()) return none();
                return new BrandLocationSplit(true, location, brand);
            }
            if (raw.length == 2) {
                if (NO_SPLIT_SECOND.contains(lastNorm)) return none();
                String first = raw[0];
                if (first.contains("'") || first.contains("\u2019")) {
                    return new BrandLocationSplit(true, raw[1], raw[0]);
                }
            }
            // "elderfield place McDonald's" — brand with apostrophe often appears last.
            String lastTok = raw[raw.length - 1];
            if (raw.length >= 2 && (lastTok.contains("'") || lastTok.contains("\u2019"))) {
                String brand = lastTok;
                String location = String.join(" ", Arrays.copyOfRange(raw, 0, raw.length - 1)).trim();
                if (location.isEmpty()) return none();
                return new BrandLocationSplit(true, location, brand);
            }
            // "Canary Wharf Barclays" — multi-word place + trailing capitalised brand (same warning as brand-first order).
            if (raw.length >= 3 && !STREET_PLACE_SUFFIXES.contains(lastNorm) && isLikelyTrailingBrandWord(lastTok)) {
                String brand = lastTok;
                String location = String.join(" ", Arrays.copyOfRange(raw, 0, raw.length - 1)).trim();
                if (!location.isEmpty()) {
                    return new BrandLocationSplit(true, location, brand);
                }
            }
            // "McDonald's Clapham Junction" — "Junction" is blocklisted as a trailing brand token, so handle explicitly.
            if (raw.length == 3 && "junction".equals(normToken(raw[2]))
                    && isLikelyBrandFirstToken(raw[0])
                    && normToken(raw[1]).length() >= 4) {
                return new BrandLocationSplit(true, raw[1] + " " + raw[2], raw[0]);
            }
            return none();
        }

        /** Brand at start of query: apostrophe (McDonald's) or long capitalised token (McDonalds). */
        private static boolean isLikelyBrandFirstToken(String token) {
            if (token == null || token.isEmpty()) return false;
            if (token.contains("'") || token.contains("\u2019")) return true;
            return token.length() >= 6 && isLikelyTrailingBrandWord(token);
        }

        private static boolean isLikelyTrailingBrandWord(String token) {
            if (token == null || token.length() < 4) return false;
            if (!Character.isUpperCase(token.charAt(0))) return false;
            for (int i = 1; i < token.length(); i++) {
                char c = token.charAt(i);
                if (c == '\'' || c == '\u2019') continue;
                if (!Character.isLetter(c)) return false;
            }
            String n = normToken(token);
            return !n.isEmpty() && !TRAILING_BRAND_LAST_BLOCKLIST.contains(n);
        }

        boolean isSplit() {
            return split;
        }

        String getLocationQuery() {
            return locationQuery;
        }

        String getBrandDisplay() {
            return brandDisplay;
        }
    }

    private static String normToken(String w) {
        if (w == null) return "";
        return w.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.UK);
    }

    /**
     * "waterloo" / "canary wharf" match the station name; "barclays canary wharf" does not (barclays not in name).
     */
    private static boolean allSignificantQueryTokensAppearInStopName(String userQuery, String stopName) {
        if (userQuery == null || stopName == null) return false;
        String name = stopName.trim().toLowerCase(Locale.UK);
        String[] parts = userQuery.trim().toLowerCase(Locale.UK).split("\\s+");
        boolean anySignificant = false;
        for (String raw : parts) {
            String part = raw.replaceAll("[^a-z0-9]", "");
            if (part.length() < 3) continue;
            if (QUERY_STOPWORDS.contains(part)) continue;
            anySignificant = true;
            if (!name.contains(part)) {
                return false;
            }
        }
        return anySignificant;
    }

    private static StopPoint findNearestTransitStop(TflApi api, double lat, double lon) throws IOException {
        List<StopPoint> all = new ArrayList<>();
        // Run bus and train nearby calls in parallel (separate pool so parent can also resolve from/to in parallel).
        ExecutorService nearbyPool = Executors.newFixedThreadPool(2);
        Response<StopPointResponse> buses;
        Response<StopPointResponse> trains;
        try {
            Future<Response<StopPointResponse>> busesFuture = nearbyPool.submit(() -> api.getNearbyBuses(lat, lon).execute());
            Future<Response<StopPointResponse>> trainsFuture = nearbyPool.submit(() -> api.getNearbyTrains(lat, lon).execute());
            try {
                buses = busesFuture.get();
                trains = trainsFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted");
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof IOException) {
                    throw (IOException) c;
                }
                if (c instanceof Error) {
                    throw (Error) c;
                }
                throw new IOException(c != null ? c.getMessage() : "Nearby stop lookup failed");
            }
        } finally {
            nearbyPool.shutdown();
        }
        if (buses.isSuccessful() && buses.body() != null && buses.body().getStopPoints() != null) {
            all.addAll(buses.body().getStopPoints());
        }
        if (trains.isSuccessful() && trains.body() != null && trains.body().getStopPoints() != null) {
            all.addAll(trains.body().getStopPoints());
        }
        if (all.isEmpty()) return null;
        StopPoint best = null;
        double bestD = Double.MAX_VALUE;
        for (StopPoint sp : all) {
            if (sp == null) continue;
            double d = sp.getDistance();
            if (d <= 0) {
                d = haversineMeters(lat, lon, sp.getLat(), sp.getLon());
            }
            if (d < bestD) {
                bestD = d;
                best = sp;
            }
        }
        return best;
    }

    /** Nearby responses usually include lat/lon; if not, load stop details from TfL. */
    private static String journeyPathForStop(StopPoint sp) {
        if (sp == null) return "";
        if (Math.abs(sp.getLat()) > 0.0001 || Math.abs(sp.getLon()) > 0.0001) {
            return sp.getLat() + "," + sp.getLon();
        }
        String id = sp.getNaptanId();
        return id != null ? id.trim() : "";
    }

    private StopPoint ensureStopCoordinates(TflApi api, StopPoint sp) throws IOException {
        if (sp == null) return null;
        if (Math.abs(sp.getLat()) > 0.0001 && Math.abs(sp.getLon()) > 0.0001) {
            return sp;
        }
        String id = sp.getNaptanId();
        if (id == null || id.trim().isEmpty()) return sp;
        Response<StopPoint> r = api.getStopPoint(id.trim()).execute();
        if (r.isSuccessful() && r.body() != null) {
            return r.body();
        }
        return sp;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    private static double walkingSpeedToMetersPerSecond(String walkingSpeed) {
        if (AccessibilityPreferences.SPEED_SLOW.equals(walkingSpeed)) return 1.1;
        if (AccessibilityPreferences.SPEED_FAST.equals(walkingSpeed)) return 1.8;
        return 1.4;
    }

    private static int estimateWalkSeconds(double meters, String walkingSpeed) {
        double mps = walkingSpeedToMetersPerSecond(walkingSpeed);
        int sec = (int) Math.round(meters / mps);
        return Math.max(60, sec);
    }

    private static boolean isWalkingLeg(Leg leg) {
        return leg != null && leg.getMode() != null
                && "walking".equalsIgnoreCase(leg.getMode().getName());
    }

    private static Leg firstNonWalkingLeg(ArrayList<Leg> legs) {
        if (legs == null) return null;
        for (Leg lg : legs) {
            if (lg != null && !isWalkingLeg(lg)) return lg;
        }
        return null;
    }

    private static boolean legModeLooksLikeBus(Leg leg) {
        if (leg == null || leg.getMode() == null) return false;
        String n = leg.getMode().getName();
        return n != null && n.toLowerCase(Locale.UK).contains("bus");
    }

    /**
     * TfL often ends with a walking leg toward the destination area when using coordinates. For place search we
     * already add a single walk from the nearest stop to the geocoded address — drop TfL's tail walk(s) to avoid
     * two walk steps in a row.
     */
    private static long stripTrailingWalkingLegs(ArrayList<Leg> legs) {
        long removedSeconds = 0;
        while (legs.size() > 1 && isWalkingLeg(legs.get(legs.size() - 1))) {
            Leg tail = legs.remove(legs.size() - 1);
            removedSeconds += tail.getDuration();
        }
        return removedSeconds;
    }

    private static long stripLeadingWalkingLegs(ArrayList<Leg> legs) {
        long removedSeconds = 0;
        while (legs.size() > 1 && isWalkingLeg(legs.get(0))) {
            Leg head = legs.remove(0);
            removedSeconds += head.getDuration();
        }
        return removedSeconds;
    }

    private static List<Leg> mergeConsecutiveWalkingLegs(List<Leg> legs) {
        if (legs == null || legs.size() < 2) return legs;
        List<Leg> merged = new ArrayList<>();
        int i = 0;
        while (i < legs.size()) {
            Leg current = legs.get(i);
            if (!isWalkingLeg(current)) {
                merged.add(current);
                i++;
                continue;
            }
            // Pin→stop connector must stay a distinct Leg; recreating it here drops synthetic flags and shows "Walk to … street".
            if (current.isSyntheticOriginConnector()) {
                merged.add(current);
                i++;
                continue;
            }
            JourneyPlace dep = current.getDeparturePoint();
            JourneyPlace arr = current.getArrivalPoint();
            int dur = Math.max(0, current.getDuration());
            int j = i + 1;
            while (j < legs.size() && isWalkingLeg(legs.get(j))) {
                Leg nxt = legs.get(j);
                if (arr != null && nxt.getDeparturePoint() != null && !samePoint(arr, nxt.getDeparturePoint())) {
                    break;
                }
                if (nxt.getArrivalPoint() != null) arr = nxt.getArrivalPoint();
                dur += Math.max(0, nxt.getDuration());
                j++;
            }
            merged.add(Leg.createWalkingLeg(dep, arr, dur, null));
            i = j;
        }
        return merged;
    }

    private static boolean samePoint(JourneyPlace a, JourneyPlace b) {
        if (a == null || b == null) return false;
        double dLat = Math.abs(a.getLat() - b.getLat());
        double dLon = Math.abs(a.getLon() - b.getLon());
        return dLat < 0.0002 && dLon < 0.0002;
    }

    private RouteItem mapJourneyToRouteItem(Journey journey, int index, LiftDisruptionChecker liftChecker, TflApi api,
                                            String userFrom, String userTo,
                                            ResolvedEndpoint fromEp, ResolvedEndpoint toEp,
                                            String walkingSpeed, int maxWalkingMinutes) {
        List<Leg> legs = journey.getLegs();
        if (legs == null || legs.isEmpty()) return null;

        ArrayList<Leg> coreLegs = new ArrayList<>(legs);
        if (fromEp.placeAnchor != null) {
            // We inject our own place->stop walk, so remove overlapping TfL leading walk legs.
            stripLeadingWalkingLegs(coreLegs);
        }
        long removedTailWalkSec = 0;
        if (toEp.placeAnchor != null) {
            // We inject our own stop->place walk, so remove overlapping TfL trailing walk legs.
            removedTailWalkSec = stripTrailingWalkingLegs(coreLegs);
        }

        List<Leg> legsOut = new ArrayList<>(coreLegs);
        int originWalkSec = 0;
        int destWalkSec = 0;

        if (fromEp.placeAnchor != null) {
            StopPoint s = fromEp.placeAnchor.nearestStop;
            double m = haversineMeters(fromEp.placeAnchor.lat, fromEp.placeAnchor.lon, s.getLat(), s.getLon());
            originWalkSec = estimateWalkSeconds(m, walkingSpeed);
            String originLabel;
            if (fromEp.splitLocationLabel != null) {
                originLabel = fromEp.splitLocationLabel.trim();
            } else if (!userFrom.isEmpty()) {
                originLabel = userFrom.trim();
            } else {
                originLabel = fromEp.placeAnchor.label != null ? fromEp.placeAnchor.label.trim() : "";
            }
            JourneyPlace dep = JourneyPlace.at(originLabel, fromEp.placeAnchor.lat, fromEp.placeAnchor.lon);
            Leg firstTransit = firstNonWalkingLeg(coreLegs);
            String boardingName = "";
            if (firstTransit != null && firstTransit.getDeparturePoint() != null) {
                String cn = firstTransit.getDeparturePoint().getCommonName();
                if (cn != null) boardingName = cn.trim();
            }
            if (boardingName.isEmpty()) {
                boardingName = s.getCommonName() != null ? s.getCommonName().trim() : "";
            }
            JourneyPlace arr = JourneyPlace.at(boardingName, s.getLat(), s.getLon());
            boolean connectorBus = firstTransit != null
                    ? legModeLooksLikeBus(firstTransit)
                    : isStreetLevelConnectorStop(s);
            InstructionRef connectorDetail = InstructionRef.withSummary(boardingName);
            legsOut.add(0, Leg.createWalkingLeg(dep, arr, originWalkSec, connectorDetail, true, connectorBus));
        }
        if (toEp.placeAnchor != null) {
            StopPoint s = toEp.placeAnchor.nearestStop;
            double m = haversineMeters(s.getLat(), s.getLon(), toEp.placeAnchor.lat, toEp.placeAnchor.lon);
            destWalkSec = estimateWalkSeconds(m, walkingSpeed);
            JourneyPlace dep = JourneyPlace.at(s.getCommonName(), s.getLat(), s.getLon());
            // After a brand+location split, label the walk with the location only (e.g. elderfield place).
            String destLabel;
            if (toEp.splitLocationLabel != null) {
                destLabel = toEp.splitLocationLabel.trim();
            } else if (!userTo.isEmpty()) {
                destLabel = userTo.trim();
            } else {
                destLabel = toEp.placeAnchor.label != null ? toEp.placeAnchor.label.trim() : "";
            }
            JourneyPlace arr = JourneyPlace.at(destLabel, toEp.placeAnchor.lat, toEp.placeAnchor.lon);
            legsOut.add(Leg.createWalkingLeg(dep, arr, destWalkSec, null));
        }
        legsOut = mergeConsecutiveWalkingLegs(legsOut);

        // Journey duration is minutes from TfL; subtract stripped tail walks, add place↔stop walks.
        int removedTailWalkMin = (int) ((removedTailWalkSec + 59) / 60);
        int durationMin = Math.max(0, journey.getDuration() - removedTailWalkMin)
                + (originWalkSec + destWalkSec) / 60;
        int totalWalkSec = 0;
        for (Leg leg : legsOut) {
            if (isWalkingLeg(leg)) totalWalkSec += Math.max(0, leg.getDuration());
        }
        // Apply walking cap after we add synthetic place-anchor walks.
        if (maxWalkingMinutes > 0 && totalWalkSec > (maxWalkingMinutes * 60)) {
            return null;
        }
        int originWalkMinRounded = (originWalkSec + 59) / 60;
        String depTime = adjustIsoExtractTime(journey.getStartDateTime(), -originWalkMinRounded);
        // TfL arrival includes stripped tail walks; final time is at the geocoded place after destWalkSec.
        String arrTime = adjustIsoExtractTimePlusSeconds(
                journey.getArrivalDateTime(), destWalkSec - removedTailWalkSec);

        int maxCrowdTfL = 0;
        for (Leg leg : coreLegs) {
            if (leg.getCrowding() != null && leg.getCrowding().getCrowdingLevel() != null) {
                int lv = leg.getCrowding().getCrowdingLevel();
                if (lv > maxCrowdTfL) maxCrowdTfL = lv;
            }
        }
        int crowding = RouteItem.CROWDING_LOW;
        if (maxCrowdTfL >= 4) crowding = RouteItem.CROWDING_HIGH;
        else if (maxCrowdTfL >= 3) crowding = RouteItem.CROWDING_MEDIUM;

        int nonWalk = 0;
        for (Leg leg : coreLegs) {
            if (leg.getMode() != null && !"walking".equalsIgnoreCase(leg.getMode().getName())) {
                nonWalk++;
            }
        }
        int transfers = Math.max(0, nonWalk - 1);
        String transfersText = transfers + (transfers == 1 ? " transfer" : " transfers");

        Set<String> badgeSet = new LinkedHashSet<>();
        for (Leg leg : coreLegs) {
            if (leg.getMode() != null && "walking".equalsIgnoreCase(leg.getMode().getName())) continue;
            String modeName = leg.getMode() != null ? leg.getMode().getName() : "";
            if (modeName != null && modeName.toLowerCase(Locale.UK).contains("bus")) {
                badgeSet.add("BUS");
                continue;
            }
            List<RouteOptionRef> opts = leg.getRouteOptions();
            if (opts != null && !opts.isEmpty()) {
                String name = opts.get(0).getName();
                String badge = lineNameToBadge(name);
                if (!badge.isEmpty()) badgeSet.add(badge);
            }
        }
        List<String> badgeList = new ArrayList<>(badgeSet);
        if (badgeList.size() > 4) {
            badgeList = badgeList.subList(0, 4);
        }
        String[] badges = badgeList.toArray(new String[0]);

        JourneyPlace firstDep = legsOut.get(0).getDeparturePoint();
        JourneyPlace lastArr = legsOut.get(legsOut.size() - 1).getArrivalPoint();

        String fromName = firstDep != null && firstDep.getCommonName() != null ? firstDep.getCommonName().trim() : "";
        String toName = lastArr != null && lastArr.getCommonName() != null ? lastArr.getCommonName().trim() : "";
        if (fromEp.splitLocationLabel != null) {
            fromName = fromEp.splitLocationLabel.trim();
        } else if (!userFrom.isEmpty()) {
            fromName = userFrom.trim();
        } else if (fromEp.placeAnchor != null && fromEp.placeAnchor.label != null && !fromEp.placeAnchor.label.isEmpty()) {
            fromName = fromEp.placeAnchor.label.trim();
        }
        if (toEp.splitLocationLabel != null) {
            toName = toEp.splitLocationLabel.trim();
        } else if (!userTo.isEmpty()) {
            toName = userTo.trim();
        } else if (toEp.placeAnchor != null && toEp.placeAnchor.label != null && !toEp.placeAnchor.label.isEmpty()) {
            toName = toEp.placeAnchor.label.trim();
        }
        String routeSummary = (fromName.isEmpty() && toName.isEmpty())
                ? ""
                : fromName + " → " + toName;

        String routeId = "j-" + index + "-" + durationMin + "-" + journey.getStartDateTime();

        boolean liftIssue = false;
        String liftDetail = null;
        if (liftChecker != null) {
            for (Leg leg : coreLegs) {
                JourneyPlace dep = leg.getDeparturePoint();
                if (dep != null && dep.getNaptanId() != null && !dep.getNaptanId().isEmpty()) {
                    if (liftChecker.hasLiftIssues(dep.getNaptanId(), api)) {
                        liftIssue = true;
                        liftDetail = appContext.getString(com.example.ajp.R.string.lift_disruption_warning);
                        break;
                    }
                }
                JourneyPlace arr = leg.getArrivalPoint();
                if (arr != null && arr.getNaptanId() != null && !arr.getNaptanId().isEmpty()) {
                    if (liftChecker.hasLiftIssues(arr.getNaptanId(), api)) {
                        liftIssue = true;
                        liftDetail = appContext.getString(com.example.ajp.R.string.lift_disruption_warning);
                        break;
                    }
                }
            }
        }

        String poiBrandFrom = fromEp.splitBrandPhrase;
        String poiBrandTo = toEp.splitBrandPhrase;
        String poiLocationFrom = fromEp.splitLocationLabel;
        String poiLocationTo = toEp.splitLocationLabel;

        return new RouteItem(
                String.valueOf(durationMin),
                depTime,
                arrTime,
                crowding,
                transfersText,
                badges,
                routeSummary,
                routeId,
                fromName,
                toName,
                new ArrayList<>(legsOut),
                liftIssue,
                liftDetail,
                poiBrandFrom,
                poiLocationFrom,
                poiBrandTo,
                poiLocationTo
        );
    }

    private static String adjustIsoExtractTime(String iso, int plusMinutes) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            OffsetDateTime odt = OffsetDateTime.parse(iso);
            odt = odt.plusMinutes(plusMinutes);
            return String.format(Locale.UK, "%02d:%02d", odt.getHour(), odt.getMinute());
        } catch (Exception e) {
            return extractTime(iso);
        }
    }

    private static String adjustIsoExtractTimePlusSeconds(String iso, long plusSeconds) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            OffsetDateTime odt = OffsetDateTime.parse(iso);
            odt = odt.plusSeconds(plusSeconds);
            return String.format(Locale.UK, "%02d:%02d", odt.getHour(), odt.getMinute());
        } catch (Exception e) {
            return extractTime(iso);
        }
    }

    private static String extractTime(String iso) {
        if (iso == null || iso.length() < 16) return "";
        int t = iso.indexOf('T');
        if (t < 0 || t + 5 > iso.length()) return "";
        return iso.substring(t + 1, t + 6);
    }

    private static String lineNameToBadge(String name) {
        if (name == null) return "";
        String lower = name.toLowerCase(Locale.UK);
        if (lower.contains("bus")) return "BUS";
        if (lower.contains("victoria")) return "VIC";
        if (lower.contains("piccadilly")) return "PIC";
        if (lower.contains("jubilee")) return "JUB";
        if (lower.contains("northern")) return "NOR";
        if (lower.contains("central")) return "CEN";
        if (lower.contains("district")) return "DIS";
        if (lower.contains("circle")) return "CIR";
        if (lower.contains("hammersmith")) return "HAM";
        if (lower.contains("metropolitan")) return "MET";
        if (lower.contains("bakerloo")) return "BAK";
        if (lower.contains("waterloo")) return "WAT";
        if (lower.contains("elizabeth")) return "ELZ";
        if (lower.contains("dlr")) return "DLR";
        if (lower.contains("overground")) return "LO";
        String trimmed = name.trim();
        if (trimmed.length() <= 3) return trimmed.toUpperCase(Locale.UK);
        return trimmed.substring(0, 3).toUpperCase(Locale.UK);
    }

    private static final class ResolvedEndpoint {
        final String journeyPath;
        final PlaceAnchor placeAnchor;
        /** Exact brand/place phrase from user input when we split brand + location; used for POI warning only. */
        final String splitBrandPhrase;
        /** Location part only for route summary and synthetic walk labels when split. */
        final String splitLocationLabel;

        ResolvedEndpoint(String journeyPath, PlaceAnchor placeAnchor, String splitBrandPhrase,
                String splitLocationLabel) {
            this.journeyPath = journeyPath;
            this.placeAnchor = placeAnchor;
            this.splitBrandPhrase = splitBrandPhrase;
            this.splitLocationLabel = splitLocationLabel;
        }

        static ResolvedEndpoint direct(String path) {
            return new ResolvedEndpoint(path, null, null, null);
        }
    }

    private static final class PlaceAnchor {
        final double lat;
        final double lon;
        final String label;
        final StopPoint nearestStop;

        PlaceAnchor(double lat, double lon, String label, StopPoint nearestStop) {
            this.lat = lat;
            this.lon = lon;
            this.label = label;
            this.nearestStop = nearestStop;
        }
    }

    public static final class FetchResult {
        private final boolean success;
        private final List<RouteItem> routes;
        private final String error;

        private FetchResult(boolean success, List<RouteItem> routes, String error) {
            this.success = success;
            this.routes = routes != null ? routes : Collections.emptyList();
            this.error = error;
        }

        public static FetchResult ok(List<RouteItem> routes) {
            return new FetchResult(true, routes, null);
        }

        public static FetchResult fail(String error) {
            return new FetchResult(false, Collections.emptyList(), error);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<RouteItem> getRoutes() {
            return routes;
        }

        public String getError() {
            return error;
        }
    }
}


