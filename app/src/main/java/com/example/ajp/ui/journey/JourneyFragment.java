package com.example.ajp.ui.journey;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;







/**
 * UI fragment for the Journey screen.
 */
public class JourneyFragment extends Fragment {


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
    private ActivityResultLauncher<Intent> voiceResultLauncher;
    private VoiceTarget pendingVoiceTarget = VoiceTarget.ORIGIN;

    private enum VoiceTarget {
        ORIGIN,
        DESTINATION
    }

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

        voiceResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || binding == null) return;
                    ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results == null || results.isEmpty()) return;
                    String spoken = results.get(0) != null ? results.get(0).trim() : "";
                    if (spoken.isEmpty()) return;
                    if (pendingVoiceTarget == VoiceTarget.DESTINATION) {
                        binding.destination.setText(spoken);
                    } else {
                        binding.origin.setText(spoken);
                    }
                });

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

        binding.btnOriginVoice.setOnClickListener(v -> launchVoiceInput(VoiceTarget.ORIGIN));
        binding.btnDestinationVoice.setOnClickListener(v -> launchVoiceInput(VoiceTarget.DESTINATION));

        binding.findRoutes.setOnClickListener(v -> findRoutes());

        networkMonitor = new NetworkMonitor(requireContext());
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


        AccessibilityPreferences accPrefs = AccessibilityPreferences.get(requireContext());
        binding.switchStepFree.setChecked(accPrefs.isStepFree());


        binding.switchStepFree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            accPrefs.setStepFree(isChecked);
            android.util.Log.d("JourneyFragment", "Step-free toggle changed to: " + isChecked);
            refreshRoutesIfSearchValid();
        });


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

    private void updateOfflineState() {
        if (networkMonitor == null || binding == null) return;
        networkMonitor.refreshOnlineState();
        boolean offline = !networkMonitor.isOnline();

        View offlineBanner = binding.getRoot().findViewById(R.id.offline_banner);
        if (offlineBanner != null) offlineBanner.setVisibility(offline ? View.VISIBLE : View.GONE);
        if (binding.findRoutes != null) {
            binding.findRoutes.setEnabled(!offline);
            binding.findRoutes.setAlpha(offline ? 0.5f : 1f);
        }
    }






    private static String buildRoutePreviewMapHtml(double sLat, double sLon, double eLat, double eLon) {
        String a = String.format(Locale.US, "[%f,%f]", sLat, sLon);
        String b = String.format(Locale.US, "[%f,%f]", eLat, eLon);
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>"
                + "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" crossorigin=\"\"/>"
                + "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\" crossorigin=\"\"></script>"
                + "<style>html,body,#map{height:100%;margin:0;padding:0}</style></head><body>"
                + "<div id=\"map\"></div><script>"
                + "var a=" + a + ",b=" + b + ";"
                + "var map=L.map('map',{zoomControl:false});"
                + "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,"
                + "attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + "L.polyline([a,b],{color:'#00BFFF',weight:4,opacity:0.95}).addTo(map);"
                + "L.circleMarker(a,{radius:7,fillColor:'#22c55e',color:'#fff',weight:2,fillOpacity:1}).addTo(map);"
                + "L.circleMarker(b,{radius:7,fillColor:'#ef4444',color:'#fff',weight:2,fillOpacity:1}).addTo(map);"
                + "if(a[0]===b[0]&&a[1]===b[1]){map.setView(a,15);}else{map.fitBounds(L.latLngBounds(a,b),{padding:[28,28],maxZoom:16});}"
                + "</script></body></html>";
    }

    private void loadRouteMap(JourneyViewModel.MapCoords coords) {
        if (binding == null) return;
        Log.d(TAG, "MAP: loadRouteMap leaflet coords=" + coords.startLat + "," + coords.startLon + " -> " + coords.endLat + "," + coords.endLon);
        binding.cvRoutePreview.setVisibility(View.VISIBLE);
        WebView wv = binding.wvRouteMap;
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        s.setUserAgentString("AccessibleJourneyPlanner/1.0 (Android; route preview)");
        wv.setWebViewClient(new WebViewClient());
        String html = buildRoutePreviewMapHtml(coords.startLat, coords.startLon, coords.endLat, coords.endLon);
        wv.loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "UTF-8", null);
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

    private void launchVoiceInput(VoiceTarget target) {
        pendingVoiceTarget = target;
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


    private void startMinuteTick() {
        if (minuteTickHandler == null) minuteTickHandler = new Handler(Looper.getMainLooper());
        if (minuteTickRunnable != null) minuteTickHandler.removeCallbacks(minuteTickRunnable);
        minuteTickRunnable = () -> {
            if (binding == null || selectedDateTime == null) return;
            Calendar now = Calendar.getInstance();

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
            WebView wv = binding.wvRouteMap;
            if (wv != null) {
                wv.stopLoading();
                wv.loadUrl("about:blank");
                ViewGroup parent = (ViewGroup) wv.getParent();
                if (parent != null) parent.removeView(wv);
                wv.destroy();
            }
        }
        super.onDestroyView();
        binding = null;
    }
}

