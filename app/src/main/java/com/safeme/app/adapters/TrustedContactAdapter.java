package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.safeme.app.models.TrustedContact;

import java.util.ArrayList;
import java.util.List;

public class TrustedContactAdapter extends RecyclerView.Adapter<TrustedContactAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(TrustedContact contact);
    }

    private final List<TrustedContact> contacts = new ArrayList<>();
    private final OnContactClickListener listener;

    public TrustedContactAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TrustedContact> list) {
        contacts.clear();
        if (list != null) {
            contacts.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trusted_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrustedContact c = contacts.get(position);
        holder.tvName.setText(c.getName() != null ? c.getName() : "Contact");
        String rel = c.getRelationship() != null && !c.getRelationship().isEmpty()
                ? c.getRelationship() : "Trusted Contact";
        holder.tvPhoneRelation.setText((c.getPhoneNumber() != null ? c.getPhoneNumber() : "") + " • " + rel);

        String typeLabel = "REGISTERED_USER".equals(c.getContactType()) ? "SafeMe User" : "External SMS";
        String verif = c.getVerificationStatus() != null ? c.getVerificationStatus() : "PENDING";
        holder.tvBadges.setText(typeLabel + " • " + verif);
        holder.tvPriority.setText("P" + Math.max(1, c.getPriority()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(c);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvPhoneRelation;
        final TextView tvBadges;
        final TextView tvPriority;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvPhoneRelation = itemView.findViewById(R.id.tvContactPhoneRelation);
            tvBadges = itemView.findViewById(R.id.tvContactBadges);
            tvPriority = itemView.findViewById(R.id.tvContactPriority);
        }
    }
}
