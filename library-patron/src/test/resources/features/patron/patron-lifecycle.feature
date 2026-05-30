Feature: Patron Lifecycle Management
  As a library administrator
  I want to manage patron lifecycle states
  So that I can control patron access to library services

  Scenario: Suspend and reactivate a patron
    Given a patron "Jane Smith" is registered with email "jane@example.com"
    When the patron is suspended with reason "Overdue books"
    Then the patron status should be "SUSPENDED"
    When the patron is reactivated with reason "Books returned"
    Then the patron status should be "ACTIVE"

  Scenario: Terminate a patron
    Given a patron "Bob" is registered with email "bob@example.com"
    When the patron is terminated with reason "Graduated"
    Then the patron status should be "TERMINATED"

  Scenario: Extend membership by 12 months
    Given a patron "Alice" is registered with email "alice@example.com"
    When the patron membership is extended by 12 months
    Then the membership should be extended by 12 months

  Scenario: Cannot suspend a terminated patron
    Given a patron "Charlie" is registered with email "charlie@example.com"
    When the patron is terminated with reason "Left school"
    When the patron is suspended with reason "Testing"
    Then the operation should fail

  Scenario: Cannot terminate a patron with outstanding fines
    Given a patron "Debt" is registered with email "debt@example.com"
    When a fine of 10.00 is added with reason "Late book"
    When the patron is terminated with reason "Graduated"
    Then the operation should fail
