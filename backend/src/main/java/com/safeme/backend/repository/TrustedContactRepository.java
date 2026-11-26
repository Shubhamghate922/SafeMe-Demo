package com.safeme.backend.repository;

import com.safeme.backend.model.TrustedContactDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TrustedContactRepository extends MongoRepository<TrustedContactDocument, String> {
    List<TrustedContactDocument> findByOwnerUserIdOrderByPriorityAsc(String ownerUserId);
    Optional<TrustedContactDocument> findByIdAndOwnerUserId(String id, String ownerUserId);
    boolean existsByOwnerUserIdAndLinkedSafeMeUserId(String ownerUserId, String linkedSafeMeUserId);
    void deleteByOwnerUserId(String ownerUserId);
}
