Feature: Low Stock Alert Notification Handling
  As the notification module
  When I receive a low stock alert event from the inventory context
  I want to create a stock alert notification

  Scenario: Create an alert notification after receiving a low stock alert event
    Given no notifications exist for LIBRARIAN
    When the notification module receives a low stock alert event for book "book-low-001" with 2 available copies
    Then a notification of type "SYSTEM_ANNOUNCEMENT" should be created for "LIBRARIAN"
    And the notification subject should be "库存预警通知"
    And the notification content should contain book ID "book-low-001"
