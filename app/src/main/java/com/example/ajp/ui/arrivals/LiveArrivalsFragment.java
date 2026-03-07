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
 * Live Arrivals fragment (inside activity or as tab). Add in Commit 10.
 * PURPOSE: Show arrivals for stopId from args; StopsViewModel.loadArrivals(stopId); TTS for soonest arrival when enabled.
 * WHY: ARG_STOP_ID/ARG_STOP_NAME from arguments; filter by line (all/line name); speak "Next bus in X minutes" etc. via pendingTtsPhrase.
 * ISSUES: When stopId contains "," (place), StopsViewModel skips TfL/NR; show empty or no-upcoming message.
 */
public class LiveArrivalsFragment extends Fragment {

    public static final String ARG_STOP_ID = "stop_id";
    public static final String ARG_STOP_NAME = "stop_name";

    private FragmentLiveArrivalsBinding binding;
    private StopsViewModel viewModel;
    private ArrivalsAdapter adapter;
    private TtsHelper ttsHelper;
    private Boolean nationalRailNoDataHint = false;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLiveArrivalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
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

    private void onNationalRailHintChanged(Boolean hint) {
        nationalRailNoDataHint = hint;
        if (binding.emptyText.getVisibility() == View.VISIBLE) {
            String msg = Boolean.TRUE.equals(hint)
                    ? getString(R.string.national_rail_arrivals_unavailable)
                    : getString(R.string.no_upcoming_arrivals);
            binding.emptyText.setText(msg);
        }
    }

    /** If Text-to-speech is on, speak "Next bus in X minutes" or "Next train in X minutes" for the soonest arrival. */
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

    /** Same minute rule as the UI: under 60s = "less than a minute", else sec/60 minutes so spoken time matches the screen. */
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
    public void onDestroyView() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
            ttsHelper = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
