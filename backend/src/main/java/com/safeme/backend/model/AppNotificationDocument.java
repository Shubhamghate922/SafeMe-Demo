package com.safeme.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
public class AppNotificationDocument {

    @Id
    private String id;

    @JsonIgnore
    @Indexed
    private String recipientUserId;

    private String type; // SOS_ALERT, CHECKIN_REMINDER, ACKNOWLEDGEMENT, ESCALATION, SYSTEM
    private String title;
    private String message;
    private String referenceId;
    private boolean read;
    private long createdAt;

    public AppNotificationDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(String recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
