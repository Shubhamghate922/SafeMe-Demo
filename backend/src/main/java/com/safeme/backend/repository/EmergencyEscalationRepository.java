package com.safeme.backend.repository;

import com.safeme.backend.model.EmergencyEscalationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmergencyEscalationRepository extends MongoRepository<EmergencyEscalationDocument, String> {
    Optional<EmergencyEscalationDocument> findTopByJourneyIdOrderByTriggeredAtDesc(String journeyId);
    void deleteByUserId(String userId);
}
