package com.safeme.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trusted_contacts")
public class TrustedContactDocument {

    @Id
    private String id;

    @Indexed
    private String ownerUserId;

    private String linkedSafeMeUserId;
    private String name;
    private String phoneNumber;
    private String relationship;
    private String contactType; // REGISTERED_USER or EXTERNAL_INVITE
    private int priority = 1; // 1 = Primary, 2 = Secondary, 3 = Tertiary
    private String verificationStatus; // VERIFIED, PENDING, INVITED
    private boolean mutualConsentGranted;
    private boolean notifySms = true;
    private boolean notifyPush = true;
    private boolean shareLiveLocation = true;
    private String profilePhotoUrl;

    public TrustedContactDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getLinkedSafeMeUserId() { return linkedSafeMeUserId; }
    public void setLinkedSafeMeUserId(String linkedSafeMeUserId) { this.linkedSafeMeUserId = linkedSafeMeUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getContactType() { return contactType; }
    public void setContactType(String contactType) { this.contactType = contactType; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public boolean isMutualConsentGranted() { return mutualConsentGranted; }
    public void setMutualConsentGranted(boolean mutualConsentGranted) { this.mutualConsentGranted = mutualConsentGranted; }

    public boolean isNotifySms() { return notifySms; }
    public void setNotifySms(boolean notifySms) { this.notifySms = notifySms; }

    public boolean isNotifyPush() { return notifyPush; }
    public void setNotifyPush(boolean notifyPush) { this.notifyPush = notifyPush; }

    public boolean isShareLiveLocation() { return shareLiveLocation; }
    public void setShareLiveLocation(boolean shareLiveLocation) { this.shareLiveLocation = shareLiveLocation; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
}
