package com.example.ajp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.Toast;
import com.example.ajp.R;
import com.example.ajp.api.MatchedStop;
import com.example.ajp.api.TflApi;
import com.example.ajp.api.TflSearchResponse;
import com.example.ajp.databinding.FragmentPopularStationsBinding;
import com.example.ajp.api.RetrofitClient;
import com.example.ajp.ui.nearby.StopItem;
import com.example.ajp.ui.main.MainActivity;
import com.example.ajp.utils.ApiKeyManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import retrofit2.Response;

/**
 * Screen that lists the app's "Popular Stations".
 * PURPOSE: Let users pick a station, then show trains to/from it.
 */
public class PopularStationsFragment extends Fragment {

    private FragmentPopularStationsBinding binding;
    private PopularStationsAdapter adapter;
    private final List<String> popularStationQueries = Arrays.asList(
            "London Liverpool Street",
            "London Waterloo",
            "London Paddington",
            "London King's Cross",
            "St Pancras International",
            "London Victoria",
            "London Bridge",
            "Euston",
            "Charing Cross"
    );

    private static String normalizeForStationMatch(String s) {
        if (s == null) return "";
        // Keep letters/numbers only so "King's Cross" vs "Kings Cross" still matches.
        String n = s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        // Ignore the word "london" when matching TfL stop names.
        n = n.replace("london ", "").replace("london", "").trim();
        // Handle "King's" where apostrophe normalization can create "king s".
        n = n.replace("king s", "kings").trim();
        return n;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPopularStationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PopularStationsAdapter();
        binding.popularStationsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.popularStationsList.setAdapter(adapter);

        binding.back.setOnClickListener(v -> {
            if (getActivity() != null) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        adapter.setOnPopularStationClickListener(stop -> {
            if (stop == null) return;
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showStationTrainsFragment(stop.getStopId(), stop.getName());
            }
        });

        loadPopularStationsFromTfL();
    }

    private void loadPopularStationsFromTfL() {
        if (!ApiKeyManager.isTflKeyValid()) {
            adapter.submitList(new ArrayList<>());
            if (getActivity() != null) {
                Toast.makeText(requireContext(), R.string.search_no_results, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        adapter.submitList(new ArrayList<>());
        new Thread(() -> {
            try {
                TflApi api = RetrofitClient.getApi();
                List<StopItem> out = new ArrayList<>();

                for (String query : popularStationQueries) {
                    Response<TflSearchResponse> resp = api.searchStops(query).execute();
                    if (!resp.isSuccessful() || resp.body() == null) continue;

                    List<MatchedStop> matches = resp.body().getMatches();
                    if (matches == null || matches.isEmpty()) continue;

                    // Prototype pick: prefer an exact-ish match on name, else first match.
                    MatchedStop best = matches.get(0);
                    String qNorm = normalizeForStationMatch(query);
                    for (MatchedStop m : matches) {
                        if (m == null) continue;
                        String name = m.getName() != null ? m.getName() : "";
                        String nNorm = normalizeForStationMatch(name);
                        if (!nNorm.isEmpty() && !qNorm.isEmpty() && nNorm.contains(qNorm)) {
                            best = m;
                            break;
                        }
                    }

                    if (best != null && best.getId() != null && !best.getId().trim().isEmpty()) {
                        out.add(new StopItem(best.getId(), best.getName(), 0, new String[0], false, "", true));
                    }
                }

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> adapter.submitList(out));
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.search_no_results, Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

