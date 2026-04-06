package com.example.ajp.ui.journey;

import android.graphics.Color;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.utils.AccessibilityPreferences;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.List;
import java.util.Locale;

/** RecyclerView rows: Route. */
public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<RouteItem> items;
    private OnRouteClickListener listener;
    private boolean showBestBadge = true;

    public interface OnRouteClickListener {
        void onRouteClick(RouteItem item);
    }

    public RouteAdapter(@NonNull List<RouteItem> items) {
        this.items = items != null ? items : java.util.Collections.emptyList();
    }

    public void submitList(List<RouteItem> list) {
        this.items = list != null ? list : java.util.Collections.emptyList();
        notifyDataSetChanged();
    }

    public void setRoutes(List<RouteItem> routes) {
        submitList(routes);
    }

    public void setOnRouteClickListener(OnRouteClickListener listener) {
        this.listener = listener;
    }

    public void setShowBestBadge(boolean show) {
        this.showBestBadge = show;
    }

    @NonNull
    @Override

    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override

    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        RouteItem item = items.get(position);

        holder.tvDuration.setText(TimeFormatUtil.formatMinutesToHourMin(item.getDurationMinutesInt()));
        holder.tvDepartureTime.setText(item.getDepartureTime());
        holder.tvCrowdingDetail.setText(getCrowdingText(item.getCrowdingLevel(), holder.itemView));
        holder.tvTransfers.setText(item.getTransfersText());
        holder.tvRouteSummary.setText(item.getRouteSummary());

        holder.tvBestBadge.setVisibility(showBestBadge && position == 0 ? View.VISIBLE : View.GONE);

        int progressPercent = getCrowdingProgress(item.getCrowdingLevel());
        int progressColor = getCrowdingColor(item.getCrowdingLevel(), holder.itemView);
        holder.progressBarCrowding.setProgress(progressPercent);
        holder.progressBarCrowding.setProgressTintList(ColorStateList.valueOf(progressColor));
        holder.tvCrowdingLabel.setVisibility(View.VISIBLE);
        holder.tvCrowdingWarning.setVisibility(item.getCrowdingLevel() == RouteItem.CROWDING_HIGH ? View.VISIBLE : View.GONE);

        boolean stepFree = AccessibilityPreferences.get(holder.itemView.getContext()).isStepFree();
        boolean showLift = stepFree && item.hasLiftDisruption();
        String liftDisruptionText = showLift
                ? (item.getLiftDisruptionDescription() != null ? item.getLiftDisruptionDescription()
                : holder.itemView.getContext().getString(R.string.lift_disruption_warning))
                : null;
        holder.tvLiftDisruptionWarning.setVisibility(showLift ? View.VISIBLE : View.GONE);
        if (showLift && liftDisruptionText != null) {
            holder.tvLiftDisruptionWarning.setText(liftDisruptionText);
        }

        bindBadge(holder.badgeLine1, badgesAt(item, 0));
        bindBadge(holder.badgeLine2, badgesAt(item, 1));
        bindBadge(holder.badgeLine3, badgesAt(item, 2));
        bindBadge(holder.badgeLine4, badgesAt(item, 3));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRouteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String getCrowdingText(int level, View context) {
        if (level == RouteItem.CROWDING_MEDIUM)
            return context.getContext().getString(R.string.crowding_medium);
        if (level == RouteItem.CROWDING_HIGH)
            return context.getContext().getString(R.string.crowding_high);
        return context.getContext().getString(R.string.crowding_low);
    }

    private static int getCrowdingProgress(int level) {
        if (level == RouteItem.CROWDING_HIGH) return 80;
        if (level == RouteItem.CROWDING_MEDIUM) return 50;
        return 25;
    }

    private static int getCrowdingColor(int level, View context) {
        if (level == RouteItem.CROWDING_HIGH)
            return Color.parseColor("#D32F2F");
        if (level == RouteItem.CROWDING_MEDIUM)
            return ContextCompat.getColor(context.getContext(), R.color.warning);
        return ContextCompat.getColor(context.getContext(), R.color.success);
    }

    private static String badgesAt(RouteItem item, int index) {
        String[] badges = item.getLineBadges();
        if (badges == null || index < 0 || index >= badges.length) return "";
        return badges[index];
    }

    private static void bindBadge(TextView badgeView, String code) {
        if (badgeView == null || code == null || code.trim().isEmpty()) {
            if (badgeView != null) badgeView.setVisibility(View.GONE);
            return;
        }
        String normalized = code.trim().toUpperCase(Locale.UK);
        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(normalized);
        badgeView.setBackgroundColor(getLineColor(normalized));
        badgeView.setTextColor(getLineTextColor(normalized));
    }

    private static int getLineColor(String code) {
        switch (code) {
            case "BUS": return Color.parseColor("#E32017");
            case "BAK": return Color.parseColor("#B36305");
            case "CEN": return Color.parseColor("#E32017");
            case "CIR": return Color.parseColor("#FFD300");
            case "DIS": return Color.parseColor("#00782A");
            case "HAM": return Color.parseColor("#F3A9BB");
            case "JUB": return Color.parseColor("#A0A5A9");
            case "MET": return Color.parseColor("#9B0056");
            case "NOR": return Color.parseColor("#000000");
            case "PIC": return Color.parseColor("#003688");
            case "VIC": return Color.parseColor("#0098D4");
            case "WAT": return Color.parseColor("#95CDBA");
            case "ELZ": return Color.parseColor("#6950A1");
            case "DLR": return Color.parseColor("#00A4A7");
            case "LO": return Color.parseColor("#EF7B10");
            case "TRA": return Color.parseColor("#84B817");
            case "WALK": return RouteLineColors.WALKING;
            case "SOU":
            case "SWR": return RouteLineColors.SOUTH_WESTERN_RAILWAY;
            default:
                if (code.matches("^N?\\d+[A-Z]?$")) return Color.parseColor("#E32017");
                // Same as StepsAdapter national rail (THA, …) — not walking indigo.
                return RouteLineColors.NATIONAL_RAIL;
        }
    }

    private static int getLineTextColor(String code) {
        return ("CIR".equals(code) || "WAT".equals(code)) ? Color.BLACK : Color.WHITE;
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDuration;
        final TextView tvBestBadge;
        final TextView tvDepartureTime;
        final TextView tvCrowdingDetail;
        final TextView tvTransfers;
        final TextView tvRouteSummary;
        final TextView badgeLine1;
        final TextView badgeLine2;
        final TextView badgeLine3;
        final TextView badgeLine4;
        final TextView tvCrowdingLabel;
        final TextView tvCrowdingWarning;
        final TextView tvLiftDisruptionWarning;
        final ProgressBar progressBarCrowding;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvBestBadge = itemView.findViewById(R.id.tvBestBadge);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvCrowdingDetail = itemView.findViewById(R.id.tvCrowdingDetail);
            tvTransfers = itemView.findViewById(R.id.tvTransfers);
            tvRouteSummary = itemView.findViewById(R.id.tvRouteSummary);
            badgeLine1 = itemView.findViewById(R.id.badgeLine1);
            badgeLine2 = itemView.findViewById(R.id.badgeLine2);
            badgeLine3 = itemView.findViewById(R.id.badgeLine3);
            badgeLine4 = itemView.findViewById(R.id.badgeLine4);
            tvCrowdingLabel = itemView.findViewById(R.id.tvCrowdingLabel);
            tvCrowdingWarning = itemView.findViewById(R.id.tvCrowdingWarning);
            tvLiftDisruptionWarning = itemView.findViewById(R.id.tvLiftDisruptionWarning);
            progressBarCrowding = itemView.findViewById(R.id.progressBarCrowding);
        }
    }
}


