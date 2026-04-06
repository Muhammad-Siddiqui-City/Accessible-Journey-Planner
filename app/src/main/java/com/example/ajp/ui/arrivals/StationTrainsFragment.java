package com.example.ajp.ui.arrivals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import com.example.ajp.databinding.FragmentStationTrainsBinding;
import com.example.ajp.ui.nearby.StopsViewModel;
import java.util.Collections;
import java.util.List;

/**
 * Screen controller for StationTrains UI interactions.
 * Handles view binding, user actions, and state observation from the ViewModel or supporting services.
 * Navigation and rendering decisions are kept here, while heavy data work is delegated to lower layers.
 */

public class StationTrainsFragment extends Fragment {

    public static final String ARG_STOP_ID = "stop_id";
    public static final String ARG_STOP_NAME = "stop_name";

    private FragmentStationTrainsBinding binding;
    private StopsViewModel viewModel;
    private ArrivalsAdapter arrivalsAdapter;
    private ArrivalsAdapter departuresAdapter;

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static StationTrainsFragment newInstance(String stopId, String stopName) {
        StationTrainsFragment f = new StationTrainsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STOP_ID, stopId);
        args.putString(ARG_STOP_NAME, stopName != null ? stopName : "");
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    // Inflate binding once; binding nulled in onDestroyView.
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStationTrainsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    // Start dual fetch and observe separate LiveData for arrivals vs departures lists.
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String stopId = getArguments() != null ? getArguments().getString(ARG_STOP_ID, "") : "";
        String stopName = getArguments() != null ? getArguments().getString(ARG_STOP_NAME, "") : "";

        binding.stationName.setText(stopName);

        RecyclerView.LayoutManager lm = new LinearLayoutManager(requireContext());
        arrivalsAdapter = new ArrivalsAdapter();
        departuresAdapter = new ArrivalsAdapter();

        binding.arrivalsList.setLayoutManager(lm);
        binding.arrivalsList.setAdapter(arrivalsAdapter);
        binding.departuresList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.departuresList.setAdapter(departuresAdapter);

        binding.back.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        viewModel = new ViewModelProvider(this).get(StopsViewModel.class);

        viewModel.loadTrainsToAndFrom(stopId);

        viewModel.getTflArrivalsToStation().observe(getViewLifecycleOwner(), this::onArrivalsToStation);
        viewModel.getNationalRailDeparturesFromStation().observe(getViewLifecycleOwner(), this::onDeparturesFromStation);
    }

    /** TfL "trains to" list: toggles empty label independently of departures. */
    private void onArrivalsToStation(List<Arrival> arrivals) {
        List<Arrival> list = arrivals != null ? arrivals : Collections.emptyList();
        arrivalsAdapter.submitList(list);
        boolean empty = list.isEmpty();
        binding.tvArrivalsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.arrivalsList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** National Rail departures list: mirrors arrivals pattern for consistent UX. */
    private void onDeparturesFromStation(List<Arrival> departures) {
        List<Arrival> list = departures != null ? departures : Collections.emptyList();
        departuresAdapter.submitList(list);
        boolean empty = list.isEmpty();
        binding.tvDeparturesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.departuresList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    // Clear binding to avoid holding detached views after back navigation.
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


