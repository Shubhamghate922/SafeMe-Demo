package com.safeme.app.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.material.snackbar.Snackbar;
import com.safeme.app.adapters.PermissionAdapter;
import com.safeme.app.models.PermissionItem;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.utils.SecureTokenManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Permission Introduction Screen (Screen C): Explains each of the 7 permissions clearly
 * and requests runtime permissions only when explicitly initiated by the user.
 */
public class PermissionsActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_SETTINGS = "extra_from_settings";

    private RecyclerView rvPermissions;
    private View rootView;
    private boolean fromSettings = false;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                refreshPermissionList();
                boolean anyDenied = false;
                for (Boolean granted : result.values()) {
                    if (!Boolean.TRUE.equals(granted)) {
                        anyDenied = true;
                        break;
                    }
                }
                if (anyDenied) {
                    Snackbar.make(rootView,
                            "Permission declined. You can still use SafeMe or enable it anytime in App Settings.",
                            Snackbar.LENGTH_LONG)
                            .setAction("Settings", v -> PermissionHelper.openAppSettings(this))
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        fromSettings = getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS, false);
        rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        rvPermissions = findViewById(R.id.rvPermissions);
        rvPermissions.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnOpenAppSettings).setOnClickListener(v -> PermissionHelper.openAppSettings(this));
        findViewById(R.id.btnContinuePermissions).setOnClickListener(v -> {
            if (fromSettings) {
                finish();
            } else if (com.safeme.app.SafeMeApplication.AUTH_DISABLED_FOR_UI_PREVIEW
                    || SecureTokenManager.getInstance(this).hasValidTokenFormat()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });

        refreshPermissionList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionList();
    }

    private void refreshPermissionList() {
        List<PermissionItem> items = new ArrayList<>();
        items.add(new PermissionItem(
                PermissionItem.TYPE_LOCATION,
                "1. Location Access (Fine & Coarse GPS)",
                "Used to capture your GPS coordinates when starting a monitored journey, checking in, or dispatching an SOS alert.",
                PermissionHelper.hasLocationPermission(this),
                false
        ));
        items.add(new PermissionItem(
                PermissionItem.TYPE_NOTIFICATIONS,
                "2. Notifications",
                "Delivers scheduled safety check-in reminders, SOS alerts from trusted contacts, and active journey status updates.",
                PermissionHelper.hasNotificationPermission(this),
                false
        ));
        items.add(new PermissionItem(
                PermissionItem.TYPE_CAMERA,
                "3. Camera (Optional)",
                "Only used when you choose to attach a photograph to an Incident Report.",
                PermissionHelper.hasCameraPermission(this),
                false
        ));
        items.add(new PermissionItem(
                PermissionItem.TYPE_MICROPHONE,
                "4. Microphone (Optional)",
                "Used strictly when you enable Voice Emergency keyword detection or attach audio to an Incident Report.",
                PermissionHelper.hasMicrophonePermission(this),
                false
        ));
        items.add(new PermissionItem(
                PermissionItem.TYPE_FOREGROUND_LOCATION,
                "5. Foreground Location Service",
                "Allows SafeMe to display an ongoing notification and stream live GPS updates while your monitored journey is active.",
                PermissionHelper.hasForegroundLocationCapability(this),
                true
        ));
        items.add(new PermissionItem(
                PermissionItem.TYPE_BACKGROUND_LOCATION,
                "6. Background Location (Optional)",
                "Only needed if you want journey location updates to continue while the screen is locked or app is minimized.",
                PermissionHelper.hasBackgroundLocationPermission(this),
                false
        ));
        items.add(new PermissionItem(
                PermissionItem.TYPE_MEDIA_PICKER,
                "7. Photo & Audio Selection",
                "SafeMe uses the privacy-friendly Android Photo Picker & Document Picker—no broad storage permission is required.",
                true,
                true
        ));

        rvPermissions.setAdapter(new PermissionAdapter(items, this::handlePermissionRequest));
    }

    private void handlePermissionRequest(PermissionItem item) {
        switch (item.getType()) {
            case PermissionItem.TYPE_LOCATION:
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
                break;

            case PermissionItem.TYPE_NOTIFICATIONS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                }
                break;

            case PermissionItem.TYPE_CAMERA:
                permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
                break;

            case PermissionItem.TYPE_MICROPHONE:
                permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                break;

            case PermissionItem.TYPE_BACKGROUND_LOCATION:
                if (!PermissionHelper.hasLocationPermission(this)) {
                    Snackbar.make(rootView, "Please grant standard Location Access first.", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    new AlertDialog.Builder(this)
                            .setTitle("Background Location Disclosure")
                            .setMessage("SafeMe accesses background location ONLY during an active user-initiated journey or active SOS emergency, and stops tracking immediately when the session ends.")
                            .setPositiveButton("Allow", (d, w) -> permissionLauncher.launch(
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}))
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                break;
        }
    }
}
