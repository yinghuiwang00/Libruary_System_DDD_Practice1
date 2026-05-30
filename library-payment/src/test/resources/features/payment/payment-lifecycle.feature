Feature: Payment Lifecycle

  Scenario: Successfully process a fine payment
    Given a payment is created for patron "patron-123" with amount 25.00
    When the payment is processed with external transaction "TXN-001"
    And the payment is completed
    Then the payment status is "COMPLETED"
    And the payment amount is 25.00

  Scenario: Create and cancel a payment
    Given a payment is created for patron "patron-456" with amount 50.00
    When the payment is cancelled with reason "Duplicate payment"
    Then the payment status is "CANCELLED"

  Scenario: Process and fail a payment
    Given a payment is created for patron "patron-789" with amount 100.00
    When the payment is processed with external transaction "TXN-002"
    When the payment fails with reason "Insufficient funds"
    Then the payment status is "FAILED"

  Scenario: Request and complete a full refund
    Given a completed payment for patron "patron-refund" with amount 75.00
    When a refund of 75.00 is requested with reason "Book returned"
    Then the refund should be in "PENDING" status
    When the refund is processed
    And the refund is completed
    Then the payment refund amount should be 75.00

  Scenario: Request multiple partial refunds
    Given a completed payment for patron "patron-partial" with amount 100.00
    When a refund of 30.00 is requested with reason "Partial return 1"
    And the refund is processed and completed
    When a refund of 20.00 is requested with reason "Partial return 2"
    And the refund is processed and completed
    Then the payment refund amount should be 50.00

  Scenario: List payments by patron
    Given a payment is created for patron "patron-list" with amount 10.00
    And another payment is created for patron "patron-list" with amount 20.00
    When I query payments for patron "patron-list"
    Then I should see 2 payments
