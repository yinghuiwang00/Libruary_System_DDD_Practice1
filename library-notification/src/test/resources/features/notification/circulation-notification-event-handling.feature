Feature: Circulation Event Notification Handling
  As the notification module
  When I receive events from the circulation context
  I want to create corresponding notifications

  Scenario: Create a borrow notification after receiving a borrow event
    Given no notifications exist for the specified patron
    When the notification module receives a borrow event for patron "patron-borrow-001"
    Then a notification of type "BOOK_RETURNED" should be created for patron "patron-borrow-001"
    And the notification subject should be "借书成功通知"

  Scenario: Create a return notification after receiving a return event
    Given no notifications exist for the specified patron
    When the notification module receives a return event for patron "patron-return-001"
    Then a notification of type "BOOK_RETURNED" should be created for patron "patron-return-001"
    And the notification subject should be "还书成功通知"

  Scenario: Create an overdue notification after receiving an overdue event
    Given no notifications exist for the specified patron
    When the notification module receives an overdue event for patron "patron-overdue-001" with 5 days overdue
    Then a notification of type "OVERDUE_NOTICE" should be created for patron "patron-overdue-001"
    And the notification subject should be "图书逾期通知"
    And the notification content should contain overdue days "5天"

  Scenario: Create a fine notification after receiving a fine event
    Given no notifications exist for the specified patron
    When the notification module receives a fine event for patron "patron-fine-001" with fine amount "30.00"
    Then a notification of type "FINE_NOTIFICATION" should be created for patron "patron-fine-001"
    And the notification subject should be "罚款通知"
    And the notification content should contain amount "30.00"
