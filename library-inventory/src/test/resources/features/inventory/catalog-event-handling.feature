Feature: New Book Event Handling
  As the inventory module
  When a BookCreatedEvent is received
  I want to create an inventory record for the new book

  Scenario: Create inventory record after receiving a new book created event
    Given a library with code "MAIN-LIB-001" exists in the system
    When the module receives a new book created event with book ID "BOOK-NEW-100"
    Then the inventory record for that book should be created
