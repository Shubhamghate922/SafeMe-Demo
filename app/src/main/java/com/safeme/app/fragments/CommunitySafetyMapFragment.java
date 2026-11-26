package com.safeme.app.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.adapters.CommunityReportAdapter;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.CommunityReport;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.viewmodel.SafetyViewModel;

import java.util.List;

public class CommunitySafetyMapFragment extends Fragment implements OnMapReadyCallback {

    private SafetyViewModel viewModel;
    private CommunityReportAdapter adapter;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvError, tvEmpty;
    private double currentLat = 28.6139, currentLng = 77.2090;
    private List<CommunityReport> latestReports;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community_safety_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SafetyViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        tvError = view.findViewById(R.id.tvCommunityError);
        tvEmpty = view.findViewById(R.id.tvCommunityEmpty);
        TextView tvNotice = view.findViewById(R.id.tvMapKeyNoticeCommunity);

        if (BuildConfig.MAPS_API_KEY == null || BuildConfig.MAPS_API_KEY.isEmpty()
                || "YOUR_GOOGLE_MAPS_API_KEY".equals(BuildConfig.MAPS_API_KEY)) {
            tvNotice.setVisibility(View.VISIBLE);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.communitySafetyMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        RecyclerView rv = view.findViewById(R.id.rvCommunityReports);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommunityReportAdapter();
        rv.setAdapter(adapter);

        view.findViewById(R.id.fabAddCommunityReport).setOnClickListener(v -> showAddReportDialog());

        fetchCurrentLocation();
        loadReports();
    }

    private void fetchCurrentLocation() {
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        currentLat = loc.getLatitude();
                        currentLng = loc.getLongitude();
                        renderMarkers();
                    }
                });
            } catch (SecurityException ignored) {}
        }
    }

    private void loadReports() {
        tvError.setVisibility(View.GONE);
        viewModel.getCommunityReports().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                latestReports = res.getData();
                adapter.submitList(latestReports);
                tvEmpty.setVisibility(latestReports.isEmpty() ? View.VISIBLE : View.GONE);
                renderMarkers();
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(res.getMessage());
            }
        });
    }

    private void showAddReportDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_report, null);
        Spinner spinner = dialogView.findViewById(R.id.spinnerCommunityCategory);
        TextInputEditText etLoc = dialogView.findViewById(R.id.etDialogCommunityLocation);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etDialogCommunityDesc);

        String[] categories = {"POOR_LIGHTING", "ROAD_HAZARD", "HARASSMENT", "UNSAFE_AREA", "OTHER"};
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));

        new AlertDialog.Builder(requireContext())
                .setTitle("Report Area Safety Hazard")
                .setView(dialogView)
                .setPositiveButton("Submit", (d, w) -> {
                    String cat = spinner.getSelectedItem().toString();
                    String loc = etLoc.getText() != null ? etLoc.getText().toString().trim() : "";
                    String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                    if (loc.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(requireContext(), "Enter location and description.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CommunityReport report = new CommunityReport(cat, desc, loc, currentLat, currentLng);
                    viewModel.submitCommunityReport(report).observe(getViewLifecycleOwner(), res -> {
                        if (res.getStatus() == ApiResult.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Advisory submitted for moderation.", Toast.LENGTH_SHORT).show();
                            loadReports();
                        } else if (res.getStatus() == ApiResult.Status.ERROR) {
                            Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        renderMarkers();
    }

    private void renderMarkers() {
        if (googleMap == null) return;
        googleMap.clear();
        LatLng center = new LatLng(currentLat, currentLng);
        if (latestReports != null) {
            for (CommunityReport r : latestReports) {
                LatLng pos = new LatLng(r.getLatitude(), r.getLongitude());
                googleMap.addMarker(new MarkerOptions().position(pos).title(r.getCategory()).snippet(r.getLocationName()));
            }
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13f));
    }
}
