Feature: 图书创建
  作为图书馆管理员
  我想创建新的图书记录
  以便管理图书馆的藏书信息

  Scenario: 成功创建新图书
    Given 系统中不存在ISBN为"9787111407010"的图书
    When 我创建一本新书，标题为"领域驱动设计"，作者为"Eric Evans"
    And ISBN为"9787111407010"
    And 分类为"SOFTWARE_ENGINEERING"
    Then 图书创建成功
    And 图书状态为"DRAFT"