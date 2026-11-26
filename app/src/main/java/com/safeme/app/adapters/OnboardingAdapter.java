package com.safeme.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

    public static class OnboardingPage {
        public final int imageRes;
        public final String title;
        public final String description;

        public OnboardingPage(int imageRes, String title, String description) {
            this.imageRes = imageRes;
            this.title = title;
            this.description = description;
        }
    }

    private final List<OnboardingPage> pages;

    public OnboardingAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.ivIllustration.setImageResource(page.imageRes);
        holder.tvTitle.setText(page.title);
        holder.tvDesc.setText(page.description);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIllustration;
        final TextView tvTitle;
        final TextView tvDesc;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIllustration = itemView.findViewById(R.id.ivOnboardingIllustration);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvDesc = itemView.findViewById(R.id.tvOnboardingDesc);
        }
    }
}
