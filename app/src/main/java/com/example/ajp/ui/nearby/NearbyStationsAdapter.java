package com.example.ajp.ui.nearby;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import java.util.List;

/**
 * RecyclerView adapter for NearbyStations item rendering.
 * Maps domain/UI models into row views and keeps list-specific formatting in one place.
 * This avoids repeating display logic in fragments and keeps row behavior consistent across updates.
 */

public class NearbyStationsAdapter extends RecyclerView.Adapter<NearbyStationsAdapter.StopViewHolder> {

    public interface OnStopClickListener {
        void onStopClick(String stopId, String stopName, String stopLetter);
    }

    private List<StopItem> items = java.util.Collections.emptyList();
    private final OnStopClickListener clickListener;

    public NearbyStationsAdapter(OnStopClickListener listener) {
        this.clickListener = listener;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void submitList(List<StopItem> list) {
        this.items = list != null ? list : java.util.Collections.emptyList();
        notifyDataSetChanged();
    }

    public void setStops(List<StopItem> stops) {
        submitList(stops);
    }

    @NonNull
    @Override
    // Initializes screen state, wiring, and startup behavior for this lifecycle stage.
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_station, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        StopItem item = items.get(position);
        String displayName = formatStationName(item.getName(), item.getStopLetter());
        holder.tvStationName.setText(displayName);

        if (item.getDistance() > 0) {
            double km = item.getDistance() / 1000.0;
            holder.tvWalkTime.setText(holder.itemView.getContext().getString(R.string.distance_km_away, km));
        } else {
            holder.tvWalkTime.setText(holder.itemView.getContext().getString(R.string.distance_not_available));
        }
        holder.iconStepFree.setVisibility(View.GONE);

        TextView[] badges = { holder.badge1, holder.badge2, holder.badge3, holder.badge4 };
        String[] lineNames = item.getLineCodes();
        for (int i = 0; i < badges.length; i++) {
            if (i < lineNames.length) {
                String lineName = lineNames[i];
                badges[i].setVisibility(View.VISIBLE);
                badges[i].setText(lineName);
                badges[i].setBackgroundColor(getLineColor(lineName));
                badges[i].setTextColor(getLineTextColor(lineName));
            } else {
                badges[i].setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < items.size()) {
                    StopItem stop = items.get(pos);
                    clickListener.onStopClick(stop.getStopId(), stop.getName(), stop.getStopLetter());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private static String formatStationName(String commonName, String stopLetter) {
        if (commonName == null) commonName = "";
        if (stopLetter == null) stopLetter = "";
        String letter = stopLetter.replace("->", "").trim();
        if (letter.isEmpty()) return commonName;
        return commonName + " (Stop " + letter + ")";
    }

    private static int getLineColor(String lineName) {
        if (lineName == null) return Color.parseColor("#0019A8");
        String n = lineName.trim().toLowerCase();

        // Bus route codes (e.g. 35, P4, N5) should always use TfL bus red.
        if (n.matches("^[a-z]?\\d+[a-z]?$")) return Color.parseColor("#E32017");
        if (n.equals("bus")) return Color.parseColor("#E32017");

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

    private static int getLineTextColor(String lineName) {
        if (lineName == null) return Color.WHITE;
        if (lineName.trim().toLowerCase().contains("circle")) return Color.BLACK;
        return Color.WHITE;
    }

    static class StopViewHolder extends RecyclerView.ViewHolder {
        final TextView tvStationName;
        final TextView tvWalkTime;
        final View iconStepFree;
        final TextView badge1, badge2, badge3, badge4;

        StopViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStationName = itemView.findViewById(R.id.tvStationName);
            tvWalkTime = itemView.findViewById(R.id.tvWalkTime);
            iconStepFree = itemView.findViewById(R.id.iconStepFree);
            badge1 = itemView.findViewById(R.id.badge1);
            badge2 = itemView.findViewById(R.id.badge2);
            badge3 = itemView.findViewById(R.id.badge3);
            badge4 = itemView.findViewById(R.id.badge4);
        }
    }
}


