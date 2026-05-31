Feature: Payment Event Notification Handling
  As the notification module
  When I receive events from the payment context
  I want to create payment confirmation notifications

  Scenario: Create a payment confirmation notification after receiving a payment completed event
    Given no notifications exist for the specified patron
    When the notification module receives a payment completed event for patron "patron-payment-001" with amount "50.00"
    Then a notification of type "PAYMENT_CONFIRMATION" should be created for patron "patron-payment-001"
    And the notification subject should be "支付确认通知"
    And the notification content should contain amount "50.00"
