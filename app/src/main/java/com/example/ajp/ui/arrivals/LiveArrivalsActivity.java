package com.example.ajp.ui.arrivals;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ajp.R;
import com.example.ajp.databinding.ActivityLiveArrivalsBinding;
import com.example.ajp.ui.nearby.StopsViewModel;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity entry point for the LiveArrivals flow.
 * Owns lifecycle-sensitive orchestration, screen wiring, and intent-based handover to adjacent features.
 * Business rules are intentionally pushed to utilities/ViewModels so this class stays focused on UI flow.
 */

public class LiveArrivalsActivity extends AppCompatActivity {

    public static final String EXTRA_STOP_ID = "stopId";
    public static final String EXTRA_STOP_NAME = "stopName";

    private ActivityLiveArrivalsBinding binding;
    private StopsViewModel viewModel;
    private ArrivalsAdapter adapter;
    private String stopId;
    private String selectedLine = "all";
    private List<Arrival> allArrivals = new ArrayList<>();

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyFull(newBase));
    }

    @Override
    // Lifecycle: initialise dependencies and wire initial UI state.
    protected void onCreate(Bundle savedInstanceState) {
        if (SettingsPrefs.get(getApplicationContext()).isHighContrast()) {
            setTheme(R.style.Theme_AJP_HighContrast);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityLiveArrivalsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        stopId = getIntent() != null ? getIntent().getStringExtra(EXTRA_STOP_ID) : null;
        String stopName = getIntent() != null ? getIntent().getStringExtra(EXTRA_STOP_NAME) : null;
        if (stopName != null && !stopName.isEmpty()) {
            binding.stationName.setText(stopName);
        }

        viewModel = new ViewModelProvider(this).get(StopsViewModel.class);
        adapter = new ArrivalsAdapter();
        binding.arrivalsList.setLayoutManager(new LinearLayoutManager(this));
        binding.arrivalsList.setAdapter(adapter);

        binding.back.setOnClickListener(v -> finish());
        binding.refresh.setOnClickListener(v -> loadArrivals());

        binding.pillAll.setOnClickListener(v -> selectPill("all"));
        binding.pillVictoria.setOnClickListener(v -> selectPill("victoria"));
        binding.pillPiccadilly.setOnClickListener(v -> selectPill("piccadilly"));
        binding.pillNorthern.setOnClickListener(v -> selectPill("northern"));
        binding.pillCentral.setOnClickListener(v -> selectPill("central"));
        binding.pillJubilee.setOnClickListener(v -> selectPill("jubilee"));

        viewModel.getSelectedStopArrivals().observe(this, arrivals -> {
            binding.progressBar.setVisibility(View.GONE);
            allArrivals = arrivals != null ? arrivals : new ArrayList<>();
            applyFilter();
        });
        viewModel.getNationalRailNoDataHint().observe(this, isNationalRail -> {
            if (binding.emptyText.getVisibility() == View.VISIBLE) {
                binding.emptyText.setText(Boolean.TRUE.equals(isNationalRail)
                        ? R.string.national_rail_arrivals_unavailable
                        : R.string.no_upcoming_arrivals);
            }
        });

        loadArrivals();
    }

    // Retrieves and prepares data needed by the current flow, including error-handling paths.
    private void loadArrivals() {
        if (stopId != null && !stopId.isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.emptyText.setVisibility(View.GONE);
            viewModel.loadArrivals(stopId);
        } else {
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.emptyText.setText(R.string.no_upcoming_arrivals);
        }
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private void selectPill(String line) {
        selectedLine = line;
        setPill(binding.pillAll, "all");
        setPill(binding.pillVictoria, "victoria");
        setPill(binding.pillPiccadilly, "piccadilly");
        setPill(binding.pillNorthern, "northern");
        setPill(binding.pillCentral, "central");
        setPill(binding.pillJubilee, "jubilee");
        applyFilter();
    }

    // Applies state changes and keeps dependent UI/data values synchronized.
    private void applyFilter() {
        List<Arrival> filtered;
        if ("all".equals(selectedLine)) {
            filtered = allArrivals;
        } else {
            filtered = new ArrayList<>();
            for (Arrival a : allArrivals) {
                String lineName = a.getLineName() != null ? a.getLineName().toLowerCase() : "";
                if (lineName.contains(selectedLine)) {
                    filtered.add(a);
                }
            }
        }
        if (filtered.isEmpty()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            Boolean isNr = viewModel.getNationalRailNoDataHint().getValue();
            binding.emptyText.setText(Boolean.TRUE.equals(isNr)
                    ? R.string.national_rail_arrivals_unavailable
                    : R.string.no_upcoming_arrivals);
            binding.arrivalsList.setVisibility(View.GONE);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.arrivalsList.setVisibility(View.VISIBLE);
            adapter.submitList(filtered);
        }
    }

    private void setPill(TextView pill, String id) {
        boolean sel = id.equals(selectedLine);
        pill.setBackgroundResource(sel ? R.drawable.card_teal_gradient : R.drawable.glass_card);
        pill.setTextColor(ContextCompat.getColor(this, sel ? android.R.color.white : R.color.foreground));
    }

    @Override
    // Releases listeners/resources to avoid leaks across lifecycle recreation.
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}


