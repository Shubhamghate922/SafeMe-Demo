package com.safeme.app.fragments;

import android.Manifest;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.BuildConfig;
import com.example.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.adapters.SelectContactAdapter;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.Journey;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.services.CheckInTimerManager;
import com.safeme.app.services.JourneyLocationService;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.viewmodel.JourneyViewModel;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class StartJourneyFragment extends Fragment implements OnMapReadyCallback {

    private JourneyViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap googleMap;
    private SelectContactAdapter contactAdapter;

    private TextInputEditText etStart, etDest, etDuration, etArrival;
    private ChipGroup chipTransport, chipInterval;
    private MaterialSwitch switchMonitoring;
    private TextView tvNoContacts, tvSummary, tvError, tvMapKeyNotice;
    private ProgressBar progressBar;

    private double startLat = 0.0, startLng = 0.0;
    private double destLat = 0.0, destLng = 0.0;
    private long expectedArrivalMillis = 0L;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (PermissionHelper.hasLocationPermission(requireContext())) {
                    fetchCurrentGps();
                } else {
                    Toast.makeText(requireContext(), "Location permission is required for GPS tracking.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start_journey, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JourneyViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        etStart = view.findViewById(R.id.etStartLocation);
        etDest = view.findViewById(R.id.etDestination);
        etDuration = view.findViewById(R.id.etDurationMinutes);
        etArrival = view.findViewById(R.id.etExpectedArrival);
        chipTransport = view.findViewById(R.id.chipGroupTransport);
        chipInterval = view.findViewById(R.id.chipGroupCheckInInterval);
        switchMonitoring = view.findViewById(R.id.switchJourneyMonitoring);
        tvNoContacts = view.findViewById(R.id.tvNoContactsJourneyHint);
        tvSummary = view.findViewById(R.id.tvJourneySummary);
        tvError = view.findViewById(R.id.tvStartJourneyError);
        tvMapKeyNotice = view.findViewById(R.id.tvMapKeyNoticeStart);
        progressBar = view.findViewById(R.id.progressStartJourney);

        if (BuildConfig.MAPS_API_KEY == null || BuildConfig.MAPS_API_KEY.isEmpty()
                || "YOUR_GOOGLE_MAPS_API_KEY".equals(BuildConfig.MAPS_API_KEY)) {
            tvMapKeyNotice.setVisibility(View.VISIBLE);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapPreviewContainer);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        RecyclerView rvContacts = view.findViewById(R.id.rvSelectJourneyContacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        contactAdapter = new SelectContactAdapter();
        contactAdapter.setOnSelectionChangedListener(this::updateSummary);
        rvContacts.setAdapter(contactAdapter);

        // Apply saved default check-in interval
        int defInterval = SecureTokenManager.getInstance(requireContext()).getDefaultCheckInIntervalMinutes();
        if (defInterval == 5) chipInterval.check(R.id.chipInterval5);
        else if (defInterval == 30) chipInterval.check(R.id.chipInterval30);
        else chipInterval.check(R.id.chipInterval15);

        updateArrivalFromDuration(30);

        etDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int mins = Integer.parseInt(s.toString().trim());
                    if (mins > 0) updateArrivalFromDuration(mins);
                } catch (NumberFormatException ignored) {}
            }
        });

        etArrival.setOnClickListener(v -> openArrivalTimePicker());
        view.findViewById(R.id.btnUseCurrentGps).setOnClickListener(v -> requestOrFetchLocation());
        view.findViewById(R.id.btnSearchDestination).setOnClickListener(v -> geocodeDestination());
        view.findViewById(R.id.btnStartJourney).setOnClickListener(v -> submitAndStartJourney());

        chipTransport.setOnCheckedStateChangeListener((g, ids) -> updateSummary());
        chipInterval.setOnCheckedStateChangeListener((g, ids) -> updateSummary());

        loadContacts();
        requestOrFetchLocation();
        updateSummary();
    }

    private void loadContacts() {
        viewModel.loadTrustedContacts().observe(getViewLifecycleOwner(), result -> {
            if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null) {
                contactAdapter.submitList(result.getData());
                tvNoContacts.setVisibility(result.getData().isEmpty() ? View.VISIBLE : View.GONE);
            } else if (result.getStatus() == ApiResult.Status.ERROR) {
                tvNoContacts.setVisibility(View.VISIBLE);
                tvNoContacts.setText(result.getMessage());
            }
        });
    }

    private void requestOrFetchLocation() {
        if (!PermissionHelper.hasLocationPermission(requireContext())) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        fetchCurrentGps();
    }

    private void fetchCurrentGps() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (!isAdded()) return;
                if (location != null) {
                    startLat = location.getLatitude();
                    startLng = location.getLongitude();
                    etStart.setText(String.format(Locale.US, "Current GPS (%.5f, %.5f)", startLat, startLng));
                    refreshMapPreview();
                } else {
                    etStart.setText("GPS acquired pending — enter start address if indoors");
                }
            });
        } catch (SecurityException ignored) {}
    }

    private void geocodeDestination() {
        String query = etDest.getText() != null ? etDest.getText().toString().trim() : "";
        if (query.isEmpty()) {
            etDest.setError("Enter a destination address or landmark");
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        destLat = addr.getLatitude();
                        destLng = addr.getLongitude();
                        refreshMapPreview();
                        updateSummary();
                        Toast.makeText(requireContext(), "Destination located on map.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Could not geocode destination; coordinates will default until resolved.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Geocoding unavailable offline.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateArrivalFromDuration(int minutes) {
        expectedArrivalMillis = System.currentTimeMillis() + (minutes * 60_000L);
        etArrival.setText(DateTimeUtils.formatTimeOnly(expectedArrivalMillis));
        updateSummary();
    }

    private void openArrivalTimePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(expectedArrivalMillis > 0 ? expectedArrivalMillis : System.currentTimeMillis());
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(Calendar.HOUR_OF_DAY, hourOfDay);
            chosen.set(Calendar.MINUTE, minute);
            if (chosen.getTimeInMillis() <= System.currentTimeMillis()) {
                chosen.add(Calendar.DAY_OF_YEAR, 1);
            }
            expectedArrivalMillis = chosen.getTimeInMillis();
            int diffMins = Math.max(5, (int) ((expectedArrivalMillis - System.currentTimeMillis()) / 60_000L));
            etDuration.setText(String.valueOf(diffMins));
            etArrival.setText(DateTimeUtils.formatTimeOnly(expectedArrivalMillis));
            updateSummary();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private String getSelectedTransportMode() {
        int id = chipTransport.getCheckedChipId();
        if (id == R.id.chipBicycle) return "BICYCLE";
        if (id == R.id.chipTwoWheeler) return "TWO_WHEELER";
        if (id == R.id.chipCar) return "CAR";
        if (id == R.id.chipPublicTransport) return "PUBLIC_TRANSPORT";
        if (id == R.id.chipOther) return "OTHER";
        return "WALKING";
    }

    private int getSelectedCheckInInterval() {
        int id = chipInterval.getCheckedChipId();
        if (id == R.id.chipInterval5) return 5;
        if (id == R.id.chipInterval30) return 30;
        return 15;
    }

    private void updateSummary() {
        String dest = etDest.getText() != null ? etDest.getText().toString().trim() : "";
        if (dest.isEmpty()) dest = "Pending destination";
        tvSummary.setText("Summary: To " + dest
                + " via " + getSelectedTransportMode().replace('_', ' ')
                + " • Arrival " + DateTimeUtils.formatTimeOnly(expectedArrivalMillis)
                + " • Check-in every " + getSelectedCheckInInterval() + "m"
                + " • " + contactAdapter.getSelectedCount() + " contact(s) selected");
    }

    private void submitAndStartJourney() {
        tvError.setVisibility(View.GONE);
        String startName = etStart.getText() != null ? etStart.getText().toString().trim() : "";
        String destName = etDest.getText() != null ? etDest.getText().toString().trim() : "";
        if (startName.isEmpty()) startName = "Current Location";
        if (destName.isEmpty()) {
            etDest.setError("Destination is required");
            return;
        }
        int durationMins = 30;
        try {
            durationMins = Integer.parseInt(etDuration.getText() != null ? etDuration.getText().toString().trim() : "30");
        } catch (NumberFormatException ignored) {}
        if (durationMins < 1) {
            etDuration.setError("Enter valid duration");
            return;
        }

        List<String> contactIds = contactAdapter.getSelectedContactIds();
        if (contactIds.isEmpty()) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Select at least one trusted contact to notify before starting a journey.");
            return;
        }

        JourneyDtos.CreateJourneyRequest req = new JourneyDtos.CreateJourneyRequest(
                startName, startLat, startLng,
                destName, destLat, destLng,
                durationMins, expectedArrivalMillis,
                getSelectedTransportMode(),
                contactIds,
                getSelectedCheckInInterval(),
                switchMonitoring.isChecked()
        );

        viewModel.createAndStartJourney(req).observe(getViewLifecycleOwner(), result -> {
            if (result.getStatus() == ApiResult.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null) {
                progressBar.setVisibility(View.GONE);
                Journey started = result.getData();
                if (switchMonitoring.isChecked() && PermissionHelper.hasLocationPermission(requireContext())) {
                    JourneyLocationService.startForJourney(requireContext(), started.getId(), started.getDestinationName());
                }
                long nextCheckInMs = started.getNextCheckInDueAt() > 0
                        ? started.getNextCheckInDueAt()
                        : System.currentTimeMillis() + (getSelectedCheckInInterval() * 60_000L);
                CheckInTimerManager.getInstance().scheduleNextCheckIn(requireContext(), started.getId(), nextCheckInMs);

                Bundle args = new Bundle();
                args.putSerializable("journey", started);
                NavHostFragment.findNavController(this).navigate(R.id.activeJourneyFragment, args);
            } else {
                progressBar.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(result.getMessage() != null ? result.getMessage() : "Failed to start journey.");
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        refreshMapPreview();
    }

    private void refreshMapPreview() {
        if (googleMap == null) return;
        googleMap.clear();
        LatLng start = (startLat != 0.0 || startLng != 0.0) ? new LatLng(startLat, startLng) : new LatLng(28.6139, 77.2090);
        googleMap.addMarker(new MarkerOptions().position(start).title("Start"));
        if (destLat != 0.0 || destLng != 0.0) {
            LatLng dest = new LatLng(destLat, destLng);
            googleMap.addMarker(new MarkerOptions().position(dest).title("Destination"));
            googleMap.addPolyline(new PolylineOptions().add(start, dest));
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 13f));
    }
}
