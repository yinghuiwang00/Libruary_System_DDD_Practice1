# 19 - 补充跨上下文测试用例

> 日期：2026-06-03
> 状态：🔄 实施中

---

## 一、背景

当前系统有 47 个 Kafka 事件（7 个 bounded context 发布），但跨上下文 BDD 测试只覆盖了 9 个场景。许多有 handler 处理并产生实际状态变更的事件流完全没有测试覆盖。

**核心发现**：现有 borrow-book / return-book 测试只验证了 Patron 和 Notification 的状态变更，完全没验证 Inventory 的状态变更（BookBorrowedInventoryHandler / BookReturnedInventoryHandler）。

---

## 二、覆盖差距分析

### 已有 handler 但无测试覆盖的事件流

| # | 缺失场景 | 事件 | Consumer Handler | 状态变更 | 优先级 |
|---|---------|------|-----------------|---------|--------|
| 1 | **借书更新库存** | BookBorrowedEvent | `BookBorrowedInventoryHandler.checkoutCopy()` | CopyInventory.status → BORROWED | HIGH |
| 2 | **还书恢复库存** | BookReturnedEvent | `BookReturnedInventoryHandler.returnCopy()` | CopyInventory.status → AVAILABLE | HIGH |
| 3 | **逾期通知** | OverdueNoticeEvent | `OverdueNotificationHandler.handle()` | 创建 OVERDUE_NOTICE 通知 | MEDIUM |

### 缺失的多步骤工作流

| # | 场景 | 事件链 | 优先级 |
|---|------|--------|--------|
| 4 | **完整借阅生命周期** | BookBorrowed → OverdueNotice → FineIncurred → PaymentCompleted | HIGH |
| 5 | **预约到借书** | HoldPlaced → HoldFulfilled → BookBorrowed | HIGH |

### 不需要新增测试的事件

以下事件没有实际消费者或 handler 仅做 logging：
- `LoanRenewedEvent` — Notification consumer 没有 case 处理
- `HoldCancelledEvent` / `HoldExpiredEvent` — Notification consumer 没有 case 处理
- `BookUpdatedEvent` / `BookDeletedEvent` / `BookPublishedEvent` — 无消费者
- Analytics handlers — 仅 logging，无状态变更

---

## 三、实施计划

### Feature 1: borrow-book-inventory.feature — 借书更新库存

**事件流**: `BookBorrowedEvent` → `BookBorrowedInventoryHandler.checkoutCopy(copyId)`

```gherkin
Feature: Borrow Book Updates Inventory
  Scenario: Borrowing a book changes copy status to BORROWED
    Given a book "book-001" exists with inventory in library "MAIN-LIB-001"
    And a copy "copy-001" exists for book "book-001" with status "AVAILABLE"
    And a patron "John Doe" with email "john@test.com" exists
    When a BookBorrowedEvent is published for patron, copy "copy-001", book "book-001"
    Then the copy "copy-001" status should be "BORROWED"
```

**关键实现**: 注入 `CopyInventoryRepository` 或 `InventoryManagementService` 验证 copy 状态

### Feature 2: return-book-inventory.feature — 还书恢复库存

**事件流**: `BookReturnedEvent` → `BookReturnedInventoryHandler.returnCopy(copyId)`

```gherkin
Feature: Return Book Updates Inventory
  Scenario: Returning a book changes copy status back to AVAILABLE
    Given a book "book-001" exists with inventory and a borrowed copy "copy-001"
    When a BookReturnedEvent is published for copy "copy-001", book "book-001"
    Then the copy "copy-001" status should be "AVAILABLE"
```

### Feature 3: overdue-notice.feature — 逾期通知

**事件流**: `OverdueNoticeEvent` → `OverdueNotificationHandler.handle()` → `notificationService.createNotification(OVERDUE_NOTICE, ...)`

```gherkin
Feature: Overdue Notice Notification
  Scenario: Overdue book triggers overdue notification to patron
    Given a patron "John Doe" with email "john@test.com" exists
    When an OverdueNoticeEvent is published for that patron with 5 days overdue
    Then a notification of type "OVERDUE_NOTICE" should exist for the patron
```

**OverdueNoticeEvent JSON 结构**:
```json
{"eventType":"OverdueNoticeEvent","patronId":{"value":"..."},"copyId":{"value":"..."},"loanId":{"value":"..."},"daysOverdue":5}
```

### Feature 4: full-lifecycle.feature — 完整借阅生命周期

**事件链**: BookBorrowedEvent → FineIncurredEvent → PaymentCompletedEvent

```gherkin
Feature: Complete Borrow Lifecycle
  Scenario: Borrow to fine payment full lifecycle
    Given a book "book-001" exists with inventory and available copy "copy-001"
    And a patron "John Doe" with email "john@test.com" exists
    When a BookBorrowedEvent is published for the patron
    Then the patron's current loan count should be 1
    And the copy "copy-001" status should be "BORROWED"
    When a FineIncurredEvent is published for the patron with amount 15.00
    Then the patron's outstanding fines should be 15.00
    And a fine notification should exist
    When a PaymentCompletedEvent is published for the patron with amount 15.00
    Then the patron's outstanding fines should be 0
    And a payment confirmation notification should exist
```

