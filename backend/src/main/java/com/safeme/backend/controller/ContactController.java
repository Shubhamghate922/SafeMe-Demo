package com.safeme.backend.controller;

import com.safeme.backend.dto.ApiResponse;
import com.safeme.backend.exception.ApiException;
import com.safeme.backend.model.TrustedContactDocument;
import com.safeme.backend.model.UserDocument;
import com.safeme.backend.repository.TrustedContactRepository;
import com.safeme.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final TrustedContactRepository contactRepository;
    private final UserRepository userRepository;

    public ContactController(TrustedContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<TrustedContactDocument>> getContacts(Authentication auth) {
        return ApiResponse.ok(contactRepository.findByOwnerUserIdOrderByPriorityAsc(auth.getName()));
    }

    @PostMapping
    public ApiResponse<TrustedContactDocument> addContact(
            Authentication auth,
            @RequestBody TrustedContactDocument request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contact name is required.");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contact phone number is required.");
        }

        String phone = request.getPhoneNumber().replaceAll("[\\s\\-()]", "");
        TrustedContactDocument doc = new TrustedContactDocument();
        doc.setOwnerUserId(auth.getName());
        doc.setName(request.getName().trim());
        doc.setPhoneNumber(phone);
        doc.setRelationship(request.getRelationship() != null ? request.getRelationship().trim() : "Trusted Contact");
        doc.setPriority(Math.max(1, Math.min(3, request.getPriority() == 0 ? 1 : request.getPriority())));
        doc.setNotifySms(request.isNotifySms());
        doc.setNotifyPush(request.isNotifyPush());
        doc.setShareLiveLocation(request.isShareLiveLocation());

        Optional<UserDocument> linkedUser = userRepository.findByMobileNumber(phone);
        if (linkedUser.isPresent()) {
            doc.setLinkedSafeMeUserId(linkedUser.get().getId());
            doc.setContactType("REGISTERED_USER");
            doc.setVerificationStatus("VERIFIED");
            doc.setMutualConsentGranted(true);
        } else {
            doc.setContactType("EXTERNAL_INVITE");
            doc.setVerificationStatus("INVITED");
            doc.setMutualConsentGranted(false);
        }

        return ApiResponse.ok(contactRepository.save(doc), "Trusted contact added.");
    }

    @PutMapping("/{id}")
    public ApiResponse<TrustedContactDocument> updateContact(
            Authentication auth,
            @PathVariable String id,
            @RequestBody TrustedContactDocument request) {
        TrustedContactDocument existing = contactRepository.findByIdAndOwnerUserId(id, auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Trusted contact not found."));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            existing.setName(request.getName().trim());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String phone = request.getPhoneNumber().replaceAll("[\\s\\-()]", "");
            existing.setPhoneNumber(phone);
            userRepository.findByMobileNumber(phone).ifPresentOrElse(linked -> {
                existing.setLinkedSafeMeUserId(linked.getId());
                existing.setContactType("REGISTERED_USER");
                existing.setVerificationStatus("VERIFIED");
                existing.setMutualConsentGranted(true);
            }, () -> {
                existing.setLinkedSafeMeUserId(null);
                existing.setContactType("EXTERNAL_INVITE");
                existing.setVerificationStatus("INVITED");
                existing.setMutualConsentGranted(false);
            });
        }
        if (request.getRelationship() != null) {
            existing.setRelationship(request.getRelationship().trim());
        }
        if (request.getPriority() >= 1 && request.getPriority() <= 3) {
            existing.setPriority(request.getPriority());
        }
        existing.setNotifySms(request.isNotifySms());
        existing.setNotifyPush(request.isNotifyPush());
        existing.setShareLiveLocation(request.isShareLiveLocation());

        return ApiResponse.ok(contactRepository.save(existing), "Trusted contact updated.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteContact(Authentication auth, @PathVariable String id) {
        TrustedContactDocument existing = contactRepository.findByIdAndOwnerUserId(id, auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Trusted contact not found."));
        contactRepository.delete(existing);
        return ApiResponse.ok(null, "Trusted contact removed.");
    }
}
