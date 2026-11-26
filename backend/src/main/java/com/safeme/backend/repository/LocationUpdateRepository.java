package com.safeme.backend.repository;

import com.safeme.backend.model.LocationUpdateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LocationUpdateRepository extends MongoRepository<LocationUpdateDocument, String> {
    Optional<LocationUpdateDocument> findFirstBySessionIdOrderByRecordedAtDesc(String sessionId);
    void deleteByUserId(String userId);
}
