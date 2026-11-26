package com.safeme.app.activities;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.R;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.utils.DateTimeUtils;

/**
 * Fake Call Screen (Screen T): Clearly labeled application simulation of an incoming call.
 */
public class FakeCallActivity extends AppCompatActivity {

    private LinearLayout layoutSetup;
    private LinearLayout layoutRinging;
    private TextInputEditText etCallerName;
    private TextView tvCallerName;
    private TextView tvCallStatus;
    private TextView tvCallTimer;
    private View fabAccept;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Ringtone ringtone;
    private Vibrator vibrator;
    private int callSeconds = 0;
    private boolean callActive = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!callActive) return;
            callSeconds++;
            tvCallTimer.setText(DateTimeUtils.formatCountdownSeconds(callSeconds));
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        layoutSetup = findViewById(R.id.layoutFakeCallSetup);
        layoutRinging = findViewById(R.id.layoutFakeCallRinging);
        etCallerName = findViewById(R.id.etFakeCallerName);
        tvCallerName = findViewById(R.id.tvSimCallerName);
        tvCallStatus = findViewById(R.id.tvSimCallStatus);
        tvCallTimer = findViewById(R.id.tvSimCallTimer);
        fabAccept = findViewById(R.id.fabAcceptFakeCall);

        findViewById(R.id.btnCloseFakeCallSetup).setOnClickListener(v -> finish());
        findViewById(R.id.btnStartFakeCall).setOnClickListener(v -> scheduleSimulatedCall());
        findViewById(R.id.fabRejectFakeCall).setOnClickListener(v -> endSimulatedCall());
        fabAccept.setOnClickListener(v -> acceptSimulatedCall());
    }

    private void scheduleSimulatedCall() {
        String caller = etCallerName.getText() != null ? etCallerName.getText().toString().trim() : "Mom";
        if (caller.isEmpty()) caller = "Mom";
        tvCallerName.setText(caller);

        long delayMs = 0L;
        if (findViewById(R.id.chipDelay5s).isSelected() || ((com.google.android.material.chip.Chip) findViewById(R.id.chipDelay5s)).isChecked()) {
            delayMs = 5000L;
        } else if (((com.google.android.material.chip.Chip) findViewById(R.id.chipDelay15s)).isChecked()) {
            delayMs = 15000L;
        }

        layoutSetup.setVisibility(View.GONE);
        layoutRinging.setVisibility(View.VISIBLE);

        if (delayMs > 0) {
            tvCallStatus.setText("Simulated call scheduled (" + (delayMs / 1000) + "s)…");
            handler.postDelayed(this::startRinging, delayMs);
        } else {
            startRinging();
        }
    }

    private void startRinging() {
        tvCallStatus.setText("Incoming Call (Simulated)…");
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setLooping(true);
                }
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build());
                ringtone.play();
            }
        } catch (Exception ignored) {}

        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 800, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {}
    }

    private void acceptSimulatedCall() {
        stopAlertAudio();
        callActive = true;
        callSeconds = 0;
        tvCallStatus.setText("Simulated Call Connected");
        tvCallTimer.setVisibility(View.VISIBLE);
        tvCallTimer.setText("00:00");
        fabAccept.setVisibility(View.GONE);
        handler.postDelayed(timerRunnable, 1000L);
    }

    private void endSimulatedCall() {
        stopAlertAudio();
        callActive = false;
        handler.removeCallbacksAndMessages(null);
        finish();
    }

    private void stopAlertAudio() {
        try {
            if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        } catch (Exception ignored) {}
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        stopAlertAudio();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
