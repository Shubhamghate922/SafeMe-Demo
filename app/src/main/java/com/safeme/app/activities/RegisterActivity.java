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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.AuthDtos;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.AuthViewModel;

/**
 * Registration Screen (Screen D): Full form validation and backend registration.
 */
public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputLayout tilName, tilEmail, tilMobile, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etMobile, etPassword, etConfirm;
    private MaterialCheckBox cbTerms;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        tilName = findViewById(R.id.tilRegFullName);
        tilEmail = findViewById(R.id.tilRegEmail);
        tilMobile = findViewById(R.id.tilRegMobile);
        tilPassword = findViewById(R.id.tilRegPassword);
        tilConfirm = findViewById(R.id.tilRegConfirmPassword);

        etName = findViewById(R.id.etRegFullName);
        etEmail = findViewById(R.id.etRegEmail);
        etMobile = findViewById(R.id.etRegMobile);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirm = findViewById(R.id.etRegConfirmPassword);

        cbTerms = findViewById(R.id.cbAcceptTerms);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressRegister);
        tvError = findViewById(R.id.tvRegisterError);

        btnRegister.setOnClickListener(v -> submitRegistration());
        findViewById(R.id.tvAlreadyHaveAccountLogin).setOnClickListener(v -> finish());
        findViewById(R.id.tvPrivacyPolicyLink).setOnClickListener(v -> showPrivacyDialog());
    }

    private void submitRegistration() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilMobile.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);
        tvError.setVisibility(View.GONE);

        String name = getText(etName);
        String email = getText(etEmail);
        String mobile = ValidationUtils.normalizeMobile(getText(etMobile));
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

        boolean valid = true;
        if (!ValidationUtils.isValidFullName(name)) {
            tilName.setError("Please enter your full name (2–80 characters).");
            valid = false;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError("Please enter a valid email address.");
            valid = false;
        }
        if (!ValidationUtils.isValidMobile(mobile)) {
            tilMobile.setError("Please enter a valid 10–13 digit mobile number.");
            valid = false;
        }
        String passwordErr = ValidationUtils.getPasswordError(password);
        if (passwordErr != null) {
            tilPassword.setError(passwordErr);
            valid = false;
        }
        if (!password.equals(confirm)) {
            tilConfirm.setError("Passwords do not match.");
            valid = false;
        }
        if (!cbTerms.isChecked()) {
            tvError.setText("Please accept the Terms & Conditions and Privacy Policy to continue.");
            tvError.setVisibility(View.VISIBLE);
            valid = false;
        }

        if (!valid) return;

        authViewModel.register(name, email, mobile, password).observe(this, result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                setLoading(true);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS) {
                setLoading(false);
                AuthDtos.AuthResponse data = result.getData();
                if (data != null && data.getAccessToken() != null && !data.isOtpRequired()) {
                    Intent main = new Intent(this, MainActivity.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(main);
                    finish();
                } else {
                    Intent otpIntent = new Intent(this, OtpVerificationActivity.class);
                    otpIntent.putExtra(OtpVerificationActivity.EXTRA_MOBILE, mobile);
                    otpIntent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, email);
                    otpIntent.putExtra(OtpVerificationActivity.EXTRA_PURPOSE, "REGISTRATION");
                    startActivity(otpIntent);
                    finish();
                }
            } else {
                setLoading(false);
                tvError.setText(result.getMessage() != null ? result.getMessage() : "Registration failed.");
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating Account…" : "Register");
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("SafeMe Privacy Policy")
                .setMessage("SafeMe collects your name, email, phone number, and explicit active-session GPS coordinates solely to provide Smart Journey Monitoring and Emergency SOS notifications to your chosen trusted contacts. Passwords are BCrypt hashed on the backend and tokens are encrypted via Android Keystore.")
                .setPositiveButton("Close", null)
                .show();
    }
}
