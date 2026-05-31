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
public class FineNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(FineNotificationHandler.class);
    private final NotificationService notificationService;
    public FineNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handle(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        String amount = event.get("amount").asText();
        log.info("Creating fine notification for patron: {}", patronId);
        try {
            notificationService.createNotification(NotificationType.FINE_NOTIFICATION, NotificationChannel.EMAIL, patronId, "罚款通知", "您有一笔逾期罚款，金额: " + amount + "元，请及时缴纳。");
        } catch (Exception e) { log.error("Failed to create fine notification: {}", e.getMessage(), e); }
    }
}
