Feature: 图书借出与归还
  作为图书馆管理员
  我想管理图书的借出和归还
  以便跟踪图书的流通状态

  Scenario: 成功借出图书
    Given 读者"PATRON-001"想要借阅图书"BOOK-001"的副本"COPY-001"
    When 读者借出该图书
    Then 借出成功
    And 借阅状态为"ACTIVE"

  Scenario: 成功归还图书
    Given 读者"PATRON-002"已经借出了图书"BOOK-002"的副本"COPY-002"
    When 读者归还该图书
    Then 归还成功
    And 借阅状态为"RETURNED"

  Scenario: 成功预约图书
    Given 图书"BOOK-003"当前不可借阅
    When 读者"PATRON-003"预约该图书
    Then 预约成功
    And 预约状态为"WAITING"
