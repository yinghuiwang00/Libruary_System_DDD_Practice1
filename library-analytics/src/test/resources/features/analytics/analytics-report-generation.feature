Feature: Analytics Report Generation
  As a library administrator
  I want to generate analytics reports
  So that I can analyze library operations and trends

  Scenario: Generate and complete a circulation report
    Given the analytics service is available
    When I request a CIRCULATION_REPORT for MONTHLY period
    Then the report should be in GENERATING status
    When I complete the report with 100 records
    Then the report should be in COMPLETED status
    And the report should have 100 total records

  Scenario: Generate and fail a report
    Given the analytics service is available
    When I request a INVENTORY_REPORT for WEEKLY period
    Then the report should be in GENERATING status
    When I fail the report with error "Data source unavailable"
    Then the report should be in FAILED status

  Scenario: Generate, cancel, and regenerate a report
    Given the analytics service is available
    When I request a PATRON_REPORT for QUARTERLY period
    Then the report should be in GENERATING status
    When I cancel the report with reason "No longer needed"
    Then the report should be in CANCELLED status
    When I regenerate the report as "admin2"
    Then the report should be in GENERATING status

  Scenario: List all reports
    Given the analytics service is available
    When I request a CIRCULATION_REPORT for MONTHLY period
    And I request a INVENTORY_REPORT for WEEKLY period
    When I list all reports
    Then I should see 2 reports

  Scenario: Filter reports by type
    Given the analytics service is available
    When I request a CIRCULATION_REPORT for MONTHLY period
    And I request a INVENTORY_REPORT for WEEKLY period
    When I filter reports by type CIRCULATION_REPORT
    Then I should see 1 report
    And the report type should be CIRCULATION_REPORT

  Scenario: Filter reports by status
    Given the analytics service is available
    When I request a CIRCULATION_REPORT for MONTHLY period
    And I complete the report with 50 records
    When I request a INVENTORY_REPORT for WEEKLY period
    When I filter reports by status COMPLETED
    Then I should see 1 report
    And the report status should be COMPLETED
