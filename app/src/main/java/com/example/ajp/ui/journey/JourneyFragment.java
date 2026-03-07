package com.example.ajp.ui.journey;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.databinding.FragmentJourneyBinding;
import com.example.ajp.ui.main.MainActivity;
import com.example.ajp.ui.routedetails.RouteDetailsActivity;
import com.example.ajp.utils.AccessibilityPreferences;
import com.example.ajp.utils.LocationManager;
import com.example.ajp.utils.NetworkMonitor;
import com.example.ajp.utils.PermissionManager;
import com.example.ajp.utils.SettingsPrefs;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Journey plan tab: from/to fields, date/time, find routes, route list. Add in Commit 11.
 * PURPOSE: Bind to JourneyViewModel; resolve from/to (location or manual); call findRoutes; open RouteDetailsActivity on route click.
 * WHY: LocationManager for current location; AccessibilityPreferences for API params; RouteAdapter with TimeFormatUtil for duration.
 * ISSUES: Request location permission before getCurrentLocation; setSavedDestination from MainActivity when place clicked in search (Commit 14).
 */
public class JourneyFragment extends Fragment {
    // AI Generated
    // Lovable.dev: UI mockup reference
    private static final String TAG = "JourneyFragment";
    private static final int REQUEST_LOCATION = 1002;

    private FragmentJourneyBinding binding;
    private JourneyViewModel viewModel;
    private PermissionManager permissionManager;
    private LocationManager locationManager;
    private RouteAdapter adapter;
    private Calendar selectedDateTime;
    private NetworkMonitor networkMonitor;
    private Handler minuteTickHandler;
    private Runnable minuteTickRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentJourneyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        permissionManager = PermissionManager.getInstance(requireContext());
        locationManager = LocationManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(JourneyViewModel.class);

        selectedDateTime = Calendar.getInstance();
        updateDateTimeButtons();

        String origin = viewModel.getSavedOrigin().getValue();
        if (origin != null && !origin.isEmpty()) binding.origin.setText(origin);
        String dest = viewModel.getSavedDestination().getValue();
        if (dest != null && !dest.isEmpty()) binding.destination.setText(dest);

        binding.origin.addTextChangedListener(saveOriginWatcher);
        binding.destination.addTextChangedListener(saveDestinationWatcher);

        binding.showFilters.setOnClickListener(v -> {
            boolean visible = binding.filtersSection.getVisibility() == View.VISIBLE;
            binding.filtersSection.setVisibility(visible ? View.GONE : View.VISIBLE);
            binding.showFilters.setText(visible ? getString(R.string.show_filters) : getString(R.string.hide_filters));
        });

