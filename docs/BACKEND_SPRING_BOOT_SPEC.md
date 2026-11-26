# SafeMe – Spring Boot REST API & MongoDB Specification

This document specifies the backend contract consumed by `com.safeme.app.api.SafeMeApiService`.

## 1. Base URL & Security
- **Default Base Path:** `/api/v1/`
- **Authentication:** Stateless JWT (`Authorization: Bearer <token>`)
- **Password Hashing:** `BCryptPasswordEncoder` (strength >= 10)
- **Input Validation:** Jakarta Bean Validation (`@Valid`) returning `ApiResponse<T>` with `{ success, message, data, errors }`.

---

## 2. REST Endpoints

### Authentication (`/api/v1/auth`)
- `POST /auth/register` — Creates user (`verified=false`), hashes password with BCrypt, generates OTP, returns user + optional pending verification token.
- `POST /auth/verify-otp` — Verifies 6-digit OTP (`emailOrMobile`, `otp`), sets `verified=true`, returns `{ token, user }`.
- `POST /auth/resend-otp` — Generates a fresh OTP with rate-limiting.
- `POST /auth/login` — Validates credentials (`emailOrMobile`, `password`), returns `{ token, user }`.
- `POST /auth/forgot-password` — Sends password reset OTP.
- `POST /auth/reset-password` — Validates reset OTP and updates BCrypt password hash.
- `POST /auth/logout` — Invalidates active device session / clears FCM token association.

### Users (`/api/v1/users`)
- `GET /users/me` — Returns authenticated user profile.
- `PUT /users/me` — Updates `fullName`, `mobileNumber`, `profilePhotoUrl`.
- `POST /users/me/change-password` — Verifies `currentPassword` and saves `newPassword`.
- `POST /users/me/device-token` — Saves/updates device FCM token for push delivery.
- `DELETE /users/me` — Deletes user account and cascades/revokes personal data.

### Trusted Contacts (`/api/v1/contacts`)
- `GET /contacts` — Lists owner's trusted contacts sorted by priority (`1`, `2`, `3`).
- `POST /contacts` — Creates `REGISTERED_USER` (links if user exists) or `EXTERNAL_INVITE` contact.
- `PUT /contacts/{id}` — Updates contact priority, relationship, SMS/Push/Location preferences.
- `DELETE /contacts/{id}` — Removes trusted contact.

### Smart Journey Monitoring (`/api/v1/journeys`)
- `POST /journeys` — Creates journey record (`CREATED`).
- `POST /journeys/{id}/start` — Transitions journey to `ACTIVE`, computes `nextCheckInDueAt`, notifies selected contacts.
- `GET /journeys/active` — Returns active journey (`ACTIVE` or `ESCALATED`).
- `GET /journeys` — Returns user journeys sorted descending by `startedAt`.
- `GET /journeys/{id}` — Returns journey details for owner.
- `PUT /journeys/{id}` — Updates destination name & coordinates.
- `POST /journeys/{id}/locations` — Appends GPS breadcrumb (`latitude`, `longitude`, `accuracyMeters`, `speedMps`, `bearing`, `recordedAt`).
- `POST /journeys/{id}/checkins` — Logs `CONFIRMED_SAFE` / `NEED_HELP` check-in and advances `nextCheckInDueAt`.
- `GET /journeys/{id}/checkins` — Lists check-ins for a journey.
- `POST /journeys/{id}/extend` — Adds `additionalMinutes` to `expectedArrivalTime`.
- `POST /journeys/{id}/complete` — Marks journey `COMPLETED`, records `completedAt`, notifies contacts of safe arrival.
- `POST /journeys/{id}/escalate` — Executes stage `1`, `2`, or `3` escalation; Stage 2 notifies Priority 1 contacts, Stage 3 notifies all remaining contacts & creates linked SOS.

