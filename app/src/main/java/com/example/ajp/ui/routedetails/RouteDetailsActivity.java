package com.example.ajp.ui.routedetails;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.api.JourneyPlace;
import com.example.ajp.api.Leg;
import com.example.ajp.api.ModeRef;
import com.example.ajp.data.local.AppDatabase;
import com.example.ajp.data.local.JourneyLog;
import com.example.ajp.data.local.SavedRouteEntity;
import com.example.ajp.databinding.ActivityRouteDetailsBinding;
import com.example.ajp.ui.journey.RouteItem;
import com.example.ajp.utils.AccessibilityPreferences;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;
import com.example.ajp.utils.TimeFormatUtil;
import com.example.ajp.utils.TtsHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import java.util.concurrent.Executors;

/** Shows one chosen route: steps, save/share, lift warning, coarse GPS progress bar. */
public class RouteDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ROUTE_ID = "route_id";
    public static final String EXTRA_ROUTE = "selected_route";
    public static final String EXTRA_FROM_OFFLINE = "from_offline";
    public static final String EXTRA_ALTERNATIVES = "alternatives";

    public static final String EXTRA_USER_FROM_INPUT = "user_from_input";

    public static final String EXTRA_USER_TO_INPUT = "user_to_input";

    private static final int REQUEST_LOCATION = 1001;

    private ActivityRouteDetailsBinding binding;
    private RouteItem currentRoute;
    private List<RouteItem> alternatives;

    private FusedLocationProviderClient fusedLocationClient;
    private Location destLocation;
    private float initialDistanceToDest = -1f;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private TtsHelper ttsHelper;

    @Override

    protected void attachBaseContext(Context newBase) {
        // Apply locale before view inflation so translated resources are used immediately.
        super.attachBaseContext(LocaleHelper.applyFull(newBase));
    }

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        if (SettingsPrefs.get(getApplicationContext()).isHighContrast()) {
            setTheme(R.style.Theme_AJP_HighContrast);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityRouteDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentRoute = (RouteItem) getIntent().getSerializableExtra(EXTRA_ROUTE);
        boolean fromOffline = getIntent().getBooleanExtra(EXTRA_FROM_OFFLINE, false);
        @SuppressWarnings("unchecked")
        List<RouteItem> alt = (List<RouteItem>) getIntent().getSerializableExtra(EXTRA_ALTERNATIVES);
        alternatives = alt != null ? alt : java.util.Collections.emptyList();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        destLocation = buildDestinationLocation(currentRoute);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);

        binding.back.setOnClickListener(v -> finish());
        binding.save.setOnClickListener(v -> saveRoute());
        binding.share.setOnClickListener(v -> shareJourney());
        binding.startNavigation.setOnClickListener(v -> logAndStartJourney());

        binding.badgeCachedOffline.setVisibility(fromOffline ? View.VISIBLE : View.GONE);

        binding.stepsList.setNestedScrollingEnabled(false);
        ttsHelper = new TtsHelper(this);
        displayRoute(currentRoute);
        announceRouteSummaryIfTtsOn(currentRoute);
    }

    // Derives destination coordinates from the final leg for progress distance estimation.
    private Location buildDestinationLocation(RouteItem route) {
        if (route == null) return null;
        List<Leg> legs = route.getLegs();
        if (legs == null || legs.isEmpty()) return null;
        JourneyPlace arrival = legs.get(legs.size() - 1).getArrivalPoint();
        if (arrival == null) return null;
        double lat = arrival.getLat();
        double lon = arrival.getLon();
        Location loc = new Location("");
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        return loc;
    }

    // Optional accessibility narration of the selected route summary and warnings.
    private void announceRouteSummaryIfTtsOn(RouteItem route) {
        if (route == null || ttsHelper == null || !ttsHelper.isTtsEnabled()) return;
        String from = resolveFromDisplay(route);
        String to = resolveToDisplay(route);
        int min = parseDuration(route.getDurationMinutes());
        String durationStr = min < 60 ? min + " minutes" : (min / 60) + " hour" + (min % 60 > 0 ? " " + (min % 60) + " minutes" : "");
        int transfers = route.getTransfersCount();
        String transfersStr = transfers == 0 ? "no transfers" : transfers == 1 ? "1 transfer" : transfers + " transfers";
        String phrase = "Route from " + from + " to " + to + ", duration " + durationStr + ", " + transfersStr;
        ttsHelper.speak(phrase);

        if (route.hasLiftDisruption()) {
            String msg = route.getLiftDisruptionDescription() != null
                    ? route.getLiftDisruptionDescription()
                    : getString(R.string.lift_disruption_warning);
            ttsHelper.speak(msg, TtsHelper.QUEUE_ADD);
        }
        if (route.getCrowdingLevel() == RouteItem.CROWDING_HIGH) {
            ttsHelper.speak(getString(R.string.crowding_warning), TtsHelper.QUEUE_ADD);
        }
        String poi = buildPoiVerificationText(route);
        if (poi != null && !poi.trim().isEmpty()) {
            ttsHelper.speak(poi.trim(), TtsHelper.QUEUE_ADD);
        }
    }

    private void announceFirstStepIfTtsOn() {
        if (currentRoute == null || ttsHelper == null || !ttsHelper.isTtsEnabled()) return;
        List<Leg> legs = currentRoute.getLegs();
        if (legs == null || legs.isEmpty()) return;
        Leg first = legs.get(0);
        String step = getLegSummary(first);
        if (!step.isEmpty()) ttsHelper.speak("Step 1. " + step);
    }

    // Records a local journey log entry, then starts coarse GPS progress monitoring.
    private void logAndStartJourney() {
        if (currentRoute == null) return;
        int currentDuration = parseDuration(currentRoute.getDurationMinutes());
        int worstCase = currentDuration;
        if (alternatives != null && !alternatives.isEmpty()) {
            for (RouteItem r : alternatives) {
                if (r != null) {
                    int d = parseDuration(r.getDurationMinutes());
                    if (d > worstCase) worstCase = d;
                }
            }
        }
        // Fall back to a small synthetic gain when alternatives are missing.
        int savedMinutes = (worstCase > currentDuration) ? (worstCase - currentDuration) : (int) (currentDuration * 0.1);
        String mode = deriveMode(currentRoute);
        boolean isCrowdLow = (currentRoute.getCrowdingLevel() == RouteItem.CROWDING_LOW);
        JourneyLog log = new JourneyLog(
                System.currentTimeMillis(),
                currentDuration,
                savedMinutes,
                mode,
                isCrowdLow,
                currentRoute.getFromStation(),
                currentRoute.getToStation()
        );
        Executors.newSingleThreadExecutor().execute(() ->
                AppDatabase.getInstance(this).journeyLogDao().insert(log));
        announceFirstStepIfTtsOn();
        startGpsProgress();
    }

    // Switches UI into active progress mode and validates location permission prerequisites.
    private void startGpsProgress() {
        binding.startNavigation.setVisibility(View.GONE);
        binding.layoutLiveProgress.setVisibility(View.VISIBLE);
        if (destLocation == null) {
            binding.tvJourneyStatus.setText(R.string.navigating);
            binding.tvProgressPercent.setText("0%");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        initialDistanceToDest = -1f;
        startProgressLoop();
    }

    // Starts periodic location updates; callback only updates UI while Activity is valid.
    private void startProgressLoop() {
        if (fusedLocationClient == null || destLocation == null || locationRequest == null || isFinishing()) return;
        locationCallback = new LocationCallback() {
            @Override

            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || isFinishing() || binding == null) return;
                Location loc = locationResult.getLastLocation();
                if (loc != null) updateProgressBar(loc);
            }
        };
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) { }
    }

    private void updateProgressBar(Location loc) {
        if (destLocation == null || binding == null) return;
        float currentDist = loc.distanceTo(destLocation);
        // Capture the first distance reading as baseline for percentage progress.
        if (initialDistanceToDest < 0f) initialDistanceToDest = currentDist;
        if (initialDistanceToDest <= 0f) return;
        float remaining = currentDist;
        // GPS jitter can temporarily increase distance; clamp to avoid negative progress.
        if (currentDist > initialDistanceToDest) remaining = initialDistanceToDest;
        int progress = (int) ((initialDistanceToDest - remaining) / initialDistanceToDest * 100f);
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        binding.progressBarJourney.setProgress(progress);
        binding.tvProgressPercent.setText(progress + "%");
        if (progress >= 100) binding.tvJourneyStatus.setText(R.string.journey_in_progress);
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initialDistanceToDest = -1f;
            startProgressLoop();
        }
    }

    private String resolveFromDisplay(RouteItem route) {
        String from = route.getFromStation() != null ? route.getFromStation().trim() : "";
        if (!from.isEmpty()) {
            return from;
        }
        String userFrom = getIntent().getStringExtra(EXTRA_USER_FROM_INPUT);
        if (userFrom != null && !userFrom.trim().isEmpty()) {
            return userFrom.trim();
        }
        return getString(R.string.from_label);
    }

    private String resolveToDisplay(RouteItem route) {
        String to = route.getToStation() != null ? route.getToStation().trim() : "";
        if (!to.isEmpty()) {
            return to;
        }
        String userTo = getIntent().getStringExtra(EXTRA_USER_TO_INPUT);
        if (userTo != null && !userTo.trim().isEmpty()) {
            return userTo.trim();
        }
        return getString(R.string.to_label);
    }

    private String buildSavedRouteSummary(RouteItem route) {
        return resolveFromDisplay(route) + " → " + resolveToDisplay(route);
    }

    private static int parseDuration(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            // Input can contain units; keep only digits before parsing.
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String deriveMode(RouteItem route) {
        java.util.List<Leg> legs = route.getLegs();
        if (legs == null || legs.isEmpty()) return "Mixed";
        java.util.Set<String> modes = new java.util.LinkedHashSet<>();
        for (Leg leg : legs) {
            if (leg == null) continue;
            String m = legMode(leg);
            if (m != null && !m.isEmpty()) modes.add(m);
        }
        if (modes.isEmpty()) return "Mixed";
        return String.join(",", modes);
    }

    private static String legMode(Leg leg) {
        ModeRef modeRef = leg.getMode();
        String name = modeRef != null ? modeRef.getName() : "";
        if (name == null) name = "";
        String lower = name.toLowerCase();
        if (lower.contains("walk")) return null;
        if (lower.contains("tube") || lower.contains("underground") || lower.contains("metro")) return "Tube";
        if (lower.contains("bus") || lower.contains("buses")) return "Bus";
        if (lower.contains("elizabeth")) return "Elizabeth";
        if (lower.contains("overground")) return "Overground";
        if (lower.contains("dlr")) return "DLR";
        if (lower.contains("rail") || lower.contains("national-rail")) return "Rail";
        for (com.example.ajp.api.RouteOptionRef ro : leg.getRouteOptions()) {
            String lineName = ro != null ? ro.getName().toLowerCase() : "";
            if (lineName.contains("victoria") || lineName.contains("piccadilly") || lineName.contains("jubilee")
                    || lineName.contains("northern") || lineName.contains("central") || lineName.contains("district")
                    || lineName.contains("bakerloo") || lineName.contains("circle") || lineName.contains("metropolitan")
                    || lineName.contains("hammersmith")) return "Tube";
            if (lineName.contains("elizabeth")) return "Elizabeth";
            if (lineName.contains("overground")) return "Overground";
        }
        return null;
    }

    // Persists selected route for offline recall in Home saved-routes section.
    private void saveRoute() {
        if (currentRoute == null) {
            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show();
            return;
        }

        final String summary = buildSavedRouteSummary(currentRoute);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SavedRouteEntity entity = new SavedRouteEntity(
                        currentRoute,
                        System.currentTimeMillis(),
                        summary
                );
                AppDatabase.getInstance(this).savedRouteDao().insert(entity);
                runOnUiThread(() -> Toast.makeText(this, R.string.journey_saved_offline, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void displayRoute(RouteItem route) {
        if (route != null) {
            String fromDisplay = resolveFromDisplay(route);
            String toDisplay = resolveToDisplay(route);
            binding.tvFrom.setText(fromDisplay);
            binding.tvTo.setText(toDisplay);
            binding.tvDuration.setText(route.getDurationMinutes());
            binding.tvDepartTime.setText(route.getDepartureTime());
            binding.tvArrivalTime.setText("→ " + route.getArrivalTime());
            binding.tvTransfers.setText(route.getTransfersText());
            binding.tvCrowdingWarning.setVisibility(route.getCrowdingLevel() == RouteItem.CROWDING_HIGH ? View.VISIBLE : View.GONE);

            if (AccessibilityPreferences.get(this).isStepFree() && route.hasLiftDisruption()) {
                binding.tvLiftDisruptionWarning.setVisibility(View.VISIBLE);
                String desc = route.getLiftDisruptionDescription();
                binding.tvLiftDisruptionWarning.setText(desc != null ? desc : getString(R.string.lift_disruption_warning));
            } else {
                binding.tvLiftDisruptionWarning.setVisibility(View.GONE);
            }

            String poiWarn = buildPoiVerificationText(route);
            if (poiWarn != null && !poiWarn.trim().isEmpty()) {
                binding.tvPoiVerificationWarning.setText(poiWarn.trim());
                binding.tvPoiVerificationWarning.setVisibility(View.VISIBLE);
            } else {
                binding.tvPoiVerificationWarning.setVisibility(View.GONE);
            }

            List<Leg> legs = route.getLegs();
            if (legs != null && !legs.isEmpty()) {
                binding.stepsList.setVisibility(View.VISIBLE);
                binding.stepsList.setLayoutManager(new LinearLayoutManager(this));
                binding.stepsList.setAdapter(new StepsAdapter(legs));
            } else {
                binding.stepsList.setVisibility(View.GONE);
            }
        } else {
            binding.tvFrom.setText(getString(R.string.kings_cross));
            binding.tvTo.setText(getString(R.string.green_park));
            binding.tvDuration.setText(TimeFormatUtil.formatMinutesToHourMin(15));
            binding.tvDepartTime.setText("14:32");
            binding.tvArrivalTime.setText("14:47");
            binding.tvTransfers.setText("0 transfers");
            binding.tvPoiVerificationWarning.setVisibility(View.GONE);
            binding.stepsList.setVisibility(View.GONE);
        }
    }

    // Builds a human-readable text itinerary for system share sheet.
    private void shareJourney() {
        if (currentRoute == null) return;
        StringBuilder shareBody = new StringBuilder();
        String from = resolveFromDisplay(currentRoute);
        String to = resolveToDisplay(currentRoute);
        String duration = currentRoute.getDurationMinutes() != null ? currentRoute.getDurationMinutes() : "";

        shareBody.append("Journey: ").append(from).append(" \u2192 ").append(to);
        int totalMin = parseDuration(duration);
        shareBody.append("\nTotal Time: ").append(TimeFormatUtil.formatMinutesToHourMin(totalMin));
        shareBody.append("\n\nRoute Details:");

        List<Leg> legs = currentRoute.getLegs();
        if (legs != null) {
            for (int i = 0; i < legs.size(); i++) {
                Leg leg = legs.get(i);
                String stepSummary = getLegSummary(leg);
                int durationMin = leg.getDuration() / 60;
                shareBody.append("\n").append(i + 1).append(". ").append(stepSummary)
                        .append(" (").append(TimeFormatUtil.formatMinutesToHourMin(durationMin)).append(")");
            }
        }

        shareBody.append("\n\nSent via Accessible Journey Planner");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_journey_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_journey)));
    }

    /** POI brands are stored on the route; format here so language matches the activity (not application context). */
    private String buildPoiVerificationText(RouteItem route) {
        if (route == null) return null;
        String bf = route.getPoiVerifyBrandFrom();
        String bt = route.getPoiVerifyBrandTo();
        boolean hasF = bf != null && !bf.trim().isEmpty();
        boolean hasT = bt != null && !bt.trim().isEmpty();
        if (!hasF && !hasT) return null;
        StringBuilder wb = new StringBuilder();
        if (hasF) {
            String locFrom = route.getPoiVerifyLocationFrom();
            if (locFrom != null && !locFrom.trim().isEmpty()) {
                wb.append(getString(R.string.poi_verify_warning_from_place, bf.trim(), locFrom.trim()));
            } else {
                wb.append(getString(R.string.poi_verify_warning_from, bf.trim()));
            }
        }
        if (hasT) {
            if (wb.length() > 0) wb.append("\n\n");
            String locTo = route.getPoiVerifyLocationTo();
            if (locTo != null && !locTo.trim().isEmpty()) {
                wb.append(getString(R.string.poi_verify_warning_to_place, bt.trim(), locTo.trim()));
            } else {
                wb.append(getString(R.string.poi_verify_warning_to, bt.trim()));
            }
        }
        return wb.toString();
    }

    private String getLegSummary(Leg leg) {
        if (leg.isSyntheticOriginConnector()) {
            String stopName = leg.getInstruction() != null ? leg.getInstruction().getSummary() : "";
            if (stopName == null) stopName = "";
            stopName = stopName.trim();
            if (stopName.isEmpty() && leg.getArrivalPoint() != null && leg.getArrivalPoint().getCommonName() != null) {
                stopName = leg.getArrivalPoint().getCommonName().trim();
            }
            String head = leg.isSyntheticConnectorBusStop()
                    ? getString(R.string.walk_connector_bus_stop)
                    : getString(R.string.walk_connector_train_station);
            if (stopName.isEmpty()) return head;
            if (leg.isSyntheticConnectorBusStop()) {
                return head + " — " + getString(R.string.walk_connector_bus_stop_detail, stopName);
            }
            return head + " (" + stopName + ")";
        }
        if (leg.getInstruction() != null) {
            String s = leg.getInstruction().getSummary();
            if (s != null && !s.trim().isEmpty()) {
                return StepsAdapter.normalizeInstructionForStepDisplay(s.trim());
            }
        }
        String rawMode = leg.getMode() != null ? leg.getMode().getName() : null;
        String prettyMode = rawMode != null ? StepsAdapter.humanizeTflModeId(rawMode) : "";
        String mode = !prettyMode.isEmpty() ? prettyMode : "Travel";
        String arrival = leg.getArrivalPoint() != null ? leg.getArrivalPoint().getCommonName() : "";
        if (arrival == null) arrival = "";
        return mode + " to " + (arrival.isEmpty() ? "Destination" : arrival);
    }

    @Override
    // Releases listeners/resources to avoid leaks across lifecycle recreation.
    protected void onDestroy() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
            ttsHelper = null;
        }
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (SecurityException ignored) { }
        }
        locationCallback = null;
        super.onDestroy();
        binding = null;
    }
}


