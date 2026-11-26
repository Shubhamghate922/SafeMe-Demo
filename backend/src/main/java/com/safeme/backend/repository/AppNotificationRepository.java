package com.safeme.backend.repository;

import com.safeme.backend.model.AppNotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends MongoRepository<AppNotificationDocument, String> {
    List<AppNotificationDocument> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
    Optional<AppNotificationDocument> findByIdAndRecipientUserId(String id, String recipientUserId);
    void deleteByRecipientUserId(String recipientUserId);
}
