Feature: Borrow Book Cross-Context Integration
  As the library system
  When a book is borrowed (Circulation publishes BookBorrowedEvent)
  I want the Patron context to update loan count
  And the Notification context to create a notification

  Scenario: Borrow book updates patron loan count and creates notification
    Given a patron "John Doe" with email "john.borrow@test.com" exists
    When a BookBorrowedEvent is published for that patron with loan "loan-001", copy "copy-001", book "book-001"
    Then the patron's current loan count should be 1
    And the patron's total borrowed count should be 1
    And a notification should exist for the patron
