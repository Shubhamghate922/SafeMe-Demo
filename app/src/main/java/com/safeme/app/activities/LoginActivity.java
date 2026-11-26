package com.safeme.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.safeme.app.api.ApiResult;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.models.AuthDtos;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.AuthViewModel;

/**
 * Login Screen (Screen F): Authenticates user via Spring Boot JWT endpoint.
 */
public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputLayout tilIdentifier;
    private TextInputLayout tilPassword;
    private TextInputEditText etIdentifier;
    private TextInputEditText etPassword;
    private MaterialCheckBox cbRemember;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvBackendStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        tilIdentifier = findViewById(R.id.tilLoginIdentifier);
        tilPassword = findViewById(R.id.tilLoginPassword);
        etIdentifier = findViewById(R.id.etLoginIdentifier);
        etPassword = findViewById(R.id.etLoginPassword);
        cbRemember = findViewById(R.id.cbRememberLogin);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressLogin);
        tvError = findViewById(R.id.tvLoginError);
        tvBackendStatus = findViewById(R.id.tvBackendStatus);
        updateBackendStatusLabel();

        if (getIntent().getBooleanExtra("session_expired", false)) {
            tvError.setText(R.string.session_expired_error);
            tvError.setVisibility(View.VISIBLE);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.btnConfigureServer).setOnClickListener(v -> showServerUrlDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBackendStatusLabel();
    }

    private void updateBackendStatusLabel() {
        SecureTokenManager tm = SecureTokenManager.getInstance(this);
        if (tm.isLocalBackendMode()) {
            boolean hasAccount = com.safeme.app.localbackend.LocalBackendDatabase.getInstance(this).hasAnyUsers();
            tvBackendStatus.setText(hasAccount
                    ? "Backend: On-Device Local Server (Ready) • Sign in with your registered Email & Password"
                    : "Backend: On-Device Local Server (Ready) • Tap Register to create your Email & Password account");
        } else {
            tvBackendStatus.setText("Backend: Remote Spring Boot API (" + tm.getApiBaseUrl() + ")");
        }
    }

    private void attemptLogin() {
        tilIdentifier.setError(null);
        tilPassword.setError(null);
        tvError.setVisibility(View.GONE);

        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (!ValidationUtils.isValidEmailOrMobile(identifier)) {
            tilIdentifier.setError("Enter a valid email address or 10-13 digit mobile number.");
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required.");
            return;
        }

        boolean remember = cbRemember.isChecked();
        authViewModel.login(identifier, password, remember).observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                setLoading(true);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS) {
                setLoading(false);
                AuthDtos.AuthResponse data = result.getData();
                if (data != null && data.isOtpRequired()) {
                    Intent otpIntent = new Intent(this, OtpVerificationActivity.class);
                    otpIntent.putExtra(OtpVerificationActivity.EXTRA_MOBILE, identifier);
                    otpIntent.putExtra(OtpVerificationActivity.EXTRA_PURPOSE, "REGISTRATION");
                    startActivity(otpIntent);
                } else {
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();
                }
            } else {
                setLoading(false);
                tvError.setText(result.getMessage() != null ? result.getMessage() : "Login failed.");
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing In…" : "Login");
    }

    private void showServerUrlDialog() {
        SecureTokenManager tokenManager = SecureTokenManager.getInstance(this);
        TextInputEditText input = new TextInputEditText(this);
        input.setText(tokenManager.getApiBaseUrl());
        input.setHint("http://10.0.2.2:8080/api/v1/");

        new AlertDialog.Builder(this)
                .setTitle("Backend Connection Mode")
                .setMessage("Use On-Device Local Server to explore the full app without an external server, or save a Remote Spring Boot API URL.")
                .setView(input)
                .setPositiveButton("Use Remote URL", (d, w) -> {
                    String entered = input.getText() != null ? input.getText().toString().trim() : "";
                    if (ValidationUtils.isValidServerUrl(entered)) {
                        tokenManager.setApiBaseUrl(entered);
                        tokenManager.setLocalBackendMode(false);
                        RetrofitClient.resetClient();
                        updateBackendStatusLabel();
                        Snackbar.make(findViewById(android.R.id.content), "Switched to Remote Spring Boot Server.", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "Enter a valid http:// or https:// URL.", Snackbar.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton("Use On-Device Server", (d, w) -> {
                    tokenManager.setLocalBackendMode(true);
                    RetrofitClient.resetClient();
                    updateBackendStatusLabel();
                    Snackbar.make(findViewById(android.R.id.content), "Switched to On-Device Local Backend.", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
