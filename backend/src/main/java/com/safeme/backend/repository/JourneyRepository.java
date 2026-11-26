package com.safeme.backend.repository;

import com.safeme.backend.model.JourneyDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface JourneyRepository extends MongoRepository<JourneyDocument, String> {
    List<JourneyDocument> findByUserIdOrderByStartedAtDesc(String userId);
    Optional<JourneyDocument> findFirstByUserIdAndStatusInOrderByStartedAtDesc(String userId, List<String> statuses);
    Optional<JourneyDocument> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
