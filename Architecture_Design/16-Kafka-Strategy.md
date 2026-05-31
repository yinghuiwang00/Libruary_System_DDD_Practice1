# Kafka Strategy 分析报告

> 生成日期：2026-05-31
> 分析范围：全项目 7 个限界上下文 + library-shared + library-integration-test

---

## 一、整体架构

**Per-Context Topic 模式** — 每个限界上下文拥有独立的 Kafka Topic：

| Topic | 发布者 | 消费者 |
|-------|--------|--------|
| `library.catalog.events` | Catalog | Inventory, Analytics |
| `library.circulation.events` | Circulation | Inventory, Patron, Payment, Notification |
| `library.patron.events` | Patron | Circulation, Notification |
| `library.inventory.events` | Inventory | Notification, Analytics |
| `library.payment.events` | Payment | Patron, Notification |

**共 23 个事件-消费者绑定关系**，覆盖 8 个跨上下文 Use Case。采用 **Choreography Saga**（无中心协调器），每个服务独立响应事件，接受最终一致性。

---

## 二、生产代码模式

### 1. 双发模式（Double Publishing）

所有 7 个 Publisher 遵循统一模式（`CatalogDomainEventPublisher` 为参考实现）：

```
本地 Spring ApplicationEventPublisher（同步，必成功）
     +
Kafka KafkaTemplate.send()（异步，可失败）
```

- 使用 `ObjectProvider<KafkaTemplate>` 可选注入
- Kafka 不可用时优雅降级，只发本地事件
- `try-catch` 包裹 Kafka 发送，失败不影响业务
- Topic 名通过 `application.yml` 配置

### 2. 消费者模式（Consumer）

所有 Consumer 统一使用 `@KafkaListener`：

```java
@KafkaListener(topics = "library.xxx.events", groupId = "library.{context}.consumer.{source}")
public void onEvent(ConsumerRecord<String, String> record) {
    JsonNode event = objectMapper.readTree(record.value());
    String eventType = event.get("eventType").asText();
    switch (eventType) {
        case "BookBorrowedEvent" -> handler.handle(event);
        ...
    }
}
```

- 接收原始 JSON 字符串，手动解析 `eventType` 路由到不同 Handler
- Consumer Group 命名规范：`library.{消费方}.consumer.{来源方}`

### 3. 防腐层（Anti-Corruption Layer）

Handler 在 `application/handler/` 层，将外部事件转换为本地领域操作：

```
Consumer → Handler (JsonNode) → DomainService → Repository
```

领域层完全不感知外部上下文的事件结构。

---

## 三、测试策略（三层）

### 方案 A：Publisher 单元测试 — 7/7 ✅

- **位置**：各模块 `infrastructure/messaging/`
- **工具**：Mockito（mock `KafkaTemplate` + `DomainEventPublisher`）
- **3 个场景**：Kafka 可用时双发 / Kafka 不可用时仅本地 / Kafka 发送异常时优雅处理
- **评价**：覆盖充分，模式统一

### 方案 B-1：@EmbeddedKafka 集成测试 — 12/12 ✅

- **位置**：各模块 `integration/`
- **工具**：`@EmbeddedKafka` + `@SpringBootTest` + Awaitility + `@DirtiesContext`
- **测试链路**：发 JSON 到 Topic → Consumer 接收 → Handler 处理 → 验证 DB 状态
- **特点**：
  - 手动构建 `KafkaTemplate<String, String>` 发送原始 JSON
  - `ContainerTestUtils.waitForAssignment()` 等待 listener 就绪
  - `await().atMost(5, SECONDS)` 异步断言
  - 测试正向 + 未知事件忽略

### 方案 B-2：BDD Feature + Steps — 15 Features + 16 Steps ✅

- **位置**：各模块 `features/integration/`
- **工具**：Cucumber + Spring
- **测试链路**：直接调用 Handler.handle(jsonNode)，不经过 Kafka
- **评价**：Handler 级别的业务场景验证，与 EmbeddedKafka 测试互补

### 方案 C-1：E2E JUnit5 测试 — 9/9 ✅

- **位置**：`library-e2e-test/` 独立 Maven 模块
- **工具**：`@SpringBootTest` + `@EmbeddedKafka` + `KafkaTemplate<String, String>` + Awaitility
- **测试链路**：手动构建 JSON → KafkaTemplate.send() → @KafkaListener 消费 → Handler 处理 → 验证 DB 状态
- **基础设施**：`BaseEndToEndTest` 提供 Kafka 生产者创建 + DB 全表清理 + JSON 构建辅助方法
- **覆盖场景**：UC-1~UC-8 共 9 个测试方法全部通过

