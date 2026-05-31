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
public class BorrowedNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(BorrowedNotificationHandler.class);
    private final NotificationService notificationService;
    public BorrowedNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handle(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        log.info("Creating borrow notification for patron: {}", patronId);
        try {
            notificationService.createNotification(NotificationType.BOOK_RETURNED, NotificationChannel.EMAIL, patronId, "借书成功通知", "您已成功借阅图书，请按时归还。");
        } catch (Exception e) { log.error("Failed to create borrow notification: {}", e.getMessage(), e); }
    }
}
