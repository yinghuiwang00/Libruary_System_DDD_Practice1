Feature: Notification Lifecycle

  Scenario: Send and deliver an email notification
    Given a notification is created for patron "patron-001"
    When the notification is sent via "EMAIL"
    Then the notification should be in "SENDING" status
    When the notification is delivered
    Then the notification should be in "DELIVERED" status
    When the notification is marked as read
    Then the notification should be in "READ" status

  Scenario: Handle failed notification and retry
    Given a notification is created for patron "patron-002"
    When the notification is sent via "EMAIL"
    And the notification fails with reason "SMTP error"
    Then the notification should be in "FAILED" status
    When the notification is retried
    Then the notification should be in "PENDING" status

  Scenario: Schedule and send a notification
    Given a notification is created for patron "patron-003"
    When the notification is scheduled for 2 hours from now
    Then the notification should be in "SCHEDULED" status
    When the notification is sent via "EMAIL"
    Then the notification should be in "SENDING" status
