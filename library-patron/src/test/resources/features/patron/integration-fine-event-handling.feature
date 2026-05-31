Feature: Fine Event Handling
  As the patron module
  When a FineIncurredEvent is received
  I want to update the patron's outstanding fine balance

  Scenario: Increase patron fine balance after receiving a fine event
    Given a patron exists with a current fine balance of 0
    When the module receives a fine event for that patron with amount 25
    Then the patron's fine balance should become 25

  Scenario: Patron status changes to suspended when accumulated fines exceed threshold
    Given a patron exists with a current fine balance of 45
    When the module receives a fine event for that patron with amount 10
    Then the patron's fine balance should become 55
    And the patron status should become SUSPENDED
