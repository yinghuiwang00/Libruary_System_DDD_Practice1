Feature: Library Branch Management
  As a library administrator
  I want to manage library branch information
  In order to support multi-branch inventory management

  Scenario: Successfully create a new library branch
    Given no library with code "LIB-001" exists in the system
    When I create a new library branch with code "LIB-001" and name "Main Library"
    And the address is "1 Zhongguancun Street"
    And the contact phone is "010-12345678"
    Then the library branch is created successfully
    And the library branch status is "ACTIVE"
