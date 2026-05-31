Feature: Patron Status Change Notification Handling
  As the notification module
  When I receive a suspension event from the patron context
  I want to create an account suspension notification

  Scenario: Create a suspension notification after receiving a patron suspended event
    Given no notifications exist for the specified patron
    When the notification module receives a patron suspended event for patron "patron-suspend-001" with reason "多次逾期未还"
    Then a notification of type "SYSTEM_ANNOUNCEMENT" should be created for patron "patron-suspend-001"
    And the notification subject should be "账户停用通知"
    And the notification content should contain suspension reason "多次逾期未还"
