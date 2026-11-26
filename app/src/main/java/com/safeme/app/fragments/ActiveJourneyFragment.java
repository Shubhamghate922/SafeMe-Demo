package com.safeme.app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.Journey;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.services.CheckInTimerManager;
import com.safeme.app.services.JourneyLocationService;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.viewmodel.JourneyViewModel;

import java.util.Locale;

public class ActiveJourneyFragment extends Fragment implements OnMapReadyCallback {

    private JourneyViewModel viewModel;
    private GoogleMap googleMap;
    private Journey activeJourney;
    private LocationUpdate latestFix;

    private TextView tvFreshness, tvStatus, tvDestination, tvCoordinates, tvEta, tvDistance, tvNextCheckIn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_journey, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JourneyViewModel.class);

        tvFreshness = view.findViewById(R.id.tvActiveLocationFreshness);
        tvStatus = view.findViewById(R.id.tvActiveJourneyStatus);
        tvDestination = view.findViewById(R.id.tvActiveDestination);
        tvCoordinates = view.findViewById(R.id.tvActiveCoordinates);
        tvEta = view.findViewById(R.id.tvActiveEtaAndDuration);
        tvDistance = view.findViewById(R.id.tvActiveDistanceRemaining);
        tvNextCheckIn = view.findViewById(R.id.tvActiveNextCheckIn);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.activeJourneyMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (getArguments() != null && getArguments().containsKey("journey")) {
            activeJourney = (Journey) getArguments().getSerializable("journey");
            bindJourney(activeJourney);
        } else {
            loadActiveJourneyFromServer();
        }

        JourneyLocationService.getLatestLocationLiveData().observe(getViewLifecycleOwner(), update -> {
            if (update != null) {
                latestFix = update;
                updateLocationDisplay(update);
            }
        });

        CheckInTimerManager.getInstance().getSecondsUntilCheckIn().observe(getViewLifecycleOwner(), secs -> {
            if (secs != null && secs > 0) {
                tvNextCheckIn.setText("Next safety check-in in: " + DateTimeUtils.formatCountdownSeconds(secs));
            } else if (activeJourney != null) {
                tvNextCheckIn.setText("Safety check-in due now — tap Check In Now");
            }
        });

        view.findViewById(R.id.btnActiveCheckInNow).setOnClickListener(v -> submitCheckInNow());
        view.findViewById(R.id.btnActiveExtend).setOnClickListener(v -> extendJourneyBy15Min());
        view.findViewById(R.id.btnActiveChangeDest).setOnClickListener(v -> showChangeDestinationDialog());
        view.findViewById(R.id.btnActiveShareJourney).setOnClickListener(v -> shareJourneyStatus());
        view.findViewById(R.id.btnActiveEndJourney).setOnClickListener(v -> confirmEndJourney());
        view.findViewById(R.id.btnActiveEmergencySos).setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (activeJourney != null) args.putString("journeyId", activeJourney.getId());
            NavHostFragment.findNavController(this).navigate(R.id.emergencySosFragment, args);
        });
    }

    private void loadActiveJourneyFromServer() {
        viewModel.getActiveJourney().observe(getViewLifecycleOwner(), result -> {
            if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null) {
                activeJourney = result.getData();
                bindJourney(activeJourney);
            } else if (result.getStatus() == ApiResult.Status.ERROR) {
                tvDestination.setText("No active journey");
                tvEta.setText(result.getMessage());
            }
        });
    }

    private void bindJourney(Journey j) {
        if (j == null) return;
        tvStatus.setText(j.getStatus() != null ? j.getStatus() : "ACTIVE");
        tvDestination.setText("Destination: " + j.getDestinationName());
        tvEta.setText("Expected arrival: " + DateTimeUtils.formatTimeOnly(j.getExpectedArrivalTime())
                + " • Mode: " + (j.getTransportMode() != null ? j.getTransportMode() : "N/A"));
        if (j.getLastLocationTimestamp() > 0) {
            tvFreshness.setText(DateTimeUtils.getLocationFreshnessText(j.getLastLocationTimestamp()));
            tvCoordinates.setText(String.format(Locale.US, "Lat %.5f, Lng %.5f", j.getLastLatitude(), j.getLastLongitude()));
        }
        updateMapMarkers();
    }

    private void updateLocationDisplay(LocationUpdate update) {
        tvFreshness.setText(DateTimeUtils.getLocationFreshnessText(update.getRecordedAt()));
        tvCoordinates.setText(String.format(Locale.US,
                "GPS: %.5f, %.5f (±%.0fm)", update.getLatitude(), update.getLongitude(), update.getAccuracyMeters()));
        if (activeJourney != null && (activeJourney.getDestinationLatitude() != 0.0 || activeJourney.getDestinationLongitude() != 0.0)) {
            float[] res = new float[1];
            Location.distanceBetween(update.getLatitude(), update.getLongitude(),
                    activeJourney.getDestinationLatitude(), activeJourney.getDestinationLongitude(), res);
            tvDistance.setText(String.format(Locale.US, "Estimated distance remaining: %.2f km", res[0] / 1000f));
        }
        updateMapMarkers();
    }

    private void submitCheckInNow() {
        if (activeJourney == null) return;
        if (!CheckInTimerManager.getInstance().tryLockCheckInSubmission()) {
            Toast.makeText(requireContext(), "Check-in already submitted for this window.", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = latestFix != null ? latestFix.getLatitude() : activeJourney.getStartLatitude();
        double lng = latestFix != null ? latestFix.getLongitude() : activeJourney.getStartLongitude();
        viewModel.submitCheckIn(activeJourney.getId(), "SAFE", lat, lng, "Confirmed safe during active journey")
                .observe(getViewLifecycleOwner(), res -> {
                    if (res.getStatus() == ApiResult.Status.SUCCESS) {
                        int interval = Math.max(5, activeJourney.getCheckInIntervalMinutes());
                        CheckInTimerManager.getInstance().scheduleNextCheckIn(
                                requireContext(), activeJourney.getId(),
                                System.currentTimeMillis() + (interval * 60_000L));
                        Toast.makeText(requireContext(), "Safety check-in recorded.", Toast.LENGTH_SHORT).show();
                    } else if (res.getStatus() == ApiResult.Status.ERROR) {
                        Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void extendJourneyBy15Min() {
        if (activeJourney == null) return;
        viewModel.extendJourney(activeJourney.getId(), 15).observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                activeJourney = res.getData();
                bindJourney(activeJourney);
                Toast.makeText(requireContext(), "Journey extended by 15 minutes.", Toast.LENGTH_SHORT).show();
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangeDestinationDialog() {
        if (activeJourney == null) return;
        EditText input = new EditText(requireContext());
        input.setHint("New destination address");
        input.setText(activeJourney.getDestinationName());
        new AlertDialog.Builder(requireContext())
                .setTitle("Change Destination")
                .setView(input)
                .setPositiveButton("Update", (d, w) -> {
                    String newDest = input.getText().toString().trim();
                    if (!newDest.isEmpty()) {
                        viewModel.changeDestination(activeJourney.getId(), newDest,
                                activeJourney.getDestinationLatitude(), activeJourney.getDestinationLongitude())
                                .observe(getViewLifecycleOwner(), res -> {
                                    if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                                        activeJourney = res.getData();
                                        bindJourney(activeJourney);
                                        Toast.makeText(requireContext(), "Destination updated.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareJourneyStatus() {
        if (activeJourney == null) return;
        double lat = latestFix != null ? latestFix.getLatitude() : activeJourney.getStartLatitude();
        double lng = latestFix != null ? latestFix.getLongitude() : activeJourney.getStartLongitude();
        String msg = "SafeMe Journey Update: Heading to " + activeJourney.getDestinationName()
                + ". Expected arrival: " + DateTimeUtils.formatTimeOnly(activeJourney.getExpectedArrivalTime())
                + ". Live Map: https://maps.google.com/?q=" + lat + "," + lng;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(Intent.createChooser(share, "Share Journey Status"));
    }

    private void confirmEndJourney() {
        if (activeJourney == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("End Monitored Journey?")
                .setMessage("Confirm you have arrived safely. Live GPS monitoring will stop immediately.")
                .setPositiveButton("Yes, I Arrived Safely", (d, w) -> completeJourney())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeJourney() {
        viewModel.completeJourney(activeJourney.getId()).observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS) {
                JourneyLocationService.stopMonitoring(requireContext());
                CheckInTimerManager.getInstance().stopTimer();
                Toast.makeText(requireContext(), "Journey completed safely.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigate(R.id.homeFragment);
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        updateMapMarkers();
    }

    private void updateMapMarkers() {
        if (googleMap == null) return;
        googleMap.clear();
        LatLng pos = latestFix != null
                ? new LatLng(latestFix.getLatitude(), latestFix.getLongitude())
                : (activeJourney != null ? new LatLng(activeJourney.getStartLatitude(), activeJourney.getStartLongitude())
                : new LatLng(28.6139, 77.2090));
        googleMap.addMarker(new MarkerOptions().position(pos).title("Current Location"));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }
}
