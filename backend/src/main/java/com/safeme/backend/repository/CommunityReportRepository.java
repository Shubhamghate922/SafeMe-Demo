package com.safeme.backend.repository;

import com.safeme.backend.model.CommunityReportDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommunityReportRepository extends MongoRepository<CommunityReportDocument, String> {
    List<CommunityReportDocument> findByExpiresAtGreaterThanOrderByReportedAtDesc(long now);
}
