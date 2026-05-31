Feature: Hold Event Notification Handling
  As the notification module
  When I receive hold events from the circulation context
  I want to create hold notifications

  Scenario: Create a hold notification after receiving a hold placed event
    Given no notifications exist for the specified patron
    When the notification module receives a hold placed event for patron "patron-hold-001" with queue position 3
    Then a notification of type "HOLD_AVAILABLE" should be created for patron "patron-hold-001"
    And the notification subject should be "图书预约通知"
    And the notification content should contain queue position "3"

  Scenario: Create a pickup notification after receiving a hold fulfilled event
    Given no notifications exist for the specified patron
    When the notification module receives a hold fulfilled event for patron "patron-hold-002"
    Then a notification of type "HOLD_AVAILABLE" should be created for patron "patron-hold-002"
    And the notification subject should be "预约图书可取通知"
