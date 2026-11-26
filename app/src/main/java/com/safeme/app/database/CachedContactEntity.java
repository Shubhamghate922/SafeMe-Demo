package com.safeme.app.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_contacts")
public class CachedContactEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String phoneNumber;
    public String relationship;
    public int priority;
    public String verificationStatus;
    public long cachedAt;

    public CachedContactEntity(@NonNull String id, String name, String phoneNumber,
                               String relationship, int priority, String verificationStatus, long cachedAt) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.priority = priority;
        this.verificationStatus = verificationStatus;
        this.cachedAt = cachedAt;
    }
}
