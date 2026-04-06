package com.example.ajp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ajp.R;
import com.example.ajp.api.MatchedStop;
import com.example.ajp.api.TflApi;
import com.example.ajp.api.TflSearchResponse;
import com.example.ajp.databinding.FragmentPopularStationsBinding;
import com.example.ajp.api.RetrofitClient;
import com.example.ajp.ui.main.MainActivity;
import com.example.ajp.utils.ApiKeyManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import retrofit2.Response;

/** PopularStations: fragment wiring; data from ViewModel / services. */
public class PopularStationsFragment extends Fragment {

    private FragmentPopularStationsBinding binding;
    private PopularStationsAdapter adapter;

    private static String normalizeForStationMatch(String s) {
        if (s == null) return "";
        String n = s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        n = n.replace("london ", "").replace("london", "").trim();
        n = n.replace("king s", "kings").trim();
        return n;
    }

    private static String toSearchQuery(String label) {
        if (label == null) return "";
        String q = label.trim();
        if (q.endsWith(" Station")) {
            q = q.substring(0, q.length() - " Station".length()).trim();
        }
        return q;
    }

    private static MatchedStop pickBestMatch(List<MatchedStop> matches, String displayLabel) {
        if (matches == null || matches.isEmpty()) return null;
        String qNorm = normalizeForStationMatch(toSearchQuery(displayLabel));
        if (qNorm.isEmpty()) return matches.get(0);
        for (MatchedStop m : matches) {
            if (m == null) continue;
            String name = m.getName() != null ? m.getName() : "";
            String nNorm = normalizeForStationMatch(name);
            if (!nNorm.isEmpty() && (nNorm.contains(qNorm) || qNorm.contains(nNorm))) {
                return m;
            }
        }
        return matches.get(0);
    }

    @Nullable
    @Override
    // Lifecycle: inflate view and prepare references for this feature section.
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPopularStationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    // Lifecycle: bind listeners/observers after the view hierarchy exists.
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PopularStationsAdapter();
        binding.popularStationsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.popularStationsList.setAdapter(adapter);

        String[] labels = getResources().getStringArray(R.array.popular_station_labels);
        adapter.submitLabels(Arrays.asList(labels));

        binding.back.setOnClickListener(v -> {
            if (getActivity() != null) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        adapter.setOnPopularStationClickListener(this::resolveStopAndOpenTrains);
    }

    private void resolveStopAndOpenTrains(String displayLabel) {
        if (!ApiKeyManager.isTflKeyValid()) {
            Toast.makeText(requireContext(), R.string.search_no_results, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.popularStationsList.setEnabled(false);

        new Thread(() -> {
            MatchedStop resolved = null;
            try {
                TflApi api = RetrofitClient.getApi();
                String primaryQuery = toSearchQuery(displayLabel);
                resolved = trySearch(api, primaryQuery, displayLabel);
                if (resolved == null && !primaryQuery.equals(displayLabel.trim())) {
                    resolved = trySearch(api, displayLabel.trim(), displayLabel);
                }
            } catch (Exception ignored) {

            }

            final MatchedStop best = resolved;
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.popularStationsList.setEnabled(true);
                if (best != null && best.getId() != null && !best.getId().trim().isEmpty()) {
                    if (getActivity() instanceof MainActivity) {
                        String name = best.getName() != null ? best.getName() : displayLabel;
                        ((MainActivity) getActivity()).showStationTrainsFragment(best.getId(), name);
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.search_no_results, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Nullable

    private MatchedStop trySearch(TflApi api, String query, String displayLabel) throws java.io.IOException {
        if (query == null || query.isEmpty()) return null;
        Response<TflSearchResponse> resp = api.searchStops(query).execute();
        if (!resp.isSuccessful() || resp.body() == null) return null;
        List<MatchedStop> matches = resp.body().getMatches();
        if (matches == null || matches.isEmpty()) return null;
        return pickBestMatch(matches, displayLabel);
    }

    @Override
    // Lifecycle: clear view references to avoid leaks in fragment recreation.
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

