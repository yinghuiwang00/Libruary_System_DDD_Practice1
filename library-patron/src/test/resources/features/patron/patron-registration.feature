Feature: Patron Registration
  As a library administrator
  I want to register new patrons
  So that they can borrow books from the library

  Scenario: Successfully register a new patron
    Given no patron exists with email "john@example.com"
    When a registration request is made with the following details:
      | firstName | lastName | email            | patronType |
      | John      | Doe      | john@example.com | STUDENT    |
    Then the patron is successfully registered
    And the patron status is "ACTIVE"
