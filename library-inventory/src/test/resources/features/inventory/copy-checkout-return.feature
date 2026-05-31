Feature: Copy Checkout and Return
  As a library administrator
  I want to manage book copy checkout and return
  In order to track the circulation status of copies

  Scenario: Successfully checkout and return a book copy
    Given a library with code "LIB-001" exists in the system
    And book "BOOK-001" has an inventory record in that library with 2 available copies
    When I checkout a copy
    Then the checkout succeeds
    And the available copy count becomes 1

    When I return that copy
    Then the return succeeds
    And the available copy count becomes 2
