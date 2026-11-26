package com.safeme.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.example.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Android Keystore-backed secure storage for JWT tokens, user session metadata,
 * configurable backend base URL, and user safety preferences.
 */
public class SecureTokenManager {

    private static final String TAG = "SecureTokenManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "SafeMeJwtMasterKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final String PREF_NAME = "safeme_secure_prefs";
    private static final String KEY_JWT_TOKEN = "enc_jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_MOBILE = "user_mobile";
    private static final String KEY_USER_VERIFIED = "user_verified";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_THEME_MODE = "theme_mode"; // system, light, dark
    private static final String KEY_LANGUAGE = "language_code"; // en, hi
    private static final String KEY_CHECKIN_INTERVAL = "default_checkin_minutes";
    private static final String KEY_ESCALATION_STAGE1_MIN = "escalation_stage1_min";
    private static final String KEY_ESCALATION_STAGE2_MIN = "escalation_stage2_min";
    private static final String KEY_VOICE_TRIGGER_ENABLED = "voice_trigger_enabled";
    private static final String KEY_VOICE_KEYWORD = "voice_trigger_keyword";
    private static final String KEY_NOTIF_PRIVACY_HIDE = "notif_privacy_hide";
    private static final String KEY_LOCATION_HIGH_ACCURACY = "location_high_accuracy";
    private static final String KEY_LOCAL_BACKEND_MODE = "local_backend_mode";
    private static final String KEY_LOCAL_JWT_SECRET = "enc_local_jwt_secret";

    private static volatile SecureTokenManager instance;
    private final SharedPreferences prefs;
    private volatile SecretKey fallbackSessionKey;

