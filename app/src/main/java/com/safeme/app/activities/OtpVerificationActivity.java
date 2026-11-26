package com.safeme.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.safeme.app.api.ApiResult;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.AuthViewModel;

import java.util.Locale;

/**
 * OTP Verification Screen (Screen E): Verifies 6-digit OTP via backend provider.
 * Never hardcodes any real OTP value.
 */
public class OtpVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_MOBILE = "extra_mobile";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_PURPOSE = "extra_purpose";

    private AuthViewModel authViewModel;
    private TextInputLayout tilOtp;
    private TextInputEditText etOtp;
    private TextView tvCountdown;
    private TextView tvError;
    private MaterialButton btnResend;
    private MaterialButton btnVerify;
    private ProgressBar progressBar;

    private String mobile;
    private String email;
    private String purpose;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        mobile = getIntent().getStringExtra(EXTRA_MOBILE);
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        purpose = getIntent().getStringExtra(EXTRA_PURPOSE);
        if (purpose == null) purpose = "REGISTRATION";

        tilOtp = findViewById(R.id.tilOtpCode);
        etOtp = findViewById(R.id.etOtpCode);
        tvCountdown = findViewById(R.id.tvOtpCountdown);
        tvError = findViewById(R.id.tvOtpError);
        btnResend = findViewById(R.id.btnResendOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        progressBar = findViewById(R.id.progressOtp);

        TextView tvSubtitle = findViewById(R.id.tvOtpSubtitle);
        if (mobile != null && !mobile.isEmpty()) {
            tvSubtitle.setText("Enter the 6-digit verification code sent to " + mobile + " via SafeMe OTP service.");
        }

        findViewById(R.id.btnOtpBack).setOnClickListener(v -> finish());
        btnVerify.setOnClickListener(v -> verifyOtp());
        btnResend.setOnClickListener(v -> resendOtp());

        startResendTimer();
    }

    private void startResendTimer() {
        btnResend.setEnabled(false);
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60_000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000L;
                tvCountdown.setText(String.format(Locale.US, "Resend available in 00:%02d", sec));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Didn't receive the code?");
                btnResend.setEnabled(true);
            }
        }.start();
    }

    private void verifyOtp() {
        tilOtp.setError(null);
        tvError.setVisibility(View.GONE);

        String otp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";
        if (!ValidationUtils.isValidOtp(otp)) {
            tilOtp.setError("Enter a valid 6-digit numeric OTP.");
            return;
        }

        authViewModel.verifyOtp(mobile, email, otp, purpose).observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                setLoading(true);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS) {
                setLoading(false);
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                setLoading(false);
                tvError.setText(result.getMessage() != null ? result.getMessage() : "OTP verification failed.");
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resendOtp() {
        tvError.setVisibility(View.GONE);
        authViewModel.resendOtp(mobile, purpose).observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                btnResend.setEnabled(false);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS) {
                Snackbar.make(findViewById(android.R.id.content), "A new OTP has been dispatched.", Snackbar.LENGTH_SHORT).show();
                startResendTimer();
            } else {
                btnResend.setEnabled(true);
                tvError.setText(result.getMessage() != null ? result.getMessage() : "Unable to resend OTP.");
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
