Feature: Patron Type Change
  As a library administrator
  I want to change a patron's type
  So that borrowing privileges reflect the patron's current role

  Scenario: Change patron type from STUDENT to FACULTY
    Given a patron "Prof. Wang" is registered with email "wang@example.com" as a "STUDENT"
    When the patron type is changed to "FACULTY"
    Then the patron type should be "FACULTY"

  Scenario: Change patron type from STUDENT to STAFF
    Given a patron "Scientist" is registered with email "scientist@example.com" as a "STUDENT"
    When the patron type is changed to "STAFF"
    Then the patron type should be "STAFF"
