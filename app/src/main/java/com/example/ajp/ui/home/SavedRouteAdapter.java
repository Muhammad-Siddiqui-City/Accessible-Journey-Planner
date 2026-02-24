package com.example.ajp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.data.local.SavedRouteEntity;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.List;

/**
 * Adapter for the Saved Routes list on Home.
 * PURPOSE: Display saved routes (summary, relative time); on tap opens RouteDetailsActivity with EXTRA_FROM_OFFLINE.
 */
public class SavedRouteAdapter extends RecyclerView.Adapter<SavedRouteAdapter.ViewHolder> {

    private List<SavedRouteEntity> items = List.of();
    private OnSavedRouteClickListener listener;

    public interface OnSavedRouteClickListener {
        void onSavedRouteClick(SavedRouteEntity entity);
    }

    public void setOnSavedRouteClickListener(OnSavedRouteClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SavedRouteEntity> list) {
        this.items = list != null ? list : List.of();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_route, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedRouteEntity entity = items.get(position);
        String summary = entity.summary != null ? entity.summary : "";
        holder.tvSummary.setText(summary.isEmpty() ? "—" : summary);
        holder.tvTime.setText(TimeFormatUtil.formatRelativeTime(entity.timestamp));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && entity.routeItem != null) {
                listener.onSavedRouteClick(entity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSummary, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvSummary = itemView.findViewById(R.id.tv_saved_route_summary);
            tvTime = itemView.findViewById(R.id.tv_saved_route_time);
        }
    }
}
