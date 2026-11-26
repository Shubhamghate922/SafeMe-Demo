package com.safeme.app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.safeme.app.adapters.HelplineAdapter;
import com.safeme.app.models.HelplineItem;

import java.util.Arrays;
import java.util.List;

public class HelplinesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_helplines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<HelplineItem> helplines = Arrays.asList(
                new HelplineItem("Emergency Response Support (ERSS)", "112",
                        "Unified national emergency helpline for Police, Fire, and Medical assistance across India.", "24x7 National Service"),
                new HelplineItem("Police Control Room", "100",
                        "Immediate police emergency dispatch and law enforcement assistance.", "24x7 Service"),
                new HelplineItem("Ambulance / Medical Emergency", "108",
                        "Emergency medical ambulance and trauma response service.", "24x7 Service"),
                new HelplineItem("Women Helpline (All India)", "1091",
                        "Dedicated women safety, distress assistance, and emergency support helpline.", "24x7 Women Support"),
                new HelplineItem("National Commission for Women", "7827170170",
                        "24x7 helpline for women facing violence or harassment.", "24x7 Helpline"),
                new HelplineItem("Childline India", "1098",
                        "Emergency outreach and protection service for children in distress.", "24x7 Service"),
                new HelplineItem("National Cyber Crime Helpline", "1930",
                        "Report online harassment, financial cyber fraud, and digital abuse.", "24x7 Cyber Cell")
        );

        RecyclerView rv = view.findViewById(R.id.rvHelplines);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new HelplineAdapter(helplines, this::confirmAndDial));
    }

    private void confirmAndDial(HelplineItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Dial " + item.getTitle() + "?")
                .setMessage("This will open your phone dialer with number " + item.getNumber()
                        + ". You can confirm the call directly in your dialer.")
                .setPositiveButton("Open Dialer (" + item.getNumber() + ")", (d, w) -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.getNumber()));
                    startActivity(dialIntent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
