Feature: 图书发布
  作为图书馆管理员
  我想将草稿状态的图书发布
  以便读者可以搜索和借阅

  Scenario: 成功发布草稿状态的图书
    Given 系统中存在一本状态为"DRAFT"的图书
    When 我发布该图书
    Then 图书状态变为"PUBLISHED"