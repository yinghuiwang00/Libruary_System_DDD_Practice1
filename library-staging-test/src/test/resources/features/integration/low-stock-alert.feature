Feature: Low Stock Alert Cross-Context Integration
  As the library system
  When stock is low (Inventory publishes LowStockAlertEvent)
  I want the Notification context to create an alert for librarians

  Scenario: Low stock alert creates notification for librarian
    When a LowStockAlertEvent is published with inventory "inv-001", book "book-lowstock-001", available copies 1, threshold 2
    Then a notification should exist for recipient "LIBRARIAN"
