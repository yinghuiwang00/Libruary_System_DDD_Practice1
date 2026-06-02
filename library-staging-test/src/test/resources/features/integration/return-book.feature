Feature: Return Book Cross-Context Integration
  As the library system
  When a book is returned (Circulation publishes BookReturnedEvent)
  I want the Patron context to decrement loan count
  And the Notification context to create a notification

  Scenario: Return book decrements patron loan count and creates notification
    Given a patron "Jane Smith" with email "jane.return@test.com" exists with 1 active loan
    When a BookReturnedEvent is published for that patron with loan "loan-001", copy "copy-001", book "book-001"
    Then the patron's current loan count should be 0
    And a notification should exist for the patron
