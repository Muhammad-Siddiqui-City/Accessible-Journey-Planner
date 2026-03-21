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
 * Detail screen shown from Popular Stations.
 * PURPOSE: Show trains that are "to this station" (TfL arrivals) and "from this station" (National Rail departures).
 */
public class StationTrainsFragment extends Fragment {

    public static final String ARG_STOP_ID = "stop_id";
    public static final String ARG_STOP_NAME = "stop_name";

    private FragmentStationTrainsBinding binding;
    private StopsViewModel viewModel;
    private ArrivalsAdapter arrivalsAdapter;
    private ArrivalsAdapter departuresAdapter;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStationTrainsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String stopId = getArguments() != null ? getArguments().getString(ARG_STOP_ID, "") : "";
        String stopName = getArguments() != null ? getArguments().getString(ARG_STOP_NAME, "") : "";

        // UI: show only train times (max 5) - hide "to/from" labels and departures list.
        binding.stationName.setVisibility(View.GONE);
        binding.tvArrivalsTitle.setVisibility(View.GONE);
        binding.tvArrivalsEmpty.setVisibility(View.GONE);
        binding.tvDeparturesTitle.setVisibility(View.GONE);
        binding.tvDeparturesEmpty.setVisibility(View.GONE);
        binding.arrivalsList.setVisibility(View.VISIBLE);
        binding.departuresList.setVisibility(View.GONE);

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

        // Trigger both API loaders: "to" from TfL, "from" from National Rail.
        viewModel.loadTrainsToAndFrom(stopId);

        viewModel.getTflArrivalsToStation().observe(getViewLifecycleOwner(), this::onArrivalsToStation);
        viewModel.getNationalRailDeparturesFromStation().observe(getViewLifecycleOwner(), this::onDeparturesFromStation);
    }

    private void onArrivalsToStation(List<Arrival> arrivals) {
        List<Arrival> list = arrivals != null ? arrivals : Collections.emptyList();
        arrivalsAdapter.submitList(list);
    }

    private void onDeparturesFromStation(List<Arrival> departures) {
        List<Arrival> list = departures != null ? departures : Collections.emptyList();
        // Departures list is hidden in this prototype; keep adapter cleared so no work is wasted.
        departuresAdapter.submitList(Collections.emptyList());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

