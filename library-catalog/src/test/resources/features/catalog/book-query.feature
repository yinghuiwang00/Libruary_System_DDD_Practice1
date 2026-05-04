Feature: 图书查询
  作为读者
  我想通过ID查询图书
  以便获取图书详细信息

  Scenario: 通过ID成功查询已发布的图书
    Given 系统中存在一本ISBN为"9787111407010"且状态为"PUBLISHED"的图书
    When 我通过该图书的ID查询图书
    Then 返回图书信息
    And 图书标题为"领域驱动设计"