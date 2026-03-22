package com.example.ajp.ui.nearby;

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
import com.example.ajp.databinding.FragmentStationSearchBinding;
import com.example.ajp.ui.main.MainActivity;
import java.util.List;

/**
 * Search results for station/place name. Add in Commit 8; place handling in Commit 14.
 * PURPOSE: searchStopsByName (TfL + PlaceSearch); show results; station click → Live Arrivals; place click → MainActivity.switchToJourneysWithDestination(stopId, name).
 * WHY: StopsViewModel.searchStopsByName(context, query) merges stations and places; stopId "lat,lon" identifies place for journey destination.
 * ISSUES: Pass requireContext() to searchStopsByName; place click does not open arrivals (no TfL/NR for places).
 */
public class StationSearchFragment extends Fragment {

    private static final String ARG_QUERY = "query";

    private FragmentStationSearchBinding binding;
    private StopsViewModel viewModel;
    private NearbyStationsAdapter adapter;

    public static StationSearchFragment newInstance(String query) {
        StationSearchFragment f = new StationSearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query != null ? query : "");
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStationSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(StopsViewModel.class);
        adapter = new NearbyStationsAdapter((stopId, stopName) -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity ma = (MainActivity) getActivity();
                if (stopId != null && stopId.contains(",")) {
                    ma.switchToJourneysWithDestination(stopId, stopName);
                } else {
                    ma.showLiveArrivalsFragment(stopId, stopName);
                }
            }
        });

        binding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewSearch.setAdapter(adapter);

        String query = "";
        if (getArguments() != null) {
            query = getArguments().getString(ARG_QUERY, "");
        }
        binding.headerQuery.setText(query.isEmpty() ? "" : "\"" + query + "\"");

        viewModel.searchStopsByName(requireContext(), query);

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::onSearchResults);
        viewModel.getSearchLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });
    }

    private void onSearchResults(List<StopItem> results) {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.GONE);
        if (results == null || results.isEmpty()) {
            binding.emptyMessage.setVisibility(View.VISIBLE);
            binding.recyclerViewSearch.setVisibility(View.GONE);
            binding.emptyMessage.setText(R.string.search_no_results);
        } else {
            binding.emptyMessage.setVisibility(View.GONE);
            binding.recyclerViewSearch.setVisibility(View.VISIBLE);
            adapter.setStops(results);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
