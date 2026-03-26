package com.example.ajp.ui.routedetails;

import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
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
 * RecyclerView adapter for Steps items.
 */
public class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.StepViewHolder> {

    private static final int COLOR_TUBE = 0xFF2196F3;
    private static final int COLOR_BUS = 0xFFE53935;
    private static final int COLOR_WALK = 0xFF9E9E9E;

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

        int color = getStepColor(leg, modeName);
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

    private int getStepColor(Leg leg, String modeName) {
        String m = modeName.toLowerCase();
        if (m.contains("tube") || m.contains("dlr") || m.contains("overground") || m.contains("rail") || m.contains("underground") || m.contains("elizabeth")) {
            return getRailStepColorByLineName(leg);
        }
        if (m.contains("bus")) return COLOR_BUS;
        return COLOR_WALK;
    }

    private int getRailStepColorByLineName(Leg leg) {
        String lineName = "";
        if (leg != null) {
            List<RouteOptionRef> opts = leg.getRouteOptions();
            if (opts != null && !opts.isEmpty() && opts.get(0) != null && opts.get(0).getName() != null) {
                lineName = opts.get(0).getName();
            }
        }
        String n = lineName.toLowerCase();
        if (n.contains("bakerloo")) return Color.parseColor("#B36305");
        if (n.contains("central")) return Color.parseColor("#E32017");
        if (n.contains("circle")) return Color.parseColor("#FFD300");
        if (n.contains("district")) return Color.parseColor("#00782A");
        if (n.contains("hammersmith")) return Color.parseColor("#F3A9BB");
        if (n.contains("jubilee")) return Color.parseColor("#A0A5A9");
        if (n.contains("metropolitan")) return Color.parseColor("#9B0056");
        if (n.contains("northern")) return Color.parseColor("#000000");
        if (n.contains("piccadilly")) return Color.parseColor("#003688");
        if (n.contains("victoria")) return Color.parseColor("#0098D4");
        if (n.contains("waterloo") && n.contains("city")) return Color.parseColor("#95CDBA");
        if (n.contains("elizabeth")) return Color.parseColor("#6950A1");
        if (n.contains("dlr")) return Color.parseColor("#00A4A7");
        if (n.contains("overground")) return Color.parseColor("#EF7B10");
        return COLOR_TUBE;
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

