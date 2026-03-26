package com.example.ajp.ui.main;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import com.example.ajp.R;
import com.example.ajp.databinding.ActivityMainBinding;
import com.example.ajp.ui.analytics.AnalyticsFragment;
import com.example.ajp.ui.arrivals.LiveArrivalsFragment;
import com.example.ajp.ui.arrivals.StationTrainsFragment;
import com.example.ajp.ui.home.HomeFragment;
import com.example.ajp.ui.home.PopularStationsFragment;
import com.example.ajp.ui.journey.JourneyFragment;
import com.example.ajp.ui.journey.JourneyViewModel;
import com.example.ajp.ui.nearby.NearbyStationsFragment;
import com.example.ajp.ui.nearby.StationSearchFragment;
import com.example.ajp.ui.settings.SettingsFragment;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;







/**
 * Activity that hosts the Main flow.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String KEY_SELECTED_ITEM = "selected_item";
    private int selectedItemId = R.id.navigation_home;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyFull(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (SettingsPrefs.get(getApplicationContext()).isHighContrast()) {
            setTheme(R.style.Theme_AJP_HighContrast);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.navigation_home);
        }

        binding.bottomNavigation.setSelectedItemId(selectedItemId);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            selectedItemId = item.getItemId();
            Fragment frag = getFragmentForItem(selectedItemId);
            if (frag != null) {
                showFragment(frag);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(getFragmentForItem(selectedItemId));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_ITEM, selectedItemId);
    }

    private Fragment getFragmentForItem(int itemId) {
        if (itemId == R.id.navigation_home) return new HomeFragment();
        if (itemId == R.id.navigation_journeys) return new JourneyFragment();
        if (itemId == R.id.navigation_analytics) return new AnalyticsFragment();
        if (itemId == R.id.navigation_settings) return new SettingsFragment();
        return new HomeFragment();
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }


    public void showNearbyStationsFragment(double lat, double lon) {
        Fragment fragment = NearbyStationsFragment.newInstance(lat, lon);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }


    public void showStationSearchFragment(String query) {
        Fragment fragment = StationSearchFragment.newInstance(query != null ? query.trim() : "");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }


    public void showLiveArrivalsFragment(String stopId, String stopName) {
        Fragment fragment = LiveArrivalsFragment.newInstance(stopId, stopName);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }


    public void showPopularStationsFragment() {
        Fragment fragment = new PopularStationsFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }


    public void showStationTrainsFragment(String stopId, String stopName) {
        Fragment fragment = StationTrainsFragment.newInstance(stopId, stopName);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }


    public void switchToJourneysTab() {
        selectedItemId = R.id.navigation_journeys;
        binding.bottomNavigation.setSelectedItemId(selectedItemId);
        showFragment(new JourneyFragment());
    }


    public void switchToHomeTab() {
        selectedItemId = R.id.navigation_home;
        binding.bottomNavigation.setSelectedItemId(selectedItemId);
        showFragment(new HomeFragment());
    }


    public void switchToJourneysWithDestination(String coords, String displayName) {
        JourneyViewModel jvm = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(JourneyViewModel.class);
        String dest = (displayName != null && !displayName.trim().isEmpty()) ? displayName.trim() : (coords != null ? coords : "");
        jvm.setSavedDestination(dest);
        selectedItemId = R.id.navigation_journeys;
        binding.bottomNavigation.setSelectedItemId(selectedItemId);
        showFragment(new JourneyFragment());
    }


    public void checkAndHandleRouteChange() {

    }

}

