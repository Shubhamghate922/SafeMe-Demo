package com.safeme.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.safeme.app.api.AuthInterceptor;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.repository.AuthRepository;
import com.safeme.app.utils.SecureTokenManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Main Navigation Shell with persistent 5-tab BottomNavigationView, MaterialToolbar,
 * edge-to-edge WindowInsets, and global 401 token-expiration redirection.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protect authenticated shell (unless temporarily disabled for UI preview)
        if (!com.safeme.app.SafeMeApplication.AUTH_DISABLED_FOR_UI_PREVIEW
                && !SecureTokenManager.getInstance(this).hasValidTokenFormat()) {
            redirectToLogin(false);
            return;
        }

        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.mainRoot);
        MaterialToolbar toolbar = findViewById(R.id.mainToolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(0, sys.top, 0, 0);
            bottomNav.setPadding(0, 0, 0, sys.bottom);
            return insets;
        });

        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            Set<Integer> topLevelDestinations = new HashSet<>(Arrays.asList(
                    R.id.homeFragment,
                    R.id.startJourneyFragment,
                    R.id.safetyHubFragment,
                    R.id.activityHistoryFragment,
                    R.id.profileFragment
            ));
            AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(topLevelDestinations).build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        AuthInterceptor interceptor = RetrofitClient.getAuthInterceptor();
        if (interceptor != null && !com.safeme.app.SafeMeApplication.AUTH_DISABLED_FOR_UI_PREVIEW) {
            interceptor.setSessionExpiredListener(() -> runOnUiThread(() -> redirectToLogin(true)));
        }

        if (!com.safeme.app.SafeMeApplication.AUTH_DISABLED_FOR_UI_PREVIEW) {
            new AuthRepository(this).syncFcmToken();
        }
        handleDeepLinkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLinkIntent(intent);
    }

    private void handleDeepLinkIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String notifType = intent.getStringExtra("notification_type");
        String refId = intent.getStringExtra("reference_id");
        String openTab = intent.getStringExtra("open_tab");

        if ("checkin".equals(openTab) || "CHECKIN_REMINDER".equals(notifType)) {
            Bundle args = new Bundle();
            args.putString("journeyId", refId);
            navController.navigate(R.id.safetyCheckInFragment, args);
        } else if ("SOS_ALERT".equals(notifType) && refId != null) {
            Bundle args = new Bundle();
            args.putString("sosId", refId);
            navController.navigate(R.id.sosAcknowledgementFragment, args);
        } else if ("ESCALATION".equals(notifType)) {
            Bundle args = new Bundle();
            args.putString("journeyId", refId);
            navController.navigate(R.id.emergencyEscalationFragment, args);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }

    public void navigateTo(int destinationId) {
        if (navController != null) {
            navController.navigate(destinationId);
        }
    }

    public void navigateTo(int destinationId, Bundle args) {
        if (navController != null) {
            navController.navigate(destinationId, args);
        }
    }

    public void redirectToLogin(boolean sessionExpired) {
        SecureTokenManager.getInstance(this).clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_expired", sessionExpired);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
