package com.safeme.app.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.safeme.app.api.ApiResult;
import com.safeme.app.services.CheckInTimerManager;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.viewmodel.JourneyViewModel;

public class EmergencyEscalationFragment extends Fragment {

    private JourneyViewModel viewModel;
    private TextView tvCountdown, tvStageLabel, tvStage1Status, tvStage2Status, tvStage3Status, tvContactsInfo;
    private CountDownTimer timer;
    private String journeyId;
    private int currentStage = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergency_escalation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JourneyViewModel.class);

        tvCountdown = view.findViewById(R.id.tvEscalationCountdown);
        tvStageLabel = view.findViewById(R.id.tvEscalationStageLabel);
        tvStage1Status = view.findViewById(R.id.tvStage1Status);
        tvStage2Status = view.findViewById(R.id.tvStage2Status);
        tvStage3Status = view.findViewById(R.id.tvStage3Status);
        tvContactsInfo = view.findViewById(R.id.tvEscalationContactsInfo);

        if (getArguments() != null) {
            journeyId = getArguments().getString("journeyId");
        }

        viewModel.loadTrustedContacts().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                tvContactsInfo.setText("Configured emergency contacts: " + res.getData().size() + " contact(s)");
            }
        });

        triggerStage(1, "Missed scheduled safety check-in");

        int stage2Grace = SecureTokenManager.getInstance(requireContext()).getEscalationStage2GraceMinutes();
        startEscalationCountdown(stage2Grace * 60_000L);

        view.findViewById(R.id.btnEscalationConfirmSafe).setOnClickListener(v -> confirmSafe());
        view.findViewById(R.id.btnEscalationTriggerNow).setOnClickListener(v -> escalateNowToSos());
    }

    private void startEscalationCountdown(long millis) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(millis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Next Escalation in " + DateTimeUtils.formatCountdownSeconds(millisUntilFinished / 1000L));
            }

            @Override
            public void onFinish() {
                if (!isAdded()) return;
                if (currentStage == 1) {
                    currentStage = 2;
                    triggerStage(2, "Stage 1 grace period expired");
                    tvStageLabel.setText("Current Stage: Stage 2 (Primary Contacts Alerted)");
                    tvStage2Status.setText("Alert dispatched to Priority 1 trusted contacts");
                    startEscalationCountdown(60_000L);
                } else if (currentStage == 2) {
                    currentStage = 3;
                    triggerStage(3, "Stage 2 unacknowledged — alerting all contacts");
                    tvStageLabel.setText("Current Stage: Stage 3 (All Contacts & SOS Escalated)");
                    tvStage3Status.setText("All additional trusted contacts notified with live GPS");
                    tvCountdown.setText("All Escalation Stages Active");
                }
            }
        }.start();
    }

    private void triggerStage(int stage, String reason) {
        if (journeyId == null) return;
        if (!CheckInTimerManager.getInstance().tryLockEscalationStage(stage)) {
            return; // Prevent duplicate escalation dispatch
        }
        viewModel.escalateJourney(journeyId, stage, reason).observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.ERROR) {
                Toast.makeText(requireContext(), "Escalation sync notice: " + res.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmSafe() {
        if (timer != null) timer.cancel();
        if (journeyId != null && CheckInTimerManager.getInstance().tryLockCheckInSubmission()) {
            viewModel.submitCheckIn(journeyId, "SAFE", 0.0, 0.0, "Escalation cancelled by user confirming safe")
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.getStatus() != ApiResult.Status.LOADING) {
                            Toast.makeText(requireContext(), "Escalation stopped. Safe check-in logged.", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(this).navigate(R.id.homeFragment);
                        }
                    });
        } else {
            NavHostFragment.findNavController(this).navigate(R.id.homeFragment);
        }
    }

    private void escalateNowToSos() {
        if (timer != null) timer.cancel();
        Bundle args = new Bundle();
        args.putString("journeyId", journeyId);
        args.putBoolean("autoTrigger", true);
        NavHostFragment.findNavController(this).navigate(R.id.emergencySosFragment, args);
    }

    @Override
    public void onDestroyView() {
        if (timer != null) timer.cancel();
        super.onDestroyView();
    }
}
