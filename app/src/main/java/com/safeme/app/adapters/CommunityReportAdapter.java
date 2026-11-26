package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.safeme.app.models.CommunityReport;
import com.safeme.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class CommunityReportAdapter extends RecyclerView.Adapter<CommunityReportAdapter.ViewHolder> {

    private final List<CommunityReport> reports = new ArrayList<>();

    public void submitList(List<CommunityReport> list) {
        reports.clear();
        if (list != null) {
            reports.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityReport report = reports.get(position);
        holder.tvCategory.setText(formatCategory(report.getCategory())
                + (report.getLocationName() != null ? " — " + report.getLocationName() : ""));
        holder.tvVerification.setText(report.getVerificationStatus() != null
                ? report.getVerificationStatus().replace('_', ' ') : "PENDING");
        holder.tvDesc.setText(report.getDescription() != null ? report.getDescription() : "");
        String expiryText = report.getExpiresAt() > 0
                ? " • Expires " + DateTimeUtils.formatTimestamp(report.getExpiresAt())
                : "";
        holder.tvMeta.setText("Reported " + DateTimeUtils.formatTimestamp(report.getReportedAt()) + expiryText);
    }

    private String formatCategory(String cat) {
        if (cat == null) return "Advisory";
        return cat.replace('_', ' ');
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory;
        final TextView tvVerification;
        final TextView tvDesc;
        final TextView tvMeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCommunityCategory);
            tvVerification = itemView.findViewById(R.id.tvCommunityVerification);
            tvDesc = itemView.findViewById(R.id.tvCommunityDesc);
            tvMeta = itemView.findViewById(R.id.tvCommunityMeta);
        }
    }
}
