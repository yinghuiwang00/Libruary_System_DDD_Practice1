# 跨上下文集成计划 (Cross-Context Integration Plan)

## Context

当前系统7个限界上下文已全部独立实现完毕，每个上下文内部功能完整（领域模型、服务、事件、API）。但模块之间完全隔离：
- 47个领域事件已定义，但**没有任何消费者**
- 只有 catalog 有Kafka发布器（`CatalogDomainEventPublisher`），其他5个模块的事件仅发布到本地Spring事件总线
- 只有 catalog 和 inventory 有 `spring-kafka` 依赖和Kafka配置
- **没有跨模块协作**：借书不会更新库存，罚款不会创建支付记录，通知不会自动发送

**本计划目标**：通过Kafka事件驱动架构，实现8个跨上下文Use Case，让所有模块协同工作。

---

## 一、8个跨上下文 Use Case 总览

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

**共23个事件-消费者绑定关系**。

---

## 二、Kafka Topic 策略

**采用 per-context topic**（每个上下文一个独立topic）：

| Topic | 发布者 | 消费者 |
|-------|--------|--------|
| `library.catalog.events` | Catalog | Inventory, Analytics |
| `library.circulation.events` | Circulation | Inventory, Patron, Payment, Notification |
| `library.patron.events` | Patron | Circulation, Notification |
| `library.inventory.events` | Inventory | Notification, Analytics |
| `library.payment.events` | Payment | Patron, Notification |

Consumer group 命名：`library.{consumer-context}.consumer.{source-context}`

---

## 三、代码结构（三层模式）

每个消费模块遵循统一的三层结构：

```
infrastructure/messaging/
  ├── {Context}DomainEventPublisher.java   ← 双发：Spring本地 + Kafka远程
  └── {Source}EventConsumer.java           ← @KafkaListener 接收事件

application/handler/
  └── {EventName}EventHandler.java         ← 防腐层：外部事件 → 本地领域操作
```

**参考模式**：`library-catalog/infrastructure/messaging/CatalogDomainEventPublisher.java`
- 使用 `ObjectProvider<KafkaTemplate>` 可选注入，Kafka不可用时优雅降级
- 双发：先发本地 `DomainEventPublisher`，再发 Kafka `KafkaTemplate.send()`

---

## 四、各模块文件清单

### 4.1 library-catalog (已完备，仅微调)

| 操作 | 文件 | 说明 |
|------|------|------|
| MODIFY | `application.yml` | topic从 `library.domain-events` 改为 `library.catalog.events` |

**不需要创建新文件。**

### 4.2 library-inventory (已有多数基础设施)

| 操作 | 文件 | 说明 |
|------|------|------|
| CREATE | `infrastructure/messaging/InventoryDomainEventPublisher.java` | 双发发布器 |
| CREATE | `infrastructure/messaging/CatalogEventConsumer.java` | 消费catalog events |
| CREATE | `application/handler/BookCreatedEventHandler.java` | 新书→创建库存记录 |
| CREATE | `infrastructure/messaging/CirculationEventConsumer.java` | 消费circulation events |
| CREATE | `application/handler/BookBorrowedInventoryHandler.java` | 借书→checkoutCopy |
| CREATE | `application/handler/BookReturnedInventoryHandler.java` | 还书→returnCopy |
| MODIFY | `application.yml` | 更新topic名 + 增加consumer配置 |
| MODIFY | `InventoryManagementService.java` | 切换到 `InventoryDomainEventPublisher` |

### 4.3 library-circulation (需添加spring-kafka)

| 操作 | 文件 | 说明 |
|------|------|------|
| CREATE | `infrastructure/messaging/CirculationDomainEventPublisher.java` | 双发发布器 |
| CREATE | `infrastructure/messaging/PatronEventConsumer.java` | 消费patron events |
| CREATE | `application/handler/PatronSuspendedEventHandler.java` | 读者停用→阻止借阅 |
| MODIFY | `pom.xml` | 添加 `spring-kafka` + `spring-kafka-test` |
| MODIFY | `application.yml` | 添加完整Kafka producer + consumer配置 |
| MODIFY | `CirculationManagementService.java` | 切换到 `CirculationDomainEventPublisher` |

### 4.4 library-patron (需添加spring-kafka，消费最多)

| 操作 | 文件 | 说明 |
|------|------|------|
| CREATE | `infrastructure/messaging/PatronDomainEventPublisher.java` | 双发发布器 |
| CREATE | `infrastructure/messaging/CirculationEventConsumer.java` | 消费circulation events |
| CREATE | `infrastructure/messaging/PaymentEventConsumer.java` | 消费payment events |
| CREATE | `application/handler/BookBorrowedEventHandler.java` | 借书→`patron.recordLoan()` |
| CREATE | `application/handler/BookReturnedEventHandler.java` | 还书→`patron.recordReturn()` |
| CREATE | `application/handler/FineIncurredEventHandler.java` | 罚款→`patron.addFine()` |
| CREATE | `application/handler/PaymentCompletedEventHandler.java` | 支付完成→`patron.payFine()` |
| MODIFY | `pom.xml` | 添加 `spring-kafka` + `spring-kafka-test` |
| MODIFY | `application.yml` | 添加Kafka配置 (producer + 2个consumer) |
| MODIFY | `PatronManagementService.java` | 切换到 `PatronDomainEventPublisher` |

