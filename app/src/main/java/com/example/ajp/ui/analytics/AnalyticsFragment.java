package com.example.ajp.ui.analytics;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.databinding.FragmentAnalyticsBinding;
import com.example.ajp.utils.TimeFormatUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.List;

/** Analytics: fragment wiring; data from ViewModel / services. */
public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private AnalyticsViewModel viewModel;
    private FrequentRouteAdapter frequentRouteAdapter;

    @Nullable
    @Override
    // Lifecycle: inflate view and prepare references for this feature section.
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    // Load Room-backed stats, wire chart observers, and bind frequent-routes RecyclerView.
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(AnalyticsViewModel.class);
        viewModel.loadWeeklyStats();

        setupChart(binding.chartWeekly);
        setupLineChart(binding.chartTrend);
        setupPieChart(binding.chartModes);

        viewModel.getJourneysCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                int c = count != null ? count : 0;
                binding.tvJourneysCount.setText(String.valueOf(c));
                binding.tvWeeklySummary.setText(c + " journeys this week");
            }
        });
        viewModel.getTimeSavedMinutes().observe(getViewLifecycleOwner(), mins -> {
            if (binding != null) {
                int m = mins != null ? mins : 0;
                binding.tvTimeSaved.setText(TimeFormatUtil.formatMinutesToHourMin(m));
                binding.achievementMessage.setText(getString(R.string.great_week_message, m));
                binding.tvTrendSummary.setText(TimeFormatUtil.formatMinutesToHourMin(m) + " saved this week");
            }
        });
        viewModel.getCrowdsAvoided().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) binding.tvCrowdsAvoided.setText(String.valueOf(count != null ? count : 0));
        });
        viewModel.getEfficiencyPercent().observe(getViewLifecycleOwner(), pct -> {
            if (binding != null) binding.tvAnalyticsPercentage.setText("+" + (pct != null ? pct : 0) + "%");
        });

        viewModel.getBarData().observe(getViewLifecycleOwner(), data -> {
            if (binding != null && data != null) {
                binding.chartWeekly.setData(data);
                binding.chartWeekly.invalidate();
            }
        });
        viewModel.getLineData().observe(getViewLifecycleOwner(), data -> {
            if (binding != null && data != null) {
                binding.chartTrend.setData(data);
                binding.chartTrend.invalidate();
            }
        });
        viewModel.getPieData().observe(getViewLifecycleOwner(), data -> {
            if (binding != null && data != null) {
                binding.chartModes.setData(data);
                binding.chartModes.setDrawEntryLabels(false);
                binding.chartModes.setDrawSliceText(false);
                if (data.getDataSet() != null) {
                    ((com.github.mikephil.charting.data.PieDataSet) data.getDataSet()).setDrawValues(false);
                }
                binding.chartModes.invalidate();
            }
        });

        viewModel.getModeBreakdown().observe(getViewLifecycleOwner(), items -> {
            if (binding == null) return;
            LinearLayout legend = binding.modesLegendRight;
            legend.removeAllViews();
            int totalSum = 0;
            for (ModeCountItem item : items) {
                legend.addView(createLegendRow(item.getMode(), item.getCount(), item.getColor()));
                totalSum += item.getCount();
            }
            if (totalSum > 0) {
                legend.addView(createLegendRow("Total", totalSum, 0));
            }
        });

        frequentRouteAdapter = new FrequentRouteAdapter();
        binding.rvFrequentRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFrequentRoutes.setNestedScrollingEnabled(false);
        binding.rvFrequentRoutes.setAdapter(frequentRouteAdapter);
        viewModel.getFrequentRoutes().observe(getViewLifecycleOwner(), items -> {
            if (binding != null) frequentRouteAdapter.submitList(items);
        });
    }

    /** Shared bar-chart styling: no legend, weekday X-axis, simple animation. */
    private void setupChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setFitBars(true);
        chart.animateY(600, Easing.EaseInOutQuad);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
    }

    private void setupLineChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        chart.animateX(600, Easing.EaseInOutQuad);
    }

    /** Donut-style pie: hide slice labels (legend built manually beside chart). */
    private void setupPieChart(PieChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.setDrawSliceText(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(62f);
        chart.setDrawCenterText(false);
        chart.setUsePercentValues(false);
        chart.setExtraOffsets(8, 8, 8, 8);
        chart.setEntryLabelColor(android.graphics.Color.TRANSPARENT);
        chart.getLegend().setEnabled(false);
        chart.animateY(600, Easing.EaseInOutQuad);
    }

    /** Programmatic legend row with optional colour dot for mode breakdown. */
    private View createLegendRow(String label, int count, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        row.setLayoutParams(rowParams);
        int dp10 = (int) (10 * getResources().getDisplayMetrics().density);
        if (color != 0) {
            View dot = new View(requireContext());
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            dot.setBackground(gd);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp10, dp10);
            dotParams.rightMargin = dp10 / 2;
            row.addView(dot, dotParams);
        }
        TextView tv = new TextView(requireContext());
        tv.setTextSize(14);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.foreground));
        tv.setText(label + ": " + count);
        if ("Total".equals(label)) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tv);
        return row;
    }

    @Override
    // Refreshes transient state that should be accurate when the screen becomes active.
    public void onResume() {
        super.onResume();
        // Refresh after returning from other screens so counts stay in sync with new logs.
        if (viewModel != null) viewModel.loadWeeklyStats();
    }

    @Override
    // Null binding only; ViewModel survives across tab switches.
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


