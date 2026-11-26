package com.safeme.backend.repository;

import com.safeme.backend.model.OtpVerificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends MongoRepository<OtpVerificationDocument, String> {
    Optional<OtpVerificationDocument> findTopByTargetIgnoreCaseAndPurposeOrderByCreatedAtDesc(String target, String purpose);
    void deleteByTargetIgnoreCaseAndPurpose(String target, String purpose);
    void deleteByUserId(String userId);
}
