# Kafka 策略文档 (Kafka Strategy)

> **版本**: v2.0
> **最后更新**: 2026-06-01
> **范围**: Kafka 事件驱动架构 — 生产者、消费者、事件契约、测试与生产就绪建议

---

## 目录

1. [架构概览](#1-架构概览)
2. [Topic 与事件路由](#2-topic-与事件路由)
3. [生产者模式（Publisher）](#3-生产者模式publisher)
4. [消费者模式（Consumer）](#4-消费者模式consumer)
5. [防腐层（Handler）](#5-防腐层handler)
6. [事件清单](#6-事件清单)
7. [测试策略](#7-测试策略)
8. [生产就绪建议](#8-生产就绪建议)

---

## 1. 架构概览

**Per-Context Topic 模式** — 每个限界上下文拥有独立的 Kafka Topic：

```
                    ┌─────────────────────────────────────────────┐
                    │           Kafka Broker                       │
                    │                                              │
                    │  ┌─────────────────────┐                    │
                    │  │ library.catalog.events│◄── Catalog        │
                    │  └──────────┬──────────┘                    │
                    │        ┌────┴────┐                           │
                    │        ▼         ▼                           │
                    │  Inventory   Analytics                      │
                    │                                              │
                    │  ┌──────────────────────────┐               │
                    │  │ library.circulation.events │◄── Circulation│
                    │  └──┬────┬─────┬──────┬────┘               │
                    │     ▼    ▼     ▼      ▼                    │
                    │  Inv  Patron Payment Notification           │
                    │                                              │
                    │  ┌──────────────────┐                       │
                    │  │ library.patron.events│◄── Patron          │
                    │  └──────┬─────┬─────┘                      │
                    │         ▼     ▼                             │
                    │  Circulation Notification                   │
                    │                                              │
                    │  ┌─────────────────────┐                    │
                    │  │ library.inventory.events│◄── Inventory   │
                    │  └──────┬─────┬─────┘                      │
                    │         ▼     ▼                             │
                    │  Notification Analytics                     │
                    │                                              │
                    │  ┌────────────────────┐                     │
                    │  │ library.payment.events│◄── Payment        │
                    │  └──────┬─────┬─────┘                      │
                    │         ▼     ▼                             │
                    │    Patron Notification                      │
                    └─────────────────────────────────────────────┘
```

**设计决策**：

| 决策 | 选择 | 理由 |
|------|------|------|
| 协调模式 | Choreography Saga | 无中心协调器，各服务独立响应事件，接受最终一致性 |
| Topic 策略 | Per-Context Topic | 独立扩展、独立调试、故障隔离 |
| 序列化 | Producer: JSON Object → Consumer: String + 手动解析 | 防腐层设计，领域层不依赖外部事件类 |
| 消息传递 | At-Least-Once | Kafka 默认语义，Consumer 需考虑幂等 |

---

## 2. Topic 与事件路由

### 2.1 Topic 映射表

| Topic | 发布者 | 消费者 | 消费者数量 |
|-------|--------|--------|:---------:|
| `library.catalog.events` | Catalog | Inventory, Analytics | 2 |
| `library.circulation.events` | Circulation | Inventory, Patron, Payment, Notification | 4 |
| `library.patron.events` | Patron | Circulation, Notification | 2 |
| `library.inventory.events` | Inventory | Notification, Analytics | 2 |
| `library.payment.events` | Payment | Patron, Notification | 2 |

> **注意**：Catalog 的 Publisher 代码中默认 topic 名为 `library.domain-events`（通过 `spring.kafka.topic.domain-events` 配置），可在 `application.yml` 中覆盖为 `library.catalog.events`。

### 2.2 跨上下文 Use Case 与事件流

| UC | Use Case | 发布方 | 核心事件 | 消费方 |
|:--:|----------|--------|---------|--------|
| 1 | 借书流程 | Circulation | `BookBorrowedEvent` | Inventory, Patron, Notification |
| 2 | 还书流程 | Circulation | `BookReturnedEvent` | Inventory, Patron, Notification |
| 3 | 预约流程 | Circulation | `HoldPlacedEvent`, `HoldFulfilledEvent` | Notification |
| 4 | 逾期/罚款 | Circulation | `FineIncurredEvent`, `OverdueNoticeEvent` | Payment, Patron, Notification |
| 5 | 支付流程 | Payment | `PaymentCompletedEvent` | Patron, Notification |
| 6 | 新书上架 | Catalog | `BookCreatedEvent` | Inventory, Analytics |
| 7 | 读者停用 | Patron | `PatronSuspendedEvent` | Circulation, Notification |
| 8 | 库存预警 | Inventory | `LowStockAlertEvent` | Notification, Analytics |

**共 23 个事件-消费者绑定关系，覆盖 8 个 Use Case。**

### 2.3 Consumer Group 命名规范

```
library.{消费方上下文}.consumer.{来源方上下文}
```

示例：
- `library.inventory.consumer.catalog` — Inventory 消费 Catalog 事件
- `library.notification.consumer.circulation` — Notification 消费 Circulation 事件

---

## 3. 生产者模式（Publisher）

### 3.1 双发模式（Double Publishing）

所有 Publisher 遵循统一的双发模式：

```
┌─────────────────────────────────────────────────┐
│              XxxDomainEventPublisher              │
│                                                   │
│  ① Spring ApplicationEventPublisher (本地，同步)  │
│     ↓ 始终成功                                    │
│                                                   │
│  ② KafkaTemplate.send() (远程，异步)              │
│     ↓ 可失败，不影响业务                          │
│                                                   │
│  Kafka 不可用 → 仅发本地                          │
│  Kafka 发送失败 → log.error，业务继续             │
└─────────────────────────────────────────────────┘
```

**关键实现**：
- `ObjectProvider<KafkaTemplate>` 可选注入 — Kafka 不在 classpath 时自动降级
- `try-catch` 包裹 Kafka 发送
- `whenComplete()` 异步回调记录成功/失败日志
- Topic 名通过 `application.yml` 配置，测试可覆盖

### 3.2 Publisher 清单

| Publisher | 模块 | Bean 名称 | Topic | 状态 |
|-----------|------|-----------|-------|:----:|
| `CatalogDomainEventPublisher` | catalog | `catalogDomainEventPublisher` | `library.catalog.events` | ✅ |
| `InventoryDomainEventPublisher` | inventory | `inventoryDomainEventPublisher` | `library.inventory.events` | ✅ |
| `CirculationDomainEventPublisher` | circulation | `circulationDomainEventPublisher` | `library.circulation.events` | ✅ |
| `PatronDomainEventPublisher` | patron | `patronDomainEventPublisher` | `library.patron.events` | ✅ |
| `PaymentDomainEventPublisher` | payment | `paymentDomainEventPublisher` | `library.payment.events` | ✅ |
| `AnalyticsDomainEventPublisher` | analytics | `analyticsDomainEventPublisher` | `library.analytics.events` | ✅ |
| — | notification | — | — | 纯消费方，无 Publisher |

> **共 6 个 Publisher**（Notification 为纯消费方，不发布事件）。

---

## 4. 消费者模式（Consumer）

### 4.1 消费处理链路

```
Kafka Topic → @KafkaListener → ObjectMapper.readTree() → switch(eventType) → Handler.handle(JsonNode) → DomainService/Repository → DB
```

**统一模式**：
1. 接收 `ConsumerRecord<String, String>` 原始 JSON 字符串
2. `ObjectMapper.readTree()` 解析为 `JsonNode`
3. 提取 `eventType` 字段，switch 路由到对应 Handler
4. 未知 `eventType` 记录 DEBUG 日志，不抛异常（防止 poison pill）
5. 处理异常记录 ERROR 日志，不重新抛出

### 4.2 Consumer 清单

| Consumer | 模块 | Bean 名称 | 订阅 Topic | 处理的事件类型 |
|----------|------|-----------|-----------|---------------|
| `CatalogEventConsumer` | inventory | `inventoryCatalogEventConsumer` | `library.catalog.events` | `BookCreatedEvent` |
| `CirculationEventConsumer` | inventory | `inventoryCirculationEventConsumer` | `library.circulation.events` | `BookBorrowedEvent`, `BookReturnedEvent` |
| `CirculationEventConsumer` | patron | `patronCirculationEventConsumer` | `library.circulation.events` | `BookBorrowedEvent`, `BookReturnedEvent`, `FineIncurredEvent` |
| `PaymentEventConsumer` | patron | `patronPaymentEventConsumer` | `library.payment.events` | `PaymentCompletedEvent` |
| `CirculationEventConsumer` | payment | `paymentCirculationEventConsumer` | `library.circulation.events` | `FineIncurredEvent` |
| `PatronEventConsumer` | circulation | `circulationPatronEventConsumer` | `library.patron.events` | `PatronSuspendedEvent` |
| `CirculationEventConsumer` | notification | `notificationCirculationEventConsumer` | `library.circulation.events` | `BookBorrowedEvent`, `BookReturnedEvent`, `OverdueNoticeEvent`, `FineIncurredEvent`, `HoldPlacedEvent`, `HoldFulfilledEvent` |
| `PaymentEventConsumer` | notification | `notificationPaymentEventConsumer` | `library.payment.events` | `PaymentCompletedEvent` |
| `InventoryEventConsumer` | notification | `notificationInventoryEventConsumer` | `library.inventory.events` | `LowStockAlertEvent` |
| `PatronEventConsumer` | notification | `notificationPatronEventConsumer` | `library.patron.events` | `PatronSuspendedEvent` |
| `CatalogEventConsumer` | analytics | `analyticsCatalogEventConsumer` | `library.catalog.events` | `BookCreatedEvent` |
| `InventoryEventConsumer` | analytics | `analyticsInventoryEventConsumer` | `library.inventory.events` | `LowStockAlertEvent` |

> **共 12 个 Consumer**，分布在 6 个消费模块中（catalog 为纯发布方，无 Consumer）。

---

## 5. 防腐层（Handler）

Handler 位于 `application/handler/`，将外部事件 JSON 转换为本地领域操作。**领域层完全不感知外部上下文的事件结构。**

### 5.1 Handler 清单

| Handler | 模块 | 调用的领域服务 | 处理的事件 |
|---------|------|-------------|-----------|
| **Inventory** | | | |
| `BookCreatedEventHandler` | inventory | `InventoryManagementService` | `BookCreatedEvent` → 创建库存记录 |
| `BookBorrowedInventoryHandler` | inventory | `InventoryManagementService` | `BookBorrowedEvent` → 标记副本借出 |
| `BookReturnedInventoryHandler` | inventory | `InventoryManagementService` | `BookReturnedEvent` → 标记副本归还 |
| **Patron** | | | |
| `BookBorrowedEventHandler` | patron | `PatronRepository` | `BookBorrowedEvent` → 更新借阅计数 |
| `BookReturnedEventHandler` | patron | `PatronRepository` | `BookReturnedEvent` → 减少借阅计数 |
| `FineIncurredEventHandler` | patron | `PatronRepository` | `FineIncurredEvent` → 增加罚款余额 |
| `PaymentCompletedEventHandler` | patron | `PatronRepository` | `PaymentCompletedEvent` → 减少罚款余额 |
| **Payment** | | | |
| `FineIncurredEventHandler` | payment | `PaymentService` | `FineIncurredEvent` → 创建支付记录 |
| **Circulation** | | | |
| `PatronSuspendedEventHandler` | circulation | Logging only | `PatronSuspendedEvent` → 日志记录 |
| **Notification** | | | |
| `BorrowedNotificationHandler` | notification | `NotificationService` | `BookBorrowedEvent` → 创建借书通知 |
| `ReturnedNotificationHandler` | notification | `NotificationService` | `BookReturnedEvent` → 创建还书通知 |
| `OverdueNotificationHandler` | notification | `NotificationService` | `OverdueNoticeEvent` → 创建逾期通知 |
| `FineNotificationHandler` | notification | `NotificationService` | `FineIncurredEvent` → 创建罚款通知 |
| `HoldNotificationHandler` | notification | `NotificationService` | `HoldPlacedEvent`, `HoldFulfilledEvent` → 创建预约通知 |
| `PaymentNotificationHandler` | notification | `NotificationService` | `PaymentCompletedEvent` → 创建支付通知 |
| `LowStockNotificationHandler` | notification | `NotificationService` | `LowStockAlertEvent` → 创建库存预警通知 |
| `PatronStatusNotificationHandler` | notification | `NotificationService` | `PatronSuspendedEvent` → 创建读者状态通知 |
| **Analytics** | | | |
| `BookCreatedAnalyticsHandler` | analytics | Logging only | `BookCreatedEvent` → 日志记录 |
| `LowStockAnalyticsHandler` | analytics | Logging only | `LowStockAlertEvent` → 日志记录 |

> **共 19 个 Handler**。其中 Analytics（2 个）和 Circulation（1 个）的 Handler 当前仅记录日志，待后续接入统计/业务逻辑。

### 5.2 Handler 注解模式

```java
@Component
@Slf4j
public class XxxEventHandler {
    
    private final DomainService domainService;

    @Transactional
    public void handle(JsonNode event) {
        // 从 JsonNode 提取字段
        String id = event.get("patronId").get("value").asText();
        // 调用领域服务
        domainService.doSomething(id);
    }
}
```

**注意**：ID 类型字段在 JSON 中为 `{"value": "string"}` 格式，需通过 `event.get("xxxId").get("value").asText()` 提取。

---

## 6. 事件清单

### 6.1 各模块领域事件（47 个）

| 模块 | 事件数 | 事件类型 |
|------|:------:|---------|
| Catalog | 4 | `BookCreatedEvent`, `BookUpdatedEvent`, `BookDeletedEvent`, `BookPublishedEvent` |
| Circulation | 14 | `BookBorrowedEvent`, `BookReturnedEvent`, `LoanRenewedEvent`, `LoanRecalledEvent`, `LoanCancelledEvent`, `HoldPlacedEvent`, `HoldFulfilledEvent`, `HoldExpiredEvent`, `HoldExpiredNotPickedUpEvent`, `HoldCancelledEvent`, `HoldPickedUpEvent`, `FineIncurredEvent`, `DueDateReminderEvent`, `OverdueNoticeEvent` |
| Inventory | 8 | `CopyAddedEvent`, `CopyReturnedEvent`, `CopyDamagedEvent`, `CopyLostEvent`, `CopyBorrowedEvent`, `CopiesBatchAddedEvent`, `InventoryCreatedEvent`, `LowStockAlertEvent` |
| Patron | 6 | `PatronRegisteredEvent`, `PatronUpdatedEvent`, `PatronSuspendedEvent`, `PatronReactivatedEvent`, `PatronTerminatedEvent`, `PatronTypeChangedEvent` |
| Payment | 6 | `PaymentCreatedEvent`, `PaymentCompletedEvent`, `PaymentCancelledEvent`, `PaymentFailedEvent`, `RefundRequestedEvent`, `RefundCompletedEvent` |
| Notification | 4 | `NotificationCreatedEvent`, `NotificationDeliveredEvent`, `NotificationFailedEvent`, `NotificationReadEvent` |
| Analytics | 4 | `ReportCreatedEvent`, `ReportCompletedEvent`, `ReportCancelledEvent`, `ReportFailedEvent` |
| **合计** | **46** | |

### 6.2 Kafka 跨上下文流转事件

仅在 Kafka 上实际流转的事件（被 Consumer 消费）：

| 事件 | 发布方 | 消费方数量 |
|------|--------|:---------:|
| `BookCreatedEvent` | Catalog | 2 |
| `BookBorrowedEvent` | Circulation | 3 |
| `BookReturnedEvent` | Circulation | 3 |
| `FineIncurredEvent` | Circulation | 3 |
| `OverdueNoticeEvent` | Circulation | 1 |
| `HoldPlacedEvent` | Circulation | 1 |
| `HoldFulfilledEvent` | Circulation | 1 |
| `PatronSuspendedEvent` | Patron | 2 |
| `PaymentCompletedEvent` | Payment | 2 |
| `LowStockAlertEvent` | Inventory | 2 |

> **共 10 个事件**在 Kafka 上跨上下文流转，其余事件仅发布到本地 Spring 事件总线。

### 6.3 JSON 事件契约

消费者解析事件为 `ConsumerRecord<String, String>` JSON。契约格式：

```json
{
  "eventType": "BookBorrowedEvent",
  "eventId": "uuid",
  "occurredAt": "2026-05-31T10:00:00",
  "version": 1,
  "patronId": {"value": "string"},
  "bookId": "string",
  "amount": 15.00
}
```

**向后兼容规则**：
1. **NEVER rename** 现有 `eventType` — Consumer switch 依赖此值
2. **NEVER remove** 消费者读取的 JSON 字段
3. **NEVER change** JSON 字段类型（string→number 等）
4. **可以安全添加**新字段（Consumer 忽略未知字段）
5. **可以安全添加**新 eventType（Consumer 的 default 分支记录 DEBUG 日志）

---

## 7. 测试策略

> 详细测试用例和代码示例请参阅 [15-Test-Strategy.md](15-Test-Strategy.md)。本节仅概述 Kafka 相关的测试层。

### 7.1 三层测试架构

```
┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  方案 A       │   │  方案 B           │   │  方案 C           │
│  Publisher    │   │  Consumer 集成    │   │  端到端跨模块     │
│  单元测试     │   │  @EmbeddedKafka  │   │  多上下文启动     │
│              │   │                  │   │                  │
│  Mockito     │   │  + BDD Handler   │   │  API → Kafka → DB│
│  6 个测试类  │   │  12 + 15 测试类  │   │  7 个测试类      │
└──────────────┘   └──────────────────┘   └──────────────────┘
```

### 7.2 测试覆盖汇总

| 方案 | 层级 | 测试类数 | 状态 |
|------|------|:-------:|:----:|
| A | Publisher 单元测试 | 6 | ✅ 100% |
| B-1 | @EmbeddedKafka 集成测试 | 12 | ✅ 100% |
| B-2 | Handler BDD Feature 测试 | 15 | ✅ 100% |
| C-1 | E2E JUnit5（library-e2e-test） | 7 | ✅ 100% |
| C-2 | E2E Cucumber BDD（library-integration-test） | 7 | ✅ 100% |
| **合计** | | **47** | **全部通过** |

---

## 8. 生产就绪建议

### 8.1 消息可靠性差距分析

当前实现从"能跑"到"敢上生产"的关键差距：

| 维度 | 当前状态 | 风险 | 优先级 |
|------|---------|------|:------:|
| 幂等消费 | 无 | Kafka at-least-once 语义下重复消费 | 🔴 高 |
| Dead Letter Queue | 无 | 反复失败的消息阻塞消费进度 | 🔴 高 |
| Offset 管理 | 自动提交 | DB 写入成功但 offset 提交失败 → 重复处理 | 🔴 高 |
| 发送确认 | 仅日志记录 | 发送失败事件丢失，无重试/补偿 | 🟡 中 |
| JSON 解析健壮性 | 手动 readTree | eventType 缺失时 NPE 或被静默吞掉 | 🟡 中 |
| 消息追踪 | 未集成 | 7 个服务交互排障困难 | 🟢 低 |
| 版本演进 | version 字段未利用 | 事件结构变更时消费方可能失败 | 🟢 低 |

### 8.2 高优先级建议

#### 1. 消息幂等性保护

**问题**：Consumer 中无幂等性检查。Kafka "at-least-once" delivery 下重复消费必然发生。

**建议方案**（任选其一）：
- **方案 A**：`processed_events` 表（`event_id` + `consumer_group` + `processed_at` 唯一约束）
- **方案 B**：领域层自然幂等（如 `patron.recordLoan()` 检查是否已记录）
- **方案 C**：`ConcurrentHashMap` / Bloom Filter 短期去重（适合低并发场景）

#### 2. Dead Letter Queue（DLQ）

**问题**：错误处理只有 `log.error()`，无 DLQ。反复失败的消息会无限重试阻塞消费进度。

**建议**：
```yaml
spring:
  kafka:
    consumer:
      properties:
        "[max.poll.records]": 10
    listener:
      ack-mode: manual_immediate
```
- 配置 `DefaultErrorHandler` + retry template + DLQ topic
- DLQ topic 命名：`library.{context}.dlq`
- 对 DLQ 消息添加告警（接入 Notification 或 Prometheus）

#### 3. 消费偏移量管理

**问题**：`auto-offset-reset: earliest` + 自动提交。DB 写入成功但 offset 提交失败 → 重复处理。

**建议**：
- 改为 `ack-mode: manual_immediate`
- Handler 成功后显式 `Acknowledgment.acknowledge()`
- 配合 `@Transactional` 保证 DB 写入和 offset 提交的一致性

### 8.3 中优先级建议

#### 4. JSON 解析健壮性

**问题**：`event.get("eventType").asText()` — eventType 缺失时 NPE。

**建议**：
- 添加 eventType 为 null / 缺失时的明确异常处理
- 或使用 Spring Kafka 的 `ErrorHandlingDeserializer` 自动处理反序列化失败
- 或定义 JSON Schema 在 Consumer 入口校验

#### 5. Producer 发送确认

**问题**：`whenComplete()` 只记日志，发送失败事件丢失。

**建议**（任选其一）：
- **方案 A**：`kafkaTemplate.send().get()` 同步发送（简单但增加延迟）
- **方案 B**：Outbox Pattern — 事件先写本地 DB，后台任务扫描发送
- **方案 C**：发送失败写 fallback 存储，定时重发

### 8.4 低优先级建议

#### 6. 分布式追踪

生产环境 7 个服务通过 Kafka 交互，排障需要分布式追踪。项目已有 Jaeger + OTLP 基础设施。

**建议**：
- 添加 `opentelemetry-spring-boot-starter` + `opentelemetry-kafka-instrumentation`
- 在消息 header 中自动传播 trace context

#### 7. 消息版本演进

`DomainEvent` 有 `version` 字段但 Consumer 不检查。

**建议**：
- Consumer 端做向后兼容解析（只读取需要的字段，忽略未知字段）
- 添加版本检查，记录不兼容版本号

#### 8. Catalog Topic 名称统一

Catalog Publisher 代码默认 topic 为 `library.domain-events`（早期命名），其他 6 个模块统一为 `library.{context}.events`。

**建议**：在 `application.yml` 中显式配置为 `library.catalog.events`，保持命名一致性。

---

## 9. 总结

| 维度 | 评价 | 分数 |
|------|------|:----:|
| 架构设计 | Per-Context Topic + Choreography Saga + 防腐层，DDD 意识强 | ⭐⭐⭐⭐⭐ |
| 代码一致性 | 7 个模块完全统一的 Publisher/Consumer/Handler 模式 | ⭐⭐⭐⭐⭐ |
| 测试覆盖 | 三层五类测试（A/B-1/B-2/C-1/C-2），47 个测试类全部通过 | ⭐⭐⭐⭐⭐ |
| 生产可靠性 | 缺少幂等、DLQ、offset 管理、发送确认 | ⭐⭐ |
| 演进能力 | 有 version 字段但未利用，缺 schema 校验 | ⭐⭐ |

**核心结论**：代码结构和测试覆盖优秀，但 **消息可靠性**（幂等、DLQ、offset 管理）是从"能跑"到"敢上生产"的关键差距。建议优先补齐 §8.2 的三项建议。

---

**文档版本**: v2.0
**最后更新**: 2026-06-01
