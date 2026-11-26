package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

public class ResponseAcknowledgement {
    @SerializedName("id")
    private String id;

    @SerializedName("sosId")
    private String sosId;

    @SerializedName("responderUserId")
    private String responderUserId;

    @SerializedName("responderName")
    private String responderName;

    @SerializedName("responseStatus")
    private String responseStatus; // RESPONDING, CONTACTED_ASSISTANCE, CANNOT_RESPOND

    @SerializedName("note")
    private String note;

    @SerializedName("respondedAt")
    private long respondedAt;

    public ResponseAcknowledgement() {}

    public ResponseAcknowledgement(String responseStatus, String note) {
        this.responseStatus = responseStatus;
        this.note = note;
    }

    public String getId() { return id; }
    public String getSosId() { return sosId; }
    public String getResponderUserId() { return responderUserId; }
    public String getResponderName() { return responderName; }
    public String getResponseStatus() { return responseStatus; }
    public String getNote() { return note; }
    public long getRespondedAt() { return respondedAt; }
}
