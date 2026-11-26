package com.safeme.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.safeme.app.activities.FakeCallActivity;

public class SafetyHubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_safety_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.cardHubSos).setOnClickListener(v -> nav(R.id.emergencySosFragment));
        view.findViewById(R.id.cardHubContacts).setOnClickListener(v -> nav(R.id.trustedContactsFragment));
        view.findViewById(R.id.cardHubRespondSos).setOnClickListener(v -> nav(R.id.sosAcknowledgementFragment));
        view.findViewById(R.id.cardHubCommunityMap).setOnClickListener(v -> nav(R.id.communitySafetyMapFragment));
        view.findViewById(R.id.cardHubIncidentReport).setOnClickListener(v -> nav(R.id.incidentReportFragment));
        view.findViewById(R.id.cardHubHelplines).setOnClickListener(v -> nav(R.id.helplinesFragment));
        view.findViewById(R.id.cardHubFakeCall).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FakeCallActivity.class)));
        view.findViewById(R.id.cardHubVoiceEmergency).setOnClickListener(v -> nav(R.id.voiceEmergencyFragment));
        view.findViewById(R.id.cardHubEscalation).setOnClickListener(v -> nav(R.id.emergencyEscalationFragment));
    }

    private void nav(int destinationId) {
        NavHostFragment.findNavController(this).navigate(destinationId);
    }
}
