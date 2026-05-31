Feature: Book Update
  As a library administrator
  I want to update published book information
  So that the information remains accurate

  Scenario: Successfully update the title and description of a published book
    Given a book with status "PUBLISHED" exists in the system
    When I update the book title to "Domain-Driven Design (Revised Edition)"
    Then the book is updated successfully
    And the book title is "Domain-Driven Design (Revised Edition)"
