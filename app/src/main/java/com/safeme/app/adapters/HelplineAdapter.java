package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.safeme.app.models.HelplineItem;

import java.util.List;

public class HelplineAdapter extends RecyclerView.Adapter<HelplineAdapter.ViewHolder> {

    public interface OnDialClickListener {
        void onDialRequest(HelplineItem item);
    }

    private final List<HelplineItem> items;
    private final OnDialClickListener listener;

    public HelplineAdapter(List<HelplineItem> items, OnDialClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_helpline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelplineItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDesc.setText(item.getDescription());
        holder.tvAvail.setText(item.getAvailability());
        holder.btnDial.setText("Call " + item.getNumber());
        holder.btnDial.setOnClickListener(v -> {
            if (listener != null) listener.onDialRequest(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvDesc;
        final TextView tvAvail;
        final MaterialButton btnDial;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHelplineTitle);
            tvDesc = itemView.findViewById(R.id.tvHelplineDesc);
            tvAvail = itemView.findViewById(R.id.tvHelplineAvail);
            btnDial = itemView.findViewById(R.id.btnDialHelpline);
        }
    }
}
