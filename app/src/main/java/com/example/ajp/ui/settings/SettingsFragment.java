package com.example.ajp.ui.settings;

// AI Generated
// Built with Claude
// Lovable.dev reference

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.example.ajp.R;
import com.example.ajp.databinding.FragmentSettingsBinding;
import com.example.ajp.ui.feedback.FeedbackActivity;
import com.example.ajp.utils.AccessibilityPreferences;
import com.example.ajp.utils.RouteMonitorPrefs;
import com.example.ajp.utils.RouteMonitorScheduler;
import com.example.ajp.utils.SettingsPrefs;
import java.util.HashSet;
import java.util.Set;

/**
 * Screen controller for Settings UI interactions.
 * Handles view binding, user actions, and state observation from the ViewModel or supporting services.
 * Navigation and rendering decisions are kept here, while heavy data work is delegated to lower layers.
 */

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    // Lifecycle: inflate view and prepare references for this feature section.
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    // Lifecycle: bind listeners/observers after the view hierarchy exists.
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SettingsPrefs prefs = SettingsPrefs.get(requireContext());

        binding.switchDarkMode.setChecked(prefs.isDarkMode());
        binding.switchHighContrast.setChecked(prefs.isHighContrast());
        binding.switchLargeText.setChecked(prefs.isLargeText());
        binding.switchTts.setChecked(prefs.isTtsEnabled());

        AccessibilityPreferences accPrefs = AccessibilityPreferences.get(requireContext());

        String speed = accPrefs.getWalkingSpeed();
        int speedButtonId = R.id.btn_speed_avg;
        if (AccessibilityPreferences.SPEED_SLOW.equals(speed)) speedButtonId = R.id.btn_speed_slow;
        else if (AccessibilityPreferences.SPEED_FAST.equals(speed)) speedButtonId = R.id.btn_speed_fast;
        binding.toggleWalkingSpeed.check(speedButtonId);
        binding.toggleWalkingSpeed.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_speed_slow) accPrefs.setWalkingSpeed(AccessibilityPreferences.SPEED_SLOW);
            else if (checkedId == R.id.btn_speed_avg) accPrefs.setWalkingSpeed(AccessibilityPreferences.SPEED_AVERAGE);
            else if (checkedId == R.id.btn_speed_fast) accPrefs.setWalkingSpeed(AccessibilityPreferences.SPEED_FAST);
        });

        int maxWalk = accPrefs.getMaxWalkingMinutes();
        binding.sliderMaxWalk.setValue(maxWalk);
        binding.tvMaxWalkValue.setText(maxWalk + " min");
        binding.sliderMaxWalk.addOnChangeListener((slider, value, fromUser) -> {
            int minutes = (int) value;
            binding.tvMaxWalkValue.setText(minutes + " min");
            if (fromUser) accPrefs.setMaxWalkingMinutes(minutes);
        });

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            if (getActivity() != null) getActivity().recreate();
        });

        binding.switchHighContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setHighContrast(isChecked);
            if (getActivity() != null) getActivity().recreate();
        });

        binding.switchLargeText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setLargeText(isChecked);
            if (getActivity() != null) getActivity().recreate();
        });

        binding.switchTts.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setTtsEnabled(isChecked));

        binding.linkAdditionalDisruption.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tfl.gov.uk/status-updates/stations-lifts-and-escalators-works-and-closures"));
            startActivity(i);
        });

        binding.langEnglish.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_EN_GB));
        binding.langSpanish.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_ES));
        binding.langChinese.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_ZH));
        binding.langArabic.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_AR));

        binding.sendFeedback.setOnClickListener(v -> startActivity(new Intent(requireContext(), FeedbackActivity.class)));

        RouteMonitorPrefs routePrefs = RouteMonitorPrefs.get(requireContext());
        Set<String> simulatedIds = routePrefs.getSimulatedDisruptedStopIds();
        boolean simulateOn = simulatedIds != null && !simulatedIds.isEmpty();
        binding.switchSimulateLift.setChecked(simulateOn);
        binding.simulateStopsLayout.setVisibility(simulateOn ? View.VISIBLE : View.GONE);
        if (simulatedIds != null && !simulatedIds.isEmpty()) {
            binding.editSimulateStopIds.setText(String.join(", ", simulatedIds));
        }
        binding.switchSimulateLift.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.simulateStopsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                routePrefs.setSimulatedDisruptedStopIds(new HashSet<>());
            } else {
                applySimulatedStopIds(routePrefs, binding.editSimulateStopIds.getText() != null ? binding.editSimulateStopIds.getText().toString() : "");
            }
        });
        binding.editSimulateStopIds.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && binding.switchSimulateLift.isChecked()) {
                applySimulatedStopIds(routePrefs, binding.editSimulateStopIds.getText() != null ? binding.editSimulateStopIds.getText().toString() : "");
            }
        });
        binding.btnTestRouteChange.setOnClickListener(v -> {
            if (binding.switchSimulateLift.isChecked() && binding.editSimulateStopIds.getText() != null) {
                applySimulatedStopIds(routePrefs, binding.editSimulateStopIds.getText().toString());
            }
            Toast.makeText(requireContext(), R.string.test_route_change_scheduled, Toast.LENGTH_SHORT).show();
            new Thread(() -> RouteMonitorScheduler.runCheckSync(requireContext())).start();
        });
    }

    // Applies state changes and keeps dependent UI/data values synchronized.
    private void applySimulatedStopIds(RouteMonitorPrefs routePrefs, String text) {
        if (text == null || text.trim().isEmpty()) {
            routePrefs.setSimulatedDisruptedStopIds(new HashSet<>());
            return;
        }
        Set<String> ids = new HashSet<>();
        for (String part : text.split("[,\\s]+")) {
            String trimmed = part != null ? part.trim() : "";
            if (!trimmed.isEmpty()) ids.add(trimmed);
        }
        routePrefs.setSimulatedDisruptedStopIds(ids);
    }

    // Applies state changes and keeps dependent UI/data values synchronized.
    private void applyLanguage(String langCode) {
        SettingsPrefs.get(requireContext()).setLanguage(langCode);
        if (getActivity() != null) getActivity().recreate();
    }

    @Override
    // Lifecycle: clear view references to avoid leaks in fragment recreation.
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