**关键对接**：Patron模型已有 `recordLoan()`, `recordReturn()`, `addFine()`, `payFine()` 方法。

### 4.5 library-payment (需添加spring-kafka)

| 操作 | 文件 | 说明 |
|------|------|------|
| CREATE | `infrastructure/messaging/PaymentDomainEventPublisher.java` | 双发发布器 |
| CREATE | `infrastructure/messaging/CirculationEventConsumer.java` | 消费circulation events |
| CREATE | `application/handler/FineIncurredEventHandler.java` | 罚款事件→创建支付记录 |
| MODIFY | `pom.xml` | 添加 `spring-kafka` + `spring-kafka-test` |
| MODIFY | `application.yml` | 添加Kafka配置 |
| MODIFY | `PaymentService.java` | 切换到 `PaymentDomainEventPublisher` |

**关键对接**：调用 `paymentService.createPayment(patronId, FINE, amount, PENDING, description)`。

### 4.6 library-notification (纯消费方，需添加spring-kafka)

| 操作 | 文件 | 说明 |
|------|------|------|
| CREATE | `infrastructure/messaging/CirculationEventConsumer.java` | 消费circulation events |
| CREATE | `infrastructure/messaging/PaymentEventConsumer.java` | 消费payment events |
| CREATE | `infrastructure/messaging/InventoryEventConsumer.java` | 消费inventory events |
| CREATE | `infrastructure/messaging/PatronEventConsumer.java` | 消费patron events |
| CREATE | `application/handler/BorrowedNotificationHandler.java` | 借书通知 |
| CREATE | `application/handler/ReturnedNotificationHandler.java` | 还书通知 |
| CREATE | `application/handler/OverdueNotificationHandler.java` | 逾期通知 |
| CREATE | `application/handler/FineNotificationHandler.java` | 罚款通知 |
| CREATE | `application/handler/PaymentNotificationHandler.java` | 支付通知 |
| CREATE | `application/handler/HoldNotificationHandler.java` | 预约通知 |
| CREATE | `application/handler/LowStockNotificationHandler.java` | 库存预警通知 |
| CREATE | `application/handler/PatronStatusNotificationHandler.java` | 读者状态通知 |
| MODIFY | `pom.xml` | 添加 `spring-kafka` + `spring-kafka-test` |
| MODIFY | `application.yml` | 添加4个topic的consumer配置 |

**关键对接**：调用 `notificationService.createNotification(type, channel, recipientId, subject, content)`。
**已有NotificationType枚举**：`DUE_DATE_REMINDER`, `OVERDUE_NOTICE`, `HOLD_AVAILABLE`, `HOLD_CANCELLED`, `FINE_NOTIFICATION`, `PAYMENT_CONFIRMATION`, `BOOK_RETURNED`, `MEMBERSHIP_RENEWAL`, `SYSTEM_ANNOUNCEMENT`。

### 4.7 library-analytics (需添加spring-kafka)

| 操作 | 文件 | 说明 |
|------|------|------|
| CREATE | `infrastructure/messaging/AnalyticsDomainEventPublisher.java` | 双发发布器 |
| CREATE | `infrastructure/messaging/CatalogEventConsumer.java` | 消费catalog events |
| CREATE | `infrastructure/messaging/InventoryEventConsumer.java` | 消费inventory events |
| CREATE | `application/handler/BookCreatedAnalyticsHandler.java` | 新书→统计记录 |
| CREATE | `application/handler/LowStockAnalyticsHandler.java` | 库存预警→统计记录 |
| MODIFY | `pom.xml` | 添加 `spring-kafka` + `spring-kafka-test` |
| MODIFY | `application.yml` | 添加Kafka配置 |
| MODIFY | `AnalyticsService.java` | 切换到 `AnalyticsDomainEventPublisher` |

---

## 五、文件统计

| 模块 | 新建 | 修改 | 合计 |
|------|:----:|:----:|:----:|
| catalog | 0 | 1 | 1 |
| inventory | 5 | 2 | 7 |
| circulation | 3 | 3 | 6 |
| patron | 7 | 3 | 10 |
| payment | 3 | 3 | 6 |
| notification | 12 | 2 | 14 |
| analytics | 5 | 3 | 8 |
| **总计** | **35** | **17** | **52** |

---

## 六、三层测试方案（A + B + C）

采用三层测试策略，覆盖从单元到端到端的完整链路：

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

---

### 方案A：Producer 模块 Publisher 单元测试

**目的**：验证每个新建的 `XxxDomainEventPublisher` 正确地将事件发送到 Kafka 和本地。

**放置位置**：各 Producer 模块的 `src/test/java/.../infrastructure/messaging/`

**测试内容**：

