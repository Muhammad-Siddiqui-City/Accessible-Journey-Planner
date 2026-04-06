package com.example.ajp.ui.arrivals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.databinding.FragmentLiveArrivalsBinding;
import com.example.ajp.ui.nearby.StopsViewModel;
import com.example.ajp.utils.TtsHelper;
import java.util.List;
import java.util.Locale;

/**
 * Screen controller for LiveArrivals UI interactions.
 * Handles view binding, user actions, and state observation from the ViewModel or supporting services.
 * Navigation and rendering decisions are kept here, while heavy data work is delegated to lower layers.
 */

public class LiveArrivalsFragment extends Fragment {

    public static final String ARG_STOP_ID = "stop_id";
    public static final String ARG_STOP_NAME = "stop_name";

    private FragmentLiveArrivalsBinding binding;
    private StopsViewModel viewModel;
    private ArrivalsAdapter adapter;
    private TtsHelper ttsHelper;
    private Boolean nationalRailNoDataHint = false;

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static LiveArrivalsFragment newInstance(String stopId, String stopName) {
        LiveArrivalsFragment f = new LiveArrivalsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STOP_ID, stopId);
        args.putString(ARG_STOP_NAME, stopName != null ? stopName : "");
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    // Lifecycle: inflate view and prepare references for this feature section.
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLiveArrivalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    // Read stop args, trigger initial load, then observe arrivals and rail-hint for empty messaging.
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String stopId = getArguments() != null ? getArguments().getString(ARG_STOP_ID, "") : "";
        final String stopName = getArguments() != null ? getArguments().getString(ARG_STOP_NAME, "") : "";

        binding.stationName.setText(stopName);

        viewModel = new ViewModelProvider(this).get(StopsViewModel.class);
        ttsHelper = new TtsHelper(requireContext());
        adapter = new ArrivalsAdapter();
        binding.arrivalsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.arrivalsList.setAdapter(adapter);

        binding.back.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });
        binding.refresh.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            viewModel.loadArrivals(stopId);
        });

        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadArrivals(stopId);

        viewModel.getSelectedStopArrivals().observe(getViewLifecycleOwner(), this::onArrivalsReceived);
        viewModel.getNationalRailNoDataHint().observe(getViewLifecycleOwner(), this::onNationalRailHintChanged);
    }

    /** Maps list state to list vs empty UI; drives TTS only on non-empty successful loads. */
    private void onArrivalsReceived(List<Arrival> arrivals) {
        binding.progressBar.setVisibility(View.GONE);
        if (arrivals == null || arrivals.isEmpty()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            String msg = Boolean.TRUE.equals(nationalRailNoDataHint)
                    ? getString(R.string.national_rail_arrivals_unavailable)
                    : getString(R.string.no_upcoming_arrivals);
            binding.emptyText.setText(msg);
            binding.arrivalsList.setVisibility(View.GONE);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.arrivalsList.setVisibility(View.VISIBLE);
            adapter.submitList(arrivals);
            announceFirstArrivalIfTtsOn(arrivals);
        }
    }

    /** When empty, swap copy between generic "no arrivals" vs National Rail unavailable. */
    private void onNationalRailHintChanged(Boolean hint) {
        nationalRailNoDataHint = hint;
        if (binding.emptyText.getVisibility() == View.VISIBLE) {
            String msg = Boolean.TRUE.equals(hint)
                    ? getString(R.string.national_rail_arrivals_unavailable)
                    : getString(R.string.no_upcoming_arrivals);
            binding.emptyText.setText(msg);
        }
    }

    /** Picks soonest arrival and speaks a short phrase for accessibility users. */
    private void announceFirstArrivalIfTtsOn(List<Arrival> arrivals) {
        if (arrivals == null || arrivals.isEmpty() || ttsHelper == null) return;

        Arrival soonest = arrivals.get(0);
        for (Arrival a : arrivals) {
            if (a.getTimeToStationSeconds() < soonest.getTimeToStationSeconds()) {
                soonest = a;
            }
        }
        String phrase = formatArrivalPhrase(soonest);
        if (phrase != null && !phrase.isEmpty()) {
            ttsHelper.speak(phrase);
        }
    }

    /** Human-readable phrase for TTS (bus vs train wording, sub-minute handling). */
    private static String formatArrivalPhrase(Arrival arrival) {
        String mode = arrival.getModeName() != null ? arrival.getModeName().toLowerCase(Locale.ROOT) : "";
        String vehicle = mode.contains("bus") ? "bus" : "train";
        int seconds = arrival.getTimeToStationSeconds();
        String time;
        if (seconds < 60) {
            time = "less than a minute";
        } else {
            int minutes = seconds / 60;
            time = minutes == 1 ? "1 minute" : minutes + " minutes";
        }
        return "Next " + vehicle + " in " + time;
    }

    @Override
    // Release TTS and binding; Fragment instance may outlive the view.
    public void onDestroyView() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
            ttsHelper = null;
        }
        super.onDestroyView();
        binding = null;
    }
}


