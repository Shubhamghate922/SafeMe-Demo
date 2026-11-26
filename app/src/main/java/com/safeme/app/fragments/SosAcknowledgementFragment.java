package com.safeme.app.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.viewmodel.SosViewModel;

import java.util.Locale;

public class SosAcknowledgementFragment extends Fragment {

    private SosViewModel viewModel;
    private TextInputEditText etSosId, etNote;
    private TextView tvLatestLocation, tvStatusMessage;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sos_acknowledgement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SosViewModel.class);

        etSosId = view.findViewById(R.id.etAckSosId);
        etNote = view.findViewById(R.id.etAckNote);
        tvLatestLocation = view.findViewById(R.id.tvAckLatestLocation);
        tvStatusMessage = view.findViewById(R.id.tvAckStatusMessage);
        progressBar = view.findViewById(R.id.progressAck);

        if (getArguments() != null) {
            String sosId = getArguments().getString("sosId");
            if (sosId != null && !sosId.isEmpty()) {
                etSosId.setText(sosId);
                fetchSenderLocation(sosId);
            }
        }

        view.findViewById(R.id.btnAckFetchLocation).setOnClickListener(v -> {
            String sosId = getSosIdInput();
            if (!sosId.isEmpty()) {
                fetchSenderLocation(sosId);
            }
        });

        view.findViewById(R.id.btnAckResponding).setOnClickListener(v -> submitAck("RESPONDING"));
        view.findViewById(R.id.btnAckContactedHelp).setOnClickListener(v -> submitAck("CONTACTED_ASSISTANCE"));
        view.findViewById(R.id.btnAckCannotRespond).setOnClickListener(v -> submitAck("CANNOT_RESPOND"));
    }

    private String getSosIdInput() {
        String id = etSosId.getText() != null ? etSosId.getText().toString().trim() : "";
        if (id.isEmpty()) {
            etSosId.setError("Enter the SOS Alert ID from notification");
        }
        return id;
    }

    private void fetchSenderLocation(String sosId) {
        viewModel.getLatestSosLocation(sosId).observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                LocationUpdate loc = res.getData();
                tvLatestLocation.setText(String.format(Locale.US,
                        "Sender GPS: %.5f, %.5f (%s)",
                        loc.getLatitude(), loc.getLongitude(),
                        DateTimeUtils.getLocationFreshnessText(loc.getRecordedAt())));
                Uri mapUri = Uri.parse("geo:" + loc.getLatitude() + "," + loc.getLongitude()
                        + "?q=" + loc.getLatitude() + "," + loc.getLongitude() + "(SafeMe+SOS)");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                tvLatestLocation.setText(res.getMessage());
            }
        });
    }

    private void submitAck(String status) {
        String sosId = getSosIdInput();
        if (sosId.isEmpty()) return;
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        viewModel.submitAcknowledgement(sosId, status, note).observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (res.getStatus() == ApiResult.Status.SUCCESS) {
                progressBar.setVisibility(View.GONE);
                tvStatusMessage.setVisibility(View.VISIBLE);
                tvStatusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.safeme_success_green));
                tvStatusMessage.setText("Acknowledgement (" + status + ") recorded and sent to the SOS sender.");
                Toast.makeText(requireContext(), "Response recorded.", Toast.LENGTH_SHORT).show();
            } else {
                progressBar.setVisibility(View.GONE);
                tvStatusMessage.setVisibility(View.VISIBLE);
                tvStatusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.safeme_emergency_red));
                tvStatusMessage.setText(res.getMessage());
            }
        });
    }
}
