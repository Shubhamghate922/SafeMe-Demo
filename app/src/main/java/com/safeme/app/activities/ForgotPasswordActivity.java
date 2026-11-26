package com.safeme.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.safeme.app.api.ApiResult;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.AuthViewModel;

/**
 * Forgot Password Screens (Screen G): Request OTP -> Verify OTP -> Reset Password -> Success.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private LinearLayout stepRequest, stepOtp, stepNewPassword, stepSuccess;
    private TextInputLayout tilIdentifier, tilOtp, tilNewPass, tilConfirmPass;
    private TextInputEditText etIdentifier, etOtp, etNewPass, etConfirmPass;
    private TextView tvError;
    private ProgressBar progressBar;

    private String targetIdentifier;
    private String enteredOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        stepRequest = findViewById(R.id.layoutStepRequest);
        stepOtp = findViewById(R.id.layoutStepOtp);
        stepNewPassword = findViewById(R.id.layoutStepNewPassword);
        stepSuccess = findViewById(R.id.layoutStepSuccess);

        tilIdentifier = findViewById(R.id.tilForgotIdentifier);
        tilOtp = findViewById(R.id.tilForgotOtp);
        tilNewPass = findViewById(R.id.tilNewPassword);
        tilConfirmPass = findViewById(R.id.tilConfirmNewPassword);

        etIdentifier = findViewById(R.id.etForgotIdentifier);
        etOtp = findViewById(R.id.etForgotOtp);
        etNewPass = findViewById(R.id.etNewPassword);
        etConfirmPass = findViewById(R.id.etConfirmNewPassword);

        tvError = findViewById(R.id.tvForgotError);
        progressBar = findViewById(R.id.progressForgot);

        findViewById(R.id.btnForgotBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendResetOtp).setOnClickListener(v -> requestResetOtp());
        findViewById(R.id.btnConfirmResetOtp).setOnClickListener(v -> confirmOtpStep());
        findViewById(R.id.btnSubmitNewPassword).setOnClickListener(v -> submitNewPassword());
        findViewById(R.id.btnBackToLoginAfterReset).setOnClickListener(v -> finish());
    }

    private void requestResetOtp() {
        tilIdentifier.setError(null);
        tvError.setVisibility(View.GONE);
        String id = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";

        if (!ValidationUtils.isValidEmailOrMobile(id)) {
            tilIdentifier.setError("Enter a valid registered email or mobile number.");
            return;
        }
        targetIdentifier = id;

        authViewModel.forgotPassword(id).observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS) {
                progressBar.setVisibility(View.GONE);
                stepRequest.setVisibility(View.GONE);
                stepOtp.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                tvError.setText(result.getMessage());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void confirmOtpStep() {
        tilOtp.setError(null);
        tvError.setVisibility(View.GONE);
        String otp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";

        if (!ValidationUtils.isValidOtp(otp)) {
            tilOtp.setError("Enter the 6-digit OTP code.");
            return;
        }
        enteredOtp = otp;
        stepOtp.setVisibility(View.GONE);
        stepNewPassword.setVisibility(View.VISIBLE);
    }

    private void submitNewPassword() {
        tilNewPass.setError(null);
        tilConfirmPass.setError(null);
        tvError.setVisibility(View.GONE);

        String pass = etNewPass.getText() != null ? etNewPass.getText().toString() : "";
        String confirm = etConfirmPass.getText() != null ? etConfirmPass.getText().toString() : "";

        String err = ValidationUtils.getPasswordError(pass);
        if (err != null) {
            tilNewPass.setError(err);
            return;
        }
        if (!pass.equals(confirm)) {
            tilConfirmPass.setError("Passwords do not match.");
            return;
        }

        authViewModel.resetPassword(targetIdentifier, enteredOtp, pass).observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS) {
                progressBar.setVisibility(View.GONE);
                stepNewPassword.setVisibility(View.GONE);
                stepSuccess.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                tvError.setText(result.getMessage());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}
