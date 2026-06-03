Feature: Borrow Book Updates Inventory
  As the library system
  When a book is borrowed (Circulation publishes BookBorrowedEvent)
  I want the Inventory context to update copy status

  Scenario: Borrowing a book changes copy status to BORROWED
    Given the main library with code "MAIN-001" exists
    And a book "book-001" exists with inventory in library "MAIN-001" and an available copy
    And a patron "John Doe" with email "john.inv@test.com" exists
    When a BookBorrowedEvent is published for the patron and that copy and book "book-001"
    Then the copy status should be "BORROWED"
