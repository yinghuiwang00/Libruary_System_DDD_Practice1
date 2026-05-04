# 测试计划 (Test Plan)

> **版本**: v1.0
> **创建日期**: 2026-05-03
> **范围**: Functional Test (Cucumber BDD) + Integration Test

本文档定义了图书馆管理系统所有 Functional Test 和 Integration Test 的测试用例，供后续自动化测试生成使用。

---

## 目录

1. [测试策略](#1-测试策略)
2. [编目上下文测试](#2-编目上下文-catalog)
3. [馆藏上下文测试](#3-馆藏上下文-inventory)
4. [借阅上下文测试](#4-借阅上下文-circulation)
5. [会员上下文测试](#5-会员上下文-patron)
6. [支付上下文测试](#6-支付上下文-payment)
7. [分析上下文测试](#7-分析上下文-analytics)
8. [通知上下文测试](#8-通知上下文-notification)
9. [跨上下文集成测试](#9-跨上下文集成测试)
10. [附录A: Cucumber配置](#附录a-cucumber依赖与配置)
11. [附录B: 测试统计](#附录b-测试统计)

---

## 1. 测试策略

### 1.1 Functional Test (Cucumber BDD)

- **框架**: Cucumber + Spring Boot
- **范围**: 每个组件的 **主要正向流程 (happy path)** only
- **目的**: 验证业务流程端到端正确性
- **位置**: `src/test/java/com/library/{context}/functional/`

### 1.2 Integration Test

- **框架**: Spring Boot Test + TestContainers (PostgreSQL, Kafka, Redis)
- **范围**: Repository、Service、Controller、Domain Event 各层
- **目的**: 验证各层与外部依赖的正确集成
- **位置**: `src/test/java/com/library/{context}/integration/`

### 1.3 命名规范

```
Functional:  {FeatureName}FunctionalTest.java
Integration: {ClassName}IntegrationTest.java
Cucumber:    {feature-name}.feature
```

---

## 2. 编目上下文 (Catalog)

### 2.1 Functional Tests (Cucumber)

#### Feature 1: 图书创建

```gherkin
Feature: 图书创建
  作为图书馆管理员
  我想创建新的图书记录
  以便管理图书馆的藏书信息

  Scenario: 成功创建新图书
    Given 系统中不存在ISBN为"978-7-111-40701-0"的图书
    When 我创建一本新书，标题为"领域驱动设计"，作者为"Eric Evans"
    And ISBN为"978-7-111-40701-0"
    And 分类为"SOFTWARE_ENGINEERING"
    Then 图书创建成功
    And 图书状态为"DRAFT"
    And 系统发布"BookCreatedEvent"事件
```

#### Feature 2: 图书发布

```gherkin
Feature: 图书发布
  作为图书馆管理员
  我想将草稿状态的图书发布
  以便读者可以搜索和借阅

  Scenario: 成功发布草稿状态的图书
    Given 系统中存在一本状态为"DRAFT"的图书
    When 我发布该图书
    Then 图书状态变为"PUBLISHED"
    And 系统发布"BookPublishedEvent"事件
```

#### Feature 3: 图书更新

```gherkin
Feature: 图书更新
  作为图书馆管理员
  我想更新已发布图书的信息
  以便保持信息的准确性

  Scenario: 成功更新已发布图书的标题和描述
    Given 系统中存在一本状态为"PUBLISHED"的图书
    When 我将该图书的标题更新为"领域驱动设计(修订版)"
    Then 图书更新成功
    And 图书标题为"领域驱动设计(修订版)"
    And 系统发布"BookUpdatedEvent"事件
```

#### Feature 4: 图书查询

```gherkin
Feature: 图书查询
  作为读者
  我想通过ISBN查询图书
  以便获取图书详细信息

  Scenario: 通过ISBN成功查询已发布的图书
    Given 系统中存在一本ISBN为"978-7-111-40701-0"且状态为"PUBLISHED"的图书
    When 我通过ISBN"978-7-111-40701-0"查询图书
    Then 返回图书信息
    And 图书标题为"领域驱动设计"
```

### 2.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| CAT-IT-01 | BookRepositoryIntegrationTest | testSaveAndFindById | 保存图书并按ID查询 |
| CAT-IT-02 | BookRepositoryIntegrationTest | testFindByIsbn | 按ISBN查询图书 |
| CAT-IT-03 | BookRepositoryIntegrationTest | testFindByStatus | 按状态查询图书列表 |
| CAT-IT-04 | BookRepositoryIntegrationTest | testFindByCategory | 按分类查询图书列表 |
| CAT-IT-05 | BookRepositoryIntegrationTest | testUpdateTitle | 更新图书标题持久化 |
| CAT-IT-06 | BookRepositoryIntegrationTest | testOptimisticLocking | 并发更新乐观锁冲突 |
| CAT-IT-07 | BookServiceIntegrationTest | testCreateBook | 创建图书完整流程 |
| CAT-IT-08 | BookServiceIntegrationTest | testPublishBook | 发布图书状态变更 |
| CAT-IT-09 | BookServiceIntegrationTest | testUpdateBook | 更新图书信息 |
| CAT-IT-10 | BookServiceIntegrationTest | testUnpublishBook | 下架图书 |
| CAT-IT-11 | BookServiceIntegrationTest | testDeleteBook | 删除图书 |
| CAT-IT-12 | BookControllerIntegrationTest | testCreateBookAPI | POST /api/catalog/books |
| CAT-IT-13 | BookControllerIntegrationTest | testGetBookAPI | GET /api/catalog/books/{id} |
| CAT-IT-14 | BookControllerIntegrationTest | testUpdateBookAPI | PUT /api/catalog/books/{id} |
| CAT-IT-15 | BookControllerIntegrationTest | testPublishBookAPI | POST /api/catalog/books/{id}/publish |
| CAT-IT-16 | BookControllerIntegrationTest | testDeleteBookAPI | DELETE /api/catalog/books/{id} |
| CAT-IT-17 | BookControllerIntegrationTest | testSearchBooksAPI | GET /api/catalog/books/search |
| CAT-IT-18 | DomainEventPublisherIntegrationTest | testBookCreatedEvent | BookCreatedEvent发布到Kafka |
| CAT-IT-19 | DomainEventPublisherIntegrationTest | testBookPublishedEvent | BookPublishedEvent发布到Kafka |

---

## 3. 馆藏上下文 (Inventory)

### 3.1 Functional Tests (Cucumber)

#### Feature 5: 创建馆藏记录

```gherkin
Feature: 创建馆藏记录
  作为图书馆管理员
  我想为新书创建馆藏记录
  以便追踪每本图书的物理副本

  Scenario: 成功为新书创建馆藏记录
    Given 系统中存在一本已发布的图书
    When 我为该图书创建馆藏记录，分馆为"主馆"
    Then 馆藏记录创建成功
    And 系统发布"InventoryCreatedEvent"事件
```

#### Feature 6: 添加图书副本

```gherkin
Feature: 添加图书副本
  作为图书馆管理员
  我想向馆藏中添加图书副本
  以便增加可借阅数量

  Scenario: 成功向馆藏添加图书副本
    Given 系统中存在某图书的馆藏记录
    When 我向该馆藏添加3个副本
    Then 副本添加成功
    And 馆藏的可用副本数量增加3
    And 系统发布"BookCopyAddedEvent"事件
```

#### Feature 7: 图书副本转移

```gherkin
Feature: 图书副本转移
  作为图书馆管理员
  我想将图书副本在分馆之间转移
  以便平衡各分馆的藏书

  Scenario: 成功将副本从主馆转移到分馆
    Given 主馆存在一个状态为"AVAILABLE"的图书副本
    And 目标分馆"科技分馆"存在
    When 我将该副本转移到"科技分馆"
    Then 副本归属分馆变为"科技分馆"
    And 系统发布"BookCopyTransferredEvent"事件
```

### 3.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| INV-IT-01 | CopyInventoryRepositoryIntegrationTest | testSaveAndFindById | 保存馆藏并按ID查询 |
| INV-IT-02 | CopyInventoryRepositoryIntegrationTest | testFindByBookId | 按图书ID查询馆藏 |
| INV-IT-03 | CopyInventoryRepositoryIntegrationTest | testFindAvailableCopies | 查询可用副本 |
| INV-IT-04 | CopyInventoryRepositoryIntegrationTest | testFindByBranchAndStatus | 按分馆和状态查询 |
| INV-IT-05 | BookCopyRepositoryIntegrationTest | testSaveAndFindByBarcode | 按条形码查询副本 |
| INV-IT-06 | BookCopyRepositoryIntegrationTest | testUpdateCopyStatus | 更新副本状态 |
| INV-IT-07 | InventoryServiceIntegrationTest | testCreateInventory | 创建馆藏完整流程 |
| INV-IT-08 | InventoryServiceIntegrationTest | testAddBookCopies | 添加副本 |
| INV-IT-09 | InventoryServiceIntegrationTest | testTransferCopy | 转移副本 |
| INV-IT-10 | InventoryServiceIntegrationTest | testMarkCopyDamaged | 标记副本损坏 |
| INV-IT-11 | InventoryServiceIntegrationTest | testMarkCopyLost | 标记副本丢失 |
| INV-IT-12 | InventoryControllerIntegrationTest | testCreateInventoryAPI | POST /api/inventory/inventories |
| INV-IT-13 | InventoryControllerIntegrationTest | testAddCopiesAPI | POST /api/inventory/inventories/{id}/copies |
| INV-IT-14 | InventoryControllerIntegrationTest | testTransferCopyAPI | POST /api/inventory/copies/{id}/transfer |
| INV-IT-15 | InventoryControllerIntegrationTest | testGetInventoryAPI | GET /api/inventory/inventories/{id} |
| INV-IT-16 | DomainEventPublisherIntegrationTest | testInventoryCreatedEvent | InventoryCreatedEvent发布 |
| INV-IT-17 | DomainEventPublisherIntegrationTest | testCopyTransferredEvent | BookCopyTransferredEvent发布 |

---

## 4. 借阅上下文 (Circulation)

### 4.1 Functional Tests (Cucumber)

#### Feature 8: 借书流程

```gherkin
Feature: 借书流程
  作为读者
  我想借阅一本可用的图书
  以便阅读

  Scenario: 读者成功借阅可用图书
    Given 读者"张三"拥有有效的会员资格
    And 图书副本"COPY-001"状态为"AVAILABLE"
    And 读者当前借阅数量未达上限
    When 读者借阅该图书副本
    Then 借阅记录创建成功
    And 图书副本状态变为"BORROWED"
    And 读者借阅数量增加1
    And 系统发布"BookBorrowedEvent"事件
```

#### Feature 9: 还书流程

```gherkin
Feature: 还书流程
  作为读者
  我想归还已借阅的图书
  以便完成借阅周期

  Scenario: 读者成功归还图书（无逾期）
    Given 读者"张三"有一本未逾期的借阅记录
    When 读者归还该图书
    Then 借阅记录状态变为"RETURNED"
    And 图书副本状态变为"AVAILABLE"
    And 系统发布"BookReturnedEvent"事件
```

#### Feature 10: 逾期还书

```gherkin
Feature: 逾期还书
  作为读者
  我想归还逾期的图书
  以便结束借阅并支付罚款

  Scenario: 读者归还逾期图书并产生罚款
    Given 读者"张三"有一本已逾期5天的借阅记录
    When 读者归还该图书
    Then 借阅记录状态变为"RETURNED"
    And 系统计算罚款为2.50元
    And 系统发布"BookReturnedEvent"事件
    And 系统发布"FineGeneratedEvent"事件
```

#### Feature 11: 续借流程

```gherkin
Feature: 续借流程
  作为读者
  我想续借当前借阅的图书
  以便延长阅读时间

  Scenario: 读者成功续借图书
    Given 读者"张三"有一本未逾期的借阅记录
    And 该借阅尚未续借过
    When 读者续借该图书
    Then 借阅到期日延长30天
    And 系统发布"BookRenewedEvent"事件
```

#### Feature 12: 预约流程

```gherkin
Feature: 预约流程
  作为读者
  我想预约已被借出的图书
  以便在归还时优先获取

  Scenario: 读者成功预约已借出的图书
    Given 读者"李四"拥有有效的会员资格
    And 图书副本"COPY-001"状态为"BORROWED"
    And 该图书无其他待处理的预约
    When 读者预约该图书
    Then 预约记录创建成功
    And 预约状态为"PENDING"
    And 系统发布"BookHoldPlacedEvent"事件
```

#### Feature 13: 预约取书

```gherkin
Feature: 预约取书
  作为读者
  我想取回已预约且可用的图书
  以便完成借阅

  Scenario: 预约读者在图书归还后成功取书
    Given 读者"李四"有一份状态为"AVAILABLE_FOR_PICKUP"的预约
    When 读者取书
    Then 借阅记录创建成功
    And 预约状态变为"FULFILLED"
    And 图书副本状态变为"BORROWED"
```

### 4.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| CIR-IT-01 | LoanRepositoryIntegrationTest | testSaveAndFindById | 保存借阅记录并查询 |
| CIR-IT-02 | LoanRepositoryIntegrationTest | testFindByPatronId | 按读者查询借阅记录 |
| CIR-IT-03 | LoanRepositoryIntegrationTest | testFindByCopyId | 按副本查询借阅记录 |
| CIR-IT-04 | LoanRepositoryIntegrationTest | testFindOverdueLoans | 查询逾期借阅记录 |
| CIR-IT-05 | LoanRepositoryIntegrationTest | testFindActiveLoansByPatron | 查询读者活跃借阅 |
| CIR-IT-06 | HoldRepositoryIntegrationTest | testSaveAndFindById | 保存预约记录并查询 |
| CIR-IT-07 | HoldRepositoryIntegrationTest | testFindByBookIdAndStatus | 按图书和状态查询预约 |
| CIR-IT-08 | HoldRepositoryIntegrationTest | testFindPendingHoldsByBookId | 查询待处理预约 |
| CIR-IT-09 | CirculationServiceIntegrationTest | testBorrowBook | 借书完整流程 |
| CIR-IT-10 | CirculationServiceIntegrationTest | testReturnBook | 还书完整流程 |
| CIR-IT-11 | CirculationServiceIntegrationTest | testReturnOverdueBook | 逾期还书及罚款计算 |
| CIR-IT-12 | CirculationServiceIntegrationTest | testRenewBook | 续借流程 |
| CIR-IT-13 | CirculationServiceIntegrationTest | testRenewExceedsMaxRenewals | 超过最大续借次数 |
| CIR-IT-14 | CirculationServiceIntegrationTest | testPlaceHold | 预约流程 |
| CIR-IT-15 | CirculationServiceIntegrationTest | testFulfillHold | 预约取书流程 |
| CIR-IT-16 | CirculationServiceIntegrationTest | testCancelHold | 取消预约 |
| CIR-IT-17 | CirculationServiceIntegrationTest | testCalculateFine | 罚款计算（标准策略） |
| CIR-IT-18 | CirculationServiceIntegrationTest | testCalculateFineWithMaxCap | 罚款封顶计算 |
| CIR-IT-19 | CirculationControllerIntegrationTest | testBorrowAPI | POST /api/circulation/loans |
| CIR-IT-20 | CirculationControllerIntegrationTest | testReturnAPI | POST /api/circulation/loans/{id}/return |
| CIR-IT-21 | CirculationControllerIntegrationTest | testRenewAPI | POST /api/circulation/loans/{id}/renew |
| CIR-IT-22 | CirculationControllerIntegrationTest | testPlaceHoldAPI | POST /api/circulation/holds |
| CIR-IT-23 | CirculationControllerIntegrationTest | testFulfillHoldAPI | POST /api/circulation/holds/{id}/fulfill |
| CIR-IT-24 | DomainEventPublisherIntegrationTest | testBookBorrowedEvent | BookBorrowedEvent发布 |
| CIR-IT-25 | DomainEventPublisherIntegrationTest | testBookReturnedEvent | BookReturnedEvent发布 |
| CIR-IT-26 | DomainEventPublisherIntegrationTest | testFineGeneratedEvent | FineGeneratedEvent发布 |

---

## 5. 会员上下文 (Patron)

### 5.1 Functional Tests (Cucumber)

#### Feature 14: 会员注册

```gherkin
Feature: 会员注册
  作为新用户
  我想注册成为图书馆会员
  以便借阅图书

  Scenario: 学生成功注册为图书馆会员
    Given 系统中不存在邮箱为"zhangsan@university.edu"的会员
    When 我注册一个新会员，姓名为"张三"，类型为"STUDENT"
    Then 会员注册成功
    And 会员状态为"ACTIVE"
    And 借阅上限为5本
    And 系统发布"PatronRegisteredEvent"事件
```

#### Feature 15: 会员信息更新

```gherkin
Feature: 会员信息更新
  作为图书馆会员
  我想更新我的联系信息
  以便接收通知

  Scenario: 成功更新会员手机号
    Given 系统中存在会员"张三"
    When 我将该会员的手机号更新为"13800138000"
    Then 会员信息更新成功
    And 手机号为"13800138000"
```

#### Feature 16: 会员状态管理

```gherkin
Feature: 会员状态管理
  作为图书馆管理员
  我想暂停违规读者的会员资格
  以便执行借阅管理

  Scenario: 成功暂停会员资格
    Given 系统中存在状态为"ACTIVE"的会员"张三"
    When 我暂停该会员的资格，原因为"多次逾期"
    Then 会员状态变为"SUSPENDED"
    And 该会员无法借阅图书
    And 系统发布"PatronSuspendedEvent"事件
```

### 5.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| PAT-IT-01 | PatronRepositoryIntegrationTest | testSaveAndFindById | 保存会员并按ID查询 |
| PAT-IT-02 | PatronRepositoryIntegrationTest | testFindByEmail | 按邮箱查询会员 |
| PAT-IT-03 | PatronRepositoryIntegrationTest | testFindByMembershipNumber | 按会员号查询 |
| PAT-IT-04 | PatronRepositoryIntegrationTest | testFindByStatus | 按状态查询会员列表 |
| PAT-IT-05 | PatronServiceIntegrationTest | testRegisterPatron | 注册会员完整流程 |
| PAT-IT-06 | PatronServiceIntegrationTest | testUpdateContactInfo | 更新联系方式 |
| PAT-IT-07 | PatronServiceIntegrationTest | testSuspendPatron | 暂停会员资格 |
| PAT-IT-08 | PatronServiceIntegrationTest | testReactivatePatron | 恢复会员资格 |
| PAT-IT-09 | PatronServiceIntegrationTest | testCheckBorrowingPrivilege | 检查借阅权限 |
| PAT-IT-10 | PatronControllerIntegrationTest | testRegisterAPI | POST /api/patrons |
| PAT-IT-11 | PatronControllerIntegrationTest | testGetPatronAPI | GET /api/patrons/{id} |
| PAT-IT-12 | PatronControllerIntegrationTest | testUpdatePatronAPI | PUT /api/patrons/{id} |
| PAT-IT-13 | PatronControllerIntegrationTest | testSuspendPatronAPI | POST /api/patrons/{id}/suspend |
| PAT-IT-14 | DomainEventPublisherIntegrationTest | testPatronRegisteredEvent | PatronRegisteredEvent发布 |
| PAT-IT-15 | DomainEventPublisherIntegrationTest | testPatronSuspendedEvent | PatronSuspendedEvent发布 |

---

## 6. 支付上下文 (Payment)

### 6.1 Functional Tests (Cucumber)

#### Feature 17: 罚款支付

```gherkin
Feature: 罚款支付
  作为读者
  我想支付逾期罚款
  以便恢复借阅资格

  Scenario: 读者通过支付宝成功支付罚款
    Given 读者"张三"有2.50元的未支付罚款
    When 读者通过"ALIPAY"支付该罚款
    Then 支付状态变为"COMPLETED"
    And 罚款状态变为"PAID"
    And 系统发布"PaymentCompletedEvent"事件
```

#### Feature 18: 支付退款

```gherkin
Feature: 支付退款
  作为图书馆管理员
  我想对错误收费进行退款
  以便维护读者权益

  Scenario: 管理员成功发起退款
    Given 系统中存在一笔状态为"COMPLETED"的支付记录
    When 我对该支付发起退款
    Then 支付状态变为"REFUNDED"
    And 系统发布"PaymentRefundedEvent"事件
```

### 6.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| PAY-IT-01 | PaymentRepositoryIntegrationTest | testSaveAndFindById | 保存支付记录并查询 |
| PAY-IT-02 | PaymentRepositoryIntegrationTest | testFindByPatronId | 按读者查询支付记录 |
| PAY-IT-03 | PaymentRepositoryIntegrationTest | testFindByStatus | 按状态查询支付记录 |
| PAY-IT-04 | PaymentServiceIntegrationTest | testProcessPayment | 处理支付完整流程 |
| PAY-IT-05 | PaymentServiceIntegrationTest | testRefundPayment | 退款流程 |
| PAY-IT-06 | PaymentServiceIntegrationTest | testProcessPaymentWithCash | 现金支付 |
| PAY-IT-07 | PaymentServiceIntegrationTest | testProcessPaymentWithCard | 银行卡支付 |
| PAY-IT-08 | PaymentControllerIntegrationTest | testProcessPaymentAPI | POST /api/payments |
| PAY-IT-09 | PaymentControllerIntegrationTest | testRefundPaymentAPI | POST /api/payments/{id}/refund |
| PAY-IT-10 | PaymentControllerIntegrationTest | testGetPaymentAPI | GET /api/payments/{id} |
| PAY-IT-11 | DomainEventPublisherIntegrationTest | testPaymentCompletedEvent | PaymentCompletedEvent发布 |
| PAY-IT-12 | DomainEventPublisherIntegrationTest | testPaymentRefundedEvent | PaymentRefundedEvent发布 |

---

## 7. 分析上下文 (Analytics)

### 7.1 Functional Tests (Cucumber)

#### Feature 19: 借阅统计报表

```gherkin
Feature: 借阅统计报表
  作为图书馆管理员
  我想查看借阅统计报表
  以便了解图书馆运营状况

  Scenario: 成功生成月度借阅统计报表
    Given 系统中存在本月50条借阅记录
    When 我生成"2026年4月"的借阅统计报表
    Then 报表生成成功
    And 报表类型为"BORROWING_STATS"
    And 总借阅次数为50
    And 系统发布"ReportGeneratedEvent"事件
```

#### Feature 20: 热门图书排行

```gherkin
Feature: 热门图书排行
  作为读者
  我想查看热门图书排行
  以便选择感兴趣的图书

  Scenario: 成功获取热门图书TOP10
    Given 系统中存在借阅统计数据
    When 我请求热门图书TOP10排行
    Then 返回10本图书的排行列表
    And 列表按借阅次数降序排列
```

### 7.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| ANA-IT-01 | AnalyticsReportRepositoryIntegrationTest | testSaveAndFindById | 保存报表并查询 |
| ANA-IT-02 | AnalyticsReportRepositoryIntegrationTest | testFindByTypeAndPeriod | 按类型和时期查询报表 |
| ANA-IT-03 | AnalyticsServiceIntegrationTest | testGenerateBorrowingStats | 生成借阅统计 |
| ANA-IT-04 | AnalyticsServiceIntegrationTest | testGetPopularBooks | 获取热门图书排行 |
| ANA-IT-05 | AnalyticsServiceIntegrationTest | testGetPatronAnalysis | 获取读者分析数据 |
| ANA-IT-06 | AnalyticsControllerIntegrationTest | testGetBorrowingStatsAPI | GET /api/analytics/borrowing-stats |
| ANA-IT-07 | AnalyticsControllerIntegrationTest | testGetPopularBooksAPI | GET /api/analytics/popular-books |
| ANA-IT-08 | AnalyticsControllerIntegrationTest | testGetPatronAnalysisAPI | GET /api/analytics/patron-analysis |
| ANA-IT-09 | DomainEventPublisherIntegrationTest | testReportGeneratedEvent | ReportGeneratedEvent发布 |

---

## 8. 通知上下文 (Notification)

### 8.1 Functional Tests (Cucumber)

#### Feature 21: 到期提醒

```gherkin
Feature: 到期提醒
  作为系统
  我想在图书到期前发送提醒
  以便读者按时归还

  Scenario: 系统在到期前3天发送提醒
    Given 读者"张三"有一本3天后到期的借阅
    When 系统执行到期提醒任务
    Then 向读者发送"DUE_REMINDER"通知
    And 通知渠道包含"EMAIL"
    And 通知状态为"SENT"
    And 系统发布"NotificationSentEvent"事件
```

#### Feature 22: 预约可取书通知

```gherkin
Feature: 预约可取书通知
  作为系统
  我想在预约图书可用时通知读者
  以便读者及时取书

  Scenario: 预约图书归还后系统发送取书通知
    Given 读者"李四"有一份状态为"AVAILABLE_FOR_PICKUP"的预约
    When 系统发送预约可用通知
    Then 向读者发送"HOLD_AVAILABLE"通知
    And 通知内容包含预约图书信息
    And 通知渠道包含"EMAIL"和"SMS"
```

#### Feature 23: 支付成功通知

```gherkin
Feature: 支付成功通知
  作为系统
  我想在支付成功后发送确认通知
  以便读者确认支付状态

  Scenario: 罚款支付成功后发送确认通知
    Given 读者"张三"完成了一笔罚款支付
    When 系统发送支付确认通知
    Then 向读者发送"PAYMENT_SUCCESS"通知
    And 通知内容包含支付金额
```

### 8.2 Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| NOT-IT-01 | NotificationRepositoryIntegrationTest | testSaveAndFindById | 保存通知并查询 |
| NOT-IT-02 | NotificationRepositoryIntegrationTest | testFindByPatronId | 按读者查询通知列表 |
| NOT-IT-03 | NotificationRepositoryIntegrationTest | testFindByStatus | 按状态查询通知 |
| NOT-IT-04 | NotificationServiceIntegrationTest | testSendDueReminder | 发送到期提醒 |
| NOT-IT-05 | NotificationServiceIntegrationTest | testSendHoldAvailableNotification | 发送预约可用通知 |
| NOT-IT-06 | NotificationServiceIntegrationTest | testSendPaymentSuccessNotification | 发送支付成功通知 |
| NOT-IT-07 | NotificationServiceIntegrationTest | testSendMultiChannelNotification | 多渠道发送通知 |
| NOT-IT-08 | NotificationControllerIntegrationTest | testGetNotificationsAPI | GET /api/notifications |
| NOT-IT-09 | NotificationControllerIntegrationTest | testMarkAsReadAPI | PUT /api/notifications/{id}/read |
| NOT-IT-10 | DomainEventPublisherIntegrationTest | testNotificationSentEvent | NotificationSentEvent发布 |

---

## 9. 跨上下文集成测试

### 9.1 Functional Tests (Cucumber)

#### Feature 24: 完整借阅生命周期

```gherkin
Feature: 完整借阅生命周期
  作为系统
  我想验证从创建到借阅的完整流程
  以便确保各上下文正确协作

  Scenario: 图书从创建到被借阅的完整流程
    Given 管理员创建了一本新书"领域驱动设计"
    And 管理员发布了该图书
    And 管理员为该图书创建了馆藏记录并添加了副本
    And 读者"张三"注册成为会员
    When 读者"张三"借阅该图书的一个副本
    Then 借阅记录创建成功
    And 副本状态变为"BORROWED"
    And 系统发送借阅确认通知
```

#### Feature 25: 逾期归还完整流程

```gherkin
Feature: 逾期归还完整流程
  作为系统
  我想验证逾期产生罚款到支付完成的完整流程
  以便确保借阅、支付、通知上下文正确协作

  Scenario: 逾期还书、罚款生成、支付、通知的完整流程
    Given 读者"张三"借阅了一本图书并已逾期5天
    When 读者归还该图书
    Then 系统生成2.50元罚款
    And 系统发送逾期归还通知
    When 读者通过支付宝支付该罚款
    Then 支付状态变为"COMPLETED"
    And 系统发送支付成功通知
    And 副本状态恢复为"AVAILABLE"
```

### 9.2 Saga Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| SAGA-IT-01 | BorrowBookSagaIntegrationTest | testCompleteBorrowSaga | 完整借阅Saga流程 |
| SAGA-IT-02 | BorrowBookSagaIntegrationTest | testBorrowSagaCompensation | 借阅Saga补偿 |
| SAGA-IT-03 | ReturnBookSagaIntegrationTest | testCompleteReturnSaga | 完整还书Saga流程 |
| SAGA-IT-04 | ReturnBookSagaIntegrationTest | testOverdueReturnSaga | 逾期还书Saga（含罚款生成） |
| SAGA-IT-05 | HoldFulfillmentSagaIntegrationTest | testHoldFulfillmentSaga | 预约取书Saga流程 |
| SAGA-IT-06 | PaymentSagaIntegrationTest | testPaymentSaga | 支付Saga完整流程 |
| SAGA-IT-07 | PaymentSagaIntegrationTest | testPaymentSagaCompensation | 支付Saga补偿（退款） |

### 9.3 Event-Driven Integration Tests

| ID | 测试类 | 测试方法 | 描述 |
|----|--------|----------|------|
| EVT-IT-01 | CrossContextEventIntegrationTest | testBookCreatedTriggersInventory | BookCreatedEvent触发馆藏创建 |
| EVT-IT-02 | CrossContextEventIntegrationTest | testBookBorrowedUpdatesInventory | BookBorrowedEvent更新副本状态 |
| EVT-IT-03 | CrossContextEventIntegrationTest | testBookReturnedUpdatesInventory | BookReturnedEvent恢复副本状态 |
| EVT-IT-04 | CrossContextEventIntegrationTest | testFineGeneratedTriggersNotification | FineGeneratedEvent触发罚款通知 |
| EVT-IT-05 | CrossContextEventIntegrationTest | testHoldAvailableTriggersNotification | HoldAvailableEvent触发预约通知 |
| EVT-IT-06 | CrossContextEventIntegrationTest | testPaymentCompletedTriggersNotification | PaymentCompletedEvent触发支付通知 |
| EVT-IT-07 | CrossContextEventIntegrationTest | testBorrowingDataSyncsToAnalytics | 借阅数据同步到分析上下文 |

---

## 附录A: Cucumber依赖与配置

### Maven依赖

```xml
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-spring</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
```

### Feature文件位置

```
src/test/resources/features/
├── catalog/
│   ├── book-creation.feature
│   ├── book-publishing.feature
│   ├── book-update.feature
│   └── book-query.feature
├── inventory/
│   ├── inventory-creation.feature
│   ├── copy-addition.feature
│   └── copy-transfer.feature
├── circulation/
│   ├── borrowing.feature
│   ├── returning.feature
│   ├── overdue-return.feature
│   ├── renewal.feature
│   ├── hold-placement.feature
│   └── hold-fulfillment.feature
├── patron/
│   ├── patron-registration.feature
│   ├── patron-update.feature
│   └── patron-status.feature
├── payment/
│   ├── fine-payment.feature
│   └── payment-refund.feature
├── analytics/
│   ├── borrowing-stats.feature
│   └── popular-books.feature
├── notification/
│   ├── due-reminder.feature
│   ├── hold-notification.feature
│   └── payment-notification.feature
└── cross-context/
    ├── borrow-lifecycle.feature
    └── overdue-lifecycle.feature
```

### 基础测试类

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
}

@SpringBootTest
@CucumberContextConfiguration
public abstract class BaseCucumberTest {
    @Autowired
    protected ApplicationEventPublisher eventPublisher;
}
```

### TestContainers配置

```java
@Testcontainers
@SpringBootTest
public abstract class BaseTestContainersTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

---

## 附录B: 测试统计

| 上下文 | Cucumber Features | Cucumber Scenarios | Integration Tests | 总计 |
|--------|------------------:|-------------------:|------------------:|------:|
| Catalog | 4 | 4 | 19 | 23 |
| Inventory | 3 | 3 | 17 | 20 |
| Circulation | 6 | 6 | 26 | 32 |
| Patron | 3 | 3 | 15 | 18 |
| Payment | 2 | 2 | 12 | 14 |
| Analytics | 2 | 2 | 9 | 11 |
| Notification | 3 | 3 | 10 | 13 |
| 跨上下文 | 2 | 2 | 14 | 16 |
| **合计** | **25** | **25** | **122** | **147** |

---

> 本文档为测试自动化生成的基础，每个测试用例可映射到具体的Java测试类和方法。
> 实施时应遵循TDD原则：先编写测试，再实现功能代码。
