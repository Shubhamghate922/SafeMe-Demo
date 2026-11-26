package com.safeme.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.repository.ContactsRepository;

import java.util.List;

public class ContactsViewModel extends AndroidViewModel {

    private final ContactsRepository contactsRepository;

    public ContactsViewModel(@NonNull Application application) {
        super(application);
        this.contactsRepository = new ContactsRepository(application);
    }

    public LiveData<ApiResult<List<TrustedContact>>> getContacts() {
        return contactsRepository.getContacts();
    }

    public LiveData<ApiResult<TrustedContact>> addContact(TrustedContact contact) {
        return contactsRepository.addContact(contact);
    }

    public LiveData<ApiResult<TrustedContact>> updateContact(String id, TrustedContact contact) {
        return contactsRepository.updateContact(id, contact);
    }

    public LiveData<ApiResult<Void>> deleteContact(String id) {
        return contactsRepository.deleteContact(id);
    }
}
