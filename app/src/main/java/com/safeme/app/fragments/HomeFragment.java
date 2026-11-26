package com.safeme.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.R;
import com.google.android.material.card.MaterialCardView;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.Journey;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.NetworkUtils;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.viewmodel.HomeViewModel;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private SwipeRefreshLayout swipeHome;
    private MaterialCardView cardError, cardActiveJourney;
    private TextView tvError, tvWelcome, tvDateTime, tvGpsStatus, tvNetStatus, tvContactsCount;
    private TextView tvActiveDest, tvActiveEta;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        swipeHome = view.findViewById(R.id.swipeHome);
        cardError = view.findViewById(R.id.cardHomeError);
        tvError = view.findViewById(R.id.tvHomeError);
        cardActiveJourney = view.findViewById(R.id.cardHomeActiveJourney);
        tvWelcome = view.findViewById(R.id.tvHomeWelcome);
        tvDateTime = view.findViewById(R.id.tvHomeDateTime);
        tvGpsStatus = view.findViewById(R.id.tvHomeGpsStatus);
        tvNetStatus = view.findViewById(R.id.tvHomeNetStatus);
        tvContactsCount = view.findViewById(R.id.tvHomeContactsCount);
        tvActiveDest = view.findViewById(R.id.tvHomeActiveDestination);
        tvActiveEta = view.findViewById(R.id.tvHomeActiveEta);

        view.findViewById(R.id.btnHomeRetry).setOnClickListener(v -> refreshDashboard());
        swipeHome.setOnRefreshListener(this::refreshDashboard);

        view.findViewById(R.id.btnHomeSosBig).setOnClickListener(v -> navigate(R.id.emergencySosFragment));
        view.findViewById(R.id.btnQuickStartJourney).setOnClickListener(v -> navigate(R.id.startJourneyFragment));
        view.findViewById(R.id.btnQuickEmergencySos).setOnClickListener(v -> navigate(R.id.emergencySosFragment));
        view.findViewById(R.id.btnQuickTrustedContacts).setOnClickListener(v -> navigate(R.id.trustedContactsFragment));
        view.findViewById(R.id.btnQuickSafetyCheckIn).setOnClickListener(v -> navigate(R.id.safetyCheckInFragment));
        view.findViewById(R.id.btnQuickCommunityMap).setOnClickListener(v -> navigate(R.id.communitySafetyMapFragment));
        view.findViewById(R.id.btnQuickHelplines).setOnClickListener(v -> navigate(R.id.helplinesFragment));
        view.findViewById(R.id.btnHomeOpenActiveJourney).setOnClickListener(v -> navigate(R.id.activeJourneyFragment));

        updateLocalDeviceIndicators();
        refreshDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLocalDeviceIndicators();
    }

    private void updateLocalDeviceIndicators() {
        if (getContext() == null) return;
        String cachedName = SecureTokenManager.getInstance(requireContext()).getUserName();
        tvWelcome.setText("Hello, " + (cachedName.isEmpty() ? "SafeMe User" : cachedName));
        tvDateTime.setText(DateTimeUtils.formatCurrentDateTime());

        boolean hasLoc = PermissionHelper.hasLocationPermission(requireContext());
        boolean gpsOn = PermissionHelper.isGpsEnabled(requireContext());
        if (!hasLoc) {
            tvGpsStatus.setText("GPS: No Perm");
        } else if (!gpsOn) {
            tvGpsStatus.setText("GPS: Off");
        } else {
            tvGpsStatus.setText("GPS: Ready");
        }

        boolean localMode = SecureTokenManager.getInstance(requireContext()).isLocalBackendMode();
        boolean online = NetworkUtils.isInternetAvailable(requireContext());
        if (localMode) {
            tvNetStatus.setText("API: Local DB");
        } else {
            tvNetStatus.setText(online ? "Net: Online" : "Net: Offline");
        }
    }

    private void refreshDashboard() {
        cardError.setVisibility(View.GONE);

        viewModel.loadUserProfile().observe(getViewLifecycleOwner(), result -> {
            if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null) {
                tvWelcome.setText("Hello, " + result.getData().getFullName());
            } else if (result.getStatus() == ApiResult.Status.ERROR) {
                showError(result.getMessage());
            }
            swipeHome.setRefreshing(false);
        });

        viewModel.loadActiveJourney().observe(getViewLifecycleOwner(), result -> {
            if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null
                    && "ACTIVE".equals(result.getData().getStatus())) {
                Journey j = result.getData();
                cardActiveJourney.setVisibility(View.VISIBLE);
                tvActiveDest.setText("To: " + j.getDestinationName());
                tvActiveEta.setText("Expected arrival: " + DateTimeUtils.formatTimeOnly(j.getExpectedArrivalTime()));
            } else {
                cardActiveJourney.setVisibility(View.GONE);
            }
        });

        viewModel.loadContacts().observe(getViewLifecycleOwner(), result -> {
            if (result.getStatus() == ApiResult.Status.SUCCESS && result.getData() != null) {
                tvContactsCount.setText("Contacts: " + result.getData().size());
            } else if (result.getStatus() == ApiResult.Status.ERROR) {
                tvContactsCount.setText("Contacts: --");
            }
        });
    }

    private void showError(String message) {
        if (message != null && !message.isEmpty()) {
            cardError.setVisibility(View.VISIBLE);
            tvError.setText(message);
        }
    }

    private void navigate(int destinationId) {
        NavHostFragment.findNavController(this).navigate(destinationId);
    }
}