| 模块 | 测试类 | 测试场景 |
|------|--------|---------|
| inventory | `InventoryDomainEventPublisherTest` | Kafka可用时双发 / Kafka不可用时仅本地发 |
| circulation | `CirculationDomainEventPublisherTest` | 同上 |
| patron | `PatronDomainEventPublisherTest` | 同上 |
| payment | `PaymentDomainEventPublisherTest` | 同上 |
| analytics | `AnalyticsDomainEventPublisherTest` | 同上 |
| catalog | `CatalogDomainEventPublisherTest`（已有可补充） | 同上 |

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

---

### 方案B：Consumer 模块 @EmbeddedKafka 集成测试

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

---

### 方案C：端到端跨模块集成测试（独立测试模块）

**目的**：验证完整的跨模块业务流程——从API调用开始，经过Kafka事件传播，最终验证多个模块的数据变化。

**放置位置**：新建 `library-integration-test/` Maven模块

**模块结构**：
```
library-integration-test/
  ├── pom.xml                          ← 依赖所有7个模块 + spring-kafka-test
  └── src/test/java/com/library/integration/
      ├── config/
      │   └── IntegrationTestConfig.java    ← 多上下文配置
      ├── borrow/
      │   ├── BorrowBookEndToEndTest.java   ← UC-1 端到端
      │   └── steps/
      │       └── BorrowBookSteps.java
      ├── return/
      │   └── ReturnBookEndToEndTest.java   ← UC-2 端到端
      ├── hold/
      │   └── HoldBookEndToEndTest.java     ← UC-3 端到端
      ├── fine/
      │   └── FinePaymentEndToEndTest.java  ← UC-4 + UC-5 端到端
      └── catalog/
          └── NewBookEndToEndTest.java      ← UC-6 端到端
```

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
    <dependency><artifactId>cucumber-java</artifactId></dependency>
    <dependency><artifactId>cucumber-spring</artifactId></dependency>
    <dependency><artifactId>cucumber-junit-platform-engine</artifactId></dependency>
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
        //    （直接操作各模块Repository或调用各模块API）

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

**共 7 个端到端测试类，约 10-15 个测试方法。**

---

### 方案A+B：BDD Feature 文件清单（Handler级测试）

这些BDD测试放在各消费模块的 `src/test/resources/features/integration/` 目录下，直接调用Handler方法（不经过Kafka）：

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

---

### 三层测试汇总

| 层级 | 方案 | 测试类数 | 测试方法数 | 覆盖范围 |
|------|------|:-------:|:---------:|---------|
| A | Publisher单元测试 | 6 | ~18 | Producer发布逻辑 |
| B | @EmbeddedKafka集成测试 | 12 | ~24 | Consumer完整链路（含Kafka序列化） |
| B | Handler级BDD测试 | 15 | ~24 | 业务场景验证 |
| C | 端到端跨模块测试 | 7 | ~15 | 全链路跨模块业务流程 |
| **合计** | | **40** | **~81** | **完整覆盖** |

---

## 七、实施阶段（按依赖顺序）

### Phase 1: 基础设施准备
1. 5个模块添加 `spring-kafka` + `spring-kafka-test` 依赖到 `pom.xml`（circulation, patron, payment, notification, analytics）
2. 所有7个模块更新 `application.yml` Kafka配置（per-context topics）
3. 5个模块创建 DomainEventPublisher（复制 `CatalogDomainEventPublisher` 模式）
4. 5个 DomainService 切换到新的 publisher
5. **方案A**：6个 Publisher 单元测试

### Phase 2: 新书上架链（Catalog → Inventory + Analytics）
6. Inventory: `CatalogEventConsumer` + `BookCreatedEventHandler`
7. Analytics: `CatalogEventConsumer` + `BookCreatedAnalyticsHandler`
8. **方案B**：Inventory + Analytics 的 EmbeddedKafka 集成测试
9. **方案B**：BDD feature `catalog-event-handling.feature` + `catalog-analytics-event-handling.feature`

### Phase 3: 借还书链（Circulation → Inventory + Patron + Notification）
10. Inventory: `CirculationEventConsumer` + 借/还书 handler
11. Patron: `CirculationEventConsumer` + 借/还书 handler
12. Notification: `CirculationEventConsumer` + 借/还/逾期/罚款 handler
13. **方案B**：Inventory + Patron + Notification 的 EmbeddedKafka 集成测试
14. **方案B**：BDD features UC-1, UC-2, UC-3, UC-4 通知相关

### Phase 4: 罚款/支付链（Circulation → Payment → Patron → Notification）
15. Payment: `CirculationEventConsumer` + `FineIncurredEventHandler`
16. Patron: `PaymentEventConsumer` + `PaymentCompletedEventHandler`
17. Notification: `PaymentEventConsumer` + `PaymentNotificationHandler`
18. **方案B**：Payment + Patron + Notification 的 EmbeddedKafka 集成测试
19. **方案B**：BDD features UC-4, UC-5 相关

### Phase 5: 读者状态链（Patron → Circulation + Notification）
20. Circulation: `PatronEventConsumer` + `PatronSuspendedEventHandler`
21. Notification: `PatronEventConsumer` + `PatronStatusNotificationHandler`
22. **方案B**：Circulation + Notification 的 EmbeddedKafka 集成测试
23. **方案B**：BDD feature UC-7

