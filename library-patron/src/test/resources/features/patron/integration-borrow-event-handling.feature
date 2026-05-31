Feature: Borrow Event Handling
  As the patron module
  When a BookBorrowedEvent is received
  I want to update the patron's current loan count

  Scenario: Update patron loan count after receiving a borrow event
    Given a patron exists with current loans of 2
    When the module receives a borrow event for that patron
    Then the patron's current loan count should become 3

  Scenario: Receiving a borrow event for a non-existent patron does not throw an error
    Given the specified patron does not exist in the database
    When the module receives a borrow event for that non-existent patron
    Then the system should log the error without throwing an exception
