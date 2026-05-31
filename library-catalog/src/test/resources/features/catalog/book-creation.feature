Feature: Book Creation
  As a library administrator
  I want to create new book records
  So that I can manage the library's collection information

  Scenario: Successfully create a new book
    Given no book with ISBN "9787111407010" exists in the system
    When I create a new book with title "Domain-Driven Design" and author "Eric Evans"
    And the ISBN is "9787111407010"
    And the category is "SOFTWARE_ENGINEERING"
    Then the book is created successfully
    And the book status is "DRAFT"
