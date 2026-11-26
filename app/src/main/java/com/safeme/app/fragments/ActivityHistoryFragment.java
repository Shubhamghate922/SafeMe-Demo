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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.R;
import com.google.android.material.chip.ChipGroup;
import com.safeme.app.adapters.ActivityHistoryAdapter;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.ActivityRecord;
import com.safeme.app.models.IncidentReport;
import com.safeme.app.models.Journey;
import com.safeme.app.models.SosAlert;
import com.safeme.app.viewmodel.SafetyViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityHistoryFragment extends Fragment {

    private SafetyViewModel viewModel;
    private ActivityHistoryAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvError, tvEmpty;
    private ChipGroup chipFilter;

    private final List<ActivityRecord> allRecords = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SafetyViewModel.class);

        swipeRefresh = view.findViewById(R.id.swipeActivity);
        tvError = view.findViewById(R.id.tvActivityError);
        tvEmpty = view.findViewById(R.id.tvActivityEmpty);
        chipFilter = view.findViewById(R.id.chipGroupActivityFilter);

        RecyclerView rv = view.findViewById(R.id.rvActivityHistory);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ActivityHistoryAdapter();
        rv.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadAllHistory);
        chipFilter.setOnCheckedStateChangeListener((g, ids) -> applyFilter());

        loadAllHistory();
    }

    private void loadAllHistory() {
        tvError.setVisibility(View.GONE);
        allRecords.clear();
        swipeRefresh.setRefreshing(true);

        viewModel.getJourneyHistory().observe(getViewLifecycleOwner(), res -> {
            swipeRefresh.setRefreshing(false);
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                for (Journey j : res.getData()) {
                    allRecords.add(new ActivityRecord(
                            j.getId(), "JOURNEY",
                            "Journey to " + j.getDestinationName(),
                            "From " + (j.getStartLocationName() != null ? j.getStartLocationName() : "Start"),
                            j.getStatus() != null ? j.getStatus() : "COMPLETED",
                            j.getStartedAt()
                    ));
                }
                sortAndFilter();
            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(res.getMessage());
            }
        });

        viewModel.getSosHistory().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                for (SosAlert s : res.getData()) {
                    allRecords.add(new ActivityRecord(
                            s.getId(), "SOS",
                            "Emergency SOS (" + (s.getTriggerSource() != null ? s.getTriggerSource() : "MANUAL") + ")",
                            s.getAddressLabel() != null ? s.getAddressLabel() : "GPS Alert",
                            s.getStatus() != null ? s.getStatus() : "RESOLVED",
                            s.getCreatedAt()
                    ));
                }
                sortAndFilter();
            }
        });

        viewModel.getIncidentReports().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                for (IncidentReport r : res.getData()) {
                    allRecords.add(new ActivityRecord(
                            r.getId(), "INCIDENT",
                            "Incident: " + r.getCategory(),
                            r.getLocationName() != null ? r.getLocationName() : "",
                            r.getStatus() != null ? r.getStatus() : "SUBMITTED",
                            r.getCreatedAt()
                    ));
                }
                sortAndFilter();
            }
        });
    }

    private void sortAndFilter() {
        Collections.sort(allRecords, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        applyFilter();
    }

    private void applyFilter() {
        int id = chipFilter.getCheckedChipId();
        String targetCat = null;
        if (id == R.id.chipActJourneys) targetCat = "JOURNEY";
        else if (id == R.id.chipActSos) targetCat = "SOS";
        else if (id == R.id.chipActIncidents) targetCat = "INCIDENT";

        List<ActivityRecord> filtered = new ArrayList<>();
        for (ActivityRecord rec : allRecords) {
            if (targetCat == null || targetCat.equals(rec.getCategory())) {
                filtered.add(rec);
            }
        }
        adapter.submitList(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
