package com.safeme.backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.safeme.backend.model.AppNotificationDocument;
import com.safeme.backend.model.UserDocument;
import com.safeme.backend.repository.AppNotificationRepository;
import com.safeme.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationService.class);

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public FcmNotificationService(
            AppNotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyUser(String recipientUserId, String type, String referenceId, String title, String messageText) {
        if (recipientUserId == null || recipientUserId.isEmpty()) {
            return;
        }

        AppNotificationDocument doc = new AppNotificationDocument();
        doc.setRecipientUserId(recipientUserId);
        doc.setType(type);
        doc.setReferenceId(referenceId);
        doc.setTitle(title);
        doc.setMessage(messageText);
        doc.setRead(false);
        doc.setCreatedAt(System.currentTimeMillis());
        notificationRepository.save(doc);

        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }

        userRepository.findById(recipientUserId).map(UserDocument::getFcmToken).ifPresent(token -> {
            if (token != null && !token.trim().isEmpty()) {
                try {
                    Message fcmMessage = Message.builder()
                            .setToken(token)
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(messageText)
                                    .build())
                            .putData("notification_type", type != null ? type : "SYSTEM")
                            .putData("reference_id", referenceId != null ? referenceId : "")
                            .build();
                    FirebaseMessaging.getInstance().send(fcmMessage);
                } catch (Exception ex) {
                    log.warn("Failed to deliver FCM push to user {}", recipientUserId, ex);
                }
            }
        });
    }
}
