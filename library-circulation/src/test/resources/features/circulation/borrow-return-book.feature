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

  Scenario: 成功续借图书
    Given 读者"PATRON-004"已经借出了图书"BOOK-004"的副本"COPY-004"
    When 读者续借该图书
    Then 续借成功
    And 借阅状态为"RENEWED"
    And 续借次数为1
    And 应还日期已延长

  Scenario: 续借次数超过限制
    Given 读者"PATRON-005"已经借出了图书"BOOK-005"的副本"COPY-005"且已达到最大续借次数
    When 读者尝试续借该图书
    Then 续借应该失败

  Scenario: 标记借阅为逾期
    Given 读者"PATRON-006"已经借出了图书"BOOK-006"的副本"COPY-006"且已逾期
    When 系统处理逾期借阅
    Then 借阅状态应该为"OVERDUE"

  Scenario: 取消预约
    Given 读者"PATRON-007"已经预约了图书"BOOK-007"
    When 读者取消该预约
    Then 预约已取消
