package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.safeme.app.models.ActivityRecord;
import com.safeme.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class ActivityHistoryAdapter extends RecyclerView.Adapter<ActivityHistoryAdapter.ViewHolder> {

    private final List<ActivityRecord> records = new ArrayList<>();

    public void submitList(List<ActivityRecord> list) {
        records.clear();
        if (list != null) {
            records.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityRecord record = records.get(position);
        holder.tvTitle.setText(record.getTitle());
        holder.tvStatus.setText(record.getStatus());
        holder.tvSubtitle.setText(record.getSubtitle());
        holder.tvTimestamp.setText(DateTimeUtils.formatTimestamp(record.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvStatus;
        final TextView tvSubtitle;
        final TextView tvTimestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvStatus = itemView.findViewById(R.id.tvActivityStatus);
            tvSubtitle = itemView.findViewById(R.id.tvActivitySubtitle);
            tvTimestamp = itemView.findViewById(R.id.tvActivityTimestamp);
        }
    }
}
