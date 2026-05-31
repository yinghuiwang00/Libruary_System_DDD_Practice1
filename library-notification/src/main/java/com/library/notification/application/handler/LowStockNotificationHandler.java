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
public class LowStockNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(LowStockNotificationHandler.class);
    private final NotificationService notificationService;
    public LowStockNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handle(JsonNode event) {
        String bookId = event.get("bookId").asText();
        int copies = event.get("availableCopies").asInt();
        try {
            notificationService.createNotification(NotificationType.SYSTEM_ANNOUNCEMENT, NotificationChannel.EMAIL, "LIBRARIAN", "库存预警通知", "图书 " + bookId + " 库存不足，可用副本: " + copies);
        } catch (Exception e) { log.error("Failed: {}", e.getMessage(), e); }
    }
}
