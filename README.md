# SafeMe – Person Safety and Emergency Alert System

**Tagline:** *Your Safety, Our Priority*  
**Project Type:** Final-Year Diploma in Information Technology Capstone Project  
**Client Stack:** Java (JDK 11) • XML Layouts • Material Design 3 • MVVM + Repository • Room Offline Cache • Retrofit + OkHttp + Gson • Google Maps SDK • Fused Location Provider • Firebase Cloud Messaging (FCM)  
**Backend Stack:** Java Spring Boot 3 • Spring Security (BCrypt + JWT) • MongoDB • Firebase Admin SDK

---

## 1. Architecture Overview

```
+-------------------------------------------------------------------+
|                      SafeMe Android Client                        |
|  - Pure Java + XML UI (Material Design 3, Purple/Lavender/Red)    |
|  - MVVM ViewModels + Repository Pattern + LiveData                |
|  - Android Keystore AES-256-GCM Encrypted JWT Storage             |
|  - Foreground Location Service (Journey & SOS Live GPS)           |
|  - CheckInTimerManager (Duplicate Check-in/Escalation Guard)      |
|  - Room Local Cache (Offline Resilience)                          |
+---------------------------------+---------------------------------+
                                  |  REST JSON (JWT Bearer) + FCM
                                  v
+-------------------------------------------------------------------+
|                  Java Spring Boot REST Backend                    |
|  - /api/v1/auth/*, /users/*, /contacts/*, /journeys/*, /sos/*     |
|  - /api/v1/community-reports/*, /incidents/*, /notifications/*    |
|  - Spring Security + BCrypt + JWT Verification + FCM Admin SDK    |
+---------------------------------+---------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                          MongoDB Database                         |
|  12 Collections with ObjectIds, Unique & Geospatial Indexes       |
+-------------------------------------------------------------------+
```

---

## 2. Implemented Screens (25 Total, A–Y)

### Intro & Authentication Activities
- **A. Splash Screen (`SplashActivity`)**: Branded splash screen checking session validity and onboarding state.
- **B. Onboarding (`OnboardingActivity`)**: 3 swipeable ViewPager2 introduction slides with custom illustrations.
- **C. Permissions Intro (`PermissionsActivity`)**: Transparent rationale + real runtime permission checks (`PermissionHelper`) for GPS, Background Location, Notifications, Camera, Microphone, and Phone.
- **D. Registration (`RegisterActivity`)**: Full name, email, mobile number, password strength check, and terms consent.
- **E. OTP Verification (`OtpVerificationActivity`)**: 6-digit OTP verification and resend timer backed by `/auth/verify-otp` & `/auth/resend-otp`.
- **F. Login (`LoginActivity`)**: Email/mobile + password sign-in with JWT persistence (`SecureTokenManager`).
- **G. Forgot & Reset Password (`ForgotPasswordActivity`)**: Request reset OTP, verify, and set new password.
- **T. Fake Call Tool (`FakeCallActivity`)**: Configurable caller name/number/delay (Instant, 5s, 15s, 30s) with ringtone and full-screen answer/decline UI.

