package com.safeme.app.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.adapters.IncidentReportAdapter;
import com.safeme.app.api.ApiResult;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.viewmodel.SafetyViewModel;

public class IncidentReportFragment extends Fragment {

    private SafetyViewModel viewModel;
    private IncidentReportAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;

    private ChipGroup chipCategory;
    private TextInputEditText etDescription, etDate, etLocation;
    private TextView tvAttachments, tvError;
    private ProgressBar progressBar;

    private Uri selectedPhotoUri;
    private Uri selectedAudioUri;
    private double lat = 0.0, lng = 0.0;

    // Google Play policy compliant zero-permission Photo Picker
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    updateAttachmentLabel();
                }
            });

    private final ActivityResultLauncher<String> pickAudioLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAudioUri = uri;
                    updateAttachmentLabel();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_incident_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SafetyViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        chipCategory = view.findViewById(R.id.chipGroupIncidentCategory);
        etDescription = view.findViewById(R.id.etIncidentDescription);
        etDate = view.findViewById(R.id.etIncidentDate);
        etLocation = view.findViewById(R.id.etIncidentLocation);
        tvAttachments = view.findViewById(R.id.tvIncidentAttachments);
        tvError = view.findViewById(R.id.tvIncidentError);
        progressBar = view.findViewById(R.id.progressIncident);

        etDate.setText(DateTimeUtils.formatTimestamp(System.currentTimeMillis()));

        RecyclerView rv = view.findViewById(R.id.rvIncidentReports);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new IncidentReportAdapter();
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnAttachPhoto).setOnClickListener(v ->
                pickPhotoLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        view.findViewById(R.id.btnAttachAudio).setOnClickListener(v ->
                pickAudioLauncher.launch("audio/*"));

        view.findViewById(R.id.btnSubmitIncident).setOnClickListener(v -> submitReport());

        if (PermissionHelper.hasLocationPermission(requireContext())) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        lat = loc.getLatitude();
                        lng = loc.getLongitude();
                    }
                });
            } catch (SecurityException ignored) {}
        }

        loadReports();
    }

    private void updateAttachmentLabel() {
        StringBuilder sb = new StringBuilder();
        if (selectedPhotoUri != null) sb.append("Photo attached ");
        if (selectedAudioUri != null) sb.append("Audio attached");
        tvAttachments.setText(sb.length() > 0 ? sb.toString().trim() : "No optional photo or audio attached.");
    }

    private String getSelectedCategory() {
        int id = chipCategory.getCheckedChipId();
        if (id == R.id.chipIncSuspicious) return "SUSPICIOUS_ACTIVITY";
        if (id == R.id.chipIncTransport) return "TRANSPORT_ISSUE";
        if (id == R.id.chipIncOther) return "OTHER";
        return "HARASSMENT";
    }

    private void submitReport() {
        tvError.setVisibility(View.GONE);
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String loc = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

        if (desc.length() < 10) {
            etDescription.setError("Provide at least 10 characters describing the incident");
            return;
        }
        if (loc.isEmpty()) {
            etLocation.setError("Enter incident location");
            return;
        }

        viewModel.submitIncidentReport(getSelectedCategory(), desc, date, loc, lat, lng, selectedPhotoUri, selectedAudioUri)
                .observe(getViewLifecycleOwner(), res -> {
                    if (res.getStatus() == ApiResult.Status.LOADING) {
                        progressBar.setVisibility(View.VISIBLE);
                    } else if (res.getStatus() == ApiResult.Status.SUCCESS) {
                        progressBar.setVisibility(View.GONE);
                        etDescription.setText("");
                        selectedPhotoUri = null;
                        selectedAudioUri = null;
                        updateAttachmentLabel();
                        Toast.makeText(requireContext(), "Incident report submitted.", Toast.LENGTH_SHORT).show();
                        loadReports();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText(res.getMessage());
                    }
                });
    }

    private void loadReports() {
        viewModel.getIncidentReports().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                adapter.submitList(res.getData());
            }
        });
    }
}
