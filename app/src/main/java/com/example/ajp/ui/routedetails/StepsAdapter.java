package com.example.ajp.ui.routedetails;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.api.Leg;
import com.example.ajp.api.ModeRef;
import com.example.ajp.api.RouteOptionRef;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.List;

/**
 * Adapter for step-by-step journey legs. Add in Commit 12.
 * PURPOSE: Bind Leg to item_step; show title (mode/destination), instruction summary, duration (TimeFormatUtil); colored indicator by mode.
 * WHY: Leg.getDuration() is seconds; convert to minutes then formatMinutesToHourMin for display.
 * ISSUES: buildStepTitle distinguishes walk/bus/tube; getStepColor returns COLOR_TUBE/BUS/WALK.
 */
public class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.StepViewHolder> {

    private static final int COLOR_TUBE = 0xFF2196F3;  // Blue
    private static final int COLOR_BUS = 0xFFE53935;   // Red
    private static final int COLOR_WALK = 0xFF9E9E9E;  // Grey

    private final List<Leg> legs;

    public StepsAdapter(List<Leg> legs) {
        this.legs = legs != null ? legs : java.util.Collections.emptyList();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        Leg leg = legs.get(position);
        String modeName = leg.getMode() != null ? leg.getMode().getName() : "";
        String instruction = leg.getInstruction() != null ? leg.getInstruction().getSummary() : "";

        String title = buildStepTitle(leg, modeName);
        String detail = instruction;
        int durationSec = leg.getDuration();
        int durationMin = durationSec / 60;
        String durationText = durationMin > 0 ? TimeFormatUtil.formatMinutesToHourMin(durationMin) : "";

        holder.tvTitle.setText(title);
        holder.tvDetail.setText(detail);
        holder.tvDetail.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvDuration.setText(durationText);
        holder.tvDuration.setVisibility(durationText.isEmpty() ? View.GONE : View.VISIBLE);

        int color = getStepColor(modeName);
        holder.indicator.setBackgroundColor(color);
    }

    private String buildStepTitle(Leg leg, String modeName) {
        String m = modeName.toLowerCase();
        if (m.contains("walk")) {
            String to = leg.getArrivalPoint() != null ? leg.getArrivalPoint().getCommonName() : "";
            return to.isEmpty() ? "Walk to destination" : "Walk to " + to;
        }
        if (m.contains("bus")) {
            String to = leg.getArrivalPoint() != null ? leg.getArrivalPoint().getCommonName() : "";
            return to.isEmpty() ? "Bus" : "Bus to " + to;
        }
        if (m.contains("tube") || m.contains("dlr") || m.contains("overground") || m.contains("rail") || m.contains("underground")) {
            StringBuilder sb = new StringBuilder();
            for (RouteOptionRef ro : leg.getRouteOptions()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(ro.getName());
            }
            String from = leg.getDeparturePoint() != null ? leg.getDeparturePoint().getCommonName() : "";
            String to = leg.getArrivalPoint() != null ? leg.getArrivalPoint().getCommonName() : "";
            if (sb.length() > 0 && !from.isEmpty() && !to.isEmpty()) {
                return sb.toString() + " → " + to;
            }
            if (!to.isEmpty()) return "Train to " + to;
            return sb.length() > 0 ? sb.toString() : "Tube";
        }
        return modeName.isEmpty() ? "Step" : modeName + " leg";
    }

    private int getStepColor(String modeName) {
        String m = modeName.toLowerCase();
        if (m.contains("tube") || m.contains("dlr") || m.contains("overground") || m.contains("rail") || m.contains("underground") || m.contains("elizabeth")) {
            return COLOR_TUBE;
        }
        if (m.contains("bus")) return COLOR_BUS;
        return COLOR_WALK;
    }

    @Override
    public int getItemCount() {
        return legs.size();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        final View indicator;
        final TextView tvTitle;
        final TextView tvDetail;
        final TextView tvDuration;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            indicator = itemView.findViewById(R.id.step_indicator);
            tvTitle = itemView.findViewById(R.id.step_title);
            tvDetail = itemView.findViewById(R.id.step_detail);
            tvDuration = itemView.findViewById(R.id.step_duration);
        }
    }
}
