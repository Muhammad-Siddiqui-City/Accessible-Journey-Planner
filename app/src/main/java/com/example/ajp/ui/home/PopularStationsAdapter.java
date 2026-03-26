package com.example.ajp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import java.util.ArrayList;
import java.util.List;




/**
 * RecyclerView adapter for PopularStations items.
 */
public class PopularStationsAdapter extends RecyclerView.Adapter<PopularStationsAdapter.ViewHolder> {

    public interface OnPopularStationClickListener {

        void onPopularStationClick(String displayLabel);
    }

    private final List<String> items = new ArrayList<>();
    private OnPopularStationClickListener listener;

    public void setOnPopularStationClickListener(OnPopularStationClickListener listener) {
        this.listener = listener;
    }

    public void submitLabels(List<String> labels) {
        items.clear();
        if (labels != null) items.addAll(labels);
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
        String label = items.get(position);
        holder.tvName.setText(label != null ? label : "");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && label != null) listener.onPopularStationClick(label);
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

