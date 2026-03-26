package com.example.ajp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import com.example.ajp.data.local.SavedRouteEntity;
import com.example.ajp.utils.TimeFormatUtil;
import java.util.List;





/**
 * RecyclerView adapter for SavedRoute items.
 */
public class SavedRouteAdapter extends RecyclerView.Adapter<SavedRouteAdapter.ViewHolder> {

    private List<SavedRouteEntity> items = List.of();
    private OnSavedRouteClickListener listener;
    private OnSavedRouteDeleteListener deleteListener;

    public interface OnSavedRouteClickListener {
        void onSavedRouteClick(SavedRouteEntity entity);
    }

    public interface OnSavedRouteDeleteListener {
        void onSavedRouteDelete(SavedRouteEntity entity);
    }

    public void setOnSavedRouteClickListener(OnSavedRouteClickListener listener) {
        this.listener = listener;
    }

    public void setOnSavedRouteDeleteListener(OnSavedRouteDeleteListener listener) {
        this.deleteListener = listener;
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

        View.OnClickListener openDetails = v -> {
            if (listener != null && entity.routeItem != null) {
                listener.onSavedRouteClick(entity);
            }
        };
        holder.textBlock.setOnClickListener(openDetails);
        holder.chevron.setOnClickListener(openDetails);

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onSavedRouteDelete(entity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View textBlock;
        final TextView tvSummary, tvTime;
        final ImageButton btnDelete;
        final ImageView chevron;

        ViewHolder(View itemView) {
            super(itemView);
            textBlock = itemView.findViewById(R.id.saved_route_text_block);
            tvSummary = itemView.findViewById(R.id.tv_saved_route_summary);
            tvTime = itemView.findViewById(R.id.tv_saved_route_time);
            btnDelete = itemView.findViewById(R.id.btn_delete_saved_route);
            chevron = itemView.findViewById(R.id.iv_saved_route_chevron);
        }
    }
}

