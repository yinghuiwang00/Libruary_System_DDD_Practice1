package com.library.notification.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.notification.domain.model.enums.NotificationChannel;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PatronStatusNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(PatronStatusNotificationHandler.class);
    private final NotificationService notificationService;
    public PatronStatusNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handlePatronSuspended(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : "";
        try {
            notificationService.createNotification(NotificationType.SYSTEM_ANNOUNCEMENT, NotificationChannel.EMAIL, patronId, "账户停用通知", "您的图书馆账户已被停用，原因: " + reason);
        } catch (Exception e) { log.error("Failed: {}", e.getMessage(), e); }
    }
}