### Phase 6: 库存预警链（Inventory → Notification + Analytics）
24. Notification: `InventoryEventConsumer` + `LowStockNotificationHandler`
25. Analytics: `InventoryEventConsumer` + `LowStockAnalyticsHandler`
26. **方案B**：Notification + Analytics 的 EmbeddedKafka 集成测试
27. **方案B**：BDD feature UC-8

### Phase 7: 端到端集成测试
28. 创建 `library-integration-test` Maven模块
29. 配置多上下文测试环境 + `@EmbeddedKafka`
30. **方案C**：7个端到端测试类（覆盖所有8个UC）

### Phase 8: 验证 & 文档
31. 全模块构建 `mvn clean install`
32. 全测试运行 `mvn test`
33. 更新 `DEVELOPMENT_PLAN.md` 勾选完成项
34. 在 `Progress/` 目录保存阶段总结

---

## 八、更新后的文件统计

### 生产代码

| 模块 | 新建 | 修改 | 合计 |
|------|:----:|:----:|:----:|
| catalog | 0 | 1 | 1 |
| inventory | 5 | 2 | 7 |
| circulation | 3 | 3 | 6 |
| patron | 7 | 3 | 10 |
| payment | 3 | 3 | 6 |
| notification | 12 | 2 | 14 |
| analytics | 5 | 3 | 8 |
| **生产代码总计** | **35** | **17** | **52** |

### 测试代码

| 类型 | 模块 | 新建文件 | 说明 |
|------|------|:-------:|------|
| 方案A：Publisher单元测试 | 各Producer模块 | 6 | PublisherTest 类 |
| 方案B：EmbeddedKafka集成测试 | 各Consumer模块 | 12 | ConsumerIntegrationTest 类 |
| 方案B：BDD Feature + Steps | 各Consumer模块 | 30 | 15个 .feature + 15个 Steps |
| 方案C：端到端测试模块 | library-integration-test | ~12 | pom.xml + config + 7个E2E测试类 |
| **测试代码总计** | | **~60** | |

### **项目总计：52个生产代码文件 + ~60个测试文件 = ~112个文件**

---

## 九、关键设计决策

1. **Per-context topic**：每个上下文独立topic，而非共享topic，便于独立扩展和调试
2. **双发模式**：遵循现有 `CatalogDomainEventPublisher` 模式，同时发本地Spring事件和Kafka
   - 使用 `ObjectProvider<KafkaTemplate>` 可选注入，Kafka不可用时仅本地发布
   - 使用 `try-catch` 包裹Kafka发送，发送失败不影响本地业务
   - Topic名通过 `application.yml` 配置，测试环境可覆盖
3. **Choreography Saga**：无中心协调器，每个服务独立响应事件，接受最终一致性
4. **Handler防腐层**：外部事件在 `application/handler/` 层转换为本地领域操作，领域层不感知外部上下文
5. **幂等消费**：使用 `DomainEvent.eventId` 作为幂等键，防止重复处理
6. **三层测试**：Publisher单元测试(A) + EmbeddedKafka集成测试(B) + 端到端跨模块测试(C)，完整覆盖从单元到集成的所有层级
7. **独立测试模块**：`library-integration-test` 专门用于端到端验证，不混入业务模块

---

## 十、完成情况检查（2026-05-31）

### 生产代码完成状态

| Phase | 描述 | 状态 | 备注 |
|-------|------|:----:|------|
| Phase 1 | 基础设施准备 | ✅ 完成 | 7模块pom.xml + application.yml + 5个Publisher + 5个Service切换 |
| Phase 2 | 新书上架链 | ✅ 完成 | Inventory + Analytics 的 Consumer + Handler |
| Phase 3 | 借还书链 | ✅ 完成 | Inventory + Patron + Notification 的 Consumer + Handler |
| Phase 4 | 罚款/支付链 | ✅ 完成 | Payment + Patron + Notification 的 Consumer + Handler |
| Phase 5 | 读者状态链 | ✅ 完成 | Circulation + Notification 的 Consumer + Handler |
| Phase 6 | 库存预警链 | ✅ 完成 | Notification + Analytics 的 Consumer + Handler |

**生产代码完成度：52/52 文件（100%）** ✅

### 测试代码完成状态

| 类别 | 计划 | 已完成 | 未完成 | 完成率 |
|------|:----:|:------:|:------:|:------:|
| 方案A：Publisher单元测试 | 6 | 5 | 1 | 83% |
| 方案B：EmbeddedKafka集成测试 | 12 | 0 | 12 | 0% |
| 方案B：BDD Feature文件 | 15 | 3 | 12 | 20% |
| 方案B：BDD Step Definitions | 15 | 3 | 12 | 20% |
| 方案C：E2E测试模块 | ~12文件 | 0 | ~12 | 0% |
| Phase 8：验证 & 文档 | 4 | 0 | 4 | 0% |
| **测试总计** | **~60** | **~11** | **~49** | **~18%** |

