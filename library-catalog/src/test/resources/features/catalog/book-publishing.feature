Feature: Book Publishing
  As a library administrator
  I want to publish books in draft status
  So that readers can search and borrow them

  Scenario: Successfully publish a book in draft status
    Given a book with status "DRAFT" exists in the system
    When I publish the book
    Then the book status becomes "PUBLISHED"
