package com.example.ajp.ui.settings;

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
import com.example.ajp.ui.main.MainActivity;
import com.example.ajp.utils.AccessibilityPreferences;
import com.example.ajp.utils.RouteMonitorPrefs;
import com.example.ajp.utils.RouteMonitorScheduler;
import com.example.ajp.utils.SettingsPrefs;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings screen. Add in Commit 16.
 * PURPOSE: Dark mode, high contrast, large text, TTS, language; max walking minutes (AccessibilityPreferences); links to Feedback.
 * WHY: SettingsPrefs for app settings; AccessibilityPreferences for journey API params; setLanguage triggers recreate for locale.
 * ISSUES: Max walk value displayed as "X min"; recreate after language/dark so theme and locale apply.
 */
// AI Generated
// Built with Claude
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SettingsPrefs prefs = SettingsPrefs.get(requireContext());

        // Load saved state into Accessibility toggles
        binding.switchDarkMode.setChecked(prefs.isDarkMode());
        binding.switchHighContrast.setChecked(prefs.isHighContrast());
        binding.switchLargeText.setChecked(prefs.isLargeText());
        binding.switchTts.setChecked(prefs.isTtsEnabled());

        // Mobility Profile: load from AccessibilityPreferences and persist on change
        AccessibilityPreferences accPrefs = AccessibilityPreferences.get(requireContext());
        
        // Walking speed toggle group
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

        // Max walking slider: show value in TextView and persist on change
        int maxWalk = accPrefs.getMaxWalkingMinutes();
        binding.sliderMaxWalk.setValue(maxWalk);
        binding.tvMaxWalkValue.setText(maxWalk + " min");
        binding.sliderMaxWalk.addOnChangeListener((slider, value, fromUser) -> {
            int minutes = (int) value;
            binding.tvMaxWalkValue.setText(minutes + " min");
            if (fromUser) accPrefs.setMaxWalkingMinutes(minutes);
        });

        // Dark Mode: save and apply theme, then recreate so UI updates
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            if (getActivity() != null) getActivity().recreate();
        });

        // High contrast: save and recreate so theme (black bg, white text/outlines) is applied
        binding.switchHighContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setHighContrast(isChecked);
            if (getActivity() != null) getActivity().recreate();
        });

        // Large text: save and recreate so attachBaseContext runs with new font scale
        binding.switchLargeText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setLargeText(isChecked);
            if (getActivity() != null) getActivity().recreate();
        });

        // Text to speech: persisted; screens that speak can check prefs.isTtsEnabled()
        binding.switchTts.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setTtsEnabled(isChecked));

        binding.linkAdditionalDisruption.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tfl.gov.uk/status-updates/stations-lifts-and-escalators-works-and-closures"));
            startActivity(i);
        });

        // Language: save and recreate so locale is applied
        binding.langEnglish.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_EN_GB));
        binding.langSpanish.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_ES));
        binding.langChinese.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_ZH));
        binding.langArabic.setOnClickListener(v -> applyLanguage(SettingsPrefs.LANG_AR));

        binding.sendFeedback.setOnClickListener(v -> startActivity(new Intent(requireContext(), FeedbackActivity.class)));

        // Test: simulate lift unavailable and run route check now
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
            new Thread(() -> {
                boolean changed = RouteMonitorScheduler.runCheckSync(requireContext());
                android.app.Activity a = getActivity();
                if (changed && a != null) {
                    a.runOnUiThread(() -> {
                        if (a instanceof MainActivity) {
                            ((MainActivity) a).checkAndHandleRouteChange();
                        }
                    });
                }
            }).start();
        });
    }

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

    private void applyLanguage(String langCode) {
        SettingsPrefs.get(requireContext()).setLanguage(langCode);
        if (getActivity() != null) getActivity().recreate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
