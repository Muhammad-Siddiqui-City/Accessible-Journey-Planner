package com.example.ajp.ui.analytics;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.graphics.Color;
import com.example.ajp.data.local.AppDatabase;
import com.example.ajp.data.local.JourneyLog;
import com.example.ajp.utils.TimeFormatUtil;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Builds MPAndroidChart datasets from JourneyLog Room data. */
public class AnalyticsViewModel extends AndroidViewModel {

    private final MutableLiveData<Integer> journeysCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> timeSavedMinutes = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> crowdsAvoided = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> efficiencyPercent = new MutableLiveData<>(0);
    private final MutableLiveData<BarData> barData = new MutableLiveData<>();
    private final MutableLiveData<LineData> lineData = new MutableLiveData<>();
    private final MutableLiveData<PieData> pieData = new MutableLiveData<>();
    private final MutableLiveData<List<FrequentRouteItem>> frequentRoutes = new MutableLiveData<>(List.of());
    private final MutableLiveData<List<ModeCountItem>> modeBreakdown = new MutableLiveData<>(List.of());

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Integer> getJourneysCount() { return journeysCount; }
    public LiveData<Integer> getTimeSavedMinutes() { return timeSavedMinutes; }
    public LiveData<Integer> getCrowdsAvoided() { return crowdsAvoided; }
    public LiveData<Integer> getEfficiencyPercent() { return efficiencyPercent; }
    public LiveData<BarData> getBarData() { return barData; }
    public LiveData<LineData> getLineData() { return lineData; }
    public LiveData<PieData> getPieData() { return pieData; }
    public LiveData<List<FrequentRouteItem>> getFrequentRoutes() { return frequentRoutes; }
    public LiveData<List<ModeCountItem>> getModeBreakdown() { return modeBreakdown; }

    public void loadWeeklyStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            List<JourneyLog> logs = AppDatabase.getInstance(getApplication()).journeyLogDao().getLogsSince(weekAgo);
            processLogs(logs);
        });
    }

    private void processLogs(List<JourneyLog> logs) {
        int total = logs.size();
        int saved = 0;
        int lowCrowd = 0;
        for (JourneyLog log : logs) {
            saved += log.savedMinutes;
            if (log.isCrowdLow) lowCrowd++;
        }
        int efficiency = total > 0 ? Math.min(100, (lowCrowd * 100) / total) : 0;
        journeysCount.postValue(total);
        timeSavedMinutes.postValue(saved);
        crowdsAvoided.postValue(lowCrowd);
        efficiencyPercent.postValue(efficiency);

        int[] journeysPerDay = new int[7];
        int[] savedPerDay = new int[7];
        Map<String, Integer> modeCounts = new HashMap<>();
        Map<String, Integer> routeCounts = new HashMap<>();
        Map<String, Long> routeLastTimestamp = new HashMap<>();
        Map<String, JourneyLog> routeMostRecent = new HashMap<>();

        Calendar cal = Calendar.getInstance(Locale.UK);
        for (JourneyLog log : logs) {
            cal.setTimeInMillis(log.timestamp);
            int dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            journeysPerDay[dayIndex]++;
            savedPerDay[dayIndex] += log.savedMinutes;

            String modeStr = log.mode != null ? log.mode : "Mixed";
            for (String mode : modeStr.split(",")) {
                String m = mode != null ? mode.trim() : "";
                if (m.isEmpty()) m = "Mixed";
                modeCounts.put(m, modeCounts.getOrDefault(m, 0) + 1);
            }

            String from = log.fromStation != null ? log.fromStation : "";
            String to = log.toStation != null ? log.toStation : "";
            String key = from + " -> " + to;
            if (!from.isEmpty() || !to.isEmpty()) {
                routeCounts.put(key, routeCounts.getOrDefault(key, 0) + 1);
                long prevTs = routeLastTimestamp.getOrDefault(key, 0L);
                if (log.timestamp > prevTs) {
                    routeLastTimestamp.put(key, log.timestamp);
                    routeMostRecent.put(key, log);
                }
            }
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            barEntries.add(new BarEntry(i, journeysPerDay[i]));
        }
        BarDataSet barDataSet = new BarDataSet(barEntries, "Journeys");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS, 200);
        barData.postValue(new BarData(barDataSet));

        ArrayList<Entry> lineEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            lineEntries.add(new Entry(i, savedPerDay[i]));
        }
        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Time Saved");
        lineDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        lineData.postValue(new LineData(lineDataSet));

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Integer> modeColors = new ArrayList<>();
        for (Map.Entry<String, Integer> e : modeCounts.entrySet()) {
            if (e.getValue() > 0) {
                pieEntries.add(new PieEntry(e.getValue(), e.getKey()));
                modeColors.add(getTflColorForMode(e.getKey()));
            }
        }
        if (pieEntries.isEmpty()) {
            pieEntries.add(new PieEntry(1f, "No data"));
            modeColors.add(Color.LTGRAY);
        }
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Modes");
        pieDataSet.setDrawValues(false);
        pieDataSet.setValueTextSize(0f);
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(0f);
        pieDataSet.setColors(modeColors);
        pieData.postValue(new PieData(pieDataSet));

        List<Map.Entry<String, Integer>> modeEntries = new ArrayList<>(modeCounts.entrySet());
        modeEntries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<ModeCountItem> breakdown = new ArrayList<>();
        for (Map.Entry<String, Integer> e : modeEntries) {
            if (e.getValue() > 0) {
                breakdown.add(new ModeCountItem(e.getKey(), e.getValue(), getTflColorForMode(e.getKey())));
            }
        }
        modeBreakdown.postValue(breakdown);

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(routeCounts.entrySet());
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            long tsA = routeLastTimestamp.getOrDefault(a.getKey(), 0L);
            long tsB = routeLastTimestamp.getOrDefault(b.getKey(), 0L);
            return Long.compare(tsB, tsA);
        });
        List<FrequentRouteItem> items = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<String, Integer> e = sorted.get(i);
            JourneyLog mostRecent = routeMostRecent.get(e.getKey());
            String modeDisplay = mostRecent != null && mostRecent.mode != null ? mostRecent.mode.replace(",", ", ") : "";
            if (mostRecent != null && modeDisplay.isEmpty()) modeDisplay = "Mixed";
            String detail = mostRecent != null ? (modeDisplay + " · ~" + TimeFormatUtil.formatMinutesToHourMin(mostRecent.durationMinutes)) : "";
            items.add(new FrequentRouteItem(e.getKey(), detail, e.getValue()));
        }
        frequentRoutes.postValue(items);
    }

    private static int getTflColorForMode(String label) {
        if (label == null) return Color.LTGRAY;
        switch (label) {
            case "Tube": return Color.parseColor("#0019A8");
            case "Bus": return Color.parseColor("#DC241F");
            case "Elizabeth": return Color.parseColor("#6950a1");
            case "Overground": return Color.parseColor("#ef7b10");
            case "Mixed": return Color.parseColor("#007229");
            case "DLR": return Color.parseColor("#00a4a7");
            case "Rail": return Color.parseColor("#0019A8");
            default: return Color.LTGRAY;
        }
    }
}


