package com.safeme.backend.repository;

import com.safeme.backend.model.ResponseAcknowledgementDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResponseAcknowledgementRepository extends MongoRepository<ResponseAcknowledgementDocument, String> {
    List<ResponseAcknowledgementDocument> findBySosIdOrderByRespondedAtDesc(String sosId);
}