### 方案 C-2：E2E Cucumber BDD 测试 — 9 scenarios ✅

- **位置**：`library-integration-test/` 独立 Maven 模块
- **工具**：Cucumber 7.15 + `@EmbeddedKafka` + `@CucumberContextConfiguration`
- **测试链路**：与 C-1 相同，但使用 Gherkin Given/When/Then 步骤定义
- **基础设施**：`CucumberSpringConfig`（Spring+Kafka 胶水）+ `E2EScenarioState`（跨步骤状态）+ `SharedSteps`（公共步骤）
- **覆盖场景**：7 个 Feature 文件，9 个 Scenario，8 个 Step Definition 类全部通过

---

## 四、建议

### 🔴 高优先级

#### 1. 缺少消息幂等性保护

Consumer 中没有任何幂等性检查。计划文档中提到"使用 `DomainEvent.eventId` 作为幂等键"，但代码中 **并未实现**。Kafka "at-least-once" delivery 语义下，重复消费是必然场景。

**建议**：
- 在每个 Handler 中添加已处理事件记录（简单的 `processed_events` 表：`event_id` + `consumer_group` + `processed_at`）
- 或使用数据库唯一约束 + 乐观锁实现自然幂等（如 `patron.recordLoan()` 检查是否已记录）
- 或在 Handler 入口用 `ConcurrentHashMap` / Bloom Filter 做短期去重

#### 2. 缺少 Dead Letter Queue（DLQ）配置

Consumer 的错误处理只有 `log.error()`，没有配置 DLQ。如果某条消息反复处理失败，会无限重试阻塞消费进度。

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

#### 3. 缺少消息消费偏移量管理策略

当前使用 `auto-offset-reset: earliest` + 自动提交。如果 Handler 处理过程中数据库写入成功但 Kafka offset 提交失败，会导致重复处理。反之如果先提交 offset 再处理，失败则丢消息。

**建议**：
- 改为 `ack-mode: manual_immediate`（或 `manual`）
- Handler 成功处理后显式 `Acknowledgment.acknowledge()`
- 配合 `@Transactional` 保证 DB 写入和 offset 提交的一致性

---

### 🟡 中优先级

#### 4. Consumer 的 JSON 解析过于脆弱

Consumer 手动解析 `objectMapper.readTree(record.value())` + `event.get("eventType").asText()` — 如果消息格式不匹配（如缺少 `eventType` 字段），会抛 NPE 或被 catch-all 吃掉，没有任何恢复机制。

**建议**：
- 定义每个事件的 JSON Schema 并在 Consumer 入口做校验
- 或使用 Spring Kafka 的 `JsonDeserializer` + 类型映射，避免手动解析
- 至少添加 `eventType` 为 null / 缺失时的明确异常处理

#### 5. Producer 发送无确认机制

`kafkaTemplate.send()` 使用 `whenComplete()` 异步回调，但回调只记 log。如果发送失败，事件丢失，没有重试或补偿机制。

**建议**：
- 方案 A：使用 `kafkaTemplate.send().get()` 同步发送（简单但增加延迟）
- 方案 B：实现 Outbox Pattern — 先将事件写入本地 DB，由后台任务扫描发送
- 方案 C：发送失败时写入 fallback 存储（如 Redis 或另一张表），定时重发

#### 6. 所有模块测试中 exclude KafkaAutoConfiguration 但又用 @EmbeddedKafka

部分模块的 `test/application.yml` 排除了 `KafkaAutoConfiguration`，但又用 `@EmbeddedKafka` 启动内嵌 broker。这意味着有些集成测试类需要 `@TestPropertyOverride` 或手动覆盖 bootstrap-servers。这种矛盾配置容易导致测试不稳定。

**建议**：
- 统一测试策略：要么全部排除 Kafka 并在需要时手动启用，要么统一使用 `@EmbeddedKafka` 自动配置
- 将 `spring.kafka.bootstrap-servers: ${spring.embedded.kafka.brokers}` 放在统一的测试配置 profile 中

---

### 🟢 低优先级（改进建议）

#### 7. 考虑引入 Spring Kafka 的 ErrorHandlingDeserializer

