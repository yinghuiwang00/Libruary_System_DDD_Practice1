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
public class PaymentNotificationHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationHandler.class);
    private final NotificationService notificationService;
    public PaymentNotificationHandler(NotificationService notificationService) { this.notificationService = notificationService; }

    @Transactional
    public void handle(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        String amount = event.get("amount").asText();
        log.info("Creating payment confirmation for patron: {}", patronId);
        try {
            notificationService.createNotification(NotificationType.PAYMENT_CONFIRMATION, NotificationChannel.EMAIL, patronId, "支付确认通知", "您的罚款已成功缴纳，金额: " + amount + "元。");
        } catch (Exception e) { log.error("Failed to create payment notification: {}", e.getMessage(), e); }
    }
}