---

### ❌ 未完成清单（共约49个文件）

#### 方案A：Publisher 单元测试（缺1个）

| # | 文件 | 模块 | 说明 |
|---|------|------|------|
| A1 | `CatalogDomainEventPublisherTest.java` | catalog | catalog模块缺少 `infrastructure/messaging/` 测试目录 |

#### 方案B：@EmbeddedKafka 集成测试（缺12个）

每个测试需要在对应模块的 `src/test/java/.../integration/` 下创建，并配置 `@EmbeddedKafka` + `@SpringBootTest`。

| # | 文件 | 模块 | 订阅Topic | 测试场景 |
|---|------|------|----------|---------|
| B1 | `CatalogEventConsumerIntegrationTest.java` | inventory | `library.catalog.events` | BookCreatedEvent → 创建库存 |
| B2 | `CirculationEventConsumerIntegrationTest.java` | inventory | `library.circulation.events` | BookBorrowed/Returned → 更新副本状态 |
| B3 | `CirculationEventConsumerIntegrationTest.java` | patron | `library.circulation.events` | BookBorrowed/Returned/Fine → 更新读者状态 |
| B4 | `PaymentEventConsumerIntegrationTest.java` | patron | `library.payment.events` | PaymentCompleted → 减少罚款余额 |
| B5 | `CirculationEventConsumerIntegrationTest.java` | payment | `library.circulation.events` | FineIncurred → 创建支付记录 |
| B6 | `CirculationEventConsumerIntegrationTest.java` | notification | `library.circulation.events` | 借/还/逾期/罚款 → 创建通知 |
| B7 | `PaymentEventConsumerIntegrationTest.java` | notification | `library.payment.events` | 支付完成 → 创建通知 |
| B8 | `InventoryEventConsumerIntegrationTest.java` | notification | `library.inventory.events` | 低库存预警 → 创建通知 |
| B9 | `PatronEventConsumerIntegrationTest.java` | notification | `library.patron.events` | 读者停用 → 创建通知 |
| B10 | `PatronEventConsumerIntegrationTest.java` | circulation | `library.patron.events` | 读者停用 → 阻止借阅 |
| B11 | `CatalogEventConsumerIntegrationTest.java` | analytics | `library.catalog.events` | 新书 → 统计记录 |
| B12 | `InventoryEventConsumerIntegrationTest.java` | analytics | `library.inventory.events` | 低库存 → 统计记录 |

**前置条件**：每个Consumer模块的 `src/test/resources/application.yml` 需添加嵌入式Kafka配置：
```yaml
spring:
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
    consumer:
      auto-offset-reset: earliest
```

#### 方案B：BDD Feature 文件（缺12个）

**已有的3个（✅）**：
- ✅ `library-patron/src/test/resources/features/patron/integration-borrow-event-handling.feature`
- ✅ `library-patron/src/test/resources/features/patron/integration-fine-event-handling.feature`
- ✅ `library-patron/src/test/resources/features/patron/integration-payment-event-handling.feature`

**缺失的12个（❌）**：

| # | Feature文件 | 模块 | 场景数 | 说明 |
|---|------------|------|:------:|------|
| F1 | `return-event-handling.feature` | patron | 2 | 还书事件→减少读者借阅计数 |
| F2 | `catalog-event-handling.feature` | inventory | 1 | 新书事件→创建库存记录 |
| F3 | `circulation-inventory-event-handling.feature` | inventory | 2 | 借/还书事件→更新副本状态 |
| F4 | `fine-payment-event-handling.feature` | payment | 1 | 罚款事件→创建支付记录 |
| F5 | `patron-event-handling.feature` | circulation | 1 | 读者停用事件→阻止借阅 |
| F6 | `circulation-notification-event-handling.feature` | notification | 4 | 借/还/逾期/罚款通知 |
| F7 | `payment-notification-event-handling.feature` | notification | 1 | 支付完成通知 |
| F8 | `hold-notification-event-handling.feature` | notification | 2 | 预约通知 |
| F9 | `low-stock-notification-event-handling.feature` | notification | 1 | 库存预警通知 |
| F10 | `patron-status-notification-event-handling.feature` | notification | 1 | 读者状态通知 |
| F11 | `catalog-analytics-event-handling.feature` | analytics | 1 | 新书统计 |
| F12 | `inventory-analytics-event-handling.feature` | analytics | 1 | 库存预警统计 |

#### 方案B：BDD Step Definitions（缺12个）

对应上面12个缺失的Feature文件，每个需要一个Step Definitions类。

| # | Steps文件 | 模块 | 对应Feature |
|---|----------|------|------------|
| S1 | `IntegrationReturnEventSteps.java` | patron | F1 |
| S2 | `CatalogEventSteps.java` | inventory | F2 |
| S3 | `CirculationInventoryEventSteps.java` | inventory | F3 |
| S4 | `FinePaymentEventSteps.java` | payment | F4 |
| S5 | `PatronEventSteps.java` | circulation | F5 |
| S6 | `CirculationNotificationEventSteps.java` | notification | F6 |
| S7 | `PaymentNotificationEventSteps.java` | notification | F7 |
| S8 | `HoldNotificationEventSteps.java` | notification | F8 |
| S9 | `LowStockNotificationEventSteps.java` | notification | F9 |
| S10 | `PatronStatusNotificationEventSteps.java` | notification | F10 |
| S11 | `CatalogAnalyticsEventSteps.java` | analytics | F11 |
| S12 | `InventoryAnalyticsEventSteps.java` | analytics | F12 |

