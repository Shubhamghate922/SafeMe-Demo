package com.safeme.app.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.safeme.app.SafeMeApplication;
import com.safeme.app.activities.MainActivity;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.AuthDtos;
import com.safeme.app.utils.SecureTokenManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles Firebase Cloud Messaging token registration and incoming push notifications
 * for SOS alerts, check-in reminders, escalations, and contact acknowledgements.
 */
public class SafeMeMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SafeMeMessagingService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SecureTokenManager tokenManager = SecureTokenManager.getInstance(this);
        tokenManager.saveFcmToken(token);

        if (tokenManager.hasValidTokenFormat()) {
            RetrofitClient.getApiService(this)
                    .registerDeviceToken(new AuthDtos.DeviceTokenRequest(token))
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                               @NonNull Response<ApiResponse<Void>> response) {
                            Log.d(TAG, "FCM device token synced with backend: " + response.code());
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                            Log.w(TAG, "Failed to sync FCM token: " + t.getMessage());
                        }
                    });
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        String type = data.getOrDefault("type", "SYSTEM");
        String referenceId = data.get("referenceId");

        String title = remoteMessage.getNotification() != null && remoteMessage.getNotification().getTitle() != null
                ? remoteMessage.getNotification().getTitle()
                : data.getOrDefault("title", "SafeMe Safety Alert");

        String body = remoteMessage.getNotification() != null && remoteMessage.getNotification().getBody() != null
                ? remoteMessage.getNotification().getBody()
                : data.getOrDefault("body", "Open SafeMe to view details.");

        // Enforce notification privacy preference on lock screen if configured
        boolean hideSensitive = SecureTokenManager.getInstance(this).isNotificationPrivacyHidden();
        String publicText = hideSensitive ? "SafeMe safety notification received. Unlock to view." : body;

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.putExtra("notification_type", type);
        if (referenceId != null) {
            openIntent.putExtra("reference_id", referenceId);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = ("SOS_ALERT".equals(type) || "ESCALATION".equals(type))
                ? SafeMeApplication.CHANNEL_EMERGENCY
                : SafeMeApplication.CHANNEL_CHECKIN;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_safety)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPublicVersion(
                        new NotificationCompat.Builder(this, channelId)
                                .setSmallIcon(R.drawable.ic_safety)
                                .setContentTitle("SafeMe Notification")
                                .setContentText(publicText)
                                .build()
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        }
    }
}
