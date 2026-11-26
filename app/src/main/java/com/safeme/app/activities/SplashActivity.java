package com.safeme.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.safeme.app.SafeMeApplication;
import com.safeme.app.api.ApiResult;
import com.safeme.app.utils.NetworkUtils;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.viewmodel.AuthViewModel;

/**
 * Splash Screen (Screen A): Displays animated SafeMe branding and verifies JWT authentication state.
 */
public class SplashActivity extends AppCompatActivity {

    private SecureTokenManager tokenManager;
    private AuthViewModel authViewModel;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tokenManager = SecureTokenManager.getInstance(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        ImageView logo = findViewById(R.id.ivSplashLogo);
        tvStatus = findViewById(R.id.tvSplashStatus);

        ScaleAnimation scale = new ScaleAnimation(
                0.8f, 1.0f, 0.8f, 1.0f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(700);
        logo.startAnimation(scale);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(700);
        findViewById(R.id.tvSplashAppName).startAnimation(fade);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 1100L);
    }

    private void checkAuthAndNavigate() {
        if (!tokenManager.isOnboardingDone()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        if (SafeMeApplication.AUTH_DISABLED_FOR_UI_PREVIEW) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        if (!tokenManager.hasValidTokenFormat() || !tokenManager.isRememberLogin()) {
            tokenManager.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!tokenManager.isLocalBackendMode() && !NetworkUtils.isInternetAvailable(this)) {
            tvStatus.setText("Offline mode: opening saved session…");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        tvStatus.setText("Verifying authentication token…");
        authViewModel.validateCurrentSession().observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                return;
            }
            if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (result.getStatus() == ApiResult.Status.UNAUTHORIZED) {
                tokenManager.clearSession();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("session_expired", true);
                startActivity(loginIntent);
                finish();
            } else {
                // Backend temporarily unreachable: allow opening MainActivity where offline cache + Retry banner are shown
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}
