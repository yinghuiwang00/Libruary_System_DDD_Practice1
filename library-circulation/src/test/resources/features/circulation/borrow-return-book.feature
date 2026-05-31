Feature: Book Borrowing and Returning
  As a library administrator
  I want to manage book borrowing and returning
  So that I can track the circulation status of books

  Scenario: Successfully borrow a book
    Given patron "PATRON-001" wants to borrow copy "COPY-001" of book "BOOK-001"
    When the patron borrows the book
    Then the borrowing succeeds
    And the loan status is "ACTIVE"

  Scenario: Successfully return a book
    Given patron "PATRON-002" has already borrowed copy "COPY-002" of book "BOOK-002"
    When the patron returns the book
    Then the return succeeds
    And the loan status is "RETURNED"

  Scenario: Successfully place a hold on a book
    Given book "BOOK-003" is currently unavailable
    When patron "PATRON-003" places a hold on the book
    Then the hold is placed successfully
    And the hold status is "WAITING"

  Scenario: Successfully renew a book
    Given patron "PATRON-004" has already borrowed copy "COPY-004" of book "BOOK-004"
    When the patron renews the book
    Then the renewal succeeds
    And the loan status is "RENEWED"
    And the renewal count is 1
    And the due date has been extended

  Scenario: Renewal count exceeds the limit
    Given patron "PATRON-005" has already borrowed copy "COPY-005" of book "BOOK-005" and reached the maximum renewal count
    When the patron attempts to renew the book
    Then the renewal should fail

  Scenario: Mark a loan as overdue
    Given patron "PATRON-006" has already borrowed copy "COPY-006" of book "BOOK-006" and it is overdue
    When the system processes overdue loans
    Then the loan status should be "OVERDUE"

  Scenario: Cancel a hold
    Given patron "PATRON-007" has already placed a hold on book "BOOK-007"
    When the patron cancels the hold
    Then the hold is cancelled
