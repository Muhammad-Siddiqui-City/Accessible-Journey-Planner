package com.example.ajp.ui.home;

import android.app.Activity;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.ajp.R;
import com.example.ajp.databinding.FragmentHomeBinding;
import com.example.ajp.ui.main.MainActivity;
import com.example.ajp.api.LineStatus;
import com.example.ajp.data.local.AppDatabase;
import com.example.ajp.data.local.SavedRouteEntity;
import com.example.ajp.api.StatusDetail;
import com.example.ajp.ui.nearby.StopItem;
import com.example.ajp.ui.routedetails.RouteDetailsActivity;
import com.example.ajp.ui.nearby.StopsViewModel;
import com.example.ajp.utils.LocationManager;
import com.example.ajp.utils.PermissionManager;
import com.example.ajp.utils.TtsHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Home tab: popular stations, search, nearby link. Add in Commit 8.
 * PURPOSE: Show popular station chips; voice/text search → StationSearchFragment; "Nearby" → location then NearbyStationsFragment.
 * WHY: LocationManager.getCurrentLocation for nearby; PermissionManager for location permission; StopsViewModel for line status on home.
 * ISSUES: REQUEST_LOCATION for permission; minCal for date picker min date; green symbol/compass removed per UI cleanup.
 */
public class HomeFragment extends Fragment {

    private static final int REQUEST_LOCATION = 1001;

    private FragmentHomeBinding binding;
    private ActivityResultLauncher<Intent> voiceResultLauncher;
    private PermissionManager permissionManager;
    private LocationManager locationManager;
    private StopsViewModel viewModel;
    private List<StopItem> currentHighlights = new ArrayList<>();
    private SavedRouteAdapter savedRouteAdapter;
    private TtsHelper ttsHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        permissionManager = PermissionManager.getInstance(requireContext());
        locationManager = LocationManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(StopsViewModel.class);

