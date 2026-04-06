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
import com.example.ajp.ui.journey.RouteLineColors;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * RecyclerView adapter for Steps item rendering.
 * Maps domain/UI models into row views and keeps list-specific formatting in one place.
 * This avoids repeating display logic in fragments and keeps row behavior consistent across updates.
 */

public class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.StepViewHolder> {

    private static final int COLOR_TUBE = 0xFF2196F3;
    private static final int COLOR_BUS = 0xFFE53935;

    /** TfL summaries often match our title with tiny differences (punctuation, "towards" vs "to"). */
    private static final Pattern WALK_PREFIX = Pattern.compile(
            "^\\s*walk(ing)?\\s+(to|towards|along|via)\\s+",
            Pattern.CASE_INSENSITIVE);

    private final List<Leg> legs;

    public StepsAdapter(List<Leg> legs) {
        this.legs = legs != null ? legs : java.util.Collections.emptyList();
    }

    @NonNull
    @Override
    // Initializes screen state, wiring, and startup behavior for this lifecycle stage.
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(v);
    }

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        Leg leg = legs.get(position);
        String modeName = leg.getMode() != null ? leg.getMode().getName() : "";
        String instruction = leg.getInstruction() != null ? leg.getInstruction().getSummary() : "";

        String title = buildStepTitle(leg, modeName);
        String detail = filterRedundantDetail(modeName, title, instruction);
        boolean isWalk = modeName.toLowerCase(Locale.UK).contains("walk");
        int durationSec = leg.getDuration();
        int durationMin = durationSec / 60;
        String durationText = (!isWalk && durationMin > 0)
                ? TimeFormatUtil.formatMinutesToHourMin(durationMin) : "";

        holder.tvTitle.setText(title);
        holder.tvDetail.setText(detail);
        holder.tvDetail.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvDuration.setText(durationText);
        holder.tvDuration.setVisibility(durationText.isEmpty() ? View.GONE : View.VISIBLE);

        int color = getStepColor(leg, modeName);
        holder.indicator.setBackgroundColor(color);
    }

    /**
     * Hides subtitle when it only repeats the bold line (TfL often sends the same wording with minor variants).
     */
    private static String filterRedundantDetail(String modeName, String title, String instruction) {
        if (instruction == null) return "";
        String detail = instruction.trim();
        if (detail.isEmpty()) return "";

        String t = title != null ? title.trim() : "";
        if (collapseForDedupe(detail).equals(collapseForDedupe(t))) {
            return "";
        }
        // Walking: subtitle is almost always a duplicate of "Walk to …" unless it's extra info (e.g. time estimate).
        if (modeName != null && modeName.toLowerCase(Locale.UK).contains("walk")) {
            if (isWalkInstructionRedundant(t, detail)) {
                return "";
            }
        }
        return detail;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private static String collapseForDedupe(String s) {
        if (s == null) return "";
        String out = s.trim().toLowerCase(Locale.UK)
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replaceAll("\\s+", " ");
        while (out.endsWith(".") || out.endsWith(",") || out.endsWith(";") || out.endsWith(":")) {
            out = out.substring(0, out.length() - 1).trim();
        }
        return out;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private static String walkDestinationCore(String line) {
        if (line == null) return "";
        String core = WALK_PREFIX.matcher(line.trim()).replaceFirst("").trim();
        return collapseForDedupe(core);
    }

    private static boolean isWalkInstructionRedundant(String title, String instruction) {
        String ins = collapseForDedupe(instruction);
        if (!ins.startsWith("walk ") && !ins.startsWith("walking ") && !ins.startsWith("continue ")) {
            // Likely an estimate or note, not a duplicate heading.
            return false;
        }
        String titleDest = walkDestinationCore(title);
        String insDest = walkDestinationCore(instruction);
        return !titleDest.isEmpty() && titleDest.equals(insDest);
    }

    // Transforms inputs into the shape required by downstream components.
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
        return RouteLineColors.WALKING;
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
        if (n.contains("south western")) return RouteLineColors.SOUTH_WESTERN_RAILWAY;
        if (isNationalRailLineName(n)) return RouteLineColors.NATIONAL_RAIL;
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

    private static boolean isNationalRailLineName(String n) {
        if (n.contains("southeastern") || n.contains("thameslink")
                || n.contains("great western") || n.contains("greater anglia") || n.contains("c2c")
                || n.contains("crosscountry") || n.contains("chiltern") || n.contains("grand central")
                || n.contains("lumo") || n.contains("east midlands") || n.contains("avanti")
                || n.contains("transport for wales") || n.contains("scotrail")
                || n.contains("southern") && n.contains("railway")) {
            return true;
        }
        return n.contains("national rail");
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


