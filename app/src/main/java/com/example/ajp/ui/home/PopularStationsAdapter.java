package com.example.ajp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.ui.nearby.StopItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the "Popular Stations" list.
 * PURPOSE: Display a fixed list of stations; on tap open StationTrainsFragment.
 */
public class PopularStationsAdapter extends RecyclerView.Adapter<PopularStationsAdapter.ViewHolder> {

    public interface OnPopularStationClickListener {
        void onPopularStationClick(StopItem stop);
    }

    private final List<StopItem> items = new ArrayList<>();
    private OnPopularStationClickListener listener;

    public void setOnPopularStationClickListener(OnPopularStationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<StopItem> stops) {
        items.clear();
        if (stops != null) items.addAll(stops);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular_station, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StopItem stop = items.get(position);
        holder.tvName.setText(stop != null ? stop.getName() : "");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPopularStationClick(stop);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_popular_station_name);
        }
    }
}