#### 方案C：端到端测试模块（缺整个模块）

需要创建 `library-integration-test/` Maven子模块，包含以下文件：

| # | 文件 | 说明 |
|---|------|------|
| C1 | `library-integration-test/pom.xml` | 依赖全部7模块 + spring-kafka-test + cucumber |
| C2 | `src/test/java/.../config/IntegrationTestConfig.java` | 多上下文启动配置 |
| C3 | `src/test/java/.../borrow/BorrowBookEndToEndTest.java` | UC-1 借书端到端 |
| C4 | `src/test/java/.../return/ReturnBookEndToEndTest.java` | UC-2 还书端到端 |
| C5 | `src/test/java/.../hold/HoldBookEndToEndTest.java` | UC-3 预约端到端 |
| C6 | `src/test/java/.../fine/FinePaymentEndToEndTest.java` | UC-4+5 罚款/支付端到端 |
| C7 | `src/test/java/.../catalog/NewBookEndToEndTest.java` | UC-6 新书上架端到端 |
| C8 | `src/test/java/.../patron/PatronSuspensionEndToEndTest.java` | UC-7 读者停用端到端 |
| C9 | `src/test/java/.../inventory/LowStockAlertEndToEndTest.java` | UC-8 库存预警端到端 |

#### Phase 8：验证 & 文档（缺4项）

| # | 任务 | 说明 |
|---|------|------|
| V1 | 全模块构建 `mvn clean install` | 确保所有模块编译通过 |
| V2 | 全测试运行 `mvn test` | 确保所有测试通过 |
| V3 | 更新 `DEVELOPMENT_PLAN.md` | 勾选跨上下文集成完成项 |
| V4 | 在 `Progress/` 保存阶段总结 | 记录集成阶段成果 |

---

### 实施优先级建议

1. **P0（必须）**：方案B BDD Feature + Steps（12+12=24个文件）— 验证所有Handler业务逻辑 ✅ 已完成
2. **P1（重要）**：方案B EmbeddedKafka集成测试（12个文件）— 验证Kafka消费链路 ✅ 已完成
3. **P1（重要）**：方案A 补充 CatalogDomainEventPublisherTest（1个文件） ✅ 已完成
4. **P2（可选）**：方案C 端到端测试模块（~9个文件）— 完整跨模块验证 ❌ 未完成
5. **P3（收尾）**：Phase 8 验证 & 文档（4项） ❌ 未完成

---

## 十一、完成情况复查（2026-05-31 代码验证）

> 以下为基于代码库实际文件扫描的验证结果，替代第十节的预估值。

### 生产代码完成状态（Phase 1-6）

| Phase | 描述 | 状态 | 验证结果 |
|-------|------|:----:|---------|
| Phase 1 | 基础设施准备 | ✅ 完成 | 5模块pom.xml含spring-kafka；7模块application.yml含per-context topic；5个DomainEventPublisher已创建；5个Service已切换 |
| Phase 2 | 新书上架链 | ✅ 完成 | Inventory + Analytics 的 CatalogEventConsumer + BookCreatedEventHandler + BookCreatedAnalyticsHandler 均存在 |
| Phase 3 | 借还书链 | ✅ 完成 | Inventory + Patron + Notification 的 Consumer + Handler 共 11 个文件全部存在 |
| Phase 4 | 罚款/支付链 | ✅ 完成 | Payment + Patron + Notification 的 Consumer + Handler 共 7 个文件全部存在 |
| Phase 5 | 读者状态链 | ✅ 完成 | Circulation + Notification 的 PatronEventConsumer + Handler 共 4 个文件全部存在 |
| Phase 6 | 库存预警链 | ✅ 完成 | Notification + Analytics 的 InventoryEventConsumer + Handler 共 4 个文件全部存在 |

**生产代码完成度：52/52 文件（100%）** ✅

### 测试代码完成状态（代码验证）

| 类别 | 计划 | 实际完成 | 状态 |
|------|:----:|:-------:|:----:|
| 方案A：Publisher单元测试 | 6 | 7 | ✅ 100% |
| 方案B：EmbeddedKafka集成测试 | 12 | 12 | ✅ 100% |
| 方案B：BDD Feature文件 | 15 | 15 | ✅ 100% |
| 方案B：BDD Step Definitions | 15 | 16（含1个SharedSteps） | ✅ 100% |
| 方案C：E2E测试模块 | ~12文件 | 0（仅空目录结构） | ❌ 0% |
| Phase 8：验证 & 文档 | 4项 | 1项 | ❌ 25% |

#### 方案A：Publisher单元测试验证明细

