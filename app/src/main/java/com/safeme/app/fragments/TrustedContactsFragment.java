package com.safeme.app.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.R;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.adapters.TrustedContactAdapter;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.ContactsViewModel;

public class TrustedContactsFragment extends Fragment {

    private ContactsViewModel viewModel;
    private TrustedContactAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvError, tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trusted_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

        swipeRefresh = view.findViewById(R.id.swipeContacts);
        tvError = view.findViewById(R.id.tvContactsError);
        tvEmpty = view.findViewById(R.id.tvContactsEmpty);

        RecyclerView rv = view.findViewById(R.id.rvTrustedContacts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrustedContactAdapter(contact -> {
            Bundle args = new Bundle();
            args.putSerializable("contact", contact);
            NavHostFragment.findNavController(this).navigate(R.id.contactDetailFragment, args);
        });
        rv.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadContacts);
        view.findViewById(R.id.fabAddContact).setOnClickListener(v -> showAddContactDialog());

        loadContacts();
    }

    private void loadContacts() {
        tvError.setVisibility(View.GONE);
        viewModel.getContacts().observe(getViewLifecycleOwner(), res -> {
            if (res.getStatus() == ApiResult.Status.LOADING) {
                swipeRefresh.setRefreshing(true);
            } else if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                swipeRefresh.setRefreshing(false);
                adapter.submitList(res.getData());
                tvEmpty.setVisibility(res.getData().isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                swipeRefresh.setRefreshing(false);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(res.getMessage());
            }
        });
    }

    private void showAddContactDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trusted_contact, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etDialogContactName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogContactPhone);
        TextInputEditText etRelation = dialogView.findViewById(R.id.etDialogContactRelation);
        RadioButton rbRegistered = dialogView.findViewById(R.id.rbRegisteredUser);
        RadioButton rbP1 = dialogView.findViewById(R.id.rbPriority1);
        RadioButton rbP2 = dialogView.findViewById(R.id.rbPriority2);
        CheckBox cbSms = dialogView.findViewById(R.id.cbDialogNotifySms);
        CheckBox cbPush = dialogView.findViewById(R.id.cbDialogNotifyPush);
        CheckBox cbLoc = dialogView.findViewById(R.id.cbDialogShareLocation);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Trusted Contact")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String rel = etRelation.getText() != null ? etRelation.getText().toString().trim() : "";

                    if (!ValidationUtils.isValidFullName(name) || !ValidationUtils.isValidMobile(phone)) {
                        Toast.makeText(requireContext(), "Enter a valid name and 10-13 digit mobile number.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String type = rbRegistered.isChecked() ? "REGISTERED_USER" : "EXTERNAL_INVITE";
                    int priority = rbP1.isChecked() ? 1 : (rbP2.isChecked() ? 2 : 3);
                    TrustedContact contact = new TrustedContact(
                            name, ValidationUtils.normalizeMobile(phone), rel, type, priority,
                            cbSms.isChecked(), cbPush.isChecked(), cbLoc.isChecked()
                    );
                    viewModel.addContact(contact).observe(getViewLifecycleOwner(), res -> {
                        if (res.getStatus() == ApiResult.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Trusted contact added.", Toast.LENGTH_SHORT).show();
                            loadContacts();
                        } else if (res.getStatus() == ApiResult.Status.ERROR) {
                            Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
