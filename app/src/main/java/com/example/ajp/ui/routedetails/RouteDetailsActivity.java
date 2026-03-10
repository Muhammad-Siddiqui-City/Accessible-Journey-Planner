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

/**
 * Route details screen. Add in Commit 12; GPS progress in 13.
 * PURPOSE: Show route summary, steps (StepsAdapter), share (formatted duration), save; Start Navigation → GPS progress.
 * WHY: FusedLocationProviderClient for distance-to-destination progress; removeLocationUpdates in onDestroy.
 * ISSUES: parseDuration for share; duration display uses TimeFormatUtil; bookmark/print button removed per UI cleanup.
 */
public class RouteDetailsActivity extends AppCompatActivity {
    // AI Generated
    // Lovable.dev: UI mockup reference
    public static final String EXTRA_ROUTE_ID = "route_id";
    public static final String EXTRA_ROUTE = "selected_route";
    public static final String EXTRA_FROM_OFFLINE = "from_offline";
    public static final String EXTRA_ALTERNATIVES = "alternatives";
    /** User's typed origin text (e.g. "wembley central"); used for display when postcode-like. */
    public static final String EXTRA_USER_FROM_INPUT = "user_from_input";
    /** User's typed destination text (e.g. "sw15 5le"); shown instead of geocoded address when postcode-like. */
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

    /** Build destination Location from last leg's arrival point (for GPS progress). */
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

    private void announceRouteSummaryIfTtsOn(RouteItem route) {
        if (route == null || ttsHelper == null || !ttsHelper.isTtsEnabled()) return;
        String from = route.getFromStation();
        String to = route.getToStation();
        int min = parseDuration(route.getDurationMinutes());
        String durationStr = min < 60 ? min + " minutes" : (min / 60) + " hour" + (min % 60 > 0 ? " " + (min % 60) + " minutes" : "");
        int transfers = route.getTransfersCount();
        String transfersStr = transfers == 0 ? "no transfers" : transfers == 1 ? "1 transfer" : transfers + " transfers";
        String phrase = "Route from " + from + " to " + to + ", duration " + durationStr + ", " + transfersStr;
        ttsHelper.speak(phrase);
        // AI Generated: speak actual disruption text
        if (route.hasLiftDisruption()) {
            String msg = route.getLiftDisruptionDescription() != null
                    ? route.getLiftDisruptionDescription()
                    : getString(R.string.lift_disruption_warning);
            ttsHelper.speak(msg, TtsHelper.QUEUE_ADD);
        }
        if (route.getCrowdingLevel() == RouteItem.CROWDING_HIGH) {
            ttsHelper.speak(getString(R.string.crowding_warning), TtsHelper.QUEUE_ADD);
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
        if (initialDistanceToDest < 0f) initialDistanceToDest = currentDist;
        if (initialDistanceToDest <= 0f) return;
        float remaining = currentDist;
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

    private static boolean looksLikeUkPostcode(String s) {
        if (s == null || s.length() < 5) return false;
        String t = s.trim().toUpperCase().replaceAll("\\s+", " ");
        return t.matches("[A-Z]{1,2}[0-9][0-9A-Z]?\\s*[0-9][A-Z]{2}");
    }

    private static int parseDuration(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /** Returns comma-separated list of all transport modes used (Tube, Bus, etc). Skips walk. */
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

    private void saveRoute() {
        if (currentRoute == null) {
            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show();
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SavedRouteEntity entity = new SavedRouteEntity(
                        currentRoute,
                        System.currentTimeMillis(),
                        currentRoute.getRouteSummary()
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
            String fromDisplay = route.getFromStation().isEmpty() ? getString(R.string.from_label) : route.getFromStation();
            String toDisplay = route.getToStation().isEmpty() ? getString(R.string.to_label) : route.getToStation();
            // Use user's entered origin when route shows "Destination" (e.g. saved route or walk leg)
            String userFrom = getIntent().getStringExtra(EXTRA_USER_FROM_INPUT);
            if (userFrom != null && !userFrom.trim().isEmpty() && "Destination".equals(fromDisplay)) {
                fromDisplay = userFrom.trim();
            }
            String userTo = getIntent().getStringExtra(EXTRA_USER_TO_INPUT);
            if (userTo != null && !userTo.trim().isEmpty() && ("Destination".equals(toDisplay) || looksLikeUkPostcode(userTo))) {
                toDisplay = userTo.trim();
            }
            binding.tvFrom.setText(fromDisplay);
            binding.tvTo.setText(toDisplay);
            binding.tvDuration.setText(route.getDurationMinutes());
            binding.tvDepartTime.setText(route.getDepartureTime());
            binding.tvArrivalTime.setText("→ " + route.getArrivalTime());
            binding.tvTransfers.setText(route.getTransfersText());
            binding.tvCrowdingWarning.setVisibility(route.getCrowdingLevel() == RouteItem.CROWDING_HIGH ? View.VISIBLE : View.GONE);
            // AI Generated: display actual TfL disruption text
            if (route.hasLiftDisruption()) {
                binding.tvLiftDisruptionWarning.setVisibility(View.VISIBLE);
                String desc = route.getLiftDisruptionDescription();
                binding.tvLiftDisruptionWarning.setText(desc != null ? desc : getString(R.string.lift_disruption_warning));
            } else {
                binding.tvLiftDisruptionWarning.setVisibility(View.GONE);
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
            binding.stepsList.setVisibility(View.GONE);
        }
    }

    private void shareJourney() {
        if (currentRoute == null) return;
        StringBuilder shareBody = new StringBuilder();
        String from = currentRoute.getFromStation() != null ? currentRoute.getFromStation() : "";
        String to = currentRoute.getToStation() != null ? currentRoute.getToStation() : "";
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

    /** Build a one-line summary for a leg: instruction summary, or "Mode to ArrivalPoint". */
    private static String getLegSummary(Leg leg) {
        if (leg.getInstruction() != null) {
            String s = leg.getInstruction().getSummary();
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        String mode = leg.getMode() != null ? leg.getMode().getName() : "Travel";
        String arrival = leg.getArrivalPoint() != null ? leg.getArrivalPoint().getCommonName() : "";
        if (arrival == null) arrival = "";
        return mode + " to " + (arrival.isEmpty() ? "Destination" : arrival);
    }

    @Override
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