### Emergency SOS & Acknowledgements (`/api/v1/sos`)
- `POST /sos` — Creates active `SosAlert`, captures coordinates, queues FCM push notifications to trusted contacts.
- `GET /sos/active` — Returns user's current `ACTIVE` SOS alert (with responder `acknowledgements`).
- `GET /sos/history` — Returns past SOS alerts.
- `POST /sos/{id}/locations` — Streams live SOS GPS coordinates.
- `GET /sos/{id}/locations/latest` — Returns latest responder-authorized GPS point for an active SOS.
- `POST /sos/{id}/resolve` — Marks SOS `RESOLVED` with closing note and notifies contacts.
- `POST /sos/{id}/acknowledgements` — Records responder status (`RESPONDING`, `CONTACTED_ASSISTANCE`, `CANNOT_RESPOND`) and notifies SOS sender.
- `GET /sos/{id}/acknowledgements` — Lists responder acknowledgements for the SOS alert.

### Community Reports, Incidents & Notifications
- `GET /community/reports` — Returns active, non-expired advisories (never returns user PII).
- `POST /community/reports` — Submits area safety advisory (`POOR_LIGHTING`, `ROAD_HAZARD`, `HARASSMENT`, `UNSAFE_AREA`, `OTHER`).
- `GET /incidents/reports` — Lists authenticated user's incident reports.
- `POST /incidents/reports` (`multipart/form-data`) — Saves incident report + optional photo/audio evidence.
- `GET /notifications` — Lists notification audit log for user.
- `PUT /notifications/{id}/read` — Marks notification read.

---

## 3. MongoDB Collections & Indexes (12 Collections)

1. `users` — `_id`, `fullName`, `email` (unique index), `mobileNumber` (unique index), `passwordHash`, `verified`, `fcmToken`, `createdAt`.
2. `otp_verifications` — `_id`, `userId`, `target`, `purpose` (`SIGNUP`, `PASSWORD_RESET`), `otpHash`, `expiresAt` (TTL index), `attempts`.
3. `trusted_contacts` — `_id`, `ownerUserId` (indexed), `linkedSafeMeUserId`, `name`, `phoneNumber`, `relationship`, `contactType`, `priority`, `verificationStatus`, `mutualConsentGranted`, `notifySms`, `notifyPush`, `shareLiveLocation`.
4. `journeys` — `_id`, `userId` (indexed + `status`), `startLocationName`, `startCoords` (`2dsphere`), `destinationName`, `destinationCoords`, `expectedDurationMinutes`, `expectedArrivalTime`, `transportMode`, `trustedContactIds`, `checkInIntervalMinutes`, `monitoringEnabled`, `status`, `startedAt`, `completedAt`, `nextCheckInDueAt`.
5. `safety_checkins` — `_id`, `journeyId` (indexed), `userId`, `status`, `latitude`, `longitude`, `note`, `timestamp`.
6. `sos_alerts` — `_id`, `userId` (indexed + `status`), `journeyId`, `triggerSource`, `status`, `location` (`2dsphere`), `accuracyMeters`, `notificationDeliveryStatus`, `createdAt`, `resolvedAt`, `resolutionNote`.
7. `location_updates` — `_id`, `sessionId` (indexed + `timestamp`), `sessionType` (`JOURNEY`, `SOS`), `userId`, `location` (`2dsphere`), `accuracyMeters`, `speedMps`, `bearing`, `timestamp`.
8. `emergency_escalations` — `_id`, `journeyId` (indexed), `userId`, `currentStage`, `status`, `notifiedContactIds`, `triggeredAt`, `resolvedAt`.
9. `response_acknowledgements` — `_id`, `sosId` (indexed), `responderUserId`, `responderName`, `responseStatus`, `note`, `respondedAt`.
10. `community_reports` — `_id`, `category`, `description`, `locationName`, `location` (`2dsphere`), `reportedAt`, `expiresAt` (TTL index), `verificationStatus` (PII excluded from public projections).
11. `incident_reports` — `_id`, `userId` (indexed), `category`, `description`, `incidentDate`, `locationName`, `latitude`, `longitude`, `photoUrl`, `audioUrl`, `status`, `createdAt`.
12. `notifications` — `_id`, `recipientUserId` (indexed), `type`, `referenceId`, `title`, `body`, `read`, `createdAt`.
