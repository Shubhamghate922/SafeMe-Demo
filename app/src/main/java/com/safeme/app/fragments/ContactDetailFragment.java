package com.safeme.app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

import com.example.R;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.api.ApiResult;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.utils.ValidationUtils;
import com.safeme.app.viewmodel.ContactsViewModel;

public class ContactDetailFragment extends Fragment {

    private ContactsViewModel viewModel;
    private TrustedContact contact;
    private TextView tvName, tvPhone, tvRelation, tvType, tvVerif, tvPriority, tvPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

        tvName = view.findViewById(R.id.tvDetailName);
        tvPhone = view.findViewById(R.id.tvDetailPhone);
        tvRelation = view.findViewById(R.id.tvDetailRelation);
        tvType = view.findViewById(R.id.tvDetailType);
        tvVerif = view.findViewById(R.id.tvDetailVerification);
        tvPriority = view.findViewById(R.id.tvDetailPriority);
        tvPrefs = view.findViewById(R.id.tvDetailPrefs);

        if (getArguments() != null && getArguments().containsKey("contact")) {
            contact = (TrustedContact) getArguments().getSerializable("contact");
        }
        bindContact();

        view.findViewById(R.id.btnDetailCall).setOnClickListener(v -> {
            if (contact != null && contact.getPhoneNumber() != null) {
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.getPhoneNumber()));
                startActivity(dial);
            }
        });
        view.findViewById(R.id.btnDetailEdit).setOnClickListener(v -> showEditDialog());
        view.findViewById(R.id.btnDetailRemove).setOnClickListener(v -> confirmRemove());
    }

    private void bindContact() {
        if (contact == null) return;
        tvName.setText(contact.getName());
        tvPhone.setText(contact.getPhoneNumber());
        tvRelation.setText(contact.getRelationship() != null && !contact.getRelationship().isEmpty()
                ? contact.getRelationship() : "Trusted Contact");
        tvType.setText("Account Type: " + ("REGISTERED_USER".equals(contact.getContactType())
                ? "Registered SafeMe User" : "External Emergency Contact"));
        tvVerif.setText("Verification & Mutual Consent: "
                + (contact.getVerificationStatus() != null ? contact.getVerificationStatus() : "PENDING")
                + (contact.isMutualConsentGranted() ? " (Mutual consent granted)" : " (Pending mutual link)"));
        tvPriority.setText("Escalation Priority: Level " + Math.max(1, contact.getPriority()));
        tvPrefs.setText("Channels: "
                + (contact.isNotifyPush() ? "Push " : "")
                + (contact.isNotifySms() ? "SMS " : "")
                + (contact.isShareLiveLocation() ? "• Live GPS Sharing Enabled" : "• Live GPS Disabled"));
    }

    private void showEditDialog() {
        if (contact == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trusted_contact, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etDialogContactName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogContactPhone);
        TextInputEditText etRelation = dialogView.findViewById(R.id.etDialogContactRelation);
        RadioButton rbRegistered = dialogView.findViewById(R.id.rbRegisteredUser);
        RadioButton rbExternal = dialogView.findViewById(R.id.rbExternalInvite);
        RadioButton rbP1 = dialogView.findViewById(R.id.rbPriority1);
        RadioButton rbP2 = dialogView.findViewById(R.id.rbPriority2);
        RadioButton rbP3 = dialogView.findViewById(R.id.rbPriority3);
        CheckBox cbSms = dialogView.findViewById(R.id.cbDialogNotifySms);
        CheckBox cbPush = dialogView.findViewById(R.id.cbDialogNotifyPush);
        CheckBox cbLoc = dialogView.findViewById(R.id.cbDialogShareLocation);

        etName.setText(contact.getName());
        etPhone.setText(contact.getPhoneNumber());
        etRelation.setText(contact.getRelationship());
        if ("EXTERNAL_INVITE".equals(contact.getContactType())) rbExternal.setChecked(true);
        else rbRegistered.setChecked(true);

        if (contact.getPriority() == 2) rbP2.setChecked(true);
        else if (contact.getPriority() == 3) rbP3.setChecked(true);
        else rbP1.setChecked(true);

        cbSms.setChecked(contact.isNotifySms());
        cbPush.setChecked(contact.isNotifyPush());
        cbLoc.setChecked(contact.isShareLiveLocation());

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Trusted Contact")
                .setView(dialogView)
                .setPositiveButton("Update", (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String rel = etRelation.getText() != null ? etRelation.getText().toString().trim() : "";
                    if (!ValidationUtils.isValidFullName(name) || !ValidationUtils.isValidMobile(phone)) {
                        Toast.makeText(requireContext(), "Invalid name or phone.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    contact.setName(name);
                    contact.setPhoneNumber(ValidationUtils.normalizeMobile(phone));
                    contact.setRelationship(rel);
                    contact.setContactType(rbRegistered.isChecked() ? "REGISTERED_USER" : "EXTERNAL_INVITE");
                    contact.setPriority(rbP1.isChecked() ? 1 : (rbP2.isChecked() ? 2 : 3));
                    contact.setNotifySms(cbSms.isChecked());
                    contact.setNotifyPush(cbPush.isChecked());
                    contact.setShareLiveLocation(cbLoc.isChecked());

                    viewModel.updateContact(contact.getId(), contact).observe(getViewLifecycleOwner(), res -> {
                        if (res.getStatus() == ApiResult.Status.SUCCESS && res.getData() != null) {
                            contact = res.getData();
                            bindContact();
                            Toast.makeText(requireContext(), "Contact updated.", Toast.LENGTH_SHORT).show();
                        } else if (res.getStatus() == ApiResult.Status.ERROR) {
                            Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRemove() {
        if (contact == null || contact.getId() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Trusted Contact?")
                .setMessage("Remove " + contact.getName() + " from your emergency contacts?")
                .setPositiveButton("Remove", (d, w) ->
                        viewModel.deleteContact(contact.getId()).observe(getViewLifecycleOwner(), res -> {
                            if (res.getStatus() == ApiResult.Status.SUCCESS) {
                                Toast.makeText(requireContext(), "Contact removed.", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(this).popBackStack();
                            } else if (res.getStatus() == ApiResult.Status.ERROR) {
                                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
