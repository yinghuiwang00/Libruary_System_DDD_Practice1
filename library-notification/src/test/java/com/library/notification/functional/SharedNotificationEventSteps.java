package com.library.notification.functional;

import com.library.notification.domain.model.Notification;
import com.library.notification.domain.model.enums.NotificationType;
import com.library.notification.domain.repository.NotificationRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared step definitions used across multiple notification event BDD scenarios.
 * Prevents duplicate step definition errors by consolidating common Given/Then steps.
 */
public class SharedNotificationEventSteps {

    @Autowired
    private NotificationRepository notificationRepository;

    @Given("no notifications exist for the specified patron")
    public void noNotificationsExist() {
        // Test cleanup is handled by CucumberSpringConfig @Before hook
    }

    @Given("no notifications exist for LIBRARIAN")
    public void noLibrarianNotificationsExist() {
        // Test cleanup is handled by CucumberSpringConfig @Before hook
    }

    @Then("a notification of type {string} should be created for patron {string}")
    public void verifyNotificationCreatedForReader(String typeName, String recipientId) {
        NotificationType expectedType = NotificationType.valueOf(typeName);
        List<Notification> notifications = notificationRepository.findByRecipientId(recipientId);
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(1);
        assertThat(notifications.stream().anyMatch(n -> n.getNotificationType() == expectedType)).isTrue();
    }

    @Then("a notification of type {string} should be created for {string}")
    public void verifyNotificationCreatedForRecipient(String typeName, String recipientId) {
        NotificationType expectedType = NotificationType.valueOf(typeName);
        List<Notification> notifications = notificationRepository.findByRecipientId(recipientId);
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(1);
        assertThat(notifications.stream().anyMatch(n -> n.getNotificationType() == expectedType)).isTrue();
    }

    @Then("the notification subject should be {string}")
    public void verifyNotificationSubject(String expectedSubject) {
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).anyMatch(n -> expectedSubject.equals(n.getSubject()));
    }

    @Then("the notification content should contain overdue days {string}")
    public void verifyOverdueDays(String expectedDays) {
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).anyMatch(n -> n.getContent() != null && n.getContent().contains(expectedDays));
    }

    @Then("the notification content should contain amount {string}")
    public void verifyAmount(String expectedAmount) {
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).anyMatch(n -> n.getContent() != null && n.getContent().contains(expectedAmount));
    }

    @Then("the notification content should contain queue position {string}")
    public void verifyQueuePosition(String expectedPosition) {
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).anyMatch(n -> n.getContent() != null && n.getContent().contains(expectedPosition));
    }

    @Then("the notification content should contain book ID {string}")
    public void verifyBookId(String expectedBookId) {
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).anyMatch(n -> n.getContent() != null && n.getContent().contains(expectedBookId));
    }

    @Then("the notification content should contain suspension reason {string}")
    public void verifyReason(String expectedReason) {
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).anyMatch(n -> n.getContent() != null && n.getContent().contains(expectedReason));
    }
}
