Feature: Inventory Creation
  As a library administrator
  I want to create inventory records for books
  In order to manage book copies across library branches

  Scenario: Successfully create inventory and add initial copies
    Given a library with code "LIB-001" exists in the system
    And the book has no inventory record in that library yet
    When I create an inventory for book "BOOK-001" in that library with 2 initial copies
    Then the inventory is created successfully
    And the total copy count is 2
    And the available copy count is 2