        // Use cached location if available, else fetch. Avoids re-fetching when swapping tabs.
        if (viewModel.hasCachedLocation()) {
            viewModel.loadNearestStops(viewModel.getCachedLat(), viewModel.getCachedLon());
        } else if (permissionManager.checkLocationPermission(requireContext())) {
            locationManager.getCurrentLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(double lat, double lon) {
                    viewModel.loadNearestStops(lat, lon);
                }
                @Override
                public void onLocationFailed() { /* no-op for home highlights */ }
            });
        }

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                binding.homeLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getNearestStops().observe(getViewLifecycleOwner(), stops -> {
            if (binding == null || stops == null) return;
            currentHighlights = StopsViewModel.getHomeHighlights(stops);
            binding.tvHomeStation1.setText(currentHighlights.size() > 0 ? currentHighlights.get(0).getName() : "—");
            binding.tvHomeDist1.setText(currentHighlights.size() > 0 ? formatDistance(requireContext(), currentHighlights.get(0).getDistance()) : "—");
            populateBadges(binding.layoutHomeBadges1, currentHighlights.size() > 0 ? currentHighlights.get(0) : null);
            binding.tvHomeStation2.setText(currentHighlights.size() > 1 ? currentHighlights.get(1).getName() : "—");
            binding.tvHomeDist2.setText(currentHighlights.size() > 1 ? formatDistance(requireContext(), currentHighlights.get(1).getDistance()) : "—");
            populateBadges(binding.layoutHomeBadges2, currentHighlights.size() > 1 ? currentHighlights.get(1) : null);
        });

        viewModel.getDisruptions().observe(getViewLifecycleOwner(), list -> {
            if (binding == null) return;
            List<LineStatus> disruptions = list != null ? list : List.of();
            if (disruptions.isEmpty()) {
                binding.goodServiceCard.setVisibility(View.VISIBLE);
                binding.disruptionCard1.setVisibility(View.GONE);
                binding.disruptionCard2.setVisibility(View.GONE);
                binding.tvDisruptionCount.setText("");
            } else {
                binding.goodServiceCard.setVisibility(View.GONE);
                binding.tvDisruptionCount.setText(getString(R.string.disruptions_active, disruptions.size()));
                announceDisruptionsIfTtsOn(disruptions);
                // Card 1
                LineStatus line1 = disruptions.get(0);
                binding.disruptionCard1.setVisibility(View.VISIBLE);
                binding.tvDisruptionLine1.setText(getLineBadgeText(line1.getName(), line1.getId()));
                binding.tvDisruptionLine1.setBackgroundColor(getLineColor(line1.getName()));
                binding.tvDisruptionLine1.setTextColor(getLineTextColor(line1.getName()));
                StatusDetail status1 = getFirstStatusDetail(line1);
                String rawReason1 = status1 != null ? status1.getReason() : null;
                binding.tvDisruptionDesc1.setText(cleanDisruptionText(
                        rawReason1,
                        status1 != null ? status1.getStatusSeverityDescription() : ""));
                String url1 = extractUrl(rawReason1);
                binding.disruptionCard1.setOnClickListener(v -> {
                    if (url1 != null) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url1)));
                    } else {
                        String query = "TfL " + line1.getName() + " status";
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))));
                    }
                });
                // Card 2
                if (disruptions.size() > 1) {
                    LineStatus line2 = disruptions.get(1);
                    binding.disruptionCard2.setVisibility(View.VISIBLE);
                    binding.tvDisruptionLine2.setText(getLineBadgeText(line2.getName(), line2.getId()));
                    binding.tvDisruptionLine2.setBackgroundColor(getLineColor(line2.getName()));
                    binding.tvDisruptionLine2.setTextColor(getLineTextColor(line2.getName()));
                    StatusDetail status2 = getFirstStatusDetail(line2);
                    String rawReason2 = status2 != null ? status2.getReason() : null;
                    binding.tvDisruptionDesc2.setText(cleanDisruptionText(
                            rawReason2,
                            status2 != null ? status2.getStatusSeverityDescription() : ""));
                    String url2 = extractUrl(rawReason2);
                    binding.disruptionCard2.setOnClickListener(v -> {
                        if (url2 != null) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url2)));
                        } else {
                            String query = "TfL " + line2.getName() + " status";
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))));
                        }
                    });
                } else {
                    binding.disruptionCard2.setVisibility(View.GONE);
                }
            }
        });

        // Voice result: put spoken text in search box and open station search screen.
        voiceResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                    ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        String spoken = results.get(0).trim();
                        if (binding != null) binding.searchInput.setText(spoken);
                        openStationSearch(spoken);
                    }
                });

        // Use OnTouchListener on the mic wrapper so it consumes the touch; otherwise the search bar steals the click.
        binding.searchMicTouch.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_station_name));
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                try {
                    voiceResultLauncher.launch(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.voice_search, Toast.LENGTH_SHORT).show();
                }
            }
            return true; // consume so search bar does not receive the click
        });

        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = binding.searchInput.getText() != null ? binding.searchInput.getText().toString().trim() : "";
                openStationSearch(q);
                return true;
            }
            return false;
        });

        binding.searchBar.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToJourneysTab();
            }
        });

        binding.cardNearbyStations.setOnClickListener(v -> openNearbyStationsWithLocation());
        binding.cardLiveArrivals.setOnClickListener(v -> openPopularStationArrivals());
        binding.cardPlanJourney.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToJourneysTab();
            }
        });

        binding.nearbyViewAll.setOnClickListener(v -> openNearbyStationsWithLocation());
        binding.nearbyStation1.setOnClickListener(v -> openStationArrivals(0));
        binding.nearbyStation2.setOnClickListener(v -> openStationArrivals(1));

        binding.transportTube.setOnClickListener(v -> Toast.makeText(requireContext(), R.string.tube_rail, Toast.LENGTH_SHORT).show());
        binding.transportBus.setOnClickListener(v -> Toast.makeText(requireContext(), R.string.buses, Toast.LENGTH_SHORT).show());

        // Saved Routes: observe Room and show list; on tap open RouteDetailsActivity with fromOffline=true.
        savedRouteAdapter = new SavedRouteAdapter();
        savedRouteAdapter.setOnSavedRouteClickListener(entity -> {
            if (entity.routeItem != null) {
                Intent i = new Intent(requireContext(), RouteDetailsActivity.class);
                i.putExtra(RouteDetailsActivity.EXTRA_ROUTE, entity.routeItem);
                i.putExtra(RouteDetailsActivity.EXTRA_FROM_OFFLINE, true);
                i.putExtra(RouteDetailsActivity.EXTRA_ALTERNATIVES, new java.util.ArrayList<com.example.ajp.ui.journey.RouteItem>());
                startActivity(i);
            }
        });
        binding.savedRoutesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.savedRoutesList.setAdapter(savedRouteAdapter);
        binding.savedRoutesList.setNestedScrollingEnabled(false);

        AppDatabase.getInstance(requireContext()).savedRouteDao().getAll()
                .observe(getViewLifecycleOwner(), list -> {
                    if (binding == null) return;
                    savedRouteAdapter.submitList(list);
                    boolean empty = list == null || list.isEmpty();
                    binding.tvSavedRoutesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.savedRoutesList.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
    }

    /** Opens the station search screen with the given query (voice or typed). Shows buses and trains for that station. */
    private void openStationSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.search_placeholder, Toast.LENGTH_SHORT).show();
            return;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showStationSearchFragment(query.trim());
        }
    }

    private void openNearbyStationsWithLocation() {
        if (viewModel.hasCachedLocation()) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showNearbyStationsFragment(viewModel.getCachedLat(), viewModel.getCachedLon());
            }
            return;
        }
        if (permissionManager.checkLocationPermission(requireContext())) {
            locationManager.getCurrentLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(double lat, double lon) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showNearbyStationsFragment(lat, lon);
                    }
                }
                @Override
                public void onLocationFailed() {
                    Toast.makeText(requireContext(), R.string.nearby_stations, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            permissionManager.askLocationPermission(this, REQUEST_LOCATION);
        }
    }

    /** Opens Live Arrivals for the station at the given index (same screen as nearby stations list). */
    private void openStationArrivals(int index) {
        if (index < 0 || index >= currentHighlights.size()) {
            openNearbyStationsWithLocation();
            return;
        }
        StopItem stop = currentHighlights.get(index);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLiveArrivalsFragment(stop.getStopId(), stop.getName());
        }
    }

    /** Opens Live Arrivals for London's top station (King's Cross St. Pancras). */
    private void openPopularStationArrivals() {
        String stopId = "940GZZLUKSX";  // King's Cross St. Pancras Underground
        String stopName = getString(R.string.kings_cross_st_pancras);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showLiveArrivalsFragment(stopId, stopName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && permissionManager.isPermissionGranted(grantResults)) {
            locationManager.getCurrentLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(double lat, double lon) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showNearbyStationsFragment(lat, lon);
                    }
                }
                @Override
                public void onLocationFailed() {
                    Toast.makeText(requireContext(), R.string.nearby_stations, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static String formatDistance(android.content.Context context, double meters) {
        double km = meters / 1000.0;
        return context.getString(R.string.distance_km_away, km);
    }

    private void populateBadges(LinearLayout container, StopItem stop) {
        if (container == null) return;
        container.removeAllViews();
        if (stop == null || stop.getLineCodes() == null) return;
        for (String lineName : stop.getLineCodes()) {
            TextView chip = new TextView(requireContext());
            chip.setText(lineName);
            chip.setBackgroundColor(getLineColor(lineName));
            chip.setTextColor(getLineTextColor(lineName));
            chip.setPadding(dp(6), dp(2), dp(6), dp(2));
            chip.setTextSize(10);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(4));
            container.addView(chip, params);
        }
    }

    private int dp(int px) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (px * density);
    }

    /** TfL line badge background (same logic as NearbyStationsAdapter). */
    private static int getLineColor(String lineName) {
        if (lineName == null) return Color.parseColor("#0019A8");
        String n = lineName.trim().toLowerCase();
        if (n.contains("south western")) return Color.parseColor("#000000");
        if (n.contains("district")) return Color.parseColor("#00782A");
        if (n.contains("bakerloo")) return Color.parseColor("#B36305");
        if (n.contains("central")) return Color.parseColor("#E32017");
        if (n.contains("circle")) return Color.parseColor("#FFD300");
        if (n.contains("hammersmith")) return Color.parseColor("#F3A9BB");
        if (n.contains("jubilee")) return Color.parseColor("#A0A5A9");
        if (n.contains("metropolitan")) return Color.parseColor("#9B0056");
        if (n.contains("northern")) return Color.parseColor("#000000");
        if (n.contains("piccadilly")) return Color.parseColor("#003688");
        if (n.contains("victoria")) return Color.parseColor("#0098D4");
        if (n.contains("waterloo") && n.contains("city")) return Color.parseColor("#95CDBA");
        if (n.contains("elizabeth")) return Color.parseColor("#6950a1");
        if (n.contains("overground")) return Color.parseColor("#EF7B10");
        if (n.isEmpty() || n.matches("^n?\\d+$")) return Color.parseColor("#E32017");
        return Color.parseColor("#0019A8");
    }

    private static int getLineTextColor(String lineName) {
        if (lineName == null) return Color.WHITE;
        if (lineName.trim().toLowerCase().contains("circle")) return Color.BLACK;
        return Color.WHITE;
    }

    /** Short badge text for disruption card (e.g. DIS, SWR, PIC). */
    private static String getLineBadgeText(String name, String id) {
        if (name != null && !name.isEmpty()) {
            String n = name.toUpperCase();
            if (n.contains("SOUTH WESTERN")) return "SWR";
            if (n.contains("DISTRICT")) return "DIS";
            if (n.contains("PICCADILLY")) return "PIC";
            if (n.contains("VICTORIA")) return "VIC";
            if (n.contains("NORTHERN")) return "NOR";
            if (n.contains("CENTRAL")) return "CEN";
            if (n.contains("BAKERLOO")) return "BAK";
            if (n.contains("JUBILEE")) return "JUB";
            if (n.contains("METROPOLITAN")) return "MET";
            if (n.contains("CIRCLE")) return "CIR";
            if (n.contains("HAMMERSMITH")) return "H&C";
            if (n.contains("WATERLOO") && n.contains("CITY")) return "W&C";
            if (n.contains("ELIZABETH")) return "ELZ";
            if (n.contains("OVERGROUND")) return "LOG";
            if (n.contains("DLR")) return "DLR";
            if (name.length() >= 3) return n.substring(0, Math.min(3, name.length()));
        }
        if (id != null && id.length() >= 3) return id.substring(0, 3).toUpperCase();
        return "—";
    }

    /** Returns the first status detail for a line, or null if none. */
    private static StatusDetail getFirstStatusDetail(LineStatus line) {
        if (line.getLineStatuses() == null || line.getLineStatuses().isEmpty()) return null;
        return line.getLineStatuses().get(0);
    }

    private static String extractUrl(String text) {
        if (text == null) return null;
        Matcher matcher = Pattern.compile("(https?://\\S+)").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void announceDisruptionsIfTtsOn(List<LineStatus> disruptions) {
        if (ttsHelper == null) ttsHelper = new TtsHelper(requireContext());
        if (!ttsHelper.isTtsEnabled() || disruptions == null || disruptions.isEmpty()) return;
        String count = String.valueOf(disruptions.size());
        String phrase = count + " disruption" + (disruptions.size() == 1 ? "" : "s") + " active";
        if (disruptions.size() >= 1) {
            LineStatus line1 = disruptions.get(0);
            StatusDetail status1 = getFirstStatusDetail(line1);
            String desc = status1 != null ? cleanDisruptionText(status1.getReason(), status1.getStatusSeverityDescription()) : "";
            phrase += ". " + getLineBadgeText(line1.getName(), line1.getId()) + ": " + (desc.isEmpty() ? "service affected" : desc);
        }
        ttsHelper.speak(phrase);
    }

    private String cleanDisruptionText(String rawReason, String severityDescription) {
        if (rawReason == null || rawReason.isEmpty()) {
            return severityDescription != null ? severityDescription : "";
        }
        String cleaned = rawReason.replaceAll("https?://\\S+", "");
        cleaned = cleaned.replaceAll("(?i)For more details.*", "");
        cleaned = cleaned.trim();
        if (cleaned.length() < 5) {
            return severityDescription != null ? severityDescription : "";
        }
        return cleaned;
    }

    @Override
    public void onDestroyView() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
            ttsHelper = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
