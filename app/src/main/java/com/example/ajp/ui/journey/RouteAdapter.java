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
import com.example.ajp.utils.TimeFormatUtil;
import java.util.List;

/**
 * Adapter for suggested route cards. Add in Commit 11.
 * PURPOSE: Bind RouteItem to item_route; duration via TimeFormatUtil; BEST badge for first when showBestBadge; crowding progress bar (Green/Orange/Red).
 * WHY: RouteOptimizer sorts so first is "best"; getCrowdingProgress/getCrowdingColor for bar; line badges from item.getLineBadges().
 * ISSUES: None.
 */
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

        // BEST badge visible only for first item when showBestBadge is true
        holder.tvBestBadge.setVisibility(showBestBadge && position == 0 ? View.VISIBLE : View.GONE);

        // Crowding progress bar: dynamic color (Green Low, Orange Medium, Red High/Busy)
        int progressPercent = getCrowdingProgress(item.getCrowdingLevel());
        int progressColor = getCrowdingColor(item.getCrowdingLevel(), holder.itemView);
        holder.progressBarCrowding.setProgress(progressPercent);
        holder.progressBarCrowding.setProgressTintList(ColorStateList.valueOf(progressColor));
        holder.tvCrowdingLabel.setVisibility(View.VISIBLE);
        holder.tvCrowdingWarning.setVisibility(item.getCrowdingLevel() == RouteItem.CROWDING_HIGH ? View.VISIBLE : View.GONE);
        
        // Show lift disruption warning if route has lift issues
        boolean hasLiftDisruption = item.hasLiftDisruption();
        android.util.Log.d("UI_Check", "RouteAdapter - Route " + item.getRouteId() + " hasLiftDisruption=" + hasLiftDisruption + ", description=" + item.getLiftDisruptionDescription());
        String liftDisruptionText = hasLiftDisruption ? 
            (item.getLiftDisruptionDescription() != null ? item.getLiftDisruptionDescription() : 
             holder.itemView.getContext().getString(R.string.lift_disruption_warning)) : null;
        holder.tvLiftDisruptionWarning.setVisibility(hasLiftDisruption ? View.VISIBLE : View.GONE);
        if (hasLiftDisruption && liftDisruptionText != null) {
            holder.tvLiftDisruptionWarning.setText(liftDisruptionText);
            android.util.Log.d("UI_Check", "RouteAdapter - Setting warning text: " + liftDisruptionText);
        } else {
            android.util.Log.d("UI_Check", "RouteAdapter - No warning to display");
        }

        // Line badges: first and second badge
        String[] badges = item.getLineBadges();
        if (badges.length > 0) {
            holder.badgeLine1.setVisibility(View.VISIBLE);
            holder.badgeLine1.setText(badges[0]);
            holder.badgeLine1.setBackgroundColor(getLineColor(badges[0], holder.itemView));
        } else {
            holder.badgeLine1.setVisibility(View.GONE);
        }
        if (badges.length > 1) {
            holder.badgeLine2.setVisibility(View.VISIBLE);
            holder.badgeLine2.setText(badges[1]);
            holder.badgeLine2.setBackgroundColor(getLineColor(badges[1], holder.itemView));
        } else {
            holder.badgeLine2.setVisibility(View.GONE);
        }

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
            return Color.parseColor("#D32F2F"); // Red for High/Busy crowding
        if (level == RouteItem.CROWDING_MEDIUM)
            return ContextCompat.getColor(context.getContext(), R.color.warning);
        return ContextCompat.getColor(context.getContext(), R.color.success);
    }

    private static int getLineColor(String lineCode, View context) {
        switch (lineCode != null ? lineCode.toUpperCase() : "") {
            case "VIC": return ContextCompat.getColor(context.getContext(), R.color.line_victoria);
            case "PIC": return ContextCompat.getColor(context.getContext(), R.color.line_piccadilly);
            case "JUB": return ContextCompat.getColor(context.getContext(), R.color.line_jubilee);
            case "NOR": return ContextCompat.getColor(context.getContext(), R.color.line_northern);
            case "CEN": return ContextCompat.getColor(context.getContext(), R.color.line_central);
            default: return ContextCompat.getColor(context.getContext(), R.color.line_default);
        }
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
            tvCrowdingLabel = itemView.findViewById(R.id.tvCrowdingLabel);
            tvCrowdingWarning = itemView.findViewById(R.id.tvCrowdingWarning);
            tvLiftDisruptionWarning = itemView.findViewById(R.id.tvLiftDisruptionWarning);
            progressBarCrowding = itemView.findViewById(R.id.progressBarCrowding);
        }
    }
}
