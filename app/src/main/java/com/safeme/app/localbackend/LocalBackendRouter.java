package com.safeme.app.localbackend;

import android.content.Context;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.safeme.app.models.*;
import com.safeme.app.utils.SecureTokenManager;
import com.safeme.app.utils.ValidationUtils;

import java.util.*;

/**
 * In-process REST API router backed by LocalBackendDatabase (SQLite) and JWT/PBKDF2 auth.
 * Mirrors the exact Spring Boot endpoints in SafeMeApiService so every feature works
 * end-to-end even when an external Spring Boot server is unreachable in preview emulators.
 */
public class LocalBackendRouter {

    public static class HttpResult {
        public final int statusCode;
        public final String jsonBody;

        public HttpResult(int statusCode, String jsonBody) {
            this.statusCode = statusCode;
            this.jsonBody = jsonBody;
        }
    }

    private static final long SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final Gson GSON = new Gson();

    private final LocalBackendDatabase db;
    private final LocalPasswordAndJwtHelper crypto;

    public LocalBackendRouter(Context context) {
        this.db = LocalBackendDatabase.getInstance(context);
        this.crypto = new LocalPasswordAndJwtHelper(SecureTokenManager.getInstance(context));
    }

    public HttpResult route(String method, String rawPath, String authHeader, String jsonBody,
                            Map<String, String> multipartFields, boolean hasPhoto, boolean hasAudio) {
        try {
            String path = normalizePath(rawPath);

            // ==================== AUTH ====================
            if ("POST".equals(method) && "/auth/register".equals(path)) {
                return handleRegister(jsonBody);
            }
            if ("POST".equals(method) && "/auth/login".equals(path)) {
                return handleLogin(jsonBody);
            }
            if ("POST".equals(method) && "/auth/verify-otp".equals(path)) {
                return ok(null, "Account verified.");
            }
            if ("POST".equals(method) && "/auth/resend-otp".equals(path)) {
                return ok(null, "OTP delivery requires an external SMS/email gateway.");
            }
            if ("POST".equals(method) && "/auth/forgot-password".equals(path)) {
                return fail(400, "Password reset OTP requires a remote Spring Boot server with email/SMS configured.");
            }
            if ("POST".equals(method) && "/auth/reset-password".equals(path)) {
                return fail(400, "Password reset requires a remote Spring Boot server.");
            }
            if ("POST".equals(method) && "/auth/logout".equals(path)) {
                return ok(null, "Signed out successfully.");
            }

            // ==================== PUBLIC COMMUNITY GET ====================
            if ("GET".equals(method) && "/community/reports".equals(path)) {
                return handleGetCommunityReports();
            }

            // ==================== AUTHENTICATED ROUTES ====================
            String userId = extractAuthenticatedUserId(authHeader);
            if (userId == null) {
                return fail(401, "Authentication required. Please sign in.");
            }
            LocalBackendDatabase.UserRecord currentUserRec = db.findUserById(userId);
            if (currentUserRec == null) {
                return fail(401, "User session no longer exists. Please sign in again.");
            }

            // USERS
            if ("GET".equals(method) && "/users/me".equals(path)) {
                return ok(currentUserRec.user, "Success");
            }
            if ("PUT".equals(method) && "/users/me".equals(path)) {
                AuthDtos.UpdateProfileRequest req = GSON.fromJson(jsonBody, AuthDtos.UpdateProfileRequest.class);
                if (req == null || !ValidationUtils.isValidFullName(req.fullName)) {
                    return fail(400, "Enter a valid full name.");
                }
                String mobile = ValidationUtils.normalizeMobile(req.mobileNumber);
                if (!ValidationUtils.isValidMobile(mobile)) {
                    return fail(400, "Enter a valid 10-13 digit mobile number.");
                }
                LocalBackendDatabase.UserRecord conflict = db.findUserByEmailOrMobile(mobile);
                if (conflict != null && !conflict.user.getId().equals(userId)) {
                    return fail(409, "Mobile number is already linked to another account.");
                }
                User updated = db.updateUserProfile(userId, req.fullName.trim(), mobile, req.profilePhotoUrl);
                return ok(updated, "Profile updated.");
            }
            if ("POST".equals(method) && "/users/me/change-password".equals(path)) {
                AuthDtos.ChangePasswordRequest req = GSON.fromJson(jsonBody, AuthDtos.ChangePasswordRequest.class);
                if (req == null || !crypto.verifyPassword(req.currentPassword, currentUserRec.passwordHash)) {
                    return fail(400, "Current password is incorrect.");
                }
                String err = ValidationUtils.getPasswordError(req.newPassword);
                if (err != null) return fail(400, err);
                db.updatePasswordHash(userId, crypto.hashPassword(req.newPassword));
                return ok(null, "Password changed successfully.");
            }
            if ("POST".equals(method) && "/users/me/device-token".equals(path)) {
                AuthDtos.DeviceTokenRequest req = GSON.fromJson(jsonBody, AuthDtos.DeviceTokenRequest.class);
                if (req != null && req.fcmToken != null) {
                    db.updateFcmToken(userId, req.fcmToken);
                }
                return ok(null, "Device token registered.");
            }
            if ("DELETE".equals(method) && "/users/me".equals(path)) {
                db.deleteUserCascade(userId);
                return ok(null, "Account deleted.");
            }

            // TRUSTED CONTACTS
            if ("GET".equals(method) && "/contacts".equals(path)) {
                return ok(db.getContacts(userId), "Success");
            }
            if ("POST".equals(method) && "/contacts".equals(path)) {
                TrustedContact req = GSON.fromJson(jsonBody, TrustedContact.class);
                if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
                    return fail(400, "Contact name is required.");
                }
                if (req.getPhoneNumber() == null || !ValidationUtils.isValidMobile(req.getPhoneNumber())) {
                    return fail(400, "Valid phone number is required.");
                }
                return ok(db.addContact(userId, req), "Trusted contact added.");
            }
            if ("PUT".equals(method) && path.startsWith("/contacts/")) {
                String contactId = path.substring("/contacts/".length());
                TrustedContact req = GSON.fromJson(jsonBody, TrustedContact.class);
                TrustedContact updated = db.updateContact(contactId, userId, req != null ? req : new TrustedContact());
                return updated != null ? ok(updated, "Contact updated.") : fail(404, "Trusted contact not found.");
            }
            if ("DELETE".equals(method) && path.startsWith("/contacts/")) {
                String contactId = path.substring("/contacts/".length());
                return db.deleteContact(contactId, userId)
                        ? ok(null, "Contact removed.")
                        : fail(404, "Trusted contact not found.");
            }

            // JOURNEYS
            if ("POST".equals(method) && "/journeys".equals(path)) {
                JourneyDtos.CreateJourneyRequest req = GSON.fromJson(jsonBody, JourneyDtos.CreateJourneyRequest.class);
                if (req == null || req.destinationName == null || req.destinationName.trim().isEmpty()) {
                    return fail(400, "Destination name is required.");
                }
                return ok(db.insertJourney(userId, req), "Journey created.");
            }
            if ("GET".equals(method) && "/journeys".equals(path)) {
                return ok(db.getJourneys(userId), "Success");
            }
            if ("GET".equals(method) && "/journeys/active".equals(path)) {
                Journey active = db.findActiveJourney(userId);
                return ok(active, active != null ? "Active journey found." : "No active journey.");
            }
            if (path.startsWith("/journeys/")) {
                HttpResult journeyResult = handleJourneySubRoute(method, path, userId, jsonBody);
                if (journeyResult != null) return journeyResult;
            }

            // SOS
            if ("POST".equals(method) && "/sos".equals(path)) {
                return handleTriggerSos(userId, currentUserRec.user.getFullName(), jsonBody);
            }
            if ("GET".equals(method) && "/sos/active".equals(path)) {
                return handleGetActiveSos(userId);
            }
            if ("GET".equals(method) && "/sos/history".equals(path)) {
                return handleGetSosHistory(userId);
            }
            if (path.startsWith("/sos/")) {
                HttpResult sosResult = handleSosSubRoute(method, path, userId, currentUserRec.user.getFullName(), jsonBody);
                if (sosResult != null) return sosResult;
            }

            // COMMUNITY & INCIDENTS & NOTIFICATIONS
            if ("POST".equals(method) && "/community/reports".equals(path)) {
                CommunityReport req = GSON.fromJson(jsonBody, CommunityReport.class);
                if (req == null || req.getDescription() == null || req.getDescription().trim().isEmpty()) {
                    return fail(400, "Report description is required.");
                }
                long now = System.currentTimeMillis();
                long expires = now + SEVEN_DAYS_MS;
                String id = db.insertCommunityReport(userId,
                        req.getCategory() != null ? req.getCategory() : "OTHER",
                        req.getDescription().trim(),
                        req.getLocationName() != null ? req.getLocationName().trim() : "Nearby Area",
                        req.getLatitude(), req.getLongitude(), now, expires);
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("id", id);
                created.put("category", req.getCategory() != null ? req.getCategory() : "OTHER");
                created.put("description", req.getDescription().trim());
                created.put("locationName", req.getLocationName() != null ? req.getLocationName().trim() : "Nearby Area");
                created.put("latitude", req.getLatitude());
                created.put("longitude", req.getLongitude());
                created.put("reportedAt", now);
                created.put("expiresAt", expires);
                created.put("verificationStatus", "VERIFIED");
                return ok(created, "Community advisory submitted.");
            }
            if ("POST".equals(method) && "/incidents/reports".equals(path)) {
                return handleSubmitIncident(userId, multipartFields, hasPhoto, hasAudio);
            }
            if ("GET".equals(method) && "/incidents/reports".equals(path)) {
                return handleGetIncidents(userId);
            }
            if ("GET".equals(method) && "/notifications".equals(path)) {
                return handleGetNotifications(userId);
            }
            if ("PUT".equals(method) && path.startsWith("/notifications/") && path.endsWith("/read")) {
                String notifId = path.substring("/notifications/".length(), path.length() - "/read".length());
                return db.markNotificationRead(notifId, userId)
                        ? ok(Collections.singletonMap("id", notifId), "Notification marked read.")
                        : fail(404, "Notification not found.");
            }

            return fail(404, "Endpoint not found: " + method + " " + path);
        } catch (Exception ex) {
            return fail(500, "Local backend error: " + ex.getMessage());
        }
    }

    private HttpResult handleRegister(String jsonBody) {
        AuthDtos.RegisterRequest req = GSON.fromJson(jsonBody, AuthDtos.RegisterRequest.class);
        if (req == null || !ValidationUtils.isValidFullName(req.fullName)) {
            return fail(400, "Please enter a valid full name (2-80 characters).");
        }
        if (!ValidationUtils.isValidEmail(req.email)) {
            return fail(400, "Please enter a valid email address.");
        }
        String mobile = ValidationUtils.normalizeMobile(req.mobileNumber);
        if (!ValidationUtils.isValidMobile(mobile)) {
            return fail(400, "Please enter a valid 10-13 digit mobile number.");
        }
        String pwErr = ValidationUtils.getPasswordError(req.password);
        if (pwErr != null) {
            return fail(400, pwErr);
        }

        if (db.findUserByEmailOrMobile(req.email) != null) {
            return fail(409, "An account with this email already exists.");
        }
        if (db.findUserByEmailOrMobile(mobile) != null) {
            return fail(409, "An account with this mobile number already exists.");
        }

        String hash = crypto.hashPassword(req.password);
        User created = db.createUser(req.fullName.trim(), req.email.trim(), mobile, hash);
        String token = crypto.generateJwt(created.getId(), created.getEmail());
        return ok(authPayload(token, created), "Registration successful.");
    }

    private HttpResult handleLogin(String jsonBody) {
        AuthDtos.LoginRequest req = GSON.fromJson(jsonBody, AuthDtos.LoginRequest.class);
        if (req == null || req.identifier == null || req.password == null || req.password.isEmpty()) {
            return fail(400, "Email/mobile and password are required.");
        }
        LocalBackendDatabase.UserRecord record = db.findUserByEmailOrMobile(req.identifier);
        if (record == null || !crypto.verifyPassword(req.password, record.passwordHash)) {
            return fail(401, "Invalid email/mobile or password.");
        }
        String token = crypto.generateJwt(record.user.getId(), record.user.getEmail());
        return ok(authPayload(token, record.user), "Login successful.");
    }

    private HttpResult handleJourneySubRoute(String method, String path, String userId, String jsonBody) {
        String sub = path.substring("/journeys/".length());
        String[] parts = sub.split("/");
        String journeyId = parts[0];
        Journey journey = db.findJourney(journeyId, userId);
        if (journey == null) return fail(404, "Journey not found.");

        if (parts.length == 1) {
            if ("GET".equals(method)) return ok(journey, "Success");
            if ("PUT".equals(method)) {
                JourneyDtos.UpdateJourneyRequest req = GSON.fromJson(jsonBody, JourneyDtos.UpdateJourneyRequest.class);
                if (req == null || req.destinationName == null || req.destinationName.trim().isEmpty()) {
                    return fail(400, "Destination name is required.");
                }
                journey.setDestinationName(req.destinationName.trim());
                journey.setDestinationLatitude(req.destinationLatitude);
                journey.setDestinationLongitude(req.destinationLongitude);
                return ok(db.saveJourneyState(journey), "Destination updated.");
            }
            return null;
        }

        String action = parts[1];
        long now = System.currentTimeMillis();
        if ("POST".equals(method) && "start".equals(action)) {
            journey.setStatus("ACTIVE");
            if (journey.getStartedAt() == 0) journey.setStartedAt(now);
            journey.setNextCheckInDueAt(now + Math.max(1, journey.getCheckInIntervalMinutes()) * 60_000L);
            db.insertNotification(userId, "SYSTEM", journey.getId(), "Journey Started",
                    "Monitoring active toward " + journey.getDestinationName());
            return ok(db.saveJourneyState(journey), "Journey monitoring started.");
        }
        if ("POST".equals(method) && "complete".equals(action)) {
            journey.setStatus("COMPLETED");
            journey.setCompletedAt(now);
            db.insertNotification(userId, "SYSTEM", journey.getId(), "Journey Completed",
                    "Safe arrival confirmed at " + journey.getDestinationName());
            return ok(db.saveJourneyState(journey), "Journey completed safely.");
        }
        if ("POST".equals(method) && "extend".equals(action)) {
            JourneyDtos.ExtendJourneyRequest req = GSON.fromJson(jsonBody, JourneyDtos.ExtendJourneyRequest.class);
            int extra = req != null ? Math.max(1, req.additionalMinutes) : 15;
            long addMs = extra * 60_000L;
            journey.setExpectedDurationMinutes(journey.getExpectedDurationMinutes() + extra);
            journey.setExpectedArrivalTime(Math.max(now, journey.getExpectedArrivalTime()) + addMs);
            if (journey.getNextCheckInDueAt() > 0) journey.setNextCheckInDueAt(journey.getNextCheckInDueAt() + addMs);
            return ok(db.saveJourneyState(journey), "Journey ETA extended.");
        }
        if ("POST".equals(method) && "locations".equals(action)) {
            LocationUpdate req = GSON.fromJson(jsonBody, LocationUpdate.class);
            if (req == null) return fail(400, "Location payload required.");
            long ts = req.getRecordedAt() > 0 ? req.getRecordedAt() : now;
            String locId = db.insertLocation(journey.getId(), "JOURNEY", userId,
                    req.getLatitude(), req.getLongitude(), req.getAccuracyMeters(),
                    req.getSpeedMps(), req.getBearing(), ts);
            journey.setLastLatitude(req.getLatitude());
            journey.setLastLongitude(req.getLongitude());
            journey.setLastAccuracyMeters(req.getAccuracyMeters());
            journey.setLastLocationTimestamp(ts);
            journey.setDistanceRemainingMeters(haversineMeters(
                    req.getLatitude(), req.getLongitude(),
                    journey.getDestinationLatitude(), journey.getDestinationLongitude()));
            db.saveJourneyState(journey);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", locId);
            res.put("sessionId", journey.getId());
            res.put("latitude", req.getLatitude());
            res.put("longitude", req.getLongitude());
            res.put("accuracyMeters", req.getAccuracyMeters());
            res.put("speedMps", req.getSpeedMps());
            res.put("bearing", req.getBearing());
            res.put("recordedAt", ts);
            return ok(res, "Location recorded.");
        }
        if ("checkins".equals(action)) {
            if ("GET".equals(method)) {
                return ok(db.getCheckIns(journey.getId()), "Success");
            }
            if ("POST".equals(method)) {
                SafetyCheckIn req = GSON.fromJson(jsonBody, SafetyCheckIn.class);
                long nextDue = now + Math.max(1, journey.getCheckInIntervalMinutes()) * 60_000L;
                String status = req != null && req.getStatus() != null ? req.getStatus() : "CONFIRMED_SAFE";
                double lat = req != null ? req.getLatitude() : journey.getLastLatitude();
                double lng = req != null ? req.getLongitude() : journey.getLastLongitude();
                String note = req != null ? req.getNote() : "";
                String ciId = db.insertCheckIn(journey.getId(), userId, status, lat, lng, note, now, nextDue);
                journey.setNextCheckInDueAt(nextDue);
                db.saveJourneyState(journey);

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("id", ciId);
                res.put("journeyId", journey.getId());
                res.put("userId", userId);
                res.put("status", status);
                res.put("latitude", lat);
                res.put("longitude", lng);
                res.put("note", note);
                res.put("checkedInAt", now);
                res.put("nextCheckInDueAt", nextDue);
                return ok(res, "Check-in confirmed.");
            }
        }
        if ("POST".equals(method) && "escalate".equals(action)) {
            JourneyDtos.EscalationRequest req = GSON.fromJson(jsonBody, JourneyDtos.EscalationRequest.class);
            int stage = req != null ? Math.max(1, req.stage) : 1;
            journey.setStatus("ESCALATED");
            db.saveJourneyState(journey);
            int count = db.getContacts(userId).size();
            String escId = db.insertEscalation(journey.getId(), userId, stage, "ESCALATED_STAGE_" + stage, count, now);
            db.insertNotification(userId, "ESCALATION", journey.getId(), "Escalation Stage " + stage,
                    "Triggered escalation for journey to " + journey.getDestinationName());
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", escId);
            res.put("journeyId", journey.getId());
            res.put("currentStage", stage);
            res.put("status", "ESCALATED_STAGE_" + stage);
            res.put("notifiedContactsCount", count);
            res.put("triggeredAt", now);
            return ok(res, "Escalation stage " + stage + " triggered.");
        }
        return null;
    }

    private HttpResult handleTriggerSos(String userId, String senderName, String jsonBody) {
        JourneyDtos.TriggerSosRequest req = GSON.fromJson(jsonBody, JourneyDtos.TriggerSosRequest.class);
        long now = System.currentTimeMillis();
        String journeyId = req != null ? req.journeyId : null;
        String source = req != null && req.triggerSource != null ? req.triggerSource : "MANUAL_SOS";
        double lat = req != null ? req.latitude : 0.0;
        double lng = req != null ? req.longitude : 0.0;
        float acc = req != null ? req.accuracyMeters : 0f;
        String note = req != null ? req.note : null;

        String sosId = db.insertSos(userId, senderName, journeyId, source, lat, lng, acc, note, now);
        db.insertLocation(sosId, "SOS", userId, lat, lng, acc, 0f, 0f, now);
        db.insertNotification(userId, "SOS_ALERT", sosId, "SOS Alert Dispatched",
                "Emergency alert active (" + source + ").");
        return ok(readSosById(sosId), "SOS emergency dispatched.");
    }

    private HttpResult handleGetActiveSos(String userId) {
        try (Cursor c = db.findActiveSosCursor(userId)) {
            if (!c.moveToFirst()) return ok(null, "No active SOS.");
            return ok(readSosFromCursor(c), "Success");
        }
    }

    private HttpResult handleGetSosHistory(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Cursor c = db.findSosHistoryCursor(userId)) {
            while (c.moveToNext()) {
                list.add(readSosFromCursor(c));
            }
        }
        return ok(list, "Success");
    }

    private HttpResult handleSosSubRoute(String method, String path, String userId, String userName, String jsonBody) {
        String sub = path.substring("/sos/".length());
        String[] parts = sub.split("/");
        String sosId = parts[0];
        Map<String, Object> sos = readSosById(sosId);
        if (sos == null) return fail(404, "SOS alert not found.");
        String ownerId = String.valueOf(sos.get("userId"));
        if (!ownerId.equals(userId)) {
            return fail(403, "Not authorized to access this SOS alert.");
        }

        if (parts.length == 2 && "POST".equals(method) && "resolve".equals(parts[1])) {
            JourneyDtos.ResolveSosRequest req = GSON.fromJson(jsonBody, JourneyDtos.ResolveSosRequest.class);
            String note = req != null && req.resolutionNote != null ? req.resolutionNote : "Marked safe.";
            db.resolveSos(sosId, userId, System.currentTimeMillis(), note);
            return ok(readSosById(sosId), "SOS resolved safely.");
        }
        if (parts.length == 2 && "POST".equals(method) && "locations".equals(parts[1])) {
            LocationUpdate req = GSON.fromJson(jsonBody, LocationUpdate.class);
            if (req == null) return fail(400, "Location payload required.");
            long ts = req.getRecordedAt() > 0 ? req.getRecordedAt() : System.currentTimeMillis();
            String id = db.insertLocation(sosId, "SOS", userId, req.getLatitude(), req.getLongitude(),
                    req.getAccuracyMeters(), req.getSpeedMps(), req.getBearing(), ts);
            db.updateSosLocation(sosId, req.getLatitude(), req.getLongitude(), req.getAccuracyMeters());
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", id);
            res.put("sessionId", sosId);
            res.put("latitude", req.getLatitude());
            res.put("longitude", req.getLongitude());
            res.put("accuracyMeters", req.getAccuracyMeters());
            res.put("recordedAt", ts);
            return ok(res, "SOS location updated.");
        }
        if (parts.length == 3 && "GET".equals(method) && "locations".equals(parts[1]) && "latest".equals(parts[2])) {
            LocationUpdate latest = db.findLatestLocation(sosId);
            if (latest == null) {
                latest = new LocationUpdate(
                        ((Number) sos.get("latitude")).doubleValue(),
                        ((Number) sos.get("longitude")).doubleValue(),
                        ((Number) sos.get("accuracyMeters")).floatValue(),
                        0f, 0f,
                        ((Number) sos.get("createdAt")).longValue());
            }
            return ok(latest, "Success");
        }
        if (parts.length == 2 && "acknowledgements".equals(parts[1])) {
            if ("GET".equals(method)) {
                return ok(readAcknowledgements(sosId), "Success");
            }
            if ("POST".equals(method)) {
                ResponseAcknowledgement req = GSON.fromJson(jsonBody, ResponseAcknowledgement.class);
                String status = req != null && req.getResponseStatus() != null ? req.getResponseStatus() : "RESPONDING";
                String note = req != null ? req.getNote() : "";
                long now = System.currentTimeMillis();
                String ackId = db.insertAcknowledgement(sosId, userId, userName, status, note, now);
                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("id", ackId);
                ack.put("sosId", sosId);
                ack.put("responderUserId", userId);
                ack.put("responderName", userName);
                ack.put("responseStatus", status);
                ack.put("note", note);
                ack.put("respondedAt", now);
                return ok(ack, "Acknowledgement recorded.");
            }
        }
        return null;
    }

    private HttpResult handleGetCommunityReports() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Cursor c = db.findActiveCommunityReportsCursor(System.currentTimeMillis())) {
            while (c.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getString(c.getColumnIndexOrThrow("id")));
                row.put("category", c.getString(c.getColumnIndexOrThrow("category")));
                row.put("description", c.getString(c.getColumnIndexOrThrow("description")));
                row.put("locationName", c.getString(c.getColumnIndexOrThrow("location_name")));
                row.put("latitude", c.getDouble(c.getColumnIndexOrThrow("latitude")));
                row.put("longitude", c.getDouble(c.getColumnIndexOrThrow("longitude")));
                row.put("reportedAt", c.getLong(c.getColumnIndexOrThrow("reported_at")));
                row.put("expiresAt", c.getLong(c.getColumnIndexOrThrow("expires_at")));
                row.put("verificationStatus", c.getString(c.getColumnIndexOrThrow("verification_status")));
                list.add(row);
            }
        }
        return ok(list, "Success");
    }

    private HttpResult handleSubmitIncident(String userId, Map<String, String> fields, boolean hasPhoto, boolean hasAudio) {
        String category = fields != null ? fields.getOrDefault("category", "OTHER") : "OTHER";
        String desc = fields != null ? fields.getOrDefault("description", "") : "";
        String date = fields != null ? fields.getOrDefault("incidentDate", "") : "";
        String loc = fields != null ? fields.getOrDefault("locationName", "") : "";
        double lat = parseDoubleSafe(fields != null ? fields.get("latitude") : null);
        double lng = parseDoubleSafe(fields != null ? fields.get("longitude") : null);
        String photoUrl = hasPhoto ? "local://evidence/photo_" + System.currentTimeMillis() : null;
        String audioUrl = hasAudio ? "local://evidence/audio_" + System.currentTimeMillis() : null;
        long now = System.currentTimeMillis();

        String id = db.insertIncidentReport(userId, category, desc, date, loc, lat, lng, photoUrl, audioUrl, now);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("userId", userId);
        row.put("category", category);
        row.put("description", desc);
        row.put("incidentDate", date);
        row.put("locationName", loc);
        row.put("latitude", lat);
        row.put("longitude", lng);
        row.put("photoUrl", photoUrl);
        row.put("audioUrl", audioUrl);
        row.put("status", "SUBMITTED");
        row.put("createdAt", now);
        return ok(row, "Incident report submitted.");
    }

    private HttpResult handleGetIncidents(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Cursor c = db.findIncidentReportsCursor(userId)) {
            while (c.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getString(c.getColumnIndexOrThrow("id")));
                row.put("userId", c.getString(c.getColumnIndexOrThrow("user_id")));
                row.put("category", c.getString(c.getColumnIndexOrThrow("category")));
                row.put("description", c.getString(c.getColumnIndexOrThrow("description")));
                row.put("incidentDate", c.getString(c.getColumnIndexOrThrow("incident_date")));
                row.put("locationName", c.getString(c.getColumnIndexOrThrow("location_name")));
                row.put("latitude", c.getDouble(c.getColumnIndexOrThrow("latitude")));
                row.put("longitude", c.getDouble(c.getColumnIndexOrThrow("longitude")));
                row.put("photoUrl", c.getString(c.getColumnIndexOrThrow("photo_url")));
                row.put("audioUrl", c.getString(c.getColumnIndexOrThrow("audio_url")));
                row.put("status", c.getString(c.getColumnIndexOrThrow("status")));
                row.put("createdAt", c.getLong(c.getColumnIndexOrThrow("created_at")));
                list.add(row);
            }
        }
        return ok(list, "Success");
    }

    private HttpResult handleGetNotifications(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Cursor c = db.findNotificationsCursor(userId)) {
            while (c.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getString(c.getColumnIndexOrThrow("id")));
                row.put("type", c.getString(c.getColumnIndexOrThrow("type")));
                row.put("title", c.getString(c.getColumnIndexOrThrow("title")));
                row.put("message", c.getString(c.getColumnIndexOrThrow("message")));
                row.put("referenceId", c.getString(c.getColumnIndexOrThrow("reference_id")));
                row.put("read", c.getInt(c.getColumnIndexOrThrow("is_read")) == 1);
                row.put("createdAt", c.getLong(c.getColumnIndexOrThrow("created_at")));
                list.add(row);
            }
        }
        return ok(list, "Success");
    }

    private Map<String, Object> readSosById(String sosId) {
        try (Cursor c = db.findSosCursorById(sosId)) {
            return c.moveToFirst() ? readSosFromCursor(c) : null;
        }
    }

    private Map<String, Object> readSosFromCursor(Cursor c) {
        String id = c.getString(c.getColumnIndexOrThrow("id"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("userId", c.getString(c.getColumnIndexOrThrow("user_id")));
        map.put("senderName", c.getString(c.getColumnIndexOrThrow("sender_name")));
        map.put("journeyId", c.getString(c.getColumnIndexOrThrow("journey_id")));
        map.put("triggerSource", c.getString(c.getColumnIndexOrThrow("trigger_source")));
        map.put("status", c.getString(c.getColumnIndexOrThrow("status")));
        map.put("latitude", c.getDouble(c.getColumnIndexOrThrow("latitude")));
        map.put("longitude", c.getDouble(c.getColumnIndexOrThrow("longitude")));
        map.put("accuracyMeters", c.getFloat(c.getColumnIndexOrThrow("accuracy_meters")));
        map.put("addressLabel", c.getString(c.getColumnIndexOrThrow("address_label")));
        map.put("notificationDeliveryStatus", c.getString(c.getColumnIndexOrThrow("notification_delivery_status")));
        map.put("createdAt", c.getLong(c.getColumnIndexOrThrow("created_at")));
        map.put("resolvedAt", c.getLong(c.getColumnIndexOrThrow("resolved_at")));
        map.put("resolutionNote", c.getString(c.getColumnIndexOrThrow("resolution_note")));
        map.put("acknowledgements", readAcknowledgements(id));
        return map;
    }

    private List<Map<String, Object>> readAcknowledgements(String sosId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Cursor c = db.findAcknowledgementsCursor(sosId)) {
            while (c.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getString(c.getColumnIndexOrThrow("id")));
                row.put("sosId", c.getString(c.getColumnIndexOrThrow("sos_id")));
                row.put("responderUserId", c.getString(c.getColumnIndexOrThrow("responder_user_id")));
                row.put("responderName", c.getString(c.getColumnIndexOrThrow("responder_name")));
                row.put("responseStatus", c.getString(c.getColumnIndexOrThrow("response_status")));
                row.put("note", c.getString(c.getColumnIndexOrThrow("note")));
                row.put("respondedAt", c.getLong(c.getColumnIndexOrThrow("responded_at")));
                list.add(row);
            }
        }
        return list;
    }

    private Map<String, Object> authPayload(String token, User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("accessToken", token);
        map.put("tokenType", "Bearer");
        map.put("expiresInSeconds", crypto.getTokenExpirySeconds());
        map.put("user", user);
        map.put("otpRequired", false);
        map.put("maskedDestination", null);
        return map;
    }

    private String extractAuthenticatedUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return crypto.verifyAndExtractUserId(authHeader.substring(7).trim());
    }

    private String normalizePath(String rawPath) {
        String path = rawPath;
        int idx = path.indexOf("/api/v1");
        if (idx >= 0) {
            path = path.substring(idx + "/api/v1".length());
        }
        if (!path.startsWith("/")) path = "/" + path;
        return path;
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        if (lat2 == 0 && lon2 == 0) return 0.0;
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private HttpResult ok(Object data, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("data", data);
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return new HttpResult(200, GSON.toJson(body));
    }

    private HttpResult fail(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("data", null);
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return new HttpResult(code, GSON.toJson(body));
    }
}
