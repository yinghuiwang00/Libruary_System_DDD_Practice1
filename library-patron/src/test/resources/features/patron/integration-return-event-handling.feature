Feature: Return Event Handling
  As the patron module
  When a BookReturnedEvent is received
  I want to decrease the patron's current loan count

  Scenario: Decrease patron loan count after receiving a return event
    Given a patron with loans exists with current loans of 3
    When the module receives a return event for that patron
    Then the patron's loan count should be reduced to 2

  Scenario: Receiving a return event for a non-existent patron does not throw an error
    Given the patron for the return event does not exist in the database
    When the module receives a return event for that non-existent patron
    Then the system should handle it safely without throwing an exception
