Feature: Payment Event Handling
  As the patron module
  When a PaymentCompletedEvent is received
  I want to reduce the patron's outstanding fine balance

  Scenario: Reduce patron fine balance after receiving a payment completed event
    Given a patron with fines is prepared with a balance of 50
    When a payment completed event arrives for that patron with amount 25
    Then that patron's fine balance should become 25
