package com.example.ajp.ui.arrivals;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * RecyclerView adapter for Arrivals item rendering.
 * Maps domain/UI models into row views and keeps list-specific formatting in one place.
 * This avoids repeating display logic in fragments and keeps row behavior consistent across updates.
 */

public class ArrivalsAdapter extends RecyclerView.Adapter<ArrivalsAdapter.ArrivalViewHolder> {

    private List<Arrival> items = Collections.emptyList();

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void submitList(List<Arrival> list) {
        this.items = list != null ? new ArrayList<>(list) : new ArrayList<>();
        Collections.sort(this.items, Comparator.comparingInt(Arrival::getTimeToStationSeconds));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    // Initializes screen state, wiring, and startup behavior for this lifecycle stage.
    public ArrivalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_arrival, parent, false);
        return new ArrivalViewHolder(view);
    }

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void onBindViewHolder(@NonNull ArrivalViewHolder holder, int position) {
        Arrival a = items.get(position);
        holder.tvLineBadge.setText(a.getLineName());
        holder.tvLineBadge.setBackgroundColor(getLineColor(a.getLineName()));
        holder.tvLineBadge.setTextColor(getLineTextColor(a.getLineName()));
        holder.tvDestination.setText(a.getDestinationName());
        boolean isBus = "bus".equalsIgnoreCase(a.getModeName());
        String platform = a.getPlatformName();
        String platformDisplay = formatPlatformForDisplay(platform);
        boolean singleCharPlatform = platform != null && platform.trim().length() == 1;
        if (isBus || singleCharPlatform || platformDisplay.isEmpty()) {
            holder.tvPlatform.setVisibility(View.GONE);
            holder.tvPlatformBadge.setVisibility(View.GONE);
        } else {
            holder.tvPlatform.setVisibility(View.GONE);
            holder.tvPlatformBadge.setVisibility(View.VISIBLE);
            holder.tvPlatformBadge.setText(platformDisplay);
        }
        int sec = a.getTimeToStationSeconds();
        if (sec < 60) {
            holder.tvTime.setText(String.valueOf(sec));
        } else {
            holder.tvTime.setText(TimeFormatUtil.formatMinutesToHourMin(sec / 60));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int getLineColor(String lineName) {
        if (lineName == null) return Color.parseColor("#0019A8");
        String n = lineName.trim().toLowerCase();
        if (n.contains("south western")) return Color.parseColor("#000000");
        if (n.contains("district")) return Color.parseColor("#00782A");
        if (n.contains("bakerloo")) return Color.parseColor("#B36305");
        if (n.contains("central")) return Color.parseColor("#E32017");
        if (n.contains("circle")) return Color.parseColor("#FFD300");
        if (n.contains("hammersmith")) return Color.parseColor("#F3A9BB");
        if (n.contains("jubilee")) return Color.parseColor("#A0A5A9");
        if (n.contains("metropolitan")) return Color.parseColor("#9B0056");
        if (n.contains("northern")) return Color.parseColor("#000000");
        if (n.contains("piccadilly")) return Color.parseColor("#003688");
        if (n.contains("victoria")) return Color.parseColor("#0098D4");
        if (n.contains("waterloo") && n.contains("city")) return Color.parseColor("#95CDBA");
        if (n.contains("elizabeth")) return Color.parseColor("#6950a1");
        if (n.contains("overground")) return Color.parseColor("#EF7B10");
        if (n.isEmpty() || n.matches("^n?\\d+$")) return Color.parseColor("#E32017");
        return Color.parseColor("#0019A8");
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private static String formatPlatformForDisplay(String platform) {
        if (platform == null || platform.trim().isEmpty()) return "";
        String p = platform.trim();
        if (p.toLowerCase().startsWith("platform")) return p;
        if (p.matches("\\d+")) return "Platform " + p;
        return p;
    }

    private static int getLineTextColor(String lineName) {
        if (lineName == null) return Color.WHITE;
        if (lineName.trim().toLowerCase().contains("circle")) return Color.BLACK;
        return Color.WHITE;
    }

    static class ArrivalViewHolder extends RecyclerView.ViewHolder {
        final TextView tvLineBadge;
        final TextView tvPlatformBadge;
        final TextView tvDestination;
        final TextView tvPlatform;
        final TextView tvTime;

        ArrivalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLineBadge = itemView.findViewById(R.id.tvLineBadge);
            tvPlatformBadge = itemView.findViewById(R.id.tvPlatformBadge);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}


