package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.safeme.app.models.ResponseAcknowledgement;
import com.safeme.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class AcknowledgementAdapter extends RecyclerView.Adapter<AcknowledgementAdapter.ViewHolder> {

    private final List<ResponseAcknowledgement> items = new ArrayList<>();

    public void submitList(List<ResponseAcknowledgement> list) {
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
                .inflate(R.layout.item_acknowledgement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResponseAcknowledgement ack = items.get(position);
        holder.tvName.setText(ack.getResponderName() != null ? ack.getResponderName() : "Trusted Contact");
        holder.tvStatus.setText(formatStatus(ack.getResponseStatus()));
        if (ack.getNote() != null && !ack.getNote().trim().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(ack.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }
        holder.tvTime.setText(DateTimeUtils.formatTimestamp(ack.getRespondedAt()));
    }

    private String formatStatus(String status) {
        if ("RESPONDING".equals(status)) return "Responding";
        if ("CONTACTED_ASSISTANCE".equals(status)) return "Contacted Help";
        if ("CANNOT_RESPOND".equals(status)) return "Cannot Respond";
        return status != null ? status : "Notified";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvStatus;
        final TextView tvNote;
        final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAckResponderName);
            tvStatus = itemView.findViewById(R.id.tvAckStatusBadge);
            tvNote = itemView.findViewById(R.id.tvAckNote);
            tvTime = itemView.findViewById(R.id.tvAckTimestamp);
        }
    }
}
