Feature: Borrow and Return Event Handling
  As the inventory module
  When a BookBorrowedEvent or BookReturnedEvent is received
  I want to update the copy's borrowing status

  Scenario: Mark copy as borrowed after receiving a borrow event
    Given a library with code "MAIN-LIB-001" exists in the system
    And book "BOOK-CIRC-100" has 1 available copy in that library
    When the module receives a borrow event for that copy
    Then the copy's status should become "BORROWED"

  Scenario: Mark copy as available after receiving a return event
    Given a library with code "MAIN-LIB-001" exists in the system
    And book "BOOK-CIRC-200" has 1 borrowed copy in that library
    When the module receives a return event for that copy
    Then the copy's status should become "AVAILABLE"
