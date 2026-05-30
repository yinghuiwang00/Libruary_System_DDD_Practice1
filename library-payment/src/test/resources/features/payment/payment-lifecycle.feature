Feature: Payment Lifecycle

  Scenario: Successfully process a fine payment
    Given a payment is created for patron "patron-123" with amount 25.00
    When the payment is processed with external transaction "TXN-001"
    And the payment is completed
    Then the payment status is "COMPLETED"
    And the payment amount is 25.00
