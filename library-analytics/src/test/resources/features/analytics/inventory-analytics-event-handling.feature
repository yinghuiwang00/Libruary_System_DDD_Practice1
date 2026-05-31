Feature: Low Stock Alert Event Analytics Processing
  As the analytics module
  When receiving a LowStockAlertEvent
  I want to record low stock alert statistics

  Scenario: Create statistics record after receiving a low stock alert event
    Given the inventory analytics module is ready
    When a low stock alert event is received with book ID "book-002" and available copies 3
    Then the system should successfully process the low stock alert event and record book ID "book-002"
