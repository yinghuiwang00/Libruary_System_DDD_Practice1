Feature: Patron Suspension Cross-Context Integration
  As the library system
  When a patron is suspended (Patron publishes PatronSuspendedEvent)
  I want the Notification context to create a status change notification

  Scenario: Patron suspension creates notification
    Given a patron "Tom Brown" with email "tom.suspend@test.com" exists
    When a PatronSuspendedEvent is published for that patron with reason "Excessive overdue books"
    Then a notification should exist for the patron
