Feature: New Book Event Analytics Processing
  As the analytics module
  When receiving a BookCreatedEvent
  I want to record new book statistics

  Scenario: Create statistics record after receiving a new book creation event
    Given the analytics module is ready
    When a new book creation event is received with book ID "book-001" and title "Clean Code"
    Then the system should successfully process the new book event and record book ID "book-001"
