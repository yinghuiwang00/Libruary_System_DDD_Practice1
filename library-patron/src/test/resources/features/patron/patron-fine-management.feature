Feature: Patron Fine Management
  As a library administrator
  I want to manage patron fines
  So that fines are tracked and patron status updated accordingly

  Scenario: Add and pay fines
    Given a patron "Tom" is registered with email "tom@example.com"
    When a fine of 25.00 is added with reason "Overdue book"
    Then the outstanding fines should be 25.00
    When the patron pays 25.00 of the fines
    Then the outstanding fines should be 0.00

  Scenario: Auto-suspend when fines exceed threshold
    Given a patron "Sam" is registered with email "sam@example.com"
    When a fine of 60.00 is added with reason "Lost book"
    Then the patron status should be "SUSPENDED"
    When the patron pays 60.00 of the fines
    Then the patron status should be "ACTIVE"

  Scenario: Waive fines
    Given a patron "Lee" is registered with email "lee@example.com"
    When a fine of 30.00 is added with reason "Late return"
    When 30.00 of the fines are waived
    Then the outstanding fines should be 0.00

  Scenario: Auto-reactivate when waived fines drop below threshold
    Given a patron "Grace" is registered with email "grace@example.com"
    When a fine of 55.00 is added with reason "Damaged book"
    Then the patron status should be "SUSPENDED"
    When 10.00 of the fines are waived
    Then the patron status should be "ACTIVE"

  Scenario: Cannot reactivate when fines still exceed threshold
    Given a patron "HighFine" is registered with email "highfine@example.com"
    When a fine of 60.00 is added with reason "Lost book"
    Then the patron status should be "SUSPENDED"
    When the patron is reactivated with reason "Attempt to reactivate"
    Then the operation should fail
    And the patron status should be "SUSPENDED"
