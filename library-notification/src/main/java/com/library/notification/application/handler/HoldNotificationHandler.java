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
public class HoldNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(HoldNotificationHandler.class);
    private final NotificationService notificationService;
    public HoldNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handleHoldPlaced(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        int pos = event.get("queuePosition").asInt();
        try {
            notificationService.createNotification(NotificationType.HOLD_AVAILABLE, NotificationChannel.EMAIL, patronId, "图书预约通知", "您已成功预约图书，排队位置: " + pos);
        } catch (Exception e) { log.error("Failed: {}", e.getMessage(), e); }
    }

    @Transactional
    public void handleHoldFulfilled(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        try {
            notificationService.createNotification(NotificationType.HOLD_AVAILABLE, NotificationChannel.EMAIL, patronId, "预约图书可取通知", "您预约的图书已到馆，请尽快前来取书。");
        } catch (Exception e) { log.error("Failed: {}", e.getMessage(), e); }
    }
}
