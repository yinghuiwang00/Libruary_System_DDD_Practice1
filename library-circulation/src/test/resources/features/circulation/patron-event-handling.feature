Feature: Patron suspension event handling
  As the circulation module
  When a PatronSuspendedEvent is received
  I want to block subsequent borrowing operations for that patron

  Scenario: Block borrowing after receiving a patron suspension event
    Given there is a patron with ID "PATRON-001"
    When the module receives a suspension event for the patron with reason "overdue fine limit exceeded"
    Then the system should record that the patron is suspended and no longer allowed to borrow
