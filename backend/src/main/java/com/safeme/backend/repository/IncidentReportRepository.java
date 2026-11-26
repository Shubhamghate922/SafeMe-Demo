package com.safeme.backend.repository;

import com.safeme.backend.model.IncidentReportDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IncidentReportRepository extends MongoRepository<IncidentReportDocument, String> {
    List<IncidentReportDocument> findByUserIdOrderByCreatedAtDesc(String userId);
    void deleteByUserId(String userId);
}
