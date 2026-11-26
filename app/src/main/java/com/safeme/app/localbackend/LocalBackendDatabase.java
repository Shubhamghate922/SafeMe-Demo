package com.safeme.app.localbackend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.safeme.app.models.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Real SQLite database backing the on-device SafeMe backend mode when a remote
 * Spring Boot server is unavailable (such as in cloud preview emulators).
 */
public class LocalBackendDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "safeme_local_backend.db";
    private static final int DB_VERSION = 1;
    private static volatile LocalBackendDatabase instance;
    private static final Gson GSON = new Gson();

    private LocalBackendDatabase(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static LocalBackendDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LocalBackendDatabase.class) {
                if (instance == null) {
                    instance = new LocalBackendDatabase(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id TEXT PRIMARY KEY, " +
                "full_name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "mobile_number TEXT NOT NULL UNIQUE, " +
                "profile_photo_url TEXT, " +
                "password_hash TEXT NOT NULL, " +
                "verified INTEGER NOT NULL, " +
                "fcm_token TEXT, " +
                "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE otp_verifications (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "target TEXT NOT NULL, " +
                "purpose TEXT NOT NULL, " +
                "otp_hash TEXT NOT NULL, " +
                "expires_at INTEGER NOT NULL, " +
                "attempts INTEGER NOT NULL, " +
                "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE trusted_contacts (" +
                "id TEXT PRIMARY KEY, " +
                "owner_user_id TEXT NOT NULL, " +
                "linked_safeme_user_id TEXT, " +
                "name TEXT NOT NULL, " +
                "phone_number TEXT NOT NULL, " +
                "relationship TEXT, " +
                "contact_type TEXT, " +
                "priority INTEGER NOT NULL, " +
                "verification_status TEXT, " +
                "mutual_consent_granted INTEGER NOT NULL, " +
                "notify_sms INTEGER NOT NULL, " +
                "notify_push INTEGER NOT NULL, " +
                "share_live_location INTEGER NOT NULL, " +
                "profile_photo_url TEXT)");

        db.execSQL("CREATE TABLE journeys (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "start_location_name TEXT, " +
                "start_latitude REAL, " +
                "start_longitude REAL, " +
                "destination_name TEXT, " +
                "destination_latitude REAL, " +
                "destination_longitude REAL, " +
                "expected_duration_minutes INTEGER, " +
                "expected_arrival_time INTEGER, " +
                "transport_mode TEXT, " +
                "trusted_contact_ids_json TEXT, " +
                "check_in_interval_minutes INTEGER, " +
                "monitoring_enabled INTEGER, " +
                "status TEXT, " +
                "started_at INTEGER, " +
                "completed_at INTEGER, " +
                "next_check_in_due_at INTEGER, " +
                "last_latitude REAL, " +
                "last_longitude REAL, " +
                "last_accuracy_meters REAL, " +
                "last_location_timestamp INTEGER, " +
                "distance_remaining_meters REAL)");

        db.execSQL("CREATE TABLE safety_checkins (" +
                "id TEXT PRIMARY KEY, " +
                "journey_id TEXT NOT NULL, " +
                "user_id TEXT NOT NULL, " +
                "status TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "note TEXT, " +
                "checked_in_at INTEGER, " +
                "next_check_in_due_at INTEGER)");

        db.execSQL("CREATE TABLE sos_alerts (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "sender_name TEXT, " +
                "journey_id TEXT, " +
                "trigger_source TEXT, " +
                "status TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "accuracy_meters REAL, " +
                "address_label TEXT, " +
                "notification_delivery_status TEXT, " +
                "created_at INTEGER, " +
                "resolved_at INTEGER, " +
                "resolution_note TEXT)");

        db.execSQL("CREATE TABLE location_updates (" +
                "id TEXT PRIMARY KEY, " +
                "session_id TEXT NOT NULL, " +
                "session_type TEXT NOT NULL, " +
                "user_id TEXT NOT NULL, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "accuracy_meters REAL, " +
                "speed_mps REAL, " +
                "bearing REAL, " +
                "recorded_at INTEGER)");

        db.execSQL("CREATE TABLE emergency_escalations (" +
                "id TEXT PRIMARY KEY, " +
                "journey_id TEXT NOT NULL, " +
                "user_id TEXT NOT NULL, " +
                "current_stage INTEGER, " +
                "status TEXT, " +
                "notified_contacts_count INTEGER, " +
                "triggered_at INTEGER)");

        db.execSQL("CREATE TABLE response_acknowledgements (" +
                "id TEXT PRIMARY KEY, " +
                "sos_id TEXT NOT NULL, " +
                "responder_user_id TEXT NOT NULL, " +
                "responder_name TEXT, " +
                "response_status TEXT, " +
                "note TEXT, " +
                "responded_at INTEGER)");

        db.execSQL("CREATE TABLE community_reports (" +
                "id TEXT PRIMARY KEY, " +
                "reporter_user_id TEXT NOT NULL, " +
                "category TEXT, " +
                "description TEXT, " +
                "location_name TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "reported_at INTEGER, " +
                "expires_at INTEGER, " +
                "verification_status TEXT)");

        db.execSQL("CREATE TABLE incident_reports (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "category TEXT, " +
                "description TEXT, " +
                "incident_date TEXT, " +
                "location_name TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "photo_url TEXT, " +
                "audio_url TEXT, " +
                "status TEXT, " +
                "created_at INTEGER)");

        db.execSQL("CREATE TABLE notifications (" +
                "id TEXT PRIMARY KEY, " +
                "recipient_user_id TEXT NOT NULL, " +
                "type TEXT, " +
                "title TEXT, " +
                "message TEXT, " +
                "reference_id TEXT, " +
                "is_read INTEGER, " +
                "created_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Initial version 1 schema
    }

    public static class UserRecord {
        public final User user;
        public final String passwordHash;

        public UserRecord(User user, String passwordHash) {
            this.user = user;
            this.passwordHash = passwordHash;
        }
    }

    public synchronized boolean hasAnyUsers() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM users", null)) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public synchronized UserRecord findUserByEmailOrMobile(String identifier) {
        String trimmed = identifier.trim();
        String sql = trimmed.contains("@")
                ? "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1"
                : "SELECT * FROM users WHERE mobile_number = ? LIMIT 1";
        String arg = trimmed.contains("@") ? trimmed.toLowerCase() : trimmed.replaceAll("[\\s\\-()]", "");
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{arg})) {
            return c.moveToFirst() ? readUserRecord(c) : null;
        }
    }

    public synchronized UserRecord findUserById(String userId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE id = ? LIMIT 1", new String[]{userId})) {
            return c.moveToFirst() ? readUserRecord(c) : null;
        }
    }

    public synchronized User createUser(String fullName, String email, String mobile, String passwordHash) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("full_name", fullName);
        cv.put("email", email.toLowerCase());
        cv.put("mobile_number", mobile);
        cv.put("password_hash", passwordHash);
        cv.put("verified", 1);
        cv.put("created_at", now);
        getWritableDatabase().insertOrThrow("users", null, cv);
        User u = new User(id, fullName, email.toLowerCase(), mobile, true);
        u.setCreatedAt(now);
        return u;
    }

    public synchronized User updateUserProfile(String userId, String fullName, String mobile, String photoUrl) {
        ContentValues cv = new ContentValues();
        cv.put("full_name", fullName);
        cv.put("mobile_number", mobile);
        if (photoUrl != null) {
            cv.put("profile_photo_url", photoUrl);
        }
        getWritableDatabase().update("users", cv, "id = ?", new String[]{userId});
        UserRecord rec = findUserById(userId);
        return rec != null ? rec.user : null;
    }

    public synchronized void updatePasswordHash(String userId, String newHash) {
        ContentValues cv = new ContentValues();
        cv.put("password_hash", newHash);
        getWritableDatabase().update("users", cv, "id = ?", new String[]{userId});
    }

    public synchronized void updateFcmToken(String userId, String fcmToken) {
        ContentValues cv = new ContentValues();
        cv.put("fcm_token", fcmToken);
        getWritableDatabase().update("users", cv, "id = ?", new String[]{userId});
    }

    public synchronized void deleteUserCascade(String userId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String[] args = new String[]{userId};
            db.delete("otp_verifications", "user_id = ?", args);
            db.delete("trusted_contacts", "owner_user_id = ?", args);
            db.delete("safety_checkins", "user_id = ?", args);
            db.delete("location_updates", "user_id = ?", args);
            db.delete("emergency_escalations", "user_id = ?", args);
            db.delete("journeys", "user_id = ?", args);
            db.delete("sos_alerts", "user_id = ?", args);
            db.delete("incident_reports", "user_id = ?", args);
            db.delete("notifications", "recipient_user_id = ?", args);
            db.delete("users", "id = ?", args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ==================== CONTACTS ====================

    public synchronized List<TrustedContact> getContacts(String ownerUserId) {
        List<TrustedContact> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM trusted_contacts WHERE owner_user_id = ? ORDER BY priority ASC",
                new String[]{ownerUserId})) {
            while (c.moveToNext()) {
                list.add(readContact(c));
            }
        }
        return list;
    }

    public synchronized TrustedContact addContact(String ownerUserId, TrustedContact req) {
        String id = UUID.randomUUID().toString();
        String phone = req.getPhoneNumber().replaceAll("[\\s\\-()]", "");
        UserRecord linked = findUserByEmailOrMobile(phone);

        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("owner_user_id", ownerUserId);
        cv.put("linked_safeme_user_id", linked != null ? linked.user.getId() : null);
        cv.put("name", req.getName().trim());
        cv.put("phone_number", phone);
        cv.put("relationship", req.getRelationship() != null ? req.getRelationship().trim() : "Trusted Contact");
        cv.put("contact_type", linked != null ? "REGISTERED_USER" : "EXTERNAL_INVITE");
        cv.put("priority", Math.max(1, Math.min(3, req.getPriority() == 0 ? 1 : req.getPriority())));
        cv.put("verification_status", linked != null ? "VERIFIED" : "INVITED");
        cv.put("mutual_consent_granted", linked != null ? 1 : 0);
        cv.put("notify_sms", req.isNotifySms() ? 1 : 0);
        cv.put("notify_push", req.isNotifyPush() ? 1 : 0);
        cv.put("share_live_location", req.isShareLiveLocation() ? 1 : 0);
        getWritableDatabase().insertOrThrow("trusted_contacts", null, cv);
        return findContact(id, ownerUserId);
    }

    public synchronized TrustedContact findContact(String id, String ownerUserId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM trusted_contacts WHERE id = ? AND owner_user_id = ? LIMIT 1",
                new String[]{id, ownerUserId})) {
            return c.moveToFirst() ? readContact(c) : null;
        }
    }

    public synchronized TrustedContact updateContact(String id, String ownerUserId, TrustedContact req) {
        TrustedContact existing = findContact(id, ownerUserId);
        if (existing == null) return null;

        ContentValues cv = new ContentValues();
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            cv.put("name", req.getName().trim());
        }
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().trim().isEmpty()) {
            String phone = req.getPhoneNumber().replaceAll("[\\s\\-()]", "");
            UserRecord linked = findUserByEmailOrMobile(phone);
            cv.put("phone_number", phone);
            cv.put("linked_safeme_user_id", linked != null ? linked.user.getId() : null);
            cv.put("contact_type", linked != null ? "REGISTERED_USER" : "EXTERNAL_INVITE");
            cv.put("verification_status", linked != null ? "VERIFIED" : "INVITED");
            cv.put("mutual_consent_granted", linked != null ? 1 : 0);
        }
        if (req.getRelationship() != null) {
            cv.put("relationship", req.getRelationship().trim());
        }
        if (req.getPriority() >= 1 && req.getPriority() <= 3) {
            cv.put("priority", req.getPriority());
        }
        cv.put("notify_sms", req.isNotifySms() ? 1 : 0);
        cv.put("notify_push", req.isNotifyPush() ? 1 : 0);
        cv.put("share_live_location", req.isShareLiveLocation() ? 1 : 0);
        getWritableDatabase().update("trusted_contacts", cv, "id = ? AND owner_user_id = ?", new String[]{id, ownerUserId});
        return findContact(id, ownerUserId);
    }

    public synchronized boolean deleteContact(String id, String ownerUserId) {
        return getWritableDatabase().delete("trusted_contacts", "id = ? AND owner_user_id = ?", new String[]{id, ownerUserId}) > 0;
    }

    // ==================== JOURNEYS & CHECK-INS ====================

    public synchronized Journey insertJourney(String userId, JourneyDtos.CreateJourneyRequest req) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long eta = req.expectedArrivalTime > 0 ? req.expectedArrivalTime : now + req.expectedDurationMinutes * 60_000L;

        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("user_id", userId);
        cv.put("start_location_name", req.startLocationName != null ? req.startLocationName : "Current Location");
        cv.put("start_latitude", req.startLatitude);
        cv.put("start_longitude", req.startLongitude);
        cv.put("destination_name", req.destinationName);
        cv.put("destination_latitude", req.destinationLatitude);
        cv.put("destination_longitude", req.destinationLongitude);
        cv.put("expected_duration_minutes", req.expectedDurationMinutes);
        cv.put("expected_arrival_time", eta);
        cv.put("transport_mode", req.transportMode != null ? req.transportMode : "WALKING");
        cv.put("trusted_contact_ids_json", GSON.toJson(req.trustedContactIds != null ? req.trustedContactIds : Collections.emptyList()));
        cv.put("check_in_interval_minutes", Math.max(1, req.checkInIntervalMinutes));
        cv.put("monitoring_enabled", req.monitoringEnabled ? 1 : 0);
        cv.put("status", "CREATED");
        cv.put("started_at", 0L);
        cv.put("completed_at", 0L);
        cv.put("next_check_in_due_at", 0L);
        cv.put("last_latitude", req.startLatitude);
        cv.put("last_longitude", req.startLongitude);
        cv.put("last_accuracy_meters", 10f);
        cv.put("last_location_timestamp", now);
        cv.put("distance_remaining_meters", 0.0);
        getWritableDatabase().insertOrThrow("journeys", null, cv);
        return findJourney(id, userId);
    }

    public synchronized Journey findJourney(String id, String userId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM journeys WHERE id = ? AND user_id = ? LIMIT 1",
                new String[]{id, userId})) {
            return c.moveToFirst() ? readJourney(c) : null;
        }
    }

    public synchronized Journey findActiveJourney(String userId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM journeys WHERE user_id = ? AND status IN ('ACTIVE', 'ESCALATED') ORDER BY started_at DESC LIMIT 1",
                new String[]{userId})) {
            return c.moveToFirst() ? readJourney(c) : null;
        }
    }

    public synchronized List<Journey> getJourneys(String userId) {
        List<Journey> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM journeys WHERE user_id = ? ORDER BY started_at DESC",
                new String[]{userId})) {
            while (c.moveToNext()) {
                list.add(readJourney(c));
            }
        }
        return list;
    }

    public synchronized Journey saveJourneyState(Journey j) {
        ContentValues cv = new ContentValues();
        cv.put("destination_name", j.getDestinationName());
        cv.put("destination_latitude", j.getDestinationLatitude());
        cv.put("destination_longitude", j.getDestinationLongitude());
        cv.put("expected_duration_minutes", j.getExpectedDurationMinutes());
        cv.put("expected_arrival_time", j.getExpectedArrivalTime());
        cv.put("status", j.getStatus());
        cv.put("started_at", j.getStartedAt());
        cv.put("completed_at", j.getCompletedAt());
        cv.put("next_check_in_due_at", j.getNextCheckInDueAt());
        cv.put("last_latitude", j.getLastLatitude());
        cv.put("last_longitude", j.getLastLongitude());
        cv.put("last_accuracy_meters", j.getLastAccuracyMeters());
        cv.put("last_location_timestamp", j.getLastLocationTimestamp());
        cv.put("distance_remaining_meters", j.getDistanceRemainingMeters());
        getWritableDatabase().update("journeys", cv, "id = ? AND user_id = ?", new String[]{j.getId(), j.getUserId()});
        return findJourney(j.getId(), j.getUserId());
    }

    public synchronized String insertLocation(String sessionId, String sessionType, String userId,
                                              double lat, double lng, float acc, float speed, float bearing, long recordedAt) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("session_id", sessionId);
        cv.put("session_type", sessionType);
        cv.put("user_id", userId);
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("accuracy_meters", acc);
        cv.put("speed_mps", speed);
        cv.put("bearing", bearing);
        cv.put("recorded_at", recordedAt);
        getWritableDatabase().insertOrThrow("location_updates", null, cv);
        return id;
    }

    public synchronized LocationUpdate findLatestLocation(String sessionId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM location_updates WHERE session_id = ? ORDER BY recorded_at DESC LIMIT 1",
                new String[]{sessionId})) {
            if (!c.moveToFirst()) return null;
            return new LocationUpdate(
                    c.getDouble(c.getColumnIndexOrThrow("latitude")),
                    c.getDouble(c.getColumnIndexOrThrow("longitude")),
                    c.getFloat(c.getColumnIndexOrThrow("accuracy_meters")),
                    c.getFloat(c.getColumnIndexOrThrow("speed_mps")),
                    c.getFloat(c.getColumnIndexOrThrow("bearing")),
                    c.getLong(c.getColumnIndexOrThrow("recorded_at"))
            );
        }
    }

    public synchronized String insertCheckIn(String journeyId, String userId, String status,
                                             double lat, double lng, String note, long checkedInAt, long nextDue) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("journey_id", journeyId);
        cv.put("user_id", userId);
        cv.put("status", status);
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("note", note);
        cv.put("checked_in_at", checkedInAt);
        cv.put("next_check_in_due_at", nextDue);
        getWritableDatabase().insertOrThrow("safety_checkins", null, cv);
        return id;
    }

    public synchronized List<SafetyCheckIn> getCheckIns(String journeyId) {
        List<SafetyCheckIn> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM safety_checkins WHERE journey_id = ? ORDER BY checked_in_at DESC",
                new String[]{journeyId})) {
            while (c.moveToNext()) {
                SafetyCheckIn ci = new SafetyCheckIn(
                        c.getString(c.getColumnIndexOrThrow("journey_id")),
                        c.getString(c.getColumnIndexOrThrow("status")),
                        c.getDouble(c.getColumnIndexOrThrow("latitude")),
                        c.getDouble(c.getColumnIndexOrThrow("longitude")),
                        c.getString(c.getColumnIndexOrThrow("note"))
                );
                list.add(ci);
            }
        }
        return list;
    }

    public synchronized String insertEscalation(String journeyId, String userId, int stage,
                                                String status, int notifiedCount, long triggeredAt) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("journey_id", journeyId);
        cv.put("user_id", userId);
        cv.put("current_stage", stage);
        cv.put("status", status);
        cv.put("notified_contacts_count", notifiedCount);
        cv.put("triggered_at", triggeredAt);
        getWritableDatabase().insertOrThrow("emergency_escalations", null, cv);
        return id;
    }

    // ==================== SOS & ACKNOWLEDGEMENTS ====================

    public synchronized String insertSos(String userId, String senderName, String journeyId, String triggerSource,
                                         double lat, double lng, float acc, String addressLabel, long createdAt) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("user_id", userId);
        cv.put("sender_name", senderName);
        cv.put("journey_id", journeyId);
        cv.put("trigger_source", triggerSource);
        cv.put("status", "ACTIVE");
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("accuracy_meters", acc);
        cv.put("address_label", addressLabel);
        cv.put("notification_delivery_status", "DISPATCHED");
        cv.put("created_at", createdAt);
        cv.put("resolved_at", 0L);
        getWritableDatabase().insertOrThrow("sos_alerts", null, cv);
        return id;
    }

    public synchronized Cursor findSosCursorById(String sosId) {
        return getReadableDatabase().rawQuery("SELECT * FROM sos_alerts WHERE id = ? LIMIT 1", new String[]{sosId});
    }

    public synchronized Cursor findActiveSosCursor(String userId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM sos_alerts WHERE user_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1",
                new String[]{userId});
    }

    public synchronized Cursor findSosHistoryCursor(String userId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM sos_alerts WHERE user_id = ? ORDER BY created_at DESC",
                new String[]{userId});
    }

    public synchronized void resolveSos(String sosId, String userId, long resolvedAt, String note) {
        ContentValues cv = new ContentValues();
        cv.put("status", "RESOLVED");
        cv.put("resolved_at", resolvedAt);
        cv.put("resolution_note", note);
        getWritableDatabase().update("sos_alerts", cv, "id = ? AND user_id = ?", new String[]{sosId, userId});
    }

    public synchronized void updateSosLocation(String sosId, double lat, double lng, float acc) {
        ContentValues cv = new ContentValues();
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("accuracy_meters", acc);
        getWritableDatabase().update("sos_alerts", cv, "id = ?", new String[]{sosId});
    }

    public synchronized String insertAcknowledgement(String sosId, String responderId, String responderName,
                                                     String status, String note, long respondedAt) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("sos_id", sosId);
        cv.put("responder_user_id", responderId);
        cv.put("responder_name", responderName);
        cv.put("response_status", status);
        cv.put("note", note);
        cv.put("responded_at", respondedAt);
        getWritableDatabase().insertOrThrow("response_acknowledgements", null, cv);
        return id;
    }

    public synchronized Cursor findAcknowledgementsCursor(String sosId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM response_acknowledgements WHERE sos_id = ? ORDER BY responded_at DESC",
                new String[]{sosId});
    }

    // ==================== COMMUNITY, INCIDENTS & NOTIFICATIONS ====================

    public synchronized String insertCommunityReport(String userId, String category, String desc, String locName,
                                                       double lat, double lng, long reportedAt, long expiresAt) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("reporter_user_id", userId);
        cv.put("category", category);
        cv.put("description", desc);
        cv.put("location_name", locName);
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("reported_at", reportedAt);
        cv.put("expires_at", expiresAt);
        cv.put("verification_status", "VERIFIED");
        getWritableDatabase().insertOrThrow("community_reports", null, cv);
        return id;
    }

    public synchronized Cursor findActiveCommunityReportsCursor(long now) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM community_reports WHERE expires_at > ? ORDER BY reported_at DESC",
                new String[]{String.valueOf(now)});
    }

    public synchronized String insertIncidentReport(String userId, String category, String desc, String incidentDate,
                                                      String locName, double lat, double lng, String photoUrl, String audioUrl, long createdAt) {
        String id = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("user_id", userId);
        cv.put("category", category);
        cv.put("description", desc);
        cv.put("incident_date", incidentDate);
        cv.put("location_name", locName);
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("photo_url", photoUrl);
        cv.put("audio_url", audioUrl);
        cv.put("status", "SUBMITTED");
        cv.put("created_at", createdAt);
        getWritableDatabase().insertOrThrow("incident_reports", null, cv);
        return id;
    }

    public synchronized Cursor findIncidentReportsCursor(String userId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM incident_reports WHERE user_id = ? ORDER BY created_at DESC",
                new String[]{userId});
    }

    public synchronized void insertNotification(String recipientUserId, String type, String refId, String title, String message) {
        ContentValues cv = new ContentValues();
        cv.put("id", UUID.randomUUID().toString());
        cv.put("recipient_user_id", recipientUserId);
        cv.put("type", type);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("reference_id", refId);
        cv.put("is_read", 0);
        cv.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("notifications", null, cv);
    }

    public synchronized Cursor findNotificationsCursor(String userId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM notifications WHERE recipient_user_id = ? ORDER BY created_at DESC",
                new String[]{userId});
    }

    public synchronized boolean markNotificationRead(String id, String userId) {
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        return getWritableDatabase().update("notifications", cv, "id = ? AND recipient_user_id = ?", new String[]{id, userId}) > 0;
    }

    // ==================== ROW READERS ====================

    private UserRecord readUserRecord(Cursor c) {
        User u = new User();
        u.setId(c.getString(c.getColumnIndexOrThrow("id")));
        u.setFullName(c.getString(c.getColumnIndexOrThrow("full_name")));
        u.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        u.setMobileNumber(c.getString(c.getColumnIndexOrThrow("mobile_number")));
        u.setProfilePhotoUrl(c.getString(c.getColumnIndexOrThrow("profile_photo_url")));
        u.setVerified(c.getInt(c.getColumnIndexOrThrow("verified")) == 1);
        u.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        String hash = c.getString(c.getColumnIndexOrThrow("password_hash"));
        return new UserRecord(u, hash);
    }

    private TrustedContact readContact(Cursor c) {
        TrustedContact tc = new TrustedContact();
        tc.setId(c.getString(c.getColumnIndexOrThrow("id")));
        tc.setOwnerUserId(c.getString(c.getColumnIndexOrThrow("owner_user_id")));
        tc.setLinkedSafeMeUserId(c.getString(c.getColumnIndexOrThrow("linked_safeme_user_id")));
        tc.setName(c.getString(c.getColumnIndexOrThrow("name")));
        tc.setPhoneNumber(c.getString(c.getColumnIndexOrThrow("phone_number")));
        tc.setRelationship(c.getString(c.getColumnIndexOrThrow("relationship")));
        tc.setContactType(c.getString(c.getColumnIndexOrThrow("contact_type")));
        tc.setPriority(c.getInt(c.getColumnIndexOrThrow("priority")));
        tc.setVerificationStatus(c.getString(c.getColumnIndexOrThrow("verification_status")));
        tc.setMutualConsentGranted(c.getInt(c.getColumnIndexOrThrow("mutual_consent_granted")) == 1);
        tc.setNotifySms(c.getInt(c.getColumnIndexOrThrow("notify_sms")) == 1);
        tc.setNotifyPush(c.getInt(c.getColumnIndexOrThrow("notify_push")) == 1);
        tc.setShareLiveLocation(c.getInt(c.getColumnIndexOrThrow("share_live_location")) == 1);
        tc.setProfilePhotoUrl(c.getString(c.getColumnIndexOrThrow("profile_photo_url")));
        return tc;
    }

    private Journey readJourney(Cursor c) {
        Journey j = new Journey();
        j.setId(c.getString(c.getColumnIndexOrThrow("id")));
        j.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        j.setStartLocationName(c.getString(c.getColumnIndexOrThrow("start_location_name")));
        j.setStartLatitude(c.getDouble(c.getColumnIndexOrThrow("start_latitude")));
        j.setStartLongitude(c.getDouble(c.getColumnIndexOrThrow("start_longitude")));
        j.setDestinationName(c.getString(c.getColumnIndexOrThrow("destination_name")));
        j.setDestinationLatitude(c.getDouble(c.getColumnIndexOrThrow("destination_latitude")));
        j.setDestinationLongitude(c.getDouble(c.getColumnIndexOrThrow("destination_longitude")));
        j.setExpectedDurationMinutes(c.getInt(c.getColumnIndexOrThrow("expected_duration_minutes")));
        j.setExpectedArrivalTime(c.getLong(c.getColumnIndexOrThrow("expected_arrival_time")));
        j.setTransportMode(c.getString(c.getColumnIndexOrThrow("transport_mode")));
        String idsJson = c.getString(c.getColumnIndexOrThrow("trusted_contact_ids_json"));
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> ids = idsJson != null ? GSON.fromJson(idsJson, listType) : new ArrayList<>();
        j.setTrustedContactIds(ids != null ? ids : new ArrayList<>());
        j.setCheckInIntervalMinutes(c.getInt(c.getColumnIndexOrThrow("check_in_interval_minutes")));
        j.setMonitoringEnabled(c.getInt(c.getColumnIndexOrThrow("monitoring_enabled")) == 1);
        j.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
        j.setStartedAt(c.getLong(c.getColumnIndexOrThrow("started_at")));
        j.setCompletedAt(c.getLong(c.getColumnIndexOrThrow("completed_at")));
        j.setNextCheckInDueAt(c.getLong(c.getColumnIndexOrThrow("next_check_in_due_at")));
        j.setLastLatitude(c.getDouble(c.getColumnIndexOrThrow("last_latitude")));
        j.setLastLongitude(c.getDouble(c.getColumnIndexOrThrow("last_longitude")));
        j.setLastAccuracyMeters(c.getFloat(c.getColumnIndexOrThrow("last_accuracy_meters")));
        j.setLastLocationTimestamp(c.getLong(c.getColumnIndexOrThrow("last_location_timestamp")));
        j.setDistanceRemainingMeters(c.getDouble(c.getColumnIndexOrThrow("distance_remaining_meters")));
        return j;
    }
}