        binding.origin.setOnTouchListener((v, event) -> {
            final int DRAWABLE_LEFT = 0;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (binding.origin.getCompoundDrawables()[DRAWABLE_LEFT] != null) {
                    int boundary = binding.origin.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width()
                            + binding.origin.getPaddingStart() + 30;
                    if (event.getX() <= boundary) {
                        useCurrentLocation();
                        return true;
                    }
                }
            }
            return false;
        });

        binding.findRoutes.setOnClickListener(v -> findRoutes());

        networkMonitor = new NetworkMonitor(requireContext());
        // AI Generated: defensive fallbacks for offline banner
        View btnGoToHome = binding.getRoot().findViewById(R.id.btn_go_to_home);
        if (btnGoToHome != null) {
            btnGoToHome.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToHomeTab();
                }
            });
        }
        updateOfflineState();

        binding.btnToday.setOnClickListener(v -> openDatePicker());
        binding.btnDepartNow.setOnClickListener(v -> openTimePicker());

        // Initialize step-free toggle from saved preference
        AccessibilityPreferences accPrefs = AccessibilityPreferences.get(requireContext());
        binding.switchStepFree.setChecked(accPrefs.isStepFree());
        
        // Save preference when toggle changes
        binding.switchStepFree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            accPrefs.setStepFree(isChecked);
            android.util.Log.d("JourneyFragment", "Step-free toggle changed to: " + isChecked);
            refreshRoutesIfSearchValid();
        });

        // Avoid crowds: save to SettingsPrefs and re-sort so crowded routes move to the bottom
        SettingsPrefs prefs = SettingsPrefs.get(requireContext());
        binding.switchAvoidCrowds.setChecked(prefs.isAvoidCrowded());
        binding.switchAvoidCrowds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAvoidCrowded(isChecked);
            viewModel.reSortRoutes();
        });

        adapter = new RouteAdapter(List.of());
        adapter.setOnRouteClickListener(this::openRouteDetails);
        binding.routesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.routesList.setAdapter(adapter);

        binding.sortChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;
            if (checkedId == R.id.chip_fastest) viewModel.setOptimizationStrategy(com.example.ajp.utils.RouteOptimizer.Strategy.FASTEST);
            else if (checkedId == R.id.chip_fewest_transfers) viewModel.setOptimizationStrategy(com.example.ajp.utils.RouteOptimizer.Strategy.FEWEST_TRANSFERS);
            else if (checkedId == R.id.chip_least_walking) viewModel.setOptimizationStrategy(com.example.ajp.utils.RouteOptimizer.Strategy.LEAST_WALKING);
            else if (checkedId == R.id.chip_least_crowded) viewModel.setOptimizationStrategy(com.example.ajp.utils.RouteOptimizer.Strategy.LEAST_CROWDED);
        });

        viewModel.getRoutes().observe(getViewLifecycleOwner(), this::onRoutes);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                boolean isLoading = loading != null && loading;
                binding.journeyLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) binding.suggestedRoutesSection.setVisibility(View.GONE);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.getRoutePreviewFrom().observe(getViewLifecycleOwner(), from -> {
            if (binding != null && from != null) binding.tvRouteFrom.setText(from);
        });
        viewModel.getRoutePreviewTo().observe(getViewLifecycleOwner(), to -> {
            if (binding != null && to != null) binding.tvRouteTo.setText(to);
        });
        viewModel.getRoutePreviewSummary().observe(getViewLifecycleOwner(), summary -> {
            if (binding != null && summary != null) binding.tvRouteSummaryBadge.setText(summary);
        });
        viewModel.getRoutePreviewMapCoords().observe(getViewLifecycleOwner(), coords -> {
            if (binding != null && coords != null) loadRouteMap(coords);
        });
    }

    // AI Generated: null-safe view access
    private void updateOfflineState() {
        if (networkMonitor == null || binding == null) return;
        networkMonitor.refreshOnlineState();
        boolean offline = !networkMonitor.isOnline();
        // Null-safe in case views are missing (e.g. stale ViewBinding)
        View offlineBanner = binding.getRoot().findViewById(R.id.offline_banner);
        if (offlineBanner != null) offlineBanner.setVisibility(offline ? View.VISIBLE : View.GONE);
        if (binding.findRoutes != null) {
            binding.findRoutes.setEnabled(!offline);
            binding.findRoutes.setAlpha(offline ? 0.5f : 1f);
        }
    }

    private String getGoogleMapsApiKey() {
        try {
            android.content.pm.ApplicationInfo app = requireContext().getPackageManager()
                    .getApplicationInfo(requireContext().getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            android.os.Bundle bundle = app.metaData;
            String key = bundle != null ? bundle.getString("com.google.android.geo.API_KEY", "") : "";
            Log.d(TAG, "MAP: API key present=" + (key != null && !key.isEmpty()));
            return key;
        } catch (Exception e) {
            Log.e(TAG, "MAP: Failed to read API key", e);
            return "";
        }
    }

    private void loadRouteMap(JourneyViewModel.MapCoords coords) {
        if (binding == null) return;
        Log.d(TAG, "MAP: loadRouteMap coords=" + coords.startLat + "," + coords.startLon + " -> " + coords.endLat + "," + coords.endLon);
        binding.cvRoutePreview.setVisibility(View.VISIBLE);
        String apiKey = getGoogleMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "MAP: No API key, skipping map load");
            return;
        }
        new Thread(() -> {
            String pathParam;
            String encodedPolyline = fetchDirectionsPolyline(apiKey, coords.startLat, coords.startLon, coords.endLat, coords.endLon);
            if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
                pathParam = "path=weight:5|color:0x00BFFF|enc:" + encodedPolyline;
                Log.d(TAG, "MAP: Using Directions polyline (real roads)");
            } else {
                pathParam = "path=weight:5|color:0x00BFFF|" + coords.startLat + "," + coords.startLon + "|" + coords.endLat + "," + coords.endLon;
                Log.d(TAG, "MAP: Using straight line (Directions API failed)");
            }
            String mapUrl = "https://maps.googleapis.com/maps/api/staticmap?size=600x300"
                    + "&markers=color:green|label:A|" + coords.startLat + "," + coords.startLon
                    + "&markers=color:red|label:B|" + coords.endLat + "," + coords.endLon
                    + "&" + pathParam
                    + "&key=" + apiKey;
            Log.d(TAG, "MAP: Loading static map URL (truncated)...");
            loadMapImage(mapUrl, binding.ivRouteMap);
        }).start();
    }

    /** Fetches encoded polyline from Directions API for real road path; returns null on failure. */
    private String fetchDirectionsPolyline(String apiKey, double startLat, double startLon, double endLat, double endLon) {
        try {
            String url = "https://maps.googleapis.com/maps/api/directions/json"
                    + "?origin=" + startLat + "," + startLon
                    + "&destination=" + endLat + "," + endLon
                    + "&mode=driving"
                    + "&key=" + apiKey;
            Log.d(TAG, "MAP: Fetching Directions API...");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            int status = conn.getResponseCode();
            Log.d(TAG, "MAP: Directions API HTTP status=" + status);
            java.io.InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                Log.e(TAG, "MAP: No response body");
                return null;
            }
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            org.json.JSONObject json = new org.json.JSONObject(sb.toString());
            String statusStr = json.optString("status", "");
            Log.d(TAG, "MAP: Directions status=" + statusStr);
            if (!"OK".equals(statusStr)) {
                Log.w(TAG, "MAP: Directions API error: " + json.optString("error_message", statusStr));
                return null;
            }
            org.json.JSONArray routes = json.optJSONArray("routes");
            if (routes != null && routes.length() > 0) {
                org.json.JSONObject polyline = routes.getJSONObject(0).optJSONObject("overview_polyline");
                if (polyline != null) {
                    String points = polyline.optString("points", "");
                    Log.d(TAG, "MAP: Got polyline, length=" + points.length());
                    return points;
                }
            }
            Log.w(TAG, "MAP: No routes or polyline in response");
        } catch (Exception e) {
            Log.e(TAG, "MAP: Directions fetch failed", e);
        }
        return null;
    }

    private void updateDateTimeButtons() {
        if (binding == null) return;
        binding.btnToday.setText(String.format(Locale.UK, "%d %s",
                selectedDateTime.get(Calendar.DAY_OF_MONTH),
                selectedDateTime.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.UK)));
        binding.btnDepartNow.setText(String.format(Locale.UK, "%02d:%02d",
                selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE)));
    }

    private void openDatePicker() {
        DatePickerDialog dlg = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeButtons();
                    refreshRoutesIfSearchValid();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH));
        Calendar minCal = Calendar.getInstance();
        minCal.set(Calendar.HOUR_OF_DAY, 0);
        minCal.set(Calendar.MINUTE, 0);
        minCal.set(Calendar.SECOND, 0);
        minCal.set(Calendar.MILLISECOND, 0);
        dlg.getDatePicker().setMinDate(minCal.getTimeInMillis());
        dlg.show();
    }

    private void openTimePicker() {
        TimePickerDialog dlg = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateDateTimeButtons();
                    refreshRoutesIfSearchValid();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true);
        dlg.show();
    }

    /** If origin and destination are filled, re-fetch routes with the current selected date/time. */
    private void refreshRoutesIfSearchValid() {
        if (binding == null) return;
        String from = binding.origin.getText() != null ? binding.origin.getText().toString().trim() : "";
        String to = binding.destination.getText() != null ? binding.destination.getText().toString().trim() : "";
        if (!from.isEmpty() && !to.isEmpty()) {
            findRoutesWithCurrentParams();
        }
    }

    private void findRoutesWithCurrentParams() {
        if (binding == null) return;
        String from = binding.origin.getText() != null ? binding.origin.getText().toString().trim() : "";
        String to = binding.destination.getText() != null ? binding.destination.getText().toString().trim() : "";
        if (from.isEmpty() || to.isEmpty()) return;
        String time = String.format(Locale.UK, "%02d%02d",
                selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE));
        String date = String.format(Locale.UK, "%04d%02d%02d",
                selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH) + 1, selectedDateTime.get(Calendar.DAY_OF_MONTH));
        viewModel.findRoutes(from, to, time, date);
    }

    private final TextWatcher saveOriginWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (viewModel != null) viewModel.setSavedOrigin(s != null ? s.toString() : "");
        }
    };
    private final TextWatcher saveDestinationWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (viewModel != null) viewModel.setSavedDestination(s != null ? s.toString() : "");
        }
    };

    private void useCurrentLocation() {
        if (permissionManager.checkLocationPermission(requireContext())) {
            locationManager.getCurrentLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(double lat, double lon) {
                    if (binding != null) {
                        String coords = String.format(Locale.UK, "%.4f,%.4f", lat, lon);
                        binding.origin.removeTextChangedListener(saveOriginWatcher);
                        binding.origin.setText(coords);
                        binding.origin.addTextChangedListener(saveOriginWatcher);
                        if (viewModel != null) viewModel.setSavedOrigin(coords);
                    }
                }
                @Override
                public void onLocationFailed() {
                    Toast.makeText(requireContext(), R.string.current_location, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            permissionManager.askLocationPermission(this, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && permissionManager.isPermissionGranted(grantResults)) {
            useCurrentLocation();
        }
    }

    private void openRouteDetails(RouteItem selectedRoute) {
        Intent i = new Intent(requireContext(), RouteDetailsActivity.class);
        i.putExtra(RouteDetailsActivity.EXTRA_ROUTE, selectedRoute);
        String userFrom = binding.origin.getText() != null ? binding.origin.getText().toString().trim() : "";
        String userTo = binding.destination.getText() != null ? binding.destination.getText().toString().trim() : "";
        if (!userFrom.isEmpty()) i.putExtra(RouteDetailsActivity.EXTRA_USER_FROM_INPUT, userFrom);
        if (!userTo.isEmpty()) i.putExtra(RouteDetailsActivity.EXTRA_USER_TO_INPUT, userTo);
        List<RouteItem> allRoutes = viewModel.getRoutes().getValue();
        if (allRoutes != null && !allRoutes.isEmpty()) {
            java.util.ArrayList<RouteItem> alternatives = new java.util.ArrayList<>(allRoutes);
            alternatives.removeIf(r -> r != null && r.equals(selectedRoute));
            i.putExtra(RouteDetailsActivity.EXTRA_ALTERNATIVES, alternatives);
        }
        startActivity(i);
    }

    private void findRoutes() {
        if (networkMonitor != null && !networkMonitor.isOnline()) {
            Toast.makeText(requireContext(), R.string.youre_offline_view_saved, Toast.LENGTH_SHORT).show();
            return;
        }
        String from = binding.origin.getText() != null ? binding.origin.getText().toString().trim() : "";
        String to = binding.destination.getText() != null ? binding.destination.getText().toString().trim() : "";
        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(requireContext(), R.string.find_routes, Toast.LENGTH_SHORT).show();
            return;
        }
        findRoutesWithCurrentParams();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateOfflineState();
        startMinuteTick();
    }

    private void onRoutes(List<RouteItem> routes) {
        if (binding == null) return;
        adapter.submitList(routes != null ? routes : List.of());
        if (routes != null && !routes.isEmpty()) {
            binding.cvRoutePreview.setVisibility(View.VISIBLE);
            binding.suggestedRoutesSection.setVisibility(View.VISIBLE);
            binding.cvRoutePreview.post(() -> {
                if (binding != null) {
                    binding.getRoot().smoothScrollTo(0, binding.cvRoutePreview.getTop());
                }
            });
            JourneyViewModel.MapCoords coords = viewModel.getRoutePreviewMapCoords().getValue();
            if (coords != null) loadRouteMap(coords);
        } else {
            binding.cvRoutePreview.setVisibility(View.GONE);
            binding.suggestedRoutesSection.setVisibility(View.GONE);
        }
    }

    private void loadMapImage(String url, ImageView imageView) {
        new Thread(() -> {
            try {
                java.io.InputStream in = new java.net.URL(url).openStream();
                final android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
                in.close();
                if (bitmap != null) {
                    Log.d(TAG, "MAP: Bitmap loaded " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    imageView.post(() -> {
                        imageView.setImageBitmap(bitmap);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    });
                } else {
                    Log.e(TAG, "MAP: Bitmap decode returned null - check URL or API key");
                }
            } catch (Exception e) {
                Log.e(TAG, "MAP: loadMapImage failed", e);
            }
        }).start();
    }

    /** Updates the displayed time every minute when the selected time is today and not in the future. */
    private void startMinuteTick() {
        if (minuteTickHandler == null) minuteTickHandler = new Handler(Looper.getMainLooper());
        if (minuteTickRunnable != null) minuteTickHandler.removeCallbacks(minuteTickRunnable);
        minuteTickRunnable = () -> {
            if (binding == null || selectedDateTime == null) return;
            Calendar now = Calendar.getInstance();
            // Only update display when selected is today and not in the future (so "live" time ticks)
            if (selectedDateTime.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && selectedDateTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    && !selectedDateTime.after(now)) {
                selectedDateTime.setTimeInMillis(now.getTimeInMillis());
                updateDateTimeButtons();
            }
            if (minuteTickHandler != null && minuteTickRunnable != null) {
                minuteTickHandler.postDelayed(minuteTickRunnable, 60000);
            }
        };
        Calendar now = Calendar.getInstance();
        long delayMs = (60 - now.get(Calendar.SECOND)) * 1000L - now.get(Calendar.MILLISECOND);
        if (delayMs <= 0) delayMs = 60000;
        minuteTickHandler.postDelayed(minuteTickRunnable, delayMs);
    }

    @Override
    public void onDestroyView() {
        if (minuteTickHandler != null && minuteTickRunnable != null) {
            minuteTickHandler.removeCallbacks(minuteTickRunnable);
            minuteTickRunnable = null;
        }
        if (binding != null) {
            binding.origin.removeTextChangedListener(saveOriginWatcher);
            binding.destination.removeTextChangedListener(saveDestinationWatcher);
        }
        super.onDestroyView();
        binding = null;
    }
}
