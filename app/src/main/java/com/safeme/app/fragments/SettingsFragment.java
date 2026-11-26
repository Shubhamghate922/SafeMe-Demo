package com.safeme.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.activities.MainActivity;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.repository.AuthRepository;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.utils.ValidationUtils;

public class SettingsFragment extends Fragment {

    private SecureTokenManager tokenManager;
    private TextInputEditText etApiUrl, etCheckInMin, etStage1Min, etStage2Min;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tokenManager = SecureTokenManager.getInstance(requireContext());

        etApiUrl = view.findViewById(R.id.etSettingsApiUrl);
        etCheckInMin = view.findViewById(R.id.etSettingsCheckInInterval);
        etStage1Min = view.findViewById(R.id.etSettingsStage1Min);
        etStage2Min = view.findViewById(R.id.etSettingsStage2Min);

        etApiUrl.setText(tokenManager.getApiBaseUrl());
        etCheckInMin.setText(String.valueOf(tokenManager.getDefaultCheckInIntervalMinutes()));
        etStage1Min.setText(String.valueOf(tokenManager.getEscalationStage1GraceMinutes()));
        etStage2Min.setText(String.valueOf(tokenManager.getEscalationStage2GraceMinutes()));

        MaterialSwitch switchLocalBackend = view.findViewById(R.id.switchLocalBackendMode);
        MaterialSwitch switchGps = view.findViewById(R.id.switchHighAccuracyGps);
        MaterialSwitch switchPrivacy = view.findViewById(R.id.switchLockScreenPrivacy);
        switchLocalBackend.setChecked(tokenManager.isLocalBackendMode());
        switchGps.setChecked(tokenManager.isHighAccuracyLocationEnabled());
        switchPrivacy.setChecked(tokenManager.isNotificationPrivacyHidden());

        switchLocalBackend.setOnCheckedChangeListener((b, checked) -> {
            tokenManager.setLocalBackendMode(checked);
            RetrofitClient.resetClient();
            Toast.makeText(requireContext(),
                    checked ? "Using On-Device Local Backend." : "Using Remote Spring Boot Server.",
                    Toast.LENGTH_SHORT).show();
        });
        switchGps.setOnCheckedChangeListener((b, checked) -> tokenManager.setHighAccuracyLocationEnabled(checked));
        switchPrivacy.setOnCheckedChangeListener((b, checked) -> tokenManager.setNotificationPrivacyHidden(checked));

        ChipGroup chipTheme = view.findViewById(R.id.chipGroupTheme);
        String themeMode = tokenManager.getThemeMode();
        if ("dark".equals(themeMode)) chipTheme.check(R.id.chipThemeDark);
        else if ("system".equals(themeMode)) chipTheme.check(R.id.chipThemeSystem);
        else chipTheme.check(R.id.chipThemeLight);

        chipTheme.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = group.getCheckedChipId();
            if (id == R.id.chipThemeDark) {
                tokenManager.setThemeMode("dark");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (id == R.id.chipThemeSystem) {
                tokenManager.setThemeMode("system");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                tokenManager.setThemeMode("light");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        ChipGroup chipLang = view.findViewById(R.id.chipGroupLanguage);
        if ("hi".equals(tokenManager.getLanguageCode())) chipLang.check(R.id.chipLangHi);
        else chipLang.check(R.id.chipLangEn);

        chipLang.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = group.getCheckedChipId();
            tokenManager.setLanguageCode(id == R.id.chipLangHi ? "hi" : "en");
            Toast.makeText(requireContext(), "Language preference saved.", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnSaveApiUrl).setOnClickListener(v -> {
            String url = etApiUrl.getText() != null ? etApiUrl.getText().toString().trim() : "";
            if (!ValidationUtils.isValidServerUrl(url)) {
                etApiUrl.setError("Enter a valid http:// or https:// URL");
                return;
            }
            tokenManager.setApiBaseUrl(url);
            tokenManager.setLocalBackendMode(false);
            switchLocalBackend.setChecked(false);
            RetrofitClient.resetClient();
            Toast.makeText(requireContext(), "Remote Server URL saved and activated.", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnSaveTimerSettings).setOnClickListener(v -> {
            try {
                int interval = Integer.parseInt(etCheckInMin.getText().toString().trim());
                int s1 = Integer.parseInt(etStage1Min.getText().toString().trim());
                int s2 = Integer.parseInt(etStage2Min.getText().toString().trim());
                if (interval < 1 || s1 < 1 || s2 < 1) {
                    Toast.makeText(requireContext(), "Timers must be at least 1 minute.", Toast.LENGTH_SHORT).show();
                    return;
                }
                tokenManager.setDefaultCheckInIntervalMinutes(interval);
                tokenManager.setEscalationStage1GraceMinutes(s1);
                tokenManager.setEscalationStage2GraceMinutes(s2);
                Toast.makeText(requireContext(), "Check-in and escalation timers saved.", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Enter valid numeric values.", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnSettingsAbout).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.aboutFragment));

        view.findViewById(R.id.btnSettingsLogout).setOnClickListener(v ->
                new AuthRepository(requireContext()).logout(() -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).redirectToLogin(false);
                    }
                }));
    }
}