| # | 文件 | 模块 | 状态 |
|---|------|------|:----:|
| A1 | `CatalogDomainEventPublisherTest.java` | catalog | ✅ 存在 |
| A2 | `PublisherTest.java` | catalog | ✅ 存在（额外） |
| A3 | `InventoryDomainEventPublisherTest.java` | inventory | ✅ 存在 |
| A4 | `CirculationDomainEventPublisherTest.java` | circulation | ✅ 存在 |
| A5 | `PatronDomainEventPublisherTest.java` | patron | ✅ 存在 |
| A6 | `PaymentDomainEventPublisherTest.java` | payment | ✅ 存在 |
| A7 | `AnalyticsDomainEventPublisherTest.java` | analytics | ✅ 存在 |

#### 方案B：EmbeddedKafka集成测试验证明细

| # | 文件 | 模块 | 订阅Topic | 状态 |
|---|------|------|----------|:----:|
| B1 | `CatalogEventConsumerIntegrationTest.java` | inventory | `library.catalog.events` | ✅ 存在 |
| B2 | `CirculationEventConsumerIntegrationTest.java` | inventory | `library.circulation.events` | ✅ 存在 |
| B3 | `CirculationEventConsumerIntegrationTest.java` | patron | `library.circulation.events` | ✅ 存在 |
| B4 | `PaymentEventConsumerIntegrationTest.java` | patron | `library.payment.events` | ✅ 存在 |
| B5 | `CirculationEventConsumerIntegrationTest.java` | payment | `library.circulation.events` | ✅ 存在 |
| B6 | `CirculationEventConsumerIntegrationTest.java` | notification | `library.circulation.events` | ✅ 存在 |
| B7 | `PaymentEventConsumerIntegrationTest.java` | notification | `library.payment.events` | ✅ 存在 |
| B8 | `InventoryEventConsumerIntegrationTest.java` | notification | `library.inventory.events` | ✅ 存在 |
| B9 | `PatronEventConsumerIntegrationTest.java` | notification | `library.patron.events` | ✅ 存在 |
| B10 | `PatronEventConsumerIntegrationTest.java` | circulation | `library.patron.events` | ✅ 存在 |
| B11 | `CatalogEventConsumerIntegrationTest.java` | analytics | `library.catalog.events` | ✅ 存在 |
| B12 | `InventoryEventConsumerIntegrationTest.java` | analytics | `library.inventory.events` | ✅ 存在 |

#### 方案B：BDD Feature文件验证明细

| # | Feature文件 | 模块 | 状态 |
|---|------------|------|:----:|
| F1 | `integration-borrow-event-handling.feature` | patron | ✅ 存在 |
| F2 | `integration-return-event-handling.feature` | patron | ✅ 存在 |
| F3 | `integration-fine-event-handling.feature` | patron | ✅ 存在 |
| F4 | `integration-payment-event-handling.feature` | patron | ✅ 存在 |
| F5 | `catalog-event-handling.feature` | inventory | ✅ 存在 |
| F6 | `circulation-inventory-event-handling.feature` | inventory | ✅ 存在 |
| F7 | `fine-payment-event-handling.feature` | payment | ✅ 存在 |
| F8 | `patron-event-handling.feature` | circulation | ✅ 存在 |
| F9 | `circulation-notification-event-handling.feature` | notification | ✅ 存在 |
| F10 | `payment-notification-event-handling.feature` | notification | ✅ 存在 |
| F11 | `hold-notification-event-handling.feature` | notification | ✅ 存在 |
| F12 | `low-stock-notification-event-handling.feature` | notification | ✅ 存在 |
| F13 | `patron-status-notification-event-handling.feature` | notification | ✅ 存在 |
| F14 | `catalog-analytics-event-handling.feature` | analytics | ✅ 存在 |
| F15 | `inventory-analytics-event-handling.feature` | analytics | ✅ 存在 |

#### 方案B：BDD Step Definitions验证明细

| # | Steps文件 | 模块 | 状态 |
|---|----------|------|:----:|
| S1 | `IntegrationBorrowEventSteps.java` | patron | ✅ 存在 |
| S2 | `IntegrationReturnEventSteps.java` | patron | ✅ 存在 |
| S3 | `IntegrationFineEventSteps.java` | patron | ✅ 存在 |
| S4 | `IntegrationPaymentEventSteps.java` | patron | ✅ 存在 |
| S5 | `CatalogEventSteps.java` | inventory | ✅ 存在 |
| S6 | `CirculationInventoryEventSteps.java` | inventory | ✅ 存在 |
| S7 | `FinePaymentEventSteps.java` | payment | ✅ 存在 |
| S8 | `PatronEventSteps.java` | circulation | ✅ 存在 |
| S9 | `CirculationNotificationEventSteps.java` | notification | ✅ 存在 |
| S10 | `PaymentNotificationEventSteps.java` | notification | ✅ 存在 |
| S11 | `HoldNotificationEventSteps.java` | notification | ✅ 存在 |
| S12 | `LowStockNotificationEventSteps.java` | notification | ✅ 存在 |
| S13 | `PatronStatusNotificationEventSteps.java` | notification | ✅ 存在 |
| S14 | `SharedNotificationEventSteps.java` | notification | ✅ 存在（额外共享Steps） |
| S15 | `CatalogAnalyticsEventSteps.java` | analytics | ✅ 存在 |
| S16 | `InventoryAnalyticsEventSteps.java` | analytics | ✅ 存在 |