### Feature 5: hold-to-borrow.feature — 预约到借书工作流

**事件链**: HoldPlacedEvent → HoldFulfilledEvent → BookBorrowedEvent

```gherkin
Feature: Hold to Borrow Workflow
  Scenario: Hold placed then fulfilled then borrowed
    Given a book "book-001" exists with inventory and available copy "copy-001"
    And a patron "John Doe" with email "john@test.com" exists
    When a HoldPlacedEvent is published for the patron and book
    Then a hold notification should exist for the patron
    When a HoldFulfilledEvent is published for the patron and copy
    Then a hold fulfilled notification should exist
    When a BookBorrowedEvent is published for the patron and copy
    Then the patron's current loan count should be 1
    And the copy "copy-001" status should be "BORROWED"
```

---

## 四、文件清单

### 新建文件

```
library-integration-test/src/test/
├── resources/features/integration/
│   ├── borrow-book-inventory.feature
│   ├── return-book-inventory.feature
│   ├── overdue-notice.feature
│   ├── full-lifecycle.feature
│   └── hold-to-borrow.feature
└── java/com/library/integration/bdd/
    ├── BorrowBookInventorySteps.java
    ├── ReturnBookInventorySteps.java
    ├── OverdueNoticeSteps.java
    ├── FullLifecycleSteps.java
    └── HoldToBorrowSteps.java

library-staging-test/src/test/
├── resources/features/integration/       # 同 integration-test 的 5 个 feature
└── java/com/library/staging/bdd/          # 同 integration-test 的 5 个 Steps
```

### 不需要修改的文件

- 现有模块代码（handler、consumer 已实现）
- 现有测试文件
- CucumberTestSuite（自动发现 .feature）

### 复用策略

- **SharedSteps.java**: 复用 patron 和 catalog setup 步骤
- **Event publish 模式**: 复用现有 `kafkaTemplate.send()` + JSON 构建
- **Await 模式**: `await().untilAsserted()` 等待异步事件消费

---

## 六、三个测试模块覆盖一致性验证

### 当前覆盖（三个模块完全一致）

| # | 场景 | integration-test | staging-test | e2e-test (JUnit5) |
|---|------|:---:|:---:|:---:|
| 1 | borrow-book (Patron+Notification) | ✅ | ✅ | ✅ |
| 2 | return-book (Patron+Notification) | ✅ | ✅ | ✅ |
| 3 | hold-book placed (Notification) | ✅ | ✅ | ✅ |
| 4 | hold-book fulfilled (Notification) | ✅ | ✅ | ✅ |
| 5 | new-book (Inventory) | ✅ | ✅ | ✅ |
| 6 | fine incurred (Patron+Payment+Notification) | ✅ | ✅ | ✅ |
| 7 | payment completed (Patron+Notification) | ✅ | ✅ | ✅ |
| 8 | low-stock-alert (Notification) | ✅ | ✅ | ✅ |
| 9 | patron-suspension (Notification) | ✅ | ✅ | ✅ |

### 新增后覆盖（三个模块应保持一致）

| # | 新增场景 | integration-test | staging-test | e2e-test (JUnit5) |
|---|---------|:---:|:---:|:---:|
| 10 | borrow-book-inventory (Inventory) | ✅ | 📋 | ✅ |
| 11 | return-book-inventory (Inventory) | 📋 | 📋 | 📋 |
| 12 | overdue-notice (Notification) | ✅ | ✅ | ✅ |
| 13 | full-lifecycle (Patron+Inventory+Payment+Notification) | ✅ | ✅ | ✅ |
| 14 | hold-to-borrow (Notification+Patron+Inventory) | ✅ | ✅ | ✅ |

### e2e-test 新增文件

```
library-e2e-test/src/test/java/com/library/integration/
├── inventory/BorrowBookInventoryEndToEndTest.java
├── inventory/ReturnBookInventoryEndToEndTest.java
├── overdue/OverdueNoticeEndToEndTest.java
├── lifecycle/FullLifecycleEndToEndTest.java
└── hold/HoldToBorrowEndToEndTest.java
```

### 实施顺序

1. **先做 `library-e2e-test`**（JUnit5，最直接，能快速验证跨上下文事件流是否正确）
2. **再做 `library-integration-test`**（Cucumber BDD，从 E2E 测试逻辑转为 BDD feature + steps）
3. **最后做 `library-staging-test`**（复用 integration-test 的 feature + 改写 steps 为真实 infra 版本）

每完成一个模块立即 `mvn test` 验证通过后再做下一个。

---

## 五、验证步骤

```bash
# 1. E2E 测试（H2 + EmbeddedKafka）
mvn test -pl library-e2e-test

# 2. Integration BDD 测试（H2 + EmbeddedKafka）
mvn test -pl library-integration-test

# 3. Staging BDD 测试（PostgreSQL + Kafka，需 Docker）
mvn test -Pstaging -pl library-staging-test

# 4. 全量构建确认无影响
mvn clean install

# 5. CI 验证（build + staging 两个 job）
git push
```
