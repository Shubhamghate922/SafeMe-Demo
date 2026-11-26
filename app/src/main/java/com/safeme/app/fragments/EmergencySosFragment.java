package com.safeme.app.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.safeme.app.adapters.AcknowledgementAdapter;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.SosAlert;
import com.safeme.app.services.JourneyLocationService;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.viewmodel.SosViewModel;

import java.util.Locale;

public class EmergencySosFragment extends Fragment {

    private SosViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private AcknowledgementAdapter ackAdapter;

    private MaterialCardView cardActiveBanner;
    private View layoutTriggerContainer, layoutResponses;
    private TextView tvCreatedAt, tvCoords, tvNotifStatus, tvError, tvNoAcks;
    private ProgressBar progressSos;

    private String currentSosId;
    private String journeyId;

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> dispatchSosWithBestGps());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergency_sos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SosViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        cardActiveBanner = view.findViewById(R.id.cardSosActiveBanner);
        layoutTriggerContainer = view.findViewById(R.id.layoutTriggerSosContainer);
        layoutResponses = view.findViewById(R.id.layoutSosResponses);
        tvCreatedAt = view.findViewById(R.id.tvSosCreatedAt);
        tvCoords = view.findViewById(R.id.tvSosCoordinates);
        tvNotifStatus = view.findViewById(R.id.tvSosNotifStatus);
        tvError = view.findViewById(R.id.tvSosError);
        tvNoAcks = view.findViewById(R.id.tvNoAcknowledgementsYet);
        progressSos = view.findViewById(R.id.progressSos);

        RecyclerView rvAcks = view.findViewById(R.id.rvSosAcknowledgements);
        rvAcks.setLayoutManager(new LinearLayoutManager(requireContext()));
        ackAdapter = new AcknowledgementAdapter();
        rvAcks.setAdapter(ackAdapter);

        boolean autoTrigger = false;
        if (getArguments() != null) {
            journeyId = getArguments().getString("journeyId");
            autoTrigger = getArguments().getBoolean("autoTrigger", false);
        }

        view.findViewById(R.id.btnTriggerSosMain).setOnClickListener(v -> confirmAndTriggerSos());
        view.findViewById(R.id.btnRefreshSosStatus).setOnClickListener(v -> refreshAcknowledgements());
        view.findViewById(R.id.btnResolveSos).setOnClickListener(v -> confirmResolveSos());

        loadActiveSos();
        if (autoTrigger) {
            initiateSosTrigger();
        }
    }

    private void loadActiveSos() {
        viewModel.getActiveSos().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null
                    && "ACTIVE".equals(res.getData().getStatus())) {
                showActiveSosUi(res.getData());
            }
        });
    }

    private void confirmAndTriggerSos() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Trigger Emergency SOS?")
                .setMessage("This will immediately alert your trusted contacts and share your live GPS coordinates.")
                .setPositiveButton("SEND SOS NOW", (d, w) -> initiateSosTrigger())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initiateSosTrigger() {
        if (!PermissionHelper.hasLocationPermission(requireContext())) {
            permLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        dispatchSosWithBestGps();
    }

    private void dispatchSosWithBestGps() {
        try {
            if (PermissionHelper.hasLocationPermission(requireContext())) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                    double lat = loc != null ? loc.getLatitude() : 0.0;
                    double lng = loc != null ? loc.getLongitude() : 0.0;
                    float acc = loc != null ? loc.getAccuracy() : 0f;
                    sendSosToBackend(lat, lng, acc);
                });
                return;
            }
        } catch (SecurityException ignored) {}
        sendSosToBackend(0.0, 0.0, 0f);
    }

    private void sendSosToBackend(double lat, double lng, float acc) {
        tvError.setVisibility(View.GONE);
        viewModel.triggerSos(journeyId, "MANUAL_SOS", lat, lng, acc, "Emergency assistance requested")
                .observe(getViewLifecycleOwner(), res -> {
                    if (res.getStatus() == ApiResult.Status.LOADING) {
                        progressSos.setVisibility(View.VISIBLE);
                    } else if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                        progressSos.setVisibility(View.GONE);
                        SosAlert alert = res.getData();
                        if (PermissionHelper.hasLocationPermission(requireContext())) {
                            JourneyLocationService.startForSos(requireContext(), alert.getId());
                        }
                        showActiveSosUi(alert);
                    } else {
                        progressSos.setVisibility(View.GONE);
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText(res.getMessage());
                    }
                });
    }

    private void showActiveSosUi(SosAlert alert) {
        currentSosId = alert.getId();
        cardActiveBanner.setVisibility(View.VISIBLE);
        layoutTriggerContainer.setVisibility(View.GONE);
        layoutResponses.setVisibility(View.VISIBLE);

        tvCreatedAt.setText("Triggered: " + DateTimeUtils.formatTimestamp(alert.getCreatedAt()));
        tvCoords.setText(String.format(Locale.US, "GPS: %.5f, %.5f", alert.getLatitude(), alert.getLongitude()));
        String delivery = alert.getNotificationDeliveryStatus();
        tvNotifStatus.setText("Delivery status: " + (delivery != null ? delivery : "Dispatched to server"));

        if (alert.getAcknowledgements() != null && !alert.getAcknowledgements().isEmpty()) {
            tvNoAcks.setVisibility(View.GONE);
            ackAdapter.submitList(alert.getAcknowledgements());
        } else {
            tvNoAcks.setVisibility(View.VISIBLE);
        }
    }

    private void refreshAcknowledgements() {
        if (currentSosId == null) return;
        viewModel.getAcknowledgements(currentSosId).observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                ackAdapter.submitList(res.getData());
                tvNoAcks.setVisibility(res.getData().isEmpty() ? View.VISIBLE : View.GONE);
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmResolveSos() {
        if (currentSosId == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Resolve Emergency SOS?")
                .setMessage("Confirm that you are safe. Trusted contacts will be notified that the emergency is resolved and live GPS tracking will stop.")
                .setPositiveButton("I Am Safe", (d, w) -> resolveSos())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resolveSos() {
        viewModel.resolveSos(currentSosId, "User confirmed safe").observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS) {
                JourneyLocationService.stopMonitoring(requireContext());
                cardActiveBanner.setVisibility(View.GONE);
                layoutResponses.setVisibility(View.GONE);
                layoutTriggerContainer.setVisibility(View.VISIBLE);
                currentSosId = null;
                Toast.makeText(requireContext(), "SOS resolved safely.", Toast.LENGTH_SHORT).show();
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
