package com.safeme.app.localbackend;

import android.util.Base64;

import com.safeme.app.utils.SecureTokenManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic helper for the on-device SQLite backend:
 * PBKDF2 salted password hashing and HMAC-SHA256 JWT signing/verification.
 */
public final class LocalPasswordAndJwtHelper {

    private static final int ITERATIONS = 10_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final long TOKEN_EXPIRY_SECONDS = 86_400L;

    private final SecureTokenManager tokenManager;

    public LocalPasswordAndJwtHelper(SecureTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = deriveKey(password, salt);
            return Base64.encodeToString(salt, Base64.NO_WRAP)
                    + ":"
                    + Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    public boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        try {
            String[] parts = storedHash.split(":", 2);
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] actualHash = deriveKey(password, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory skf;
        try {
            skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (Exception fallback) {
            skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        }
        return skf.generateSecret(spec).getEncoded();
    }

    public String generateJwt(String userId, String email) {
        try {
            long nowSec = System.currentTimeMillis() / 1000L;
            JSONObject header = new JSONObject();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            JSONObject payload = new JSONObject();
            payload.put("sub", userId);
            payload.put("email", email);
            payload.put("iat", nowSec);
            payload.put("exp", nowSec + TOKEN_EXPIRY_SECONDS);

            String encodedHeader = base64Url(header.toString().getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64Url(payload.toString().getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = signHmacSha256(signingInput);
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("JWT generation failed", e);
        }
    }

    public String verifyAndExtractUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length != 3) return null;

            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = signHmacSha256(signingInput);
            if (!MessageDigest.isEqual(
                    expectedSig.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            byte[] payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject payload = new JSONObject(new String(payloadBytes, StandardCharsets.UTF_8));
            long exp = payload.optLong("exp", 0L);
            if (exp <= System.currentTimeMillis() / 1000L) {
                return null;
            }
            String sub = payload.optString("sub", "");
            return sub.isEmpty() ? null : sub;
        } catch (Exception e) {
            return null;
        }
    }

    public long getTokenExpirySeconds() {
        return TOKEN_EXPIRY_SECONDS;
    }

    private String signHmacSha256(String data) throws Exception {
        String secretB64 = tokenManager.getOrCreateLocalJwtSecret();
        byte[] keyBytes = Base64.decode(secretB64, Base64.NO_WRAP);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64Url(sig);
    }

    private String base64Url(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
