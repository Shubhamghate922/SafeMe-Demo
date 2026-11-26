package com.safeme.app.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.Journey;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.services.CheckInTimerManager;
import com.safeme.app.services.JourneyLocationService;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.viewmodel.JourneyViewModel;

public class SafetyCheckInFragment extends Fragment {

    private JourneyViewModel viewModel;
    private TextView tvDetails, tvCountdown;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private String journeyId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_safety_checkin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JourneyViewModel.class);

        tvDetails = view.findViewById(R.id.tvCheckInJourneyDetails);
        tvCountdown = view.findViewById(R.id.tvCheckInCountdown);
        progressBar = view.findViewById(R.id.progressCheckIn);

        if (getArguments() != null) {
            journeyId = getArguments().getString("journeyId");
        }

        viewModel.getActiveJourney().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                Journey j = res.getData();
                journeyId = j.getId();
                tvDetails.setText("Journey to " + j.getDestinationName());
            }
        });

        int graceMinutes = SecureTokenManager.getInstance(requireContext()).getEscalationStage1GraceMinutes();
        startGraceTimer(graceMinutes * 60_000L);

        view.findViewById(R.id.btnConfirmSafe).setOnClickListener(v -> confirmSafe());
        view.findViewById(R.id.btnNeedHelp).setOnClickListener(v -> triggerHelpSos());
    }

    private void startGraceTimer(long millis) {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(millis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(DateTimeUtils.formatCountdownSeconds(millisUntilFinished / 1000L));
            }

            @Override
            public void onFinish() {
                if (!isAdded()) return;
                tvCountdown.setText("00:00");
                Bundle args = new Bundle();
                args.putString("journeyId", journeyId);
                NavHostFragment.findNavController(SafetyCheckInFragment.this)
                        .navigate(R.id.emergencyEscalationFragment, args);
            }
        }.start();
    }

    private void confirmSafe() {
        if (journeyId == null) {
            Toast.makeText(requireContext(), "No active journey found for check-in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!CheckInTimerManager.getInstance().tryLockCheckInSubmission()) {
            Toast.makeText(requireContext(), "Check-in already recorded.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (countDownTimer != null) countDownTimer.cancel();

        LocationUpdate fix = JourneyLocationService.getLatestLocationLiveData().getValue();
        double lat = fix != null ? fix.getLatitude() : 0.0;
        double lng = fix != null ? fix.getLongitude() : 0.0;

        viewModel.submitCheckIn(journeyId, "SAFE", lat, lng, "User confirmed safe").observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (res.getStatus() == ApiResult.Status.SUCCESS) {
                progressBar.setVisibility(View.GONE);
                int interval = SecureTokenManager.getInstance(requireContext()).getDefaultCheckInIntervalMinutes();
                CheckInTimerManager.getInstance().scheduleNextCheckIn(
                        requireContext(), journeyId, System.currentTimeMillis() + (interval * 60_000L));
                Toast.makeText(requireContext(), "Safety confirmed. Next check-in scheduled.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void triggerHelpSos() {
        if (countDownTimer != null) countDownTimer.cancel();
        Bundle args = new Bundle();
        args.putString("journeyId", journeyId);
        args.putBoolean("autoTrigger", true);
        NavHostFragment.findNavController(this).navigate(R.id.emergencySosFragment, args);
    }

    @Override
    public void onDestroyView() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroyView();
    }
}
