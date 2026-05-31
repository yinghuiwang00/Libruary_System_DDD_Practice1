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
public class OverdueNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(OverdueNotificationHandler.class);
    private final NotificationService notificationService;
    public OverdueNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handle(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        long days = event.get("daysOverdue").asLong();
        log.info("Creating overdue notification for patron: {}", patronId);
        try {
            notificationService.createNotification(NotificationType.OVERDUE_NOTICE, NotificationChannel.EMAIL, patronId, "图书逾期通知", "您借阅的图书已逾期" + days + "天，请尽快归还。");
        } catch (Exception e) { log.error("Failed to create overdue notification: {}", e.getMessage(), e); }
    }
}
