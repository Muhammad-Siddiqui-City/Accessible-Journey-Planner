package com.example.ajp.ui.nearby;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ajp.databinding.FragmentNearbyStationsBinding;
import com.example.ajp.ui.main.MainActivity;

/**
 * Nearby stops list for given lat/lon. Add in Commit 8.
 * PURPOSE: newInstance(lat, lon); load stops via StopsViewModel.loadNearbyStops(lat, lon); click → Live Arrivals (stopId, stopName).
 * WHY: Home passes location from LocationManager; NearbyStationsAdapter binds StopItem; line status and distance shown.
 * ISSUES: None.
 */
public class NearbyStationsFragment extends Fragment {

    private static final String ARG_LAT = "lat";
    private static final String ARG_LON = "lon";

    private FragmentNearbyStationsBinding binding;
    private StopsViewModel viewModel;
    private NearbyStationsAdapter adapter;

    /** Create fragment with lat/lon for loading nearest stops. */
    public static NearbyStationsFragment newInstance(double lat, double lon) {
        NearbyStationsFragment f = new NearbyStationsFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LON, lon);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNearbyStationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(StopsViewModel.class);
        adapter = new NearbyStationsAdapter((stopId, stopName) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showLiveArrivalsFragment(stopId, stopName);
            }
        });

        binding.recyclerViewNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNearby.setAdapter(adapter);

        binding.back.setOnClickListener(v -> {
            if (getActivity() != null) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        double lat = 51.5072;
        double lon = -0.1276;
        if (getArguments() != null) {
            lat = getArguments().getDouble(ARG_LAT, lat);
            lon = getArguments().getDouble(ARG_LON, lon);
        }
        viewModel.loadNearestStops(lat, lon);

        viewModel.getNearestStops().observe(getViewLifecycleOwner(), stops -> {
            adapter.setStops(stops);
            binding.progressBar.setVisibility(View.GONE);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading != null && isLoading ? View.VISIBLE : View.GONE);
            binding.recyclerViewNearby.setVisibility(isLoading != null && isLoading ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
