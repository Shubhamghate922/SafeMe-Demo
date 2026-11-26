package com.safeme.app.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.R;
import com.safeme.app.SafeMeApplication;
import com.safeme.app.activities.MainActivity;

/**
 * Manages scheduled safety check-in countdowns, prevents duplicate check-ins or duplicate
 * escalation triggers, and coordinates the configurable grace-period escalation workflow.
 */
public class CheckInTimerManager {

    private static final int CHECKIN_NOTIFICATION_ID = 4205;
    private static volatile CheckInTimerManager instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Long> secondsUntilCheckIn = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> checkInDue = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> escalationStage = new MutableLiveData<>(0);

    private String trackedJourneyId;
    private long nextCheckInDueMillis = 0L;
    private boolean checkInSubmittedForCurrentWindow = false;
    private boolean escalationTriggeredForCurrentWindow = false;
    private int currentEscalationStage = 0;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (trackedJourneyId == null || nextCheckInDueMillis <= 0L) {
                return;
            }
            long remainingMs = nextCheckInDueMillis - System.currentTimeMillis();
            if (remainingMs > 0) {
                secondsUntilCheckIn.postValue(remainingMs / 1000L);
                checkInDue.postValue(false);
            } else {
                secondsUntilCheckIn.postValue(0L);
                if (!checkInSubmittedForCurrentWindow) {
                    checkInDue.postValue(true);
                }
            }
            handler.postDelayed(this, 1000L);
        }
    };

    private CheckInTimerManager() {}

    public static CheckInTimerManager getInstance() {
        if (instance == null) {
            synchronized (CheckInTimerManager.class) {
                if (instance == null) {
                    instance = new CheckInTimerManager();
                }
            }
        }
        return instance;
    }

    public void scheduleNextCheckIn(Context context, String journeyId, long dueAtMillis) {
        this.trackedJourneyId = journeyId;
        this.nextCheckInDueMillis = dueAtMillis;
        this.checkInSubmittedForCurrentWindow = false;
        this.escalationTriggeredForCurrentWindow = false;
        this.currentEscalationStage = 0;
        this.escalationStage.postValue(0);

        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);

        if (dueAtMillis <= System.currentTimeMillis() && context != null) {
            postCheckInNotification(context);
        }
    }

    public synchronized boolean tryLockCheckInSubmission() {
        if (checkInSubmittedForCurrentWindow) {
            return false; // Prevent duplicate check-in
        }
        checkInSubmittedForCurrentWindow = true;
        checkInDue.postValue(false);
        return true;
    }

    public synchronized boolean tryLockEscalationStage(int stage) {
        if (currentEscalationStage >= stage) {
            return false; // Prevent duplicate escalation for same stage
        }
        currentEscalationStage = stage;
        escalationTriggeredForCurrentWindow = true;
        escalationStage.postValue(stage);
        return true;
    }

    public boolean isEscalationTriggered() {
        return escalationTriggeredForCurrentWindow;
    }

    public synchronized void stopTimer() {
        trackedJourneyId = null;
        nextCheckInDueMillis = 0L;
        checkInSubmittedForCurrentWindow = false;
        escalationTriggeredForCurrentWindow = false;
        currentEscalationStage = 0;
        secondsUntilCheckIn.postValue(0L);
        checkInDue.postValue(false);
        escalationStage.postValue(0);
        handler.removeCallbacks(tickRunnable);
    }

    public LiveData<Long> getSecondsUntilCheckIn() {
        return secondsUntilCheckIn;
    }

    public LiveData<Boolean> isCheckInDue() {
        return checkInDue;
    }

    public LiveData<Integer> getEscalationStage() {
        return escalationStage;
    }

    public void postCheckInNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("open_tab", "checkin");
        PendingIntent pi = PendingIntent.getActivity(
                context, 102, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SafeMeApplication.CHANNEL_CHECKIN)
                .setSmallIcon(R.drawable.ic_safety)
                .setContentTitle("SafeMe Check-in Due: Are you safe?")
                .setContentText("Please confirm your safety check-in before escalation begins.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        manager.notify(CHECKIN_NOTIFICATION_ID, builder.build());
    }
}