当前直接 `readTree()` + `switch` 的模式可以替换为 Spring Kafka 的 `ErrorHandlingDeserializer`，它能自动处理反序列化失败，将无法解析的消息路由到 DLQ，避免 poison pill 阻塞消费。

#### 8. 缺少消息版本演进策略

`DomainEvent` 有 `version` 字段，但 Consumer 完全不检查版本。如果发布方升级事件结构（增加字段），消费方可能解析失败。

**建议**：
- Consumer 端做向后兼容的解析（只读取需要的字段，忽略未知字段）
- 在 Consumer 中添加版本检查，记录不兼容的版本号

#### 9. ~~完成方案 C 端到端测试~~ ✅ 已完成

E2E 测试已通过两个互补模块完整实现：
- `library-e2e-test`（JUnit 5）：9 个测试，覆盖 UC-1~UC-8 所有跨上下文场景
- `library-integration-test`（Cucumber BDD）：9 个 Scenario，7 个 Feature 文件

两个模块均使用 `@EmbeddedKafka` + H2 内存数据库，验证了事件从 Kafka Topic → @KafkaListener → Handler → Repository 的完整链路。

#### 10. 考虑添加消息追踪（Tracing）

生产环境 7 个服务通过 Kafka 交互，排障需要分布式追踪。项目 CLAUDE.md 中已列出 Jaeger + OTLP 配置，但代码中未见集成。

**建议**：
- 添加 `opentelemetry-spring-boot-starter` + `opentelemetry-kafka-instrumentation`
- 在消息 header 中自动传播 trace context

---

## 五、总结

| 维度 | 评价 | 分数 |
|------|------|:----:|
| 架构设计 | Per-Context Topic + Choreography Saga + 防腐层，DDD 意识强 | ⭐⭐⭐⭐⭐ |
| 代码一致性 | 7 个模块完全统一的 Publisher/Consumer/Handler 模式 | ⭐⭐⭐⭐⭐ |
| 测试覆盖 | 四层测试（A/B-1/B-2/C-1/C-2），全部 100% 完成 | ⭐⭐⭐⭐⭐ |
| 生产可靠性 | 缺少幂等、DLQ、offset 管理、发送确认 | ⭐⭐ |
| 演进能力 | 有 version 字段但未利用，缺 schema 校验 | ⭐⭐ |