---

### ❌ 未完成清单（代码验证后更新）

#### 方案C：端到端测试模块（缺整个模块实现）

`library-integration-test/` 目录结构已创建（空目录），但缺少所有实际文件。

**当前状态**：
```
library-integration-test/
  └── src/test/java/com/library/integration/
      ├── borrow/      ← 空目录
      ├── catalog/     ← 空目录
      ├── config/      ← 空目录
      ├── fine/        ← 空目录
      ├── hold/        ← 空目录
      ├── inventory/   ← 空目录
      ├── patron/      ← 空目录
      └── returnn/     ← 空目录
  └── src/test/resources/   ← 空目录
```

| # | 文件 | 说明 | 状态 |
|---|------|------|:----:|
| C1 | `library-integration-test/pom.xml` | 依赖全部7模块 + spring-kafka-test + cucumber | ❌ 缺失 |
| C2 | `src/test/resources/application.yml` | 测试环境Kafka + DB配置 | ❌ 缺失 |
| C3 | `src/test/java/.../config/IntegrationTestConfig.java` | 多上下文启动配置 | ❌ 缺失 |
| C4 | `src/test/java/.../borrow/BorrowBookEndToEndTest.java` | UC-1 借书端到端 | ❌ 缺失 |
| C5 | `src/test/java/.../return/ReturnBookEndToEndTest.java` | UC-2 还书端到端 | ❌ 缺失 |
| C6 | `src/test/java/.../hold/HoldBookEndToEndTest.java` | UC-3 预约端到端 | ❌ 缺失 |
| C7 | `src/test/java/.../fine/FinePaymentEndToEndTest.java` | UC-4+5 罚款/支付端到端 | ❌ 缺失 |
| C8 | `src/test/java/.../catalog/NewBookEndToEndTest.java` | UC-6 新书上架端到端 | ❌ 缺失 |
| C9 | `src/test/java/.../patron/PatronSuspensionEndToEndTest.java` | UC-7 读者停用端到端 | ❌ 缺失 |
| C10 | `src/test/java/.../inventory/LowStockAlertEndToEndTest.java` | UC-8 库存预警端到端 | ❌ 缺失 |

**额外问题**：`returnn/` 目录名疑似拼写错误，应为 `return/`。

#### Phase 8：验证 & 文档（缺3项）

| # | 任务 | 状态 | 备注 |
|---|------|:----:|------|
| V1 | 全模块构建 `mvn clean install` | ❓ 未验证 | 需运行验证 |
| V2 | 全测试运行 `mvn test` | ❓ 未验证 | 需运行验证 |
| V3 | 更新 `DEVELOPMENT_PLAN.md` | ❌ 未完成 | 阶段九验收标准全部未勾选 |
| V4 | 在 `Progress/` 保存阶段总结 | ❌ 未完成 | `Progress/` 目录缺少跨上下文集成阶段总结 |

---

### 总体完成度（代码验证后）

| 层级 | 类别 | 计划文件数 | 已完成 | 完成率 |
|------|------|:---------:|:------:|:------:|
| 生产代码 | Phase 1-6 消费者/处理器 | 52 | 52 | **100%** |
| 测试-A | Publisher 单元测试 | 6 | 7 | **117%** |
| 测试-B1 | EmbeddedKafka 集成测试 | 12 | 12 | **100%** |
| 测试-B2 | BDD Feature 文件 | 15 | 15 | **100%** |
| 测试-B3 | BDD Step Definitions | 15 | 16 | **107%** |
| 测试-C | E2E 测试模块 | ~10 | 0 | **0%** |
| 收尾 | Phase 8 验证 & 文档 | 4 | 0-1 | **0-25%** |
| **总计** | | **~114** | **~102-103** | **~90%** |

### 下一步行动建议

1. **P0（推荐）**：完成 Phase 8 验证
   - 运行 `mvn clean install` 确保全模块编译通过
   - 运行 `mvn test` 确保所有测试通过
   - 更新 `DEVELOPMENT_PLAN.md` 勾选阶段九验收标准
   - 在 `Progress/` 创建跨上下文集成阶段总结

2. **P1（可选）**：完成方案C端到端测试模块
   - 创建 `library-integration-test/pom.xml`
   - 创建 `IntegrationTestConfig.java`
   - 创建 7 个端到端测试类（覆盖 8 个 UC）
   - 修复 `returnn/` → `return/` 目录名

3. **P2（未来）**：DEVELOPMENT_PLAN.md 中的额外任务
   - 10.1.2 Saga协调器（当前采用Choreography模式，无中心协调器）
   - 10.2 API网关（Spring Cloud Gateway）
   - 10.3 监控和运维（Prometheus + Grafana + Jaeger）
