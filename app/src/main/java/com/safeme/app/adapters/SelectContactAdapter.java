package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.safeme.app.models.TrustedContact;

import java.util.ArrayList;
import java.util.List;

public class SelectContactAdapter extends RecyclerView.Adapter<SelectContactAdapter.ViewHolder> {

    private final List<TrustedContact> contacts = new ArrayList<>();
    private Runnable onSelectionChanged;

    public void setOnSelectionChangedListener(Runnable listener) {
        this.onSelectionChanged = listener;
    }

    public void submitList(List<TrustedContact> list) {
        contacts.clear();
        if (list != null) {
            for (TrustedContact c : list) {
                if (c.getPriority() == 1) {
                    c.setSelectedForJourney(true);
                }
                contacts.add(c);
            }
        }
        notifyDataSetChanged();
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    public List<String> getSelectedContactIds() {
        List<String> ids = new ArrayList<>();
        for (TrustedContact c : contacts) {
            if (c.isSelectedForJourney() && c.getId() != null) {
                ids.add(c.getId());
            }
        }
        return ids;
    }

    public int getSelectedCount() {
        int count = 0;
        for (TrustedContact c : contacts) {
            if (c.isSelectedForJourney()) count++;
        }
        return count;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrustedContact c = contacts.get(position);
        holder.tvName.setText(c.getName() != null ? c.getName() : "Contact");
        holder.tvSubtitle.setText("P" + Math.max(1, c.getPriority()) + " • "
                + (c.getPhoneNumber() != null ? c.getPhoneNumber() : ""));

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(c.isSelectedForJourney());
        holder.checkBox.setOnCheckedChangeListener((btn, isChecked) -> {
            c.setSelectedForJourney(isChecked);
            if (onSelectionChanged != null) onSelectionChanged.run();
        });

        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCheckBox checkBox;
        final TextView tvName;
        final TextView tvSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbSelectContact);
            tvName = itemView.findViewById(R.id.tvSelectContactName);
            tvSubtitle = itemView.findViewById(R.id.tvSelectContactSubtitle);
        }
    }
}
