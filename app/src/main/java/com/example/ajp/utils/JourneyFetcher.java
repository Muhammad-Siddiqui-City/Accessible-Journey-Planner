package com.example.ajp.utils;

import android.content.Context;
import com.example.ajp.api.Journey;
import com.example.ajp.api.JourneyPlace;
import com.example.ajp.api.JourneyResponse;
import com.example.ajp.api.Leg;
import com.example.ajp.api.MatchedStop;
import com.example.ajp.api.RetrofitClient;
import com.example.ajp.api.RouteOptionRef;
import com.example.ajp.api.TflApi;
import com.example.ajp.api.TflSearchResponse;
import com.example.ajp.ui.journey.RouteItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import retrofit2.Response;





/**
 * Resolves free-text endpoints and maps TfL journey responses into RouteItem models.
 */
public final class JourneyFetcher {

    private static final Pattern COORDS_PATTERN = Pattern.compile(
            "^\\s*-?\\d+(?:\\.\\d+)?\\s*,\\s*-?\\d+(?:\\.\\d+)?\\s*$");

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
        TflApi api = RetrofitClient.getApi();
        try {
            String fromPath = resolveEndpoint(api, fromInput);
            String toPath = resolveEndpoint(api, toInput);
            AccessibilityPreferences acc = AccessibilityPreferences.get(appContext);
            String walkingSpeed = acc.getWalkingSpeed();
            int maxWalk = acc.getMaxWalkingMinutes();
            // TfL expects null when no accessibility filter is requested.
            String accessibility = acc.isStepFree() ? "noSolidStairs" : null;

            Response<JourneyResponse> response = api.getJourneyResults(
                    fromPath,
                    toPath,
                    timeHHmm,
                    dateyyyyMMdd,
                    "Departing",
                    walkingSpeed,
                    maxWalk,
                    accessibility,
                    null
            ).execute();

            if (!response.isSuccessful()) {
                String err = "Journey request failed (" + response.code() + ")";
                return FetchResult.fail(err);
            }
            JourneyResponse body = response.body();
            if (body == null || body.getJourneys().isEmpty()) {
                return FetchResult.fail("No routes found");
            }


            // Lift checks are expensive; only run them when step-free mode is enabled.
            boolean stepFree = acc.isStepFree();
            LiftDisruptionChecker liftChecker = stepFree ? new LiftDisruptionChecker(appContext) : null;
            String userFrom = fromInput != null ? fromInput.trim() : "";
            String userTo = toInput != null ? toInput.trim() : "";
            List<RouteItem> items = new ArrayList<>();
            int idx = 0;
            for (Journey j : body.getJourneys()) {
                RouteItem item = mapJourneyToRouteItem(j, idx++, liftChecker, api, userFrom, userTo);
                if (item != null) {
                    items.add(item);
                }
            }
            if (items.isEmpty()) {
                return FetchResult.fail("No routes found");
            }
            return FetchResult.ok(items);
        } catch (IOException e) {
            return FetchResult.fail(e.getMessage() != null ? e.getMessage() : "Network error");
        } catch (Exception e) {
            return FetchResult.fail(e.getMessage() != null ? e.getMessage() : "Failed to plan journey");
        }
    }

    private String resolveEndpoint(TflApi api, String input) throws IOException {
        String t = input != null ? input.trim() : "";
        if (t.isEmpty()) {
            throw new IOException("Empty place");
        }
        if (COORDS_PATTERN.matcher(t).matches()) {
            // Strip spaces so "lat, lon" still works with TfL endpoint format.
            return t.replaceAll("\\s+", "");
        }
        Response<TflSearchResponse> resp = api.searchStops(t).execute();
        if (!resp.isSuccessful() || resp.body() == null || resp.body().getMatches().isEmpty()) {
            throw new IOException("Could not find: " + t);
        }
        MatchedStop m = resp.body().getMatches().get(0);
        return m.getLat() + "," + m.getLon();
    }

    private RouteItem mapJourneyToRouteItem(Journey journey, int index, LiftDisruptionChecker liftChecker, TflApi api,
                                            String userFrom, String userTo) {
        List<Leg> legs = journey.getLegs();
        if (legs == null || legs.isEmpty()) return null;

        // Journey duration is already minutes; leg durations elsewhere are seconds.
        int durationMin = Math.max(0, journey.getDuration());
        String depTime = extractTime(journey.getStartDateTime());
        String arrTime = extractTime(journey.getArrivalDateTime());

        int maxCrowdTfL = 0;
        for (Leg leg : legs) {
            if (leg.getCrowding() != null && leg.getCrowding().getCrowdingLevel() != null) {
                int lv = leg.getCrowding().getCrowdingLevel();
                if (lv > maxCrowdTfL) maxCrowdTfL = lv;
            }
        }
        int crowding = RouteItem.CROWDING_LOW;
        if (maxCrowdTfL >= 4) crowding = RouteItem.CROWDING_HIGH;
        else if (maxCrowdTfL >= 3) crowding = RouteItem.CROWDING_MEDIUM;

        int nonWalk = 0;
        for (Leg leg : legs) {
            if (leg.getMode() != null && !"walking".equalsIgnoreCase(leg.getMode().getName())) {
                nonWalk++;
            }
        }
        int transfers = Math.max(0, nonWalk - 1);
        String transfersText = transfers + (transfers == 1 ? " transfer" : " transfers");

        Set<String> badgeSet = new LinkedHashSet<>();
        for (Leg leg : legs) {
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

        JourneyPlace firstDep = legs.get(0).getDeparturePoint();
        JourneyPlace lastArr = legs.get(legs.size() - 1).getArrivalPoint();

        String fromName = firstDep != null && firstDep.getCommonName() != null ? firstDep.getCommonName().trim() : "";
        String toName = lastArr != null && lastArr.getCommonName() != null ? lastArr.getCommonName().trim() : "";
        // Preserve what the user typed so UI labels stay consistent across screens.
        if (!userFrom.isEmpty()) fromName = userFrom.trim();
        if (!userTo.isEmpty()) toName = userTo.trim();
        String routeSummary = (fromName.isEmpty() && toName.isEmpty())
                ? ""
                : fromName + " → " + toName;

        String routeId = "j-" + index + "-" + durationMin + "-" + journey.getStartDateTime();

        boolean liftIssue = false;
        String liftDetail = null;
        if (liftChecker != null) {
            for (Leg leg : legs) {
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
                new ArrayList<>(legs),
                liftIssue,
                liftDetail
        );
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

