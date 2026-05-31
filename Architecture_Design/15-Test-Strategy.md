# 测试策略文档 (Test Strategy)

> **版本**: v3.0
> **创建日期**: 2026-05-03
> **最后更新**: 2026-05-31
> **范围**: Unit Test + Integration Test + Functional Test (Cucumber BDD) + Cross-Context E2E Test

本文档定义了图书馆管理系统的完整测试策略，涵盖各限界上下文内部测试和跨上下文集成测试。

---

## 目录

1. [测试策略总览](#1-测试策略总览)
2. [编目上下文测试](#2-编目上下文-catalog)
3. [馆藏上下文测试](#3-馆藏上下文-inventory)
4. [借阅上下文测试](#4-借阅上下文-circulation)
5. [会员上下文测试](#5-会员上下文-patron)
6. [支付上下文测试](#6-支付上下文-payment)
7. [分析上下文测试](#7-分析上下文-analytics)
8. [通知上下文测试](#8-通知上下文-notification)
9. [跨上下文集成测试](#9-跨上下文集成测试)
10. [三层 Kafka 事件测试策略](#10-三层-kafka-事件测试策略)
11. [测试实施进度](#11-测试实施进度)
12. [附录A: Cucumber配置](#附录a-cucumber依赖与配置)
13. [附录B: 测试统计](#附录b-测试统计)

---

## 1. 测试策略总览

### 1.1 测试架构

本系统采用四层测试架构，覆盖从单元到端到端的完整链路：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         测试架构总览                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐       │
│   │ 单元测试       │   │ 集成测试          │   │ 端到端测试        │       │
│   │ (Unit Test)  │   │ (Integration)    │   │ (E2E Test)       │       │
│   │              │   │                  │   │                  │       │
│   │ JUnit 5      │   │ MockMvc + H2     │   │ 多模块启动        │       │
│   │ Mockito      │   │ @EmbeddedKafka   │   │ API → Kafka → DB │       │
│   │ AssertJ      │   │ Awaitility       │   │ 完整业务流程      │       │
│   └──────────────┘   └──────────────────┘   └──────────────────┘       │
│         ↑                     ↑                      ↑                  │
│   各上下文内部          Consumer 集成测试          跨模块 E2E             │
│   (Sections 2-8)      (Section 10)             (Section 9)            │
│                                                                         │
│   ┌──────────────┐                                                       │
│   │ BDD 测试      │                                                       │
│   │ (Cucumber)   │                                                       │
│   │              │                                                       │
│   │ Given/When/  │                                                       │
│   │ Then 场景     │                                                       │
│   └──────────────┘                                                       │
│         ↑                                                                │
│   各上下文 + 跨模块                                                        │
│   (Sections 2-8, 9)                                                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Functional Test (Cucumber BDD)

- **框架**: Cucumber + Spring Boot
- **范围**: 每个组件的 **主要正向流程 (happy path)** only
- **目的**: 验证业务流程端到端正确性
- **位置**: `src/test/java/com/library/{context}/functional/`

### 1.3 Integration Test

- **框架**: Spring Boot Test + H2 (PostgreSQL mode) + EmbeddedKafka
- **范围**: Repository、Service、Controller、Domain Event 各层
- **目的**: 验证各层与外部依赖的正确集成
- **位置**: `src/test/java/com/library/{context}/integration/`

### 1.4 跨上下文 E2E Test

- **框架**: Spring Boot Test + @EmbeddedKafka + Awaitility + Cucumber
- **范围**: 8 个跨上下文 Use Case 的完整事件流
- **目的**: 验证跨模块协作（API → Kafka → Consumer → DB）
- **位置**: `library-e2e-test/` 和 `library-integration-test/`

### 1.5 命名规范

```
单元测试:     {ClassName}Test.java
集成测试:     {ClassName}IntegrationTest.java
Functional:  {FeatureName}FunctionalTest.java
Cucumber:    {feature-name}.feature
E2E:         {Scenario}EndToEndTest.java
Publisher:   {Context}DomainEventPublisherTest.java
Consumer:    {Source}EventConsumerIntegrationTest.java
```

### 1.6 Kafka Topic 策略

跨上下文测试依赖以下 per-context topic 布局：

| Topic | 发布者 | 消费者 |
|-------|--------|--------|
| `library.catalog.events` | Catalog | Inventory, Analytics |
| `library.circulation.events` | Circulation | Inventory, Patron, Payment, Notification |
| `library.patron.events` | Patron | Circulation, Notification |
| `library.inventory.events` | Inventory | Notification, Analytics |
| `library.payment.events` | Payment | Patron, Notification |

Consumer group 命名：`library.{consumer-context}.consumer.{source-context}`

### 1.7 跨上下文事件三层代码结构

每个消费模块遵循统一的三层结构：

```
infrastructure/messaging/
  ├── {Context}DomainEventPublisher.java   ← 双发：Spring本地 + Kafka远程
  └── {Source}EventConsumer.java           ← @KafkaListener 接收事件

application/handler/
  └── {EventName}EventHandler.java         ← 防腐层：外部事件 → 本地领域操作
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

### 9.1 8个跨上下文 Use Case 总览

系统共 8 个跨上下文 Use Case，23 个事件-消费者绑定关系：

| # | Use Case | 事件发布方 | 事件消费方 | 核心事件 |
|---|----------|-----------|-----------|---------|
| UC-1 | 借书流程 | Circulation | Inventory, Patron, Notification | `BookBorrowedEvent` |
| UC-2 | 还书流程 | Circulation | Inventory, Patron, Notification | `BookReturnedEvent` |
| UC-3 | 预约流程 | Circulation | Notification | `HoldPlacedEvent`, `HoldFulfilledEvent` |
| UC-4 | 逾期/罚款流程 | Circulation | Payment, Patron, Notification | `FineIncurredEvent`, `OverdueNoticeEvent` |
| UC-5 | 支付流程 | Payment | Patron, Notification | `PaymentCompletedEvent` |
| UC-6 | 新书上架流程 | Catalog | Inventory, Analytics | `BookCreatedEvent` |
| UC-7 | 读者状态变更 | Patron | Circulation, Notification | `PatronSuspendedEvent` |
| UC-8 | 库存预警流程 | Inventory | Notification, Analytics | `LowStockAlertEvent` |

### 9.2 测试模块架构

项目包含两个互补的跨上下文测试模块：

| 模块 | 框架 | 用途 |
|------|------|------|
| `library-e2e-test` | JUnit 5 + Awaitility + @EmbeddedKafka | 直接验证跨上下文 Kafka 事件流 |
| `library-integration-test` | Cucumber BDD + @EmbeddedKafka | 业务可读的 Given/When/Then 场景 |

### 9.3 library-e2e-test（JUnit 5 直测）

**位置**: `library-e2e-test/src/test/java/com/library/integration/`

| 测试类 | 场景 | 验证 |
|--------|------|------|
| `BorrowBookEndToEndTest` | UC-1: 借书 | Patron 贷款数 +1, Notification 创建 |
| `ReturnBookEndToEndTest` | UC-2: 还书 | Patron 贷款数 -1, Notification 创建 |
| `HoldBookEndToEndTest` | UC-3: 预约 (2 tests) | Notification 创建 |
| `FinePaymentEndToEndTest` | UC-4+5: 罚金/支付 (2 tests) | Patron 罚款余额, Notification |
| `NewBookEndToEndTest` | UC-6: 新书上架 | Inventory 记录创建 |
| `PatronSuspensionEndToEndTest` | UC-7: 读者暂停 | Notification 创建 |
| `LowStockAlertEndToEndTest` | UC-8: 低库存预警 | LIBRARIAN Notification |

**测试总数**: 9 tests, 0 failures

### 9.4 library-integration-test（Cucumber BDD）

**位置**: `library-integration-test/src/test/`

**Feature 文件** (`resources/features/integration/`):

| Feature | 场景数 | 描述 |
|---------|--------|------|
| `borrow-book.feature` | 1 | 借书更新贷款数 + 创建通知 |
| `return-book.feature` | 1 | 还书减少贷款数 + 创建通知 |
| `hold-book.feature` | 2 | 预约排队 + 预约到馆 |
| `fine-payment.feature` | 2 | 罚金产生 + 支付完成 |
| `new-book.feature` | 1 | 新书上架创建库存 |
| `patron-suspension.feature` | 1 | 读者暂停创建通知 |
| `low-stock-alert.feature` | 1 | 低库存预警通知 LIBRARIAN |

**Step 定义类** (`java/com/library/integration/bdd/`):

| 类 | 职责 |
|----|------|
| `CucumberTestSuite` | JUnit Platform Suite 入口 |
| `CucumberSpringConfig` | @SpringBootTest + @EmbeddedKafka + @Before 清理 |
| `E2EScenarioState` | @Component 跨 Step 共享状态 |
| `SharedSteps` | 公共 Given/Then（创建 Patron, 验证 Notification） |
| `BorrowBookSteps` | UC-1 唯一 When/Then |
| `ReturnBookSteps` | UC-2 唯一 When |
| `HoldBookSteps` | UC-3 When (HoldPlaced + HoldFulfilled) |
| `FinePaymentSteps` | UC-4+5 Given/When/Then |
| `NewBookSteps` | UC-6 When/Then |
| `PatronSuspensionSteps` | UC-7 When |
| `LowStockAlertSteps` | UC-8 When |

**测试总数**: 9 scenarios, 0 failures

---

## 10. 三层 Kafka 事件测试策略

### 10.1 策略概览

采用三层测试策略，覆盖 Kafka 事件从发布到消费的完整链路：

```
覆盖范围示意：

  ✅ 方案A                 ✅ 方案B                    ✅ 方案C
  Producer单元测试          Consumer模块集成测试          端到端跨模块测试
  ┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
  │ Publisher     │     │ @EmbeddedKafka   │     │ 多上下文启动       │
  │ 序列化正确    │     │ KafkaListener    │     │ API → Kafka → DB  │
  │ Kafka调用正确 │     │ EventHandler     │     │ 完整业务流程       │
  └──────────────┘     └──────────────────┘     └──────────────────┘
  每个Producer模块       每个Consumer模块           独立测试模块
```

### 10.2 方案A：Publisher 模块单元测试

**目的**：验证每个 `XxxDomainEventPublisher` 正确地将事件发送到 Kafka 和本地。

**放置位置**：各 Producer 模块的 `src/test/java/.../infrastructure/messaging/`

**测试内容**：

| 模块 | 测试类 | 测试场景 |
|------|--------|---------|
| catalog | `CatalogDomainEventPublisherTest` | Kafka可用时双发 / Kafka不可用时仅本地发 |
| inventory | `InventoryDomainEventPublisherTest` | 同上 |
| circulation | `CirculationDomainEventPublisherTest` | 同上 |
| patron | `PatronDomainEventPublisherTest` | 同上 |
| payment | `PaymentDomainEventPublisherTest` | 同上 |
| analytics | `AnalyticsDomainEventPublisherTest` | 同上 |

**测试模式**：
```java
// 示例：CirculationDomainEventPublisherTest
@Test
void should_publish_to_kafka_and_locally_when_kafka_available() {
    BookBorrowedEvent event = new BookBorrowedEvent(...);
    publisher.publish(event);
    verify(kafkaTemplate).send("library.circulation.events", event.getEventId(), event);
    verify(localPublisher).publish(event);
}

@Test
void should_only_publish_locally_when_kafka_unavailable() {
    // kafkaTemplate = null（模拟测试环境无Kafka）
    publisher.publish(event);
    verify(localPublisher).publish(event);
    verify(kafkaTemplate, never()).send(any(), any(), any());
}

@Test
void should_not_fail_when_kafka_send_throws_exception() {
    when(kafkaTemplate.send(any(), any(), any()))
        .thenReturn(new CompletableFuture<>());
    // 不应抛异常
    assertDoesNotThrow(() -> publisher.publish(event));
    verify(localPublisher).publish(event);  // 本地仍正常
}
```

**共 6 个测试类，每个 3 个测试方法 = 18 个测试。**

### 10.3 方案B-1：Consumer 模块 @EmbeddedKafka 集成测试

**目的**：验证从 Kafka 接收事件 → @KafkaListener → EventHandler → DomainService 的完整消费者链路，包含序列化/反序列化。

**放置位置**：各 Consumer 模块的 `src/test/java/.../integration/`

**关键依赖**：`spring-kafka-test` 中的 `@EmbeddedKafka`

**测试内容**：

| 模块 | 测试类 | 订阅Topic | 测试场景 |
|------|--------|----------|---------|
| inventory | `CatalogEventConsumerIntegrationTest` | `library.catalog.events` | BookCreatedEvent → 创建库存 |
| inventory | `CirculationEventConsumerIntegrationTest` | `library.circulation.events` | BookBorrowed/Returned → 更新副本状态 |
| patron | `CirculationEventConsumerIntegrationTest` | `library.circulation.events` | BookBorrowed/Returned/Fine → 更新读者状态 |
| patron | `PaymentEventConsumerIntegrationTest` | `library.payment.events` | PaymentCompleted → 减少罚款余额 |
| payment | `CirculationEventConsumerIntegrationTest` | `library.circulation.events` | FineIncurred → 创建支付记录 |
| notification | `CirculationEventConsumerIntegrationTest` | `library.circulation.events` | 借/还/逾期/罚款 → 创建通知 |
| notification | `PaymentEventConsumerIntegrationTest` | `library.payment.events` | 支付完成 → 创建通知 |
| notification | `InventoryEventConsumerIntegrationTest` | `library.inventory.events` | 低库存预警 → 创建通知 |
| notification | `PatronEventConsumerIntegrationTest` | `library.patron.events` | 读者停用 → 创建通知 |
| circulation | `PatronEventConsumerIntegrationTest` | `library.patron.events` | 读者停用 → 阻止借阅 |
| analytics | `CatalogEventConsumerIntegrationTest` | `library.catalog.events` | 新书 → 统计记录 |
| analytics | `InventoryEventConsumerIntegrationTest` | `library.inventory.events` | 低库存 → 统计记录 |

**测试模式**：
```java
// 示例：Patron模块消费Circulation事件
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"library.circulation.events"})
public class CirculationEventConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Autowired
    private PatronRepository patronRepository;

    @Test
    void should_update_patron_loan_count_when_book_borrowed() {
        // 1. 准备测试数据
        Patron patron = registerPatron("test@example.com");

        // 2. 模拟 Circulation 发布事件（发到 EmbeddedKafka）
        BookBorrowedEvent event = new BookBorrowedEvent(
            loanId, copyId, patron.getId(), bookId,
            LocalDateTime.now(), LocalDateTime.now().plusDays(30)
        );
        kafkaTemplate.send("library.circulation.events", event.getEventId(), event);

        // 3. 等待 Consumer 消费并处理（异步，最多等5秒）
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(patron.getId()).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(1);
            assertThat(updated.getTotalBorrowed()).isEqualTo(1);
        });
    }
}
```

**共 12 个集成测试类，约 24 个测试方法。**

**测试环境配置**（每个Consumer模块的 `src/test/resources/application.yml`）：
```yaml
spring:
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
    consumer:
      group-id: library.test.consumer
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.library.*"
```

### 10.4 方案B-2：Handler 级 BDD Feature 测试

BDD 测试放在各消费模块的 `src/test/resources/features/integration/` 目录下，直接调用 Handler 方法（不经过 Kafka）：

| # | Feature文件 | 模块 | 场景数 | 说明 |
|---|------------|------|:------:|------|
| 1 | `borrow-event-handling.feature` | patron | 2 | 借书事件→更新读者借阅计数 |
| 2 | `return-event-handling.feature` | patron | 2 | 还书事件→减少读者借阅计数 |
| 3 | `fine-event-handling.feature` | patron | 2 | 罚款事件→更新罚款余额 |
| 4 | `payment-event-handling.feature` | patron | 2 | 支付事件→减少罚款余额 |
| 5 | `catalog-event-handling.feature` | inventory | 1 | 新书事件→创建库存记录 |
| 6 | `circulation-inventory-event-handling.feature` | inventory | 2 | 借/还书事件→更新副本状态 |
| 7 | `fine-payment-event-handling.feature` | payment | 1 | 罚款事件→创建支付记录 |
| 8 | `patron-event-handling.feature` | circulation | 1 | 读者停用事件→阻止借阅 |
| 9 | `circulation-notification-event-handling.feature` | notification | 4 | 借/还/逾期/罚款通知 |
| 10 | `payment-notification-event-handling.feature` | notification | 1 | 支付完成通知 |
| 11 | `hold-notification-event-handling.feature` | notification | 2 | 预约通知 |
| 12 | `low-stock-notification-event-handling.feature` | notification | 1 | 库存预警通知 |
| 13 | `patron-status-notification-event-handling.feature` | notification | 1 | 读者状态通知 |
| 14 | `catalog-analytics-event-handling.feature` | analytics | 1 | 新书统计 |
| 15 | `inventory-analytics-event-handling.feature` | analytics | 1 | 库存预警统计 |

**共计 15 个 BDD Feature 文件，约 24 个 Scenario，每个对应一个 Step Definitions 类。**

### 10.5 方案C：端到端跨模块集成测试

**目的**：验证完整的跨模块业务流程——从API调用开始，经过Kafka事件传播，最终验证多个模块的数据变化。

**放置位置**：`library-e2e-test/` 和 `library-integration-test/` Maven 模块

**pom.xml 关键依赖**：
```xml
<dependencies>
    <!-- 所有模块 -->
    <dependency><artifactId>library-catalog</artifactId></dependency>
    <dependency><artifactId>library-inventory</artifactId></dependency>
    <dependency><artifactId>library-circulation</artifactId></dependency>
    <dependency><artifactId>library-patron</artifactId></dependency>
    <dependency><artifactId>library-payment</artifactId></dependency>
    <dependency><artifactId>library-notification</artifactId></dependency>
    <dependency><artifactId>library-analytics</artifactId></dependency>
    <!-- Kafka Test -->
    <dependency><artifactId>spring-kafka-test</artifactId></dependency>
    <!-- 测试框架 -->
    <dependency><artifactId>spring-boot-starter-test</artifactId></dependency>
</dependencies>
```

**端到端测试模式**：

```java
// UC-1: 借书流程端到端测试
@SpringBootTest(classes = IntegrationTestConfig.class, webEnvironment = RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {
    "library.catalog.events", "library.circulation.events",
    "library.patron.events", "library.inventory.events",
    "library.payment.events"
})
public class BorrowBookEndToEndTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PatronRepository patronRepo;
    @Autowired private CopyInventoryRepository inventoryRepo;
    @Autowired private NotificationRepository notificationRepo;

    @Test
    void full_borrow_book_flow_across_contexts() {
        // 1. 准备：创建patron、catalog book、inventory copy

        // 2. 触发：通过Circulation API借书
        ResponseEntity<?> response = restTemplate.postForEntity(
            "/api/circulation/loans", borrowRequest, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. 验证Inventory：副本状态变为BORROWED
        await().atMost(10, SECONDS).untilAsserted(() -> {
            BookCopy copy = copyRepo.findById(copyId).orElseThrow();
            assertThat(copy.getStatus()).isEqualTo(CopyStatus.BORROWED);
        });

        // 4. 验证Patron：借阅计数+1
        await().untilAsserted(() -> {
            Patron patron = patronRepo.findById(patronId).orElseThrow();
            assertThat(patron.getCurrentLoans()).isEqualTo(1);
        });

        // 5. 验证Notification：创建了借书通知
        await().untilAsserted(() -> {
            List<Notification> notifications = notificationRepo.findByRecipientId(patronIdStr);
            assertThat(notifications).anyMatch(n ->
                n.getNotificationType() == NotificationType.BOOK_RETURNED
            );
        });
    }
}
```

**端到端测试清单**：

| 测试类 | UC | 触发操作 | 验证模块 | 验证内容 |
|--------|:--:|---------|---------|---------|
| `BorrowBookEndToEndTest` | UC-1 | Circulation借书API | Inventory, Patron, Notification | 副本状态、借阅计数、通知 |
| `ReturnBookEndToEndTest` | UC-2 | Circulation还书API | Inventory, Patron, Notification | 副本状态、借阅计数、通知 |
| `HoldBookEndToEndTest` | UC-3 | Circulation预约API | Notification | 预约通知 |
| `FinePaymentEndToEndTest` | UC-4+5 | 还书(逾期)→支付API | Payment, Patron, Notification | 罚款记录、余额、通知 |
| `NewBookEndToEndTest` | UC-6 | Catalog创建图书API | Inventory, Analytics | 库存记录、统计 |
| `PatronSuspensionEndToEndTest` | UC-7 | Patron停用API | Circulation, Notification | 借阅阻止、通知 |
| `LowStockAlertEndToEndTest` | UC-8 | Inventory减少库存 | Notification, Analytics | 预警通知、统计 |

**共 7 个端到端测试类（9 个测试方法），覆盖全部 8 个 UC。**

### 10.6 三层测试汇总

| 层级 | 方案 | 测试类数 | 测试方法数 | 覆盖范围 |
|------|------|:-------:|:---------:|---------|
| A | Publisher单元测试 | 6 | ~18 | Producer发布逻辑 |
| B | @EmbeddedKafka集成测试 | 12 | ~24 | Consumer完整链路（含Kafka序列化） |
| B | Handler级BDD测试 | 15 | ~24 | 业务场景验证 |
| C | 端到端跨模块测试 | 7 | ~9 | 全链路跨模块业务流程 |
| **合计** | | **40** | **~75** | **完整覆盖** |

---

## 11. 测试实施进度

### 11.1 总体进度：~90% 完成

| 阶段 | 状态 | 完成度 |
|-------|------|:------:|
| Phase 1: 基础设施准备（spring-kafka 依赖 + publisher） | ✅ 完成 | 100% |
| Phase 2: 新书上架链（Catalog → Inventory + Analytics） | ✅ 完成 | 100% |
| Phase 3: 借还书链（Circulation → Inventory + Patron + Notification） | ✅ 完成 | 100% |
| Phase 4: 罚款/支付链（Circulation → Payment → Patron → Notification） | ✅ 完成 | 100% |
| Phase 5: 读者状态链（Patron → Circulation + Notification） | ✅ 完成 | 100% |
| Phase 6: 库存预警链（Inventory → Notification + Analytics） | ✅ 完成 | 100% |
| Phase 7: 端到端集成测试 | ✅ 完成 | 100% |
| Phase 8: 验证 & 文档 | ⚠️ 部分 | ~25% |

### 11.2 生产代码：52/52 文件 ✅ 全部完成

| 模块 | 新建 | 修改 | 状态 |
|------|:----:|:----:|------|
| catalog | 0 | 1 (application.yml) | ✅ |
| inventory | 5 | 2 | ✅ |
| circulation | 3 | 3 | ✅ |
| patron | 7 | 3 | ✅ |
| payment | 3 | 3 | ✅ |
| notification | 12 | 2 | ✅ |
| analytics | 5 | 3 | ✅ |

### 11.3 方案A：Publisher 单元测试 (6/6) ✅ 100%

| 测试类 | 状态 |
|--------|------|
| CatalogDomainEventPublisherTest | ✅ |
| InventoryDomainEventPublisherTest | ✅ |
| CirculationDomainEventPublisherTest | ✅ |
| PatronDomainEventPublisherTest | ✅ |
| PaymentDomainEventPublisherTest | ✅ |
| AnalyticsDomainEventPublisherTest | ✅ |

### 11.4 方案B-1：EmbeddedKafka 集成测试 (12/12) ✅ 100%

| 测试类 | 状态 |
|--------|------|
| inventory/CatalogEventConsumerIntegrationTest | ✅ |
| inventory/CirculationEventConsumerIntegrationTest | ✅ |
| patron/CirculationEventConsumerIntegrationTest | ✅ |
| patron/PaymentEventConsumerIntegrationTest | ✅ |
| payment/CirculationEventConsumerIntegrationTest | ✅ |
| notification/CirculationEventConsumerIntegrationTest | ✅ |
| notification/PaymentEventConsumerIntegrationTest | ✅ |
| notification/InventoryEventConsumerIntegrationTest | ✅ |
| notification/PatronEventConsumerIntegrationTest | ✅ |
| circulation/PatronEventConsumerIntegrationTest | ✅ |
| analytics/CatalogEventConsumerIntegrationTest | ✅ |
| analytics/InventoryEventConsumerIntegrationTest | ✅ |

### 11.5 方案B-2：BDD Feature 文件 (15/15) ✅ 100%

所有 15 个 `.feature` 文件全部存在（patron 4 + inventory 2 + payment 1 + circulation 1 + notification 5 + analytics 2）。

所有 15 个 Steps 类全部存在。

### 11.6 方案C：端到端跨模块测试 ✅ 100%

`library-e2e-test/` + `library-integration-test/` 模块已完整实现，18 个测试全部通过：

**library-e2e-test（JUnit 5）**：

| 测试类 | UC | 测试方法 | 状态 |
|--------|:--:|---------|------|
| BorrowBookEndToEndTest | UC-1 | shouldUpdatePatronLoanCountAndCreateNotification_whenBookBorrowedEvent | ✅ |
| ReturnBookEndToEndTest | UC-2 | shouldDecrementPatronLoanCountAndCreateNotification_whenBookReturnedEvent | ✅ |
| HoldBookEndToEndTest | UC-3 | shouldCreateNotification_whenHoldPlacedEvent | ✅ |
| HoldBookEndToEndTest | UC-3 | shouldCreateNotification_whenHoldFulfilledEvent | ✅ |
| FinePaymentEndToEndTest | UC-4 | shouldUpdatePatronFineAndCreatePayment_whenFineIncurredEvent | ✅ |
| FinePaymentEndToEndTest | UC-5 | shouldReducePatronFine_whenPaymentCompletedEvent | ✅ |
| NewBookEndToEndTest | UC-6 | shouldCreateInventoryRecord_whenBookCreatedEvent | ✅ |
| PatronSuspensionEndToEndTest | UC-7 | shouldCreateNotification_whenPatronSuspendedEvent | ✅ |
| LowStockAlertEndToEndTest | UC-8 | shouldCreateNotification_whenLowStockAlertEvent | ✅ |

**额外修改**（解决 bean 名称冲突）：
- 为 12 个 EventConsumer 类添加了模块级 bean 名称（如 `@Component("patronCirculationEventConsumer")`）
- 为 2 个 FineIncurredEventHandler 类添加了模块级 bean 名称

### 11.7 Phase 8 验证 & 文档

| 项目 | 状态 |
|------|------|
| 全模块构建 `mvn clean install` | ✅ BUILD SUCCESS |
| E2E 测试 `mvn test -pl library-e2e-test` | ✅ 9/9 passed |
| BDD 测试 `mvn test -pl library-integration-test` | ✅ 9/9 passed |

### 11.8 关键设计决策

1. **Per-context topic**：每个上下文独立topic，而非共享topic，便于独立扩展和调试
2. **双发模式**：遵循 `CatalogDomainEventPublisher` 模式，同时发本地Spring事件和Kafka
   - 使用 `ObjectProvider<KafkaTemplate>` 可选注入，Kafka不可用时仅本地发布
   - 使用 `try-catch` 包裹Kafka发送，发送失败不影响本地业务
   - Topic名通过 `application.yml` 配置，测试环境可覆盖
3. **Choreography Saga**：无中心协调器，每个服务独立响应事件，接受最终一致性
4. **Handler防腐层**：外部事件在 `application/handler/` 层转换为本地领域操作，领域层不感知外部上下文
5. **幂等消费**：使用 `DomainEvent.eventId` 作为幂等键，防止重复处理
6. **三层测试**：Publisher单元测试(A) + EmbeddedKafka集成测试(B) + 端到端跨模块测试(C)，完整覆盖从单元到集成的所有层级
7. **独立测试模块**：`library-integration-test` 和 `library-e2e-test` 专门用于端到端验证，不混入业务模块

---

## 附录 A: Cucumber 依赖与配置

### A.1 Maven 依赖（各模块统一）

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
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-suite</artifactId>
    <scope>test</scope>
</dependency>
```

### A.2 Surefire 配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
            <include>**/CucumberTestSuite.java</include>
        </includes>
    </configuration>
</plugin>
```

---

## 附录 B: 测试统计

### B.1 各模块测试统计（截至 2026-05-31）

| 模块 | 单元/集成测试 | BDD Steps | BDD Features | 合计 |
|------|-------------|-----------|-------------|------|
| library-shared | 6 | 0 | 0 | 6 |
| library-catalog | 22 | 5 | 4 | 31 |
| library-inventory | 9 | 4 | 5 | 18 |
| library-circulation | 7 | 1 | 2 | 10 |
| library-patron | 7 | 6 | 8 | 21 |
| library-payment | 6 | 2 | 2 | 10 |
| library-analytics | 7 | 3 | 3 | 13 |
| library-notification | 7 | 7 | 6 | 20 |
| library-e2e-test | 9 | 0 | 0 | 9 |
| library-integration-test | 0 | 9 | 7 | 16 |
| **总计** | **80** | **37** | **37** | **154** |

### B.2 项目整体代码统计

| 指标 | 数量 |
|------|------|
| 模块数 | 10 |
| Main Java 文件 | 318 |
| Test Java 文件 | 115+ |
| Feature 文件 | 37 |
| 领域事件 | 47 |
| 领域异常 | 37 |
| Kafka Consumers | 12 |
| Kafka Publishers | 7 |
| 跨上下文 Use Cases | 8 |
| 事件-消费者绑定 | 23 |

### B.3 测试文件统计

| 类型 | 模块 | 文件数 | 说明 |
|------|------|:-----:|------|
| 方案A：Publisher单元测试 | 各Producer模块 | 6 | PublisherTest 类 |
| 方案B-1：EmbeddedKafka集成测试 | 各Consumer模块 | 12 | ConsumerIntegrationTest 类 |
| 方案B-2：BDD Feature + Steps | 各Consumer模块 | 30 | 15个 .feature + 15个 Steps |
| 方案C：E2E测试 (JUnit 5) | library-e2e-test | 7+ | 7个E2E测试类 + config |
| 方案C：E2E测试 (Cucumber BDD) | library-integration-test | 12+ | 7个feature + steps + config |
| **测试代码总计** | | **~67** | |

**项目总计：52个生产代码文件 + ~67个测试文件 = ~119个文件**

---

**文档版本**: v3.0
**最后更新**: 2026-05-31
