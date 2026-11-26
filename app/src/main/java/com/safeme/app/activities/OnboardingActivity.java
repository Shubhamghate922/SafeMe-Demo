package com.safeme.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.safeme.app.adapters.OnboardingAdapter;
import com.safeme.app.utils.SecureTokenManager;

import java.util.Arrays;
import java.util.List;

/**
 * Onboarding Screens (Screen B): Introduces Personal Safety, Smart Journey Monitoring, and Emergency Assistance.
 */
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private MaterialButton btnNext;
    private MaterialButton btnSkip;
    private List<OnboardingAdapter.OnboardingPage> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.viewPagerOnboarding);
        layoutDots = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        pages = Arrays.asList(
                new OnboardingAdapter.OnboardingPage(
                        R.drawable.img_onboard_safety,
                        getString(R.string.onboard_title_1),
                        getString(R.string.onboard_desc_1)
                ),
                new OnboardingAdapter.OnboardingPage(
                        R.drawable.img_onboard_journey,
                        getString(R.string.onboard_title_2),
                        getString(R.string.onboard_desc_2)
                ),
                new OnboardingAdapter.OnboardingPage(
                        R.drawable.img_onboard_sos,
                        getString(R.string.onboard_title_3),
                        getString(R.string.onboard_desc_3)
                )
        );

        viewPager.setAdapter(new OnboardingAdapter(pages));
        setupIndicators(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupIndicators(position);
                if (position == pages.size() - 1) {
                    btnNext.setText(R.string.btn_get_started);
                    btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    btnNext.setText(R.string.btn_next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void setupIndicators(int activeIndex) {
        layoutDots.removeAllViews();
        for (int i = 0; i < pages.size(); i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_pill_lavender));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == activeIndex ? 44 : 18, 18);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setColorFilter(ContextCompat.getColor(this,
                    i == activeIndex ? R.color.safeme_primary : R.color.safeme_border));
            layoutDots.addView(dot);
        }
    }

    private void completeOnboarding() {
        SecureTokenManager.getInstance(this).setOnboardingDone(true);
        startActivity(new Intent(this, PermissionsActivity.class));
        finish();
    }
}
