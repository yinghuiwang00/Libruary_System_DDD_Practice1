Feature: Fine and Payment Cross-Context Integration
  As the library system
  When a fine is incurred or a payment is completed
  I want the Patron context to update fine balance accordingly
  And the Notification context to create appropriate notifications

  Scenario: Fine incurred updates patron fine balance and creates notification
    Given a patron "Alice Williams" with email "alice.fine@test.com" exists
    When a FineIncurredEvent is published for that patron with fine "fine-001", loan "loan-001", amount 15.00
    Then the patron's outstanding fines should be 15.00
    And a notification should exist for the patron

  Scenario: Payment completed reduces patron fine balance and creates notification
    Given a patron "Alice Williams" with email "alice.pay@test.com" exists with outstanding fines of 25.00
    When a PaymentCompletedEvent is published for that patron with payment "pay-001", amount 25.00, reference "REF-001"
    Then the patron's outstanding fines should be 0.00
    And a notification should exist for the patron
