package com.example.ajp.ui.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ajp.R;
import java.util.List;




/**
 * RecyclerView adapter for FrequentRoute items.
 */
public class FrequentRouteAdapter extends RecyclerView.Adapter<FrequentRouteAdapter.ViewHolder> {

    private List<FrequentRouteItem> items = List.of();

    public void submitList(List<FrequentRouteItem> list) {
        this.items = list != null ? list : List.of();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_frequent_route, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FrequentRouteItem item = items.get(position);
        holder.tvRank.setText("#" + (position + 1));
        holder.tvRoute.setText(item.getRouteLabel());
        holder.tvDetail.setText(item.getDetail().isEmpty() ? "—" : item.getDetail());
        holder.tvCount.setText(String.valueOf(item.getCount()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvRoute, tvDetail, tvCount;

        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvRoute = itemView.findViewById(R.id.tv_route);
            tvDetail = itemView.findViewById(R.id.tv_detail);
            tvCount = itemView.findViewById(R.id.tv_count);
        }
    }
}

