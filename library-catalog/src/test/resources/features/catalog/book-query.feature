Feature: Book Query
  As a reader
  I want to query a book by ID
  So that I can get the book's detailed information

  Scenario: Successfully query a published book by ID
    Given a book with ISBN "9787111407010" and status "PUBLISHED" exists in the system
    When I query the book by its ID
    Then the book information is returned
    And the book title is "Domain-Driven Design"
