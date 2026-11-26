package com.safeme.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final String serviceAccountPath;

    public FirebaseConfig(@Value("${safeme.firebase.service-account-path:}") String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    @PostConstruct
    public void initFirebase() {
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            log.info("FIREBASE_SERVICE_ACCOUNT_PATH not set; FCM push dispatch disabled until configured.");
            return;
        }
        Path path = Path.of(serviceAccountPath.trim());
        if (!Files.exists(path)) {
            log.warn("Firebase service account file not found at {}; FCM push dispatch disabled.", path);
            return;
        }
        try (InputStream stream = new FileInputStream(path.toFile())) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully.");
            }
        } catch (Exception ex) {
            log.error("Failed to initialize Firebase Admin SDK", ex);
        }
    }
}
