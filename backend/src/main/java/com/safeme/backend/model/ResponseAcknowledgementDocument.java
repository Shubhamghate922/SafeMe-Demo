package com.safeme.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "response_acknowledgements")
public class ResponseAcknowledgementDocument {

    @Id
    private String id;

    @Indexed
    private String sosId;

    private String responderUserId;
    private String responderName;
    private String responseStatus; // RESPONDING, CONTACTED_ASSISTANCE, CANNOT_RESPOND
    private String note;
    private long respondedAt;

    public ResponseAcknowledgementDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSosId() { return sosId; }
    public void setSosId(String sosId) { this.sosId = sosId; }

    public String getResponderUserId() { return responderUserId; }
    public void setResponderUserId(String responderUserId) { this.responderUserId = responderUserId; }

    public String getResponderName() { return responderName; }
    public void setResponderName(String responderName) { this.responderName = responderName; }

    public String getResponseStatus() { return responseStatus; }
    public void setResponseStatus(String responseStatus) { this.responseStatus = responseStatus; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getRespondedAt() { return respondedAt; }
    public void setRespondedAt(long respondedAt) { this.respondedAt = respondedAt; }
}