    private SecureTokenManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureKeyExists();
    }

    public static SecureTokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SecureTokenManager.class) {
                if (instance == null) {
                    instance = new SecureTokenManager(context);
                }
            }
        }
        return instance;
    }

    private void ensureKeyExists() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build();
                keyGenerator.init(spec);
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            Log.w(TAG, "AndroidKeyStore unavailable in current runtime; using ephemeral AES-256 session key");
            initFallbackKeyIfNeeded();
        }
    }

    private synchronized void initFallbackKeyIfNeeded() {
        if (fallbackSessionKey == null) {
            try {
                KeyGenerator gen = KeyGenerator.getInstance("AES");
                gen.init(256);
                fallbackSessionKey = gen.generateKey();
            } catch (Exception ex) {
                Log.e(TAG, "Fallback AES key generation failed", ex);
            }
        }
    }

    private SecretKey getSecretKey() throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            if (key != null) {
                return key;
            }
        } catch (Exception ignored) {
            // Fall through to ephemeral in-memory AES key when Keystore is unavailable
        }
        initFallbackKeyIfNeeded();
        return fallbackSessionKey;
    }

    private String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[1 + iv.length + encrypted.length];
            combined[0] = (byte) iv.length;
            System.arraycopy(iv, 0, combined, 1, iv.length);
            System.arraycopy(encrypted, 0, combined, 1 + iv.length, encrypted.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    private String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
            int ivLength = combined[0] & 0xFF;
            byte[] iv = new byte[ivLength];
            byte[] encrypted = new byte[combined.length - 1 - ivLength];
            System.arraycopy(combined, 1, iv, 0, ivLength);
            System.arraycopy(combined, 1 + ivLength, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }

    public void saveToken(String token) {
        String encrypted = encrypt(token);
        if (encrypted != null) {
            prefs.edit().putString(KEY_JWT_TOKEN, encrypted).apply();
        }
    }

    public String getToken() {
        String encrypted = prefs.getString(KEY_JWT_TOKEN, null);
        return decrypt(encrypted);
    }

    public boolean hasValidTokenFormat() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void saveUserSession(String userId, String fullName, String email, String mobile, boolean verified) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, fullName)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_MOBILE, mobile)
                .putBoolean(KEY_USER_VERIFIED, verified)
                .apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserMobile() {
        return prefs.getString(KEY_USER_MOBILE, "");
    }

    public boolean isUserVerified() {
        return prefs.getBoolean(KEY_USER_VERIFIED, false);
    }

    public void setRememberLogin(boolean remember) {
        prefs.edit().putBoolean(KEY_REMEMBER_LOGIN, remember).apply();
    }

    public boolean isRememberLogin() {
        return prefs.getBoolean(KEY_REMEMBER_LOGIN, true);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void saveFcmToken(String fcmToken) {
        prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply();
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, "");
    }

    public String getApiBaseUrl() {
        String configured = prefs.getString(KEY_API_BASE_URL, null);
        if (configured != null && !configured.trim().isEmpty()) {
            return normalizeBaseUrl(configured);
        }
        String defaultUrl = BuildConfig.API_BASE_URL;
        if (defaultUrl == null || defaultUrl.trim().isEmpty()) {
            defaultUrl = "http://10.0.2.2:8080/api/v1/";
        }
        return normalizeBaseUrl(defaultUrl);
    }

    public void setApiBaseUrl(String url) {
        prefs.edit().putString(KEY_API_BASE_URL, normalizeBaseUrl(url)).apply();
    }

    private String normalizeBaseUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }

    public String getThemeMode() {
        return prefs.getString(KEY_THEME_MODE, "light");
    }

    public void setThemeMode(String mode) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply();
    }

    public String getLanguageCode() {
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public void setLanguageCode(String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    public int getDefaultCheckInIntervalMinutes() {
        return prefs.getInt(KEY_CHECKIN_INTERVAL, 15);
    }

    public void setDefaultCheckInIntervalMinutes(int minutes) {
        prefs.edit().putInt(KEY_CHECKIN_INTERVAL, minutes).apply();
    }

    public int getEscalationStage1GraceMinutes() {
        return prefs.getInt(KEY_ESCALATION_STAGE1_MIN, 2);
    }

    public void setEscalationStage1GraceMinutes(int minutes) {
        prefs.edit().putInt(KEY_ESCALATION_STAGE1_MIN, minutes).apply();
    }

    public int getEscalationStage2GraceMinutes() {
        return prefs.getInt(KEY_ESCALATION_STAGE2_MIN, 5);
    }

    public void setEscalationStage2GraceMinutes(int minutes) {
        prefs.edit().putInt(KEY_ESCALATION_STAGE2_MIN, minutes).apply();
    }

    public boolean isVoiceTriggerEnabled() {
        return prefs.getBoolean(KEY_VOICE_TRIGGER_ENABLED, false);
    }

    public void setVoiceTriggerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_TRIGGER_ENABLED, enabled).apply();
    }

    public String getVoiceKeyword() {
        return prefs.getString(KEY_VOICE_KEYWORD, "help me safeme");
    }

    public void setVoiceKeyword(String keyword) {
        prefs.edit().putString(KEY_VOICE_KEYWORD, keyword).apply();
    }

    public boolean isNotificationPrivacyHidden() {
        return prefs.getBoolean(KEY_NOTIF_PRIVACY_HIDE, true);
    }

    public void setNotificationPrivacyHidden(boolean hide) {
        prefs.edit().putBoolean(KEY_NOTIF_PRIVACY_HIDE, hide).apply();
    }

    public boolean isHighAccuracyLocationEnabled() {
        return prefs.getBoolean(KEY_LOCATION_HIGH_ACCURACY, true);
    }

    public void setHighAccuracyLocationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCATION_HIGH_ACCURACY, enabled).apply();
    }

    public boolean isLocalBackendMode() {
        return prefs.getBoolean(KEY_LOCAL_BACKEND_MODE, true);
    }

    public void setLocalBackendMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCAL_BACKEND_MODE, enabled).apply();
    }

    public synchronized String getOrCreateLocalJwtSecret() {
        String encrypted = prefs.getString(KEY_LOCAL_JWT_SECRET, null);
        String decrypted = decrypt(encrypted);
        if (decrypted != null && decrypted.length() >= 32) {
            return decrypted;
        }
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        String generated = Base64.encodeToString(bytes, Base64.NO_WRAP);
        String enc = encrypt(generated);
        prefs.edit().putString(KEY_LOCAL_JWT_SECRET, enc != null ? enc : generated).apply();
        return generated;
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_JWT_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_MOBILE)
                .remove(KEY_USER_VERIFIED)
                .apply();
    }
}
