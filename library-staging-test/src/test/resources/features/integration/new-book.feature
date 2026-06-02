Feature: New Book Cataloging Cross-Context Integration
  As the library system
  When a new book is cataloged (Catalog publishes BookCreatedEvent)
  I want the Inventory context to create an inventory record

  Scenario: New book creates inventory record
    Given the main library with code "MAIN-LIB-001" exists
    When a BookCreatedEvent is published with book "book-new-001", ISBN "9780134685991", title "Effective Java"
    Then an inventory record should exist for book "book-new-001" with total copies 0
