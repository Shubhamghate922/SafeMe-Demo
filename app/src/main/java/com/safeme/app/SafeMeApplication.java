package com.safeme.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.R;
import com.safeme.app.utils.SecureTokenManager;

/**
 * Application entry point initializing notification channels and user theme preference.
 */
public class SafeMeApplication extends Application {

    public static final boolean AUTH_DISABLED_FOR_UI_PREVIEW = false;

    public static final String CHANNEL_JOURNEY = "safeme_journey_channel";
    public static final String CHANNEL_EMERGENCY = "safeme_emergency_channel";
    public static final String CHANNEL_CHECKIN = "safeme_checkin_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedTheme();
        createNotificationChannels();
    }

    private void applySavedTheme() {
        String mode = SecureTokenManager.getInstance(this).getThemeMode();
        if ("dark".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("system".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel journeyChannel = new NotificationChannel(
                    CHANNEL_JOURNEY,
                    getString(R.string.notif_channel_journey_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            journeyChannel.setDescription(getString(R.string.notif_channel_journey_desc));

            NotificationChannel emergencyChannel = new NotificationChannel(
                    CHANNEL_EMERGENCY,
                    getString(R.string.notif_channel_emergency_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            emergencyChannel.setDescription(getString(R.string.notif_channel_emergency_desc));
            emergencyChannel.enableVibration(true);

            NotificationChannel checkInChannel = new NotificationChannel(
                    CHANNEL_CHECKIN,
                    getString(R.string.notif_channel_checkin_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            checkInChannel.setDescription(getString(R.string.notif_channel_checkin_desc));
            checkInChannel.enableVibration(true);

            manager.createNotificationChannel(journeyChannel);
            manager.createNotificationChannel(emergencyChannel);
            manager.createNotificationChannel(checkInChannel);
        }
    }
}
