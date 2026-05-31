Feature: Fine Event Handling
  As the payment module
  When a FineIncurredEvent is received
  I want to create a fine payment record for the corresponding patron

  Scenario: Create a payment record after receiving a fine event
    Given a patron exists with ID "PATRON-001"
    When the module receives a fine event for the patron with amount "25.00" and 5 overdue days
    Then the system should create a fine payment record for the patron with amount "25.00"
