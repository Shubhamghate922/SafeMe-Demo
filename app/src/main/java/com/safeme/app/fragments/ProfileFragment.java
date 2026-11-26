package com.safeme.app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.safeme.app.activities.MainActivity;
import com.safeme.app.activities.PermissionsActivity;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.User;
import com.safeme.app.services.CheckInTimerManager;
import com.safeme.app.services.JourneyLocationService;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private TextView tvName, tvEmail, tvMobile, tvVerified;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvMobile = view.findViewById(R.id.tvProfileMobile);
        tvVerified = view.findViewById(R.id.tvProfileVerified);

        bindCachedUser();
        loadUserFromBackend();

        view.findViewById(R.id.btnProfileEdit).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.btnProfileChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.btnProfileContacts).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.trustedContactsFragment));
        view.findViewById(R.id.btnProfilePermissions).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PermissionsActivity.class)));
        view.findViewById(R.id.btnProfileSettings).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.settingsFragment));
        view.findViewById(R.id.btnProfileAbout).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.aboutFragment));
        view.findViewById(R.id.btnProfileLogout).setOnClickListener(v -> performLogout());
        view.findViewById(R.id.btnProfileDeleteAccount).setOnClickListener(v -> confirmDeleteAccount());
    }

    private void bindCachedUser() {
        SecureTokenManager tm = SecureTokenManager.getInstance(requireContext());
        tvName.setText(tm.getUserName().isEmpty() ? "SafeMe User" : tm.getUserName());
        tvEmail.setText(tm.getUserEmail());
        tvMobile.setText(tm.getUserMobile());
        tvVerified.setText(tm.isUserVerified() ? "Verified Account" : "Pending OTP Verification");
    }

    private void loadUserFromBackend() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                currentUser = res.getData();
                tvName.setText(currentUser.getFullName());
                tvEmail.setText(currentUser.getEmail());
                tvMobile.setText(currentUser.getMobileNumber());
                tvVerified.setText(currentUser.isVerified() ? "Verified Account" : "Pending OTP Verification");
            }
        });
    }

    private void showEditProfileDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etName = new EditText(requireContext());
        etName.setHint("Full Name");
        etName.setText(tvName.getText());
        EditText etMobile = new EditText(requireContext());
        etMobile.setHint("Mobile Number");
        etMobile.setInputType(InputType.TYPE_CLASS_PHONE);
        etMobile.setText(tvMobile.getText());

        layout.addView(etName);
        layout.addView(etMobile);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String mobile = etMobile.getText().toString().trim();
                    if (!ValidationUtils.isValidFullName(name) || !ValidationUtils.isValidMobile(mobile)) {
                        Toast.makeText(requireContext(), "Enter valid name and mobile number.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.updateProfile(name, ValidationUtils.normalizeMobile(mobile),
                            currentUser != null ? currentUser.getProfilePhotoUrl() : null)
                            .observe(getViewLifecycleOwner(), res -> {
                                if (res.getStatus() == ApiResult.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Profile updated.", Toast.LENGTH_SHORT).show();
                                    loadUserFromBackend();
                                } else if (res.getStatus() == ApiResult.Status.ERROR) {
                                    Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etCurrent = new EditText(requireContext());
        etCurrent.setHint("Current Password");
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText etNew = new EditText(requireContext());
        etNew.setHint("New Password (8+ chars, upper, lower, digit)");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etCurrent);
        layout.addView(etNew);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (d, w) -> {
                    String curr = etCurrent.getText().toString();
                    String next = etNew.getText().toString();
                    String err = ValidationUtils.getPasswordError(next);
                    if (err != null) {
                        Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                        return;
                    }
                    viewModel.changePassword(curr, next).observe(getViewLifecycleOwner(), res -> {
                        if (res.getStatus() == ApiResult.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Password updated.", Toast.LENGTH_SHORT).show();
                        } else if (res.getStatus() == ApiResult.Status.ERROR) {
                            Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        JourneyLocationService.stopMonitoring(requireContext());
        CheckInTimerManager.getInstance().stopTimer();
        viewModel.logout(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).redirectToLogin(false);
            }
        });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account?")
                .setMessage("This permanently removes your account, journeys, and emergency contacts from SafeMe.")
                .setPositiveButton("Delete Forever", (d, w) ->
                        viewModel.deleteAccount().observe(getViewLifecycleOwner(), res -> {
                            if (res.getStatus() == ApiResult.Status.SUCCESS) {
                                JourneyLocationService.stopMonitoring(requireContext());
                                CheckInTimerManager.getInstance().stopTimer();
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).redirectToLogin(false);
                                }
                            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
