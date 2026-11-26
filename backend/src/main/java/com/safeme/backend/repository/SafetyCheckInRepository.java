package com.safeme.backend.repository;

import com.safeme.backend.model.SafetyCheckInDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SafetyCheckInRepository extends MongoRepository<SafetyCheckInDocument, String> {
    List<SafetyCheckInDocument> findByJourneyIdOrderByCheckedInAtDesc(String journeyId);
    void deleteByUserId(String userId);
}
