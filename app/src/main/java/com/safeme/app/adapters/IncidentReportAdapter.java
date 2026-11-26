package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.safeme.app.models.IncidentReport;

import java.util.ArrayList;
import java.util.List;

public class IncidentReportAdapter extends RecyclerView.Adapter<IncidentReportAdapter.ViewHolder> {

    private final List<IncidentReport> items = new ArrayList<>();

    public void submitList(List<IncidentReport> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incident_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IncidentReport report = items.get(position);
        holder.tvCategory.setText(report.getCategory() != null ? report.getCategory() : "Incident");
        holder.tvStatus.setText(report.getStatus() != null ? report.getStatus() : "SUBMITTED");
        holder.tvDesc.setText(report.getDescription() != null ? report.getDescription() : "");
        holder.tvLocationDate.setText((report.getLocationName() != null ? report.getLocationName() : "")
                + " • " + (report.getIncidentDate() != null ? report.getIncidentDate() : ""));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory;
        final TextView tvStatus;
        final TextView tvDesc;
        final TextView tvLocationDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvIncidentItemCategory);
            tvStatus = itemView.findViewById(R.id.tvIncidentItemStatus);
            tvDesc = itemView.findViewById(R.id.tvIncidentItemDesc);
            tvLocationDate = itemView.findViewById(R.id.tvIncidentItemLocationDate);
        }
    }
}
