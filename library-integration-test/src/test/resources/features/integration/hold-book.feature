Feature: Hold Book Cross-Context Integration
  As the library system
  When a hold is placed or fulfilled (Circulation publishes hold events)
  I want the Notification context to create appropriate notifications

  Scenario: Hold placed creates notification for patron
    Given a patron "Bob Johnson" with email "bob.hold@test.com" exists as faculty
    When a HoldPlacedEvent is published for that patron with hold "hold-001", book "book-001", queue position 1
    Then a notification should exist for the patron

  Scenario: Hold fulfilled creates notification for patron
    Given a patron "Bob Johnson" with email "bob.hold2@test.com" exists as faculty
    When a HoldFulfilledEvent is published for that patron with hold "hold-001", book "book-001", copy "copy-001"
    Then a notification should exist for the patron
