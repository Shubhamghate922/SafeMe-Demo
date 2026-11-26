package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class TrustedContact implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("ownerUserId")
    private String ownerUserId;

    @SerializedName("linkedSafeMeUserId")
    private String linkedSafeMeUserId;

    @SerializedName("name")
    private String name;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("relationship")
    private String relationship;

    @SerializedName("contactType")
    private String contactType; // REGISTERED_USER or EXTERNAL_INVITE

    @SerializedName("priority")
    private int priority; // 1 = Primary, 2 = Secondary, 3 = Tertiary

    @SerializedName("verificationStatus")
    private String verificationStatus; // VERIFIED, PENDING, INVITED

    @SerializedName("mutualConsentGranted")
    private boolean mutualConsentGranted;

    @SerializedName("notifySms")
    private boolean notifySms = true;

    @SerializedName("notifyPush")
    private boolean notifyPush = true;

    @SerializedName("shareLiveLocation")
    private boolean shareLiveLocation = true;

    @SerializedName("profilePhotoUrl")
    private String profilePhotoUrl;

    private transient boolean selectedForJourney;

    public TrustedContact() {}

    public TrustedContact(String name, String phoneNumber, String relationship, String contactType,
                          int priority, boolean notifySms, boolean notifyPush, boolean shareLiveLocation) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.contactType = contactType;
        this.priority = priority;
        this.notifySms = notifySms;
        this.notifyPush = notifyPush;
        this.shareLiveLocation = shareLiveLocation;
    }

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

    public boolean isSelectedForJourney() { return selectedForJourney; }
    public void setSelectedForJourney(boolean selectedForJourney) { this.selectedForJourney = selectedForJourney; }
}