**核心建议**：测试覆盖和生产代码结构优秀，但 **消息可靠性**（幂等、DLQ、offset 管理）是从"能跑"到"敢上生产"的关键差距。建议优先补齐第 1-3 条建议。





 Plan: 修复 application.yml Kafka 配置问题                                                                                                                                                                             │
     │                                                                                                                                                                                                                       │
     │ Context                                                                                                                                                                                                               │
     │                                                                                                                                                                                                                       │
     │ 在审查项目 7 个模块的 src/main/resources/application.yml 和 src/test/resources/application.yml 后，发现 6 个配置一致性问题。这些问题会导致 EmbeddedKafka 集成测试行为不确定、序列化配置语义矛盾、以及密码管理不统一。 │
     │                                                                                                                                                                                                                       │
     │ 本计划的目标是：将所有解决方案写入一个独立的 MD 文件（Architecture_Design/Kafka-Config-Fix-Plan.md），不修改任何实际代码或配置文件。                                                                                  │
     │                                                                                                                                                                                                                       │
     │ ---                                                                                                                                                                                                                   │
     │ 要写入的文档内容                                                                                                                                                                                                      │
     │                                                                                                                                                                                                                       │
     │ 输出文件                                                                                                                                                                                                              │
     │                                                                                                                                                                                                                       │
     │ Architecture_Design/Kafka-Config-Fix-Plan.md                                                                                                                                                                          │
     │                                                                                                                                                                                                                       │
     │ 文档结构                                                                                                                                                                                                              │
     │                                                                                                                                                                                                                       │
     │ 1. 问题 1：test yml 全部 exclude KafkaAutoConfiguration                                                                                                                                                               │
     │   - 现状：7/7 模块 test/application.yml 都 exclude Kafka                                                                                                                                                              │
     │   - 影响：@EmbeddedKafka 集成测试（12 个类）能否正常工作依赖隐式行为                                                                                                                                                  │
     │   - 推荐方案：统一采用 analytics 模块的 profile 模式                                                                                                                                                                  │
     │       - 默认段 exclude Kafka（普通测试用）                                                                                                                                                                            │
     │     - 新增 embedded-kafka profile，清空 exclude 列表 + 配置 kafka 段                                                                                                                                                  │
     │     - EmbeddedKafka 测试类加 @ActiveProfiles("embedded-kafka")                                                                                                                                                        │
     │   - 涉及文件：7 个模块的 src/test/resources/application.yml + 12 个 EmbeddedKafka 测试类                                                                                                                              │
     │   - 示例模板（可直接复用的 yaml）                                                                                                                                                                                     │
     │ 2. 问题 2：Producer JsonSerializer vs Consumer StringDeserializer                                                                                                                                                     │
     │   - 现状：6 个模块 Producer 发 JSON 对象，Consumer 接收 String 手动解析                                                                                                                                               │
     │   - 影响：配置语义矛盾，spring.json.trusted.packages 无意义，Spring Kafka 类型映射失效                                                                                                                                │
     │   - 两种方案对比：                                                                                                                                                                                                    │
     │       - 方案 A：Consumer 改为 JsonDeserializer（利用 Spring Kafka 类型映射，需定义事件类共享）                                                                                                                        │
     │     - 方案 B：保持现状，删除无意义的 trusted.packages 配置，加注释说明意图                                                                                                                                            │
     │   - 推荐：方案 B（改动最小，且 Consumer 手动解析 + switch 路由是当前防腐层的核心设计）                                                                                                                                │
     │   - 涉及文件：6 个模块的 src/main/resources/application.yml                                                                                                                                                           │
     │ 3. 问题 3：密码管理不统一                                                                                                                                                                                             │
     │   - 现状：catalog/inventory/circulation 硬编码 dev_pg_2026，其余 4 个用 ${DB_PASSWORD:postgres}                                                                                                                       │
     │   - 推荐：统一为环境变量模式，fallback 值用 dev_pg_2026 保持本地开发兼容                                                                                                                                              │
     │   - 涉及文件：catalog、inventory、circulation 的 src/main/resources/application.yml                                                                                                                                   │
     │ 4. 问题 4：main yml 内嵌 test profile 与 test yml 重复                                                                                                                                                                │
     │   - 现状：catalog/inventory 的 main yml 有 ---\n spring.profiles: test 段，test yml 也 exclude Kafka                                                                                                                  │
     │   - 推荐：删除 main yml 中的 test profile 段（测试配置应全部放在 test 资源目录）                                                                                                                                      │
     │   - 涉及文件：catalog、inventory 的 src/main/resources/application.yml                                                                                                                                                │
     │ 5. 问题 5：notification 无 producer 配置                                                                                                                                                                              │
     │   - 现状：notification 是纯消费方，无 producer/topic 配置                                                                                                                                                             │
     │   - 推荐：保持现状，加注释标注"纯消费方，无 producer 配置"；如未来需要发布事件再补充                                                                                                                                  │
     │ 6. 问题 6：analytics 默认段 exclude + kafka 配置矛盾                                                                                                                                                                  │
     │   - 现状：默认段同时 exclude KafkaAutoConfiguration 和配置 kafka 段                                                                                                                                                   │
     │   - 推荐：默认段删除 kafka 配置段，仅保留 exclude；kafka 配置放到 embedded-kafka profile 中                                                                                                                           │
     │   - 涉及文件：analytics 的 src/test/resources/application.yml                                                                                                                                                         │
     │                                                                                                                                                                                                                       │
     │ ---                                                                                                                                                                                                                   │
     │ 执行步骤                                                                                                                                                                                                              │
     │                                                                                                                                                                                                                       │
     │ 1. 创建 Architecture_Design/Kafka-Config-Fix-Plan.md，包含上述 6 个问题的详细解决方案                                                                                                                                 │
     │ 2. 每个问题包含：现状描述、影响分析、推荐方案、涉及文件列表、示例代码/配置模板                                                                                                                                        │
     │ 3. 文档末尾附统一的 test/application.yml 模板，供后续批量修改时参考                                                                                                                                                   │
     │                                                                                                                                                                                                                       │
     │ 验证                                                                                                                                                                                                                  │
     │                                                                                                                                                                                                                       │
     │ - 文档写完后确认内容覆盖了所有 6 个问题                                                                                                                                                                               │
     │ - 无需运行测试（本任务只输出文档）                                             
