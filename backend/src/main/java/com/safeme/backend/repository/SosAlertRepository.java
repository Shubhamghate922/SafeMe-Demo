package com.safeme.backend.repository;

import com.safeme.backend.model.SosAlertDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SosAlertRepository extends MongoRepository<SosAlertDocument, String> {
    Optional<SosAlertDocument> findFirstByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
    List<SosAlertDocument> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<SosAlertDocument> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