### Main Shell (`MainActivity` + 5-Tab Bottom Navigation)
- **H. Home Dashboard (`HomeFragment`)**: User welcome header, real GPS/network/contact counts, active journey banner, big red SOS button, and quick action shortcuts.
- **I. Start Journey (`StartJourneyFragment`)**: GPS origin detection, `Geocoder` destination lookup, Google Map route preview, arrival time picker, 6 transport mode chips, check-in interval, contact selector, and `JourneyLocationService` launch.
- **J. Active Journey (`ActiveJourneyFragment`)**: Real-time Google Map marker, GPS freshness badge (`Live`, `Updated Xs ago`, `Stale`), Check In Now, Extend +15m, Change Destination, Share Journey, End Journey, and Emergency SOS.
- **K. Safety Check-in (`SafetyCheckInFragment`)**: "Are you safe?" prompt with response countdown, duplicate submission guard, Confirm Safe, Need Help (SOS), and automatic transition to Escalation on timeout.
- **L. Emergency SOS (`EmergencySosFragment`)**: Red SOS trigger button, live GPS tracking (`JourneyLocationService`), responder acknowledgement list, and safe resolution.
- **M. Emergency Escalation (`EmergencyEscalationFragment`)**: 3-stage escalation monitor (Reminder -> Priority 1 Contacts -> All Contacts + SOS) with duplicate stage guard.
- **Safety Hub (`SafetyHubFragment`)**: Central Safety tab launching SOS, Contacts, Respond to SOS, Community Map, Incidents, Helplines, Fake Call, Voice Trigger, and Escalation.
- **N. Trusted Contacts (`TrustedContactsFragment`)**: Manage SafeMe registered or external SMS contacts with priority levels 1–3 and notification channel preferences.
- **O. Contact Details (`ContactDetailFragment`)**: Verification/mutual consent status, call dialer, edit, and remove contact.
- **P. Emergency Response Acknowledgement (`SosAcknowledgementFragment`)**: Responder screen to fetch latest sender GPS coordinates and submit `RESPONDING`, `CONTACTED_ASSISTANCE`, or `CANNOT_RESPOND`.
- **Q. Community Safety Map (`CommunitySafetyMapFragment`)**: Google Map markers + advisory list (`POOR_LIGHTING`, `ROAD_HAZARD`, `HARASSMENT`, `UNSAFE_AREA`, `OTHER`) excluding PII.
- **R. Incident Reporting (`IncidentReportFragment`)**: Category, description, date, GPS location, Android Photo Picker (`PickVisualMedia`) + audio attachment (`GetContent`), multipart upload.
- **S. Emergency Helplines (`HelplinesFragment`)**: India emergency numbers (`112`, `100`, `108`, `1091`, `7827170170`, `1098`, `1930`) with confirmation dialog before `Intent.ACTION_DIAL`.
- **U. Voice Emergency (`VoiceEmergencyFragment`)**: Explicit user-initiated `SpeechRecognizer` testing for configurable emergency phrase (zero hidden background audio capture).
- **V. Profile (`ProfileFragment`)**: Profile summary, edit details, change password, contacts, permissions, settings, about, logout, and permanent account deletion.
- **W. Activity History (`ActivityHistoryFragment`)**: Combined chronological timeline of journeys, SOS alerts, and incident reports with category filter chips.
- **X. Settings (`SettingsFragment`)**: Runtime configurable Spring Boot API Base URL, check-in/escalation timers, high-accuracy GPS toggle, lock-screen privacy toggle, Light/Dark/System theme, and language preference.
- **Y. About (`AboutFragment`)**: Capstone summary, privacy guarantees, and emergency support notice.

---

## 3. Configuration & Setup

### Secrets & Environment Variables
Configure the following in AI Studio **Secrets** (or `.env` locally, never commit `.env` with real secrets):
- `API_BASE_URL=http://10.0.2.2:8080/api/v1/` (Default Android Emulator host loopback; can also be changed at runtime inside **Settings -> Backend Server Configuration**).
- `MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY` (Google Maps SDK for Android key).

### Firebase Cloud Messaging (FCM)
1. Create a Firebase project and register Android package `com.aistudio.safeher.kqwnpz`.
2. Download `google-services.json` and place it in `app/google-services.json` (the project uses `MissingGoogleServicesStrategy.WARN` so local builds succeed while awaiting your Firebase config).
3. Configure your Spring Boot backend with Firebase Admin SDK credentials to dispatch push notifications to stored FCM device tokens.

### Backend Server (`/backend`)
The Java 17 + Spring Boot 3 + MongoDB REST API implementation is located in `/backend`.
1. Start MongoDB locally (or set `MONGODB_URI` to your MongoDB Atlas connection string).
2. Configure environment variables from `backend/.env.example` (optionally set `SAFEME_BOOTSTRAP_EMAIL` and `SAFEME_BOOTSTRAP_PASSWORD` to pre-create an initial account, or register directly from the app's **Register** screen).
3. Start the backend server:
```bash
cd backend && mvn spring-boot:run
```
4. In the SafeMe Android app, tap **Register** to create an email & password account or sign in on the **Login** screen with your email & password. Tap **Configure Backend API URL** if your backend is hosted on a LAN IP or HTTPS tunnel instead of `http://10.0.2.2:8080/api/v1/`.

### Backend API & Database Specification
See [`docs/BACKEND_SPRING_BOOT_SPEC.md`](docs/BACKEND_SPRING_BOOT_SPEC.md) for the complete Spring Boot REST API contracts, JWT security architecture, and 12 MongoDB collection schemas & indexes.

---

## 4. Build & Test Commands

Run unit + Robolectric JVM tests:
```bash
gradle :app:testDebugUnitTest
```

Build debug APK:
```bash
gradle :app:assembleDebug
```
