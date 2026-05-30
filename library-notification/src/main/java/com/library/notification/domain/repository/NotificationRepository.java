package com.library.notification.domain.repository;

import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationStatus;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.shared.domain.model.NotificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, NotificationId> {

    List<Notification> findByRecipientId(String recipientId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByNotificationType(NotificationType notificationType);
}
