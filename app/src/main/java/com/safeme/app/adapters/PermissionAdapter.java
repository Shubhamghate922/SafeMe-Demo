package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.safeme.app.models.PermissionItem;

import java.util.List;

public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.PermViewHolder> {

    public interface OnPermissionActionListener {
        void onRequestPermission(PermissionItem item);
    }

    private final List<PermissionItem> items;
    private final OnPermissionActionListener listener;

    public PermissionAdapter(List<PermissionItem> items, OnPermissionActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PermViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permission, parent, false);
        return new PermViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PermViewHolder holder, int position) {
        PermissionItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvReason.setText(item.getReason());

        if (item.isSystemHandledWithoutPrompt()) {
            holder.tvStatus.setText("System Managed");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.safeme_deep_purple));
            holder.btnGrant.setVisibility(View.GONE);
        } else if (item.isGranted()) {
            holder.tvStatus.setText("Granted");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.safeme_success_green));
            holder.btnGrant.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setText("Not Granted");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.safeme_emergency_red));
            holder.btnGrant.setVisibility(View.VISIBLE);
            holder.btnGrant.setOnClickListener(v -> listener.onRequestPermission(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PermViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvReason;
        final TextView tvStatus;
        final MaterialButton btnGrant;

        PermViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPermissionTitle);
            tvReason = itemView.findViewById(R.id.tvPermissionReason);
            tvStatus = itemView.findViewById(R.id.tvPermissionStatus);
            btnGrant = itemView.findViewById(R.id.btnGrantPermission);
        }
    }
}
