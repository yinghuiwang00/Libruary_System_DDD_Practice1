# Library Analytics 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-analytics` 模块中每一个源文件（不含测试文件）的设计意图、DDD 定位和实现细节。

---

## 目录

- [1. 模块概览](#1-模块概览)
- [2. DDD 分层架构总览](#2-ddd-分层架构总览)
- [3. 引导与配置层](#3-引导与配置层)
- [4. 领域层（Domain Layer）](#4-领域层domain-layer)
  - [4.1 领域模型（domain/model）](#41-领域模型domainmodel)
  - [4.2 领域事件（domain/event）](#42-领域事件domainevent)
  - [4.3 领域异常（domain/exception）](#43-领域异常domainexception)
  - [4.4 仓储接口（domain/repository）](#44-仓储接口domainrepository)
  - [4.5 领域服务（domain/service）](#45-领域服务domainservice)
- [5. 应用层（Application Layer）](#5-应用层application-layer)
  - [5.1 应用服务（application/service）](#51-应用服务applicationservice)
  - [5.2 命令对象（application/command）](#52-命令对象applicationcommand)
  - [5.3 数据传输对象（application/dto）](#53-数据传输对象applicationdto)
- [6. 基础设施层（Infrastructure Layer）](#6-基础设施层infrastructure-layer)
- [7. 接口层（Interfaces Layer）](#7-接口层interfaces-layer)
- [8. 层间调用流程](#8-层间调用流程)
- [9. 文件清单速查表](#9-文件清单速查表)

---

## 1. 模块概览

`library-analytics` 是图书馆管理系统的**统计分析上下文（Analytics Context）**，负责管理报表的生成、趋势分析和数据统计：

| 职责 | 说明 |
|------|------|
| 报表管理 | 创建、完成、失败、取消、重新生成分析报表 |
| 报表类型 | 流通报表、库存报表、读者报表、财务报表、热门报表、逾期报表、利用率报表 |
| 报表周期 | 日报、周报、月报、季报、年报、自定义周期 |
| 趋势分析 | 对比当前值与历史值，计算变化百分比和趋势方向 |
| 仪表盘 | 概览、流通、库存、读者、财务、自定义六类仪表盘 |
| 领域事件 | 报表状态变更时发布事件通知其他上下文 |

运行端口：`8086`，基础路径：`/api/analytics/*`

---

## 2. DDD 分层架构总览

```
library-analytics/src/main/java/com/library/analytics/
│
├── AnalyticsApplication.java             ← Spring Boot 启动类
├── config/                               ← 基础设施配置
│   └── JpaConfig.java
│
├── domain/                               ← 领域层（纯业务逻辑，无外部依赖）
│   ├── model/                            ← 聚合根、值对象
│   │   ├── AnalyticsReport.java          ← 聚合根：分析报表
│   │   ├── TrendAnalysis.java            ← 值对象：趋势分析
│   │   └── enums/                        ← 枚举
│   │       ├── ReportStatus.java
│   │       ├── ReportType.java
│   │       ├── ReportPeriod.java
│   │       └── DashboardType.java
│   ├── event/                            ← 领域事件
│   │   ├── ReportCreatedEvent.java
│   │   ├── ReportCompletedEvent.java
│   │   ├── ReportFailedEvent.java
│   │   └── ReportCancelledEvent.java
│   ├── exception/                        ← 领域异常（含错误码）
│   │   ├── DomainException.java          ← 异常基类
│   │   ├── ReportNotFoundException.java
│   │   ├── DashboardNotFoundException.java
│   │   └── InvalidOperationException.java
│   ├── repository/                       ← 仓储接口（由 Spring Data 自动实现）
│   │   └── AnalyticsReportRepository.java
│   └── service/                          ← 领域服务
│       └── AnalyticsService.java
│
├── application/                          ← 应用层（编排、协调）
│   ├── service/
│   │   └── AnalyticsApplicationService.java  ← 应用服务：编排报表用例
│   ├── command/                          ← 写操作入参
│   │   ├── CreateReportCommand.java
│   │   ├── CompleteReportCommand.java
│   │   ├── FailReportCommand.java
│   │   ├── CancelReportCommand.java
│   │   └── RegenerateReportCommand.java
│   └── dto/                              ← 输出传输对象
│       ├── ApiResponse.java
│       └── ReportDTO.java
│
└── interfaces/                           ← 接口层（REST API）
    └── rest/
        ├── AnalyticsController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces → application → domain ← infrastructure
                                  （实现 domain 的 repository 接口）
```

- **domain 层**不依赖任何其他层，纯 Java + JPA 注解
- **application 层**依赖 domain 层（调用领域服务和仓储接口）
- **infrastructure 层**由 Spring Data JPA 自动实现仓储接口，无需手写实现类
- **interfaces 层**依赖 application 层（调用应用服务）

---

## 3. 引导与配置层

### `AnalyticsApplication.java`

```java
@SpringBootApplication(scanBasePackages = {"com.library.analytics", "com.library.shared"})
@EnableJpaRepositories
public class AnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类，启动整个统计分析上下文的 Spring 容器。

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| `scanBasePackages` | 同时扫描 `com.library.analytics` 和 `com.library.shared`，确保共享模块的 `DomainEventPublisher`、`ReportId` 等类能被注入 |
| `@EnableJpaRepositories` | 启用 Spring Data JPA 仓储自动发现，无需指定 basePackage（默认扫描启动类所在包） |

---

### `config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**DDD 定位**：基础设施配置。`@EnableJpaAuditing` 启用 JPA 审计功能，让 `AnalyticsReport` 实体上的 `@CreatedDate`、`@LastModifiedDate` 注解自动填充创建/修改时间。

**为什么放在 config 包而不是 infrastructure？** 这是一个全局性的 JPA 配置，不属于某个具体的持久化实现，所以独立放在 config 包。

---

## 4. 领域层（Domain Layer）

> 领域层是 DDD 的核心。这里包含纯业务逻辑，不依赖任何外部框架（除了 JPA 注解作为持久化映射）。

### 4.1 领域模型（domain/model）

#### 4.1.1 `AnalyticsReport.java` —— 聚合根：分析报表

**DDD 角色**：**聚合根（Aggregate Root）**

AnalyticsReport 是统计分析上下文中唯一的聚合根，管理报表的完整生命周期。

```java
@Entity
@Table(name = "analytics_reports", indexes = {
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_date", columnList = "report_date")
})
@EntityListeners(AuditingEntityListener.class)
public class AnalyticsReport {

    @EmbeddedId
    private ReportId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_period", nullable = false, length = 20)
    private ReportPeriod reportPeriod;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "data_summary", columnDefinition = "TEXT")
    private String dataSummary;

    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId ReportId id` | 使用共享模块的强类型 ID，而非裸 String |
| 乐观锁 | `@Version Long version` | 防止并发修改冲突，多用户同时操作报表时不丢失更新 |
| 审计字段 | `@CreatedDate`, `@LastModifiedDate` | 自动记录创建/修改时间 |
| 构造控制 | `protected AnalyticsReport()` + `private AnalyticsReport(...)` | JPA 要求无参构造器，但 `protected` 防止外部直接实例化 |
| 工厂方法 | `AnalyticsReport.create(...)` | 静态工厂方法，确保创建时总是生成新 ID 和 `GENERATING` 初始状态 |
| 数据库索引 | 三个 `@Index` 注解 | 为 `report_type`、`status`、`report_date` 建立索引，优化查询性能 |
| 审计监听器 | `@EntityListeners(AuditingEntityListener.class)` | 配合 JpaConfig 中的 `@EnableJpaAuditing` 自动填充审计字段 |

**状态机：报表生命周期**

```
                     ┌──────────────────────────────┐
                     │         GENERATING            │
                     │    （创建时的初始状态）          │
                     └──────┬───────┬────────────────┘
                            │       │
                  complete() │       │ fail()
                            │       │
                            ▼       ▼
                      COMPLETED   FAILED
                            │       │
                   cancel() │       │ regenerate()
                            │       │
                            ▼       ▼
                        CANCELLED ──→ GENERATING（重新生成）
                            │       regenerate()
                            └───────┘

  规则：
  - GENERATING 状态可以 complete()、fail()、cancel()
  - COMPLETED 状态不能 cancel()、fail() 或 regenerate()
  - FAILED 和 CANCELLED 状态可以 regenerate() 回到 GENERATING
```

**业务规则（编码在领域模型中）：**

- `complete()`：只有 `GENERATING` 状态才能标记完成，设置 totalRecords、dataSummary、reportData
- `fail()`：只有 `GENERATING` 状态才能标记失败，设置 failureReason
- `cancel()`：`COMPLETED` 状态的报表不能取消，其他状态可以
- `regenerate()`：只有 `FAILED` 或 `CANCELLED` 状态才能重新生成，重置所有数据字段回到 `GENERATING`
- `create()`：工厂方法，自动生成 `ReportId`、设置状态为 `GENERATING`、记录 `generatedAt` 时间戳

**状态查询方法：**

```java
public boolean isGenerating() { return this.status == ReportStatus.GENERATING; }
public boolean isCompleted()  { return this.status == ReportStatus.COMPLETED; }
public boolean isFailed()     { return this.status == ReportStatus.FAILED; }
public boolean isCancelled()  { return this.status == ReportStatus.CANCELLED; }
```

这些布尔查询方法封装了状态判断逻辑，使调用方不需要直接比较枚举值。

---

#### 4.1.2 `TrendAnalysis.java` —— 值对象：趋势分析

**DDD 角色**：**值对象（Value Object）**

TrendAnalysis 封装了指标趋势的计算逻辑，是一个自包含的值对象。

```java
@Embeddable
public class TrendAnalysis {

    private String metricName;
    private BigDecimal currentValue;
    private BigDecimal previousValue;
    private BigDecimal changePercentage;
    private String trendDirection;
    private String trendStrength;

    public TrendAnalysis(String metricName, BigDecimal currentValue, BigDecimal previousValue) {
        this.metricName = Objects.requireNonNull(metricName, "Metric name must not be null");
        this.currentValue = Objects.requireNonNull(currentValue, "Current value must not be null");
        this.previousValue = Objects.requireNonNull(previousValue, "Previous value must not be null");
        this.changePercentage = calculateChangePercentage(currentValue, previousValue);
        this.trendDirection = determineTrendDirection(currentValue, previousValue);
        this.trendStrength = determineTrendStrength(this.changePercentage);
    }
}
```

**值对象特征：**

1. **不可变性**：构造后所有字段不可修改（无 setter），changePercentage、trendDirection、trendStrength 由构造函数自动计算
2. **自验证**：构造函数中对 metricName、currentValue、previousValue 进行空值校验
3. **自计算**：构造时自动完成趋势计算，外部不需要调用额外方法

**趋势计算逻辑：**

| 计算项 | 算法 | 输出示例 |
|--------|------|----------|
| `changePercentage` | `(current - previous) * 100 / abs(previous)`，保留2位小数 | `25.00` |
| `trendDirection` | current > previous → `"UP"`；< previous → `"DOWN"`；= previous → `"STABLE"` | `"UP"` |
| `trendStrength` | abs >= 25% → `"STRONG"`；>= 10% → `"MODERATE"`；>= 1% → `"WEAK"`；< 1% → `"NEGLIGIBLE"` | `"MODERATE"` |

**边界情况处理：**

```java
private BigDecimal calculateChangePercentage(BigDecimal current, BigDecimal previous) {
    if (previous.compareTo(BigDecimal.ZERO) == 0) {
        if (current.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;          // 0/0 = 0
        }
        return current.compareTo(BigDecimal.ZERO) > 0
            ? BigDecimal.valueOf(100)        // +100%（从0增长）
            : BigDecimal.valueOf(-100);      // -100%（从0下降）
    }
    return current.subtract(previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(previous.abs(), 2, RoundingMode.HALF_UP);
}
```

**业务查询方法：**

```java
public boolean isUpwardTrend() { return "UP".equals(this.trendDirection); }
public boolean isDownwardTrend() { return "DOWN".equals(this.trendDirection); }
public boolean isSignificant() {
    return this.changePercentage != null
        && this.changePercentage.abs().compareTo(BigDecimal.valueOf(10)) >= 0;
}
```

`isSignificant()` 方法判断变化幅度是否超过 10%，用于决定是否需要关注该趋势。

**`@Embeddable` 注解**：TrendAnalysis 作为值对象可以嵌入到其他实体中（目前 AnalyticsReport 未直接嵌入，但设计上预留了可嵌入能力），不会创建独立的数据库表。

---

#### 4.1.3 `enums/ReportStatus.java` —— 枚举：报表状态

```java
public enum ReportStatus {
    GENERATING,   // 生成中（创建时的初始状态）
    COMPLETED,    // 已完成
    FAILED,       // 生成失败
    CANCELLED     // 已取消
}
```

**DDD 意义**：定义了报表的生命周期状态。注意与图书状态（DRAFT/PUBLISHED）不同，报表状态反映了异步生成过程的状态变迁。

---

#### 4.1.4 `enums/ReportType.java` —— 枚举：报表类型

```java
public enum ReportType {
    CIRCULATION_REPORT,    // 流通报表
    INVENTORY_REPORT,      // 库存报表
    PATRON_REPORT,         // 读者报表
    FINANCIAL_REPORT,      // 财务报表
    POPULARITY_REPORT,     // 热门图书报表
    OVERDUE_REPORT,        // 逾期报表
    UTILIZATION_REPORT     // 利用率报表
}
```

**DDD 意义**：精确分类报表的统计维度，每种类型对应不同的数据采集和计算逻辑。七种报表类型覆盖了图书馆运营的核心指标。

---

#### 4.1.5 `enums/ReportPeriod.java` —— 枚举：报表周期

```java
public enum ReportPeriod {
    DAILY,       // 日报
    WEEKLY,      // 周报
    MONTHLY,     // 月报
    QUARTERLY,   // 季报
    YEARLY,      // 年报
    CUSTOM       // 自定义周期
}
```

**DDD 意义**：定义报表的时间跨度。`CUSTOM` 类型需要配合 `startDate` 和 `endDate` 字段使用。

---

#### 4.1.6 `enums/DashboardType.java` —— 枚举：仪表盘类型

```java
public enum DashboardType {
    OVERVIEW,      // 概览仪表盘
    CIRCULATION,   // 流通仪表盘
    INVENTORY,     // 库存仪表盘
    PATRON,        // 读者仪表盘
    FINANCIAL,     // 财务仪表盘
    CUSTOM         // 自定义仪表盘
}
```

**DDD 意义**：定义仪表盘的分类维度。与 ReportType 不同，DashboardType 关注的是数据展示面板的类型，而非报表类型。当前模块中 DashboardType 已定义但尚未在聚合根中使用，预留了仪表盘功能的扩展空间。

---

### 4.2 领域事件（domain/event）

> 领域事件表示领域中已经发生的、有业务意义的事情。在 DDD 中，事件用于实现聚合间和上下文间的解耦通信。

所有领域事件都继承自 `library-shared` 模块中的 `DomainEvent` 基类：

```java
// 共享模块中的基类
public abstract class DomainEvent implements Serializable {
    private final String eventId;           // UUID 唯一标识
    private final LocalDateTime occurredAt; // 事件发生时间
    private final String eventType;         // 事件类型（类名）
    private final int version;              // 事件版本
}
```

#### 4.2.1 `ReportCreatedEvent.java`

```java
public class ReportCreatedEvent extends DomainEvent {
    private final ReportId reportId;
    private final ReportType reportType;
    private final String generatedBy;
}
```

**触发时机**：
- `AnalyticsService.createReport()` 成功保存新报表后发布
- `AnalyticsService.regenerateReport()` 重新生成报表后发布

**携带信息**：报表 ID、报表类型、生成人——这是其他上下文（如通知上下文需要告知用户新报表已创建）最需要的信息。

---

#### 4.2.2 `ReportCompletedEvent.java`

```java
public class ReportCompletedEvent extends DomainEvent {
    private final ReportId reportId;
    private final ReportType reportType;
    private final Long totalRecords;
}
```

**触发时机**：报表从 `GENERATING` 状态变为 `COMPLETED` 时发布。

**携带 totalRecords**：报表完成的记录总数，方便订阅方快速判断报表的数据规模，无需再回查报表详情。

---

#### 4.2.3 `ReportFailedEvent.java`

```java
public class ReportFailedEvent extends DomainEvent {
    private final ReportId reportId;
    private final ReportType reportType;
    private final String errorMessage;
}
```

**触发时机**：报表生成失败时发布。

**携带 errorMessage**：失败原因，供监控告警系统和运维人员排查问题。

---

#### 4.2.4 `ReportCancelledEvent.java`

```java
public class ReportCancelledEvent extends DomainEvent {
    private final ReportId reportId;
    private final String reason;
}
```

**触发时机**：报表被取消时发布。

**设计对比**：与其他三个事件不同，`ReportCancelledEvent` 不携带 `ReportType`，因为取消操作的业务语义是"停止生成"，订阅方通常只需要知道哪个报表被取消以及原因。

---

### 4.3 领域异常（domain/exception）

> DDD 中，异常也是领域逻辑的一部分。每个异常都带有**错误码（errorCode）**，方便 API 层统一处理和前端国际化。

#### 4.3.1 `DomainException.java` —— 异常基类

```java
public class DomainException extends RuntimeException {
    private final String errorCode;

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| 非抽象 | 与 catalog 模块的 `abstract DomainException` 不同，analytics 模块的基类是具体类，允许在某些场景下直接抛出 |
| `errorCode` | 每个子类构造时传入自己的错误码字符串 |
| 继承 `RuntimeException` | 非受检异常，不强制调用方处理，由 `GlobalExceptionHandler` 统一捕获 |

---

#### 4.3.2 具体异常类

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `ReportNotFoundException` | `REPORT_NOT_FOUND` | 通过 ID 查找报表不存在 | 404 |
| `DashboardNotFoundException` | `DASHBOARD_NOT_FOUND` | 查找仪表盘不存在 | 404 |
| `InvalidOperationException` | `INVALID_OPERATION` | 状态不允许的操作（如完成已完成的报表） | 409 Conflict |

每个异常类的实现都非常简洁，例如：

```java
public class ReportNotFoundException extends DomainException {
    public ReportNotFoundException(ReportId reportId) {
        super("REPORT_NOT_FOUND", "Report not found: " + reportId);
    }
}
```

```java
public class DashboardNotFoundException extends DomainException {
    public DashboardNotFoundException(String dashboardType) {
        super("DASHBOARD_NOT_FOUND", "Dashboard not found: " + dashboardType);
    }
}
```

**错误码到 HTTP 状态的映射**：在 `GlobalExceptionHandler` 中通过 `mapToHttpStatus()` 方法实现。

---

### 4.4 仓储接口（domain/repository）

> 仓储（Repository）是 DDD 中领域模型与数据存储之间的桥梁。**领域层只定义接口**，具体实现由 Spring Data JPA 自动生成。

#### `AnalyticsReportRepository.java`

```java
@Repository
public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, ReportId> {

    List<AnalyticsReport> findByReportType(ReportType reportType);

    List<AnalyticsReport> findByStatus(ReportStatus status);

    List<AnalyticsReport> findByReportTypeAndReportDateBetween(
        ReportType reportType, LocalDate startDate, LocalDate endDate);
}
```

**DDD 定位**：继承 `JpaRepository<AnalyticsReport, ReportId>` 获得 CRUD 能力，Spring Data JPA 在运行时自动提供实现。

**方法类型分析：**

| 方法 | 用途 | 实现方式 |
|------|------|----------|
| `findByReportType()` | 按报表类型过滤查询 | Spring Data 派生查询 |
| `findByStatus()` | 按报表状态过滤查询 | Spring Data 派生查询 |
| `findByReportTypeAndReportDateBetween()` | 按类型和日期范围组合查询 | Spring Data 派生查询（Between 关键字） |

**设计特点**：与 catalog 模块不同，analytics 模块没有自定义仓储接口（如 `CustomBookRepository`）和 Criteria API 实现，因为报表查询场景相对简单，Spring Data 派生查询已满足需求。

---

### 4.5 领域服务（domain/service）

> 领域服务用于承载**不适合放在单个实体或值对象中的业务逻辑**，例如跨聚合的协调、仓储的调用、领域事件的发布等。

#### `AnalyticsService.java`

**DDD 角色**：**领域服务**—— 报表聚合的核心业务编排

```java
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsReportRepository reportRepository;
    private final DomainEventPublisher eventPublisher;

    public AnalyticsService(AnalyticsReportRepository reportRepository,
                            DomainEventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.eventPublisher = eventPublisher;
    }
}
```

**职责：**

| 方法 | 业务逻辑 |
|------|----------|
| `createReport()` | 调用 `AnalyticsReport.create()` → 保存 → 发布 `ReportCreatedEvent` |
| `completeReport()` | 查找报表 → 调用 `report.complete()` → 保存 → 发布 `ReportCompletedEvent` |
| `failReport()` | 查找报表 → 调用 `report.fail()` → 保存 → 发布 `ReportFailedEvent` |
| `cancelReport()` | 查找报表 → 调用 `report.cancel()` → 保存 → 发布 `ReportCancelledEvent` |
| `regenerateReport()` | 查找报表 → 调用 `report.regenerate()` → 保存 → 发布 `ReportCreatedEvent` |
| `getReport()` | 查找单个报表 |
| `getAllReports()` | 查找所有报表 |
| `getReportsByType()` | 按类型查找报表 |
| `getReportsByStatus()` | 按状态查找报表 |

**DDD 设计要点：**

1. **事务管理**：类级别 `@Transactional(readOnly = true)`，写操作方法覆盖为 `@Transactional`
2. **事件发布**：每个状态变更操作完成后都发布对应的领域事件，实现与其他上下文的解耦
3. **私有辅助方法**：`findOrThrow()` 封装了"查找不存在则抛异常"的通用逻辑

```java
private AnalyticsReport findOrThrow(ReportId reportId) {
    return reportRepository.findById(reportId)
        .orElseThrow(() -> new ReportNotFoundException(reportId));
}
```

4. **保护聚合不变量**：业务规则（如"只有 GENERATING 状态才能完成"）编码在 `AnalyticsReport` 实体中，而非服务中。领域服务只负责编排流程（查找 → 调用实体方法 → 保存 → 发布事件）。

5. **事件发布策略**：`regenerateReport()` 发布的是 `ReportCreatedEvent`（而非单独的 RegeneratedEvent），因为重新生成在语义上等同于创建一个新的报表生成任务。

---

## 5. 应用层（Application Layer）

> 应用层是 DDD 中的**"编排层"**，它不包含业务逻辑，而是协调领域对象完成用例。它的职责是：接收外部请求 → 转换参数 → 调用领域服务 → 转换输出 → 返回结果。

### 5.1 应用服务（application/service）

#### `AnalyticsApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional
public class AnalyticsApplicationService {

    private final AnalyticsService analyticsService;

    public AnalyticsApplicationService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
}
```

**AnalyticsService vs AnalyticsApplicationService 的区别：**

| 维度 | AnalyticsService（领域服务） | AnalyticsApplicationService（应用服务） |
|------|------|------|
| 输入 | 领域对象（ReportId, ReportType, ReportPeriod） | 外部格式（String, Command 对象） |
| 输出 | 领域对象（AnalyticsReport） | DTO（ReportDTO） |
| 职责 | 业务规则执行和领域事件发布 | 参数转换和结果转换 |
| 调用者 | 应用服务 | 控制器 |

**典型方法流程**（以 `createReport` 为例）：

```
Controller 调用 → AnalyticsApplicationService.createReport(CreateReportCommand)
  1. 将 String reportType 转换为 ReportType 枚举
  2. 将 String reportPeriod 转换为 ReportPeriod 枚举
  3. 调用 analyticsService.createReport(...)
  4. 将返回的 AnalyticsReport 实体转换为 ReportDTO
  5. 返回 ReportDTO
```

**类级别事务注解**：`@Transactional`（非 readOnly），因为应用服务的大多数方法都是写操作。读方法通过 `@Transactional(readOnly = true)` 覆盖。

**完整方法清单：**

| 方法 | 事务类型 | 说明 |
|------|----------|------|
| `createReport(CreateReportCommand)` | 写 | 创建报表 |
| `completeReport(CompleteReportCommand)` | 写 | 完成报表 |
| `failReport(FailReportCommand)` | 写 | 标记报表失败 |
| `cancelReport(CancelReportCommand)` | 写 | 取消报表 |
| `regenerateReport(RegenerateReportCommand)` | 写 | 重新生成报表 |
| `getReport(String)` | 只读 | 查询单个报表 |
| `getAllReports()` | 只读 | 查询所有报表 |
| `getReportsByType(String)` | 只读 | 按类型查询 |
| `getReportsByStatus(String)` | 只读 | 按状态查询 |

---

### 5.2 命令对象（application/command）

> 命令（Command）是 CQRS 模式中"写操作"的输入对象，封装了执行某个操作所需的全部参数。与 catalog 模块使用 `record` 不同，analytics 模块的命令使用传统 class + getter/setter，因为需要 Spring MVC 的请求体绑定。

#### `CreateReportCommand.java` —— 创建报表命令

```java
public class CreateReportCommand {

    @NotNull(message = "Report type must not be null")
    private String reportType;

    @NotNull(message = "Report period must not be null")
    private String reportPeriod;

    @NotNull(message = "Report date must not be null")
    private LocalDate reportDate;

    private LocalDate startDate;
    private LocalDate endDate;
    private String generatedBy;
}
```

**设计要点：**

- `reportType` 和 `reportPeriod` 使用 String 类型（而非枚举），因为 JSON 反序列化时枚举对大小写敏感
- `@NotNull` 注解在 Controller 层由 `@Valid` 触发自动验证
- `startDate`/`endDate` 为可选字段，仅在 `CUSTOM` 周期时需要
- `generatedBy` 为可选字段，记录触发报表生成的操作人

---

#### `CompleteReportCommand.java` —— 完成报表命令

```java
public class CompleteReportCommand {

    @NotBlank(message = "Report ID must not be blank")
    private String reportId;

    @NotNull(message = "Total records must not be null")
    private Long totalRecords;

    private String dataSummary;
    private String reportData;
}
```

**设计要点：**

- `reportId` 使用 `@NotBlank`（而非 `@NotNull`），因为 String 类型还需要防止空字符串
- `totalRecords` 使用 `@NotNull`，报表完成时必须提供记录总数
- `dataSummary` 和 `reportData` 为可选字段，但通常在完成时会填充

---

#### `FailReportCommand.java` —— 标记失败命令

```java
public class FailReportCommand {
    @NotBlank(message = "Report ID must not be blank")
    private String reportId;
    private String errorMessage;
}
```

结构简洁，只需知道哪个报表失败以及失败原因。

---

#### `CancelReportCommand.java` —— 取消报表命令

```java
public class CancelReportCommand {
    @NotBlank(message = "Report ID must not be blank")
    private String reportId;
    private String reason;
}
```

与 FailReportCommand 结构类似但语义不同：fail 是系统行为（生成出错），cancel 是用户行为（主动取消）。

---

#### `RegenerateReportCommand.java` —— 重新生成命令

```java
public class RegenerateReportCommand {
    @NotBlank(message = "Report ID must not be blank")
    private String reportId;
    private String regeneratedBy;
}
```

记录重新生成的操作人，用于审计追踪。

---

### 5.3 数据传输对象（application/dto）

#### `ApiResponse.java` —— 统一响应信封

```java
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success(String message, T data) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) { ... }
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| 泛型 `<T>` | 支持任意数据类型（`ReportDTO`、`List<ReportDTO>` 等） |
| 成功时 | `success=true, data=实际数据, errorMessage=null` |
| 失败时 | `success=false, data=null, errorMessage=错误信息, errorCode=错误码` |
| `timestamp` | 自动记录响应生成时间 |
| 静态工厂方法 | `success()` / `error()` 提供便捷构造 |

---

#### `ReportDTO.java`

```java
public class ReportDTO {

    private String id;
    private String reportType;
    private String reportPeriod;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private Long totalRecords;
    private String dataSummary;
    private String reportData;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportDTO from(AnalyticsReport report) {
        ReportDTO dto = new ReportDTO();
        dto.id = report.getId().getValue();           // ReportId → String
        dto.reportType = report.getReportType().name(); // 枚举 → String
        dto.reportPeriod = report.getReportPeriod().name();
        dto.reportDate = report.getReportDate();
        dto.status = report.getStatus().name();
        // ... 其他字段映射
        return dto;
    }
}
```

**关键设计**：`from(AnalyticsReport)` 是一个**组装器方法（Assembler）**，将 AnalyticsReport 聚合根展平为扁平的 DTO。注意 `id` 从 `ReportId` 变成了 String，`reportType`/`reportPeriod`/`status` 从枚举变成了 String——DTO 总是使用基本类型，方便 JSON 序列化。

**与 catalog 模块的 DTO 对比**：catalog 模块的 DTO 使用 `record` 类型（不可变），analytics 模块使用传统 class + getter/setter。这是两种不同的 DTO 实现策略，后者更灵活，前者更安全。

---

## 6. 基础设施层（Infrastructure Layer）

> analytics 模块的基础设施层较为简洁——没有独立的 `infrastructure` 包。

**原因分析：**

| 基础设施组件 | 是否需要独立实现 | 说明 |
|------------|-----------------|------|
| 仓储实现 | 否 | `AnalyticsReportRepository` 继承 `JpaRepository`，Spring Data JPA 自动提供实现，无需手写 |
| 复杂查询 | 否 | 报表查询需求简单，Spring Data 派生查询即可满足，不需要 Criteria API |
| 事件发布 | 否 | 直接使用共享模块的 `DomainEventPublisher`（基于 Spring ApplicationEventPublisher），无需双通道 Kafka 发布器 |

**共享模块提供的跨切面基础设施：**

```java
// library-shared 中的事件发布器（Spring 本地事件总线）
@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }
}
```

analytics 模块的领域服务 `AnalyticsService` 直接注入共享模块的 `DomainEventPublisher`，由 Spring 事件总线完成同 JVM 内的事件传播。未来需要跨服务通信时，可以在本模块添加 `infrastructure/messaging/` 包引入 Kafka。

---

## 7. 接口层（Interfaces Layer）

> 接口层是系统与外部世界的边界。它接收 HTTP 请求，调用应用服务，返回 HTTP 响应。**不包含任何业务逻辑。**

### 7.1 `AnalyticsController.java`

```java
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics Management", description = "APIs for managing analytics reports")
public class AnalyticsController {

    private final AnalyticsApplicationService analyticsService;

    public AnalyticsController(AnalyticsApplicationService analyticsService) {
        this.analyticsService = analyticsService;
    }
}
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/analytics/reports` | 创建报表 | 命令 |
| `GET` | `/api/analytics/reports` | 查询报表列表（支持 type/status 过滤） | 查询 |
| `GET` | `/api/analytics/reports/{id}` | 查询单个报表 | 查询 |
| `POST` | `/api/analytics/reports/{id}/complete` | 完成报表 | 命令（状态变更） |
| `POST` | `/api/analytics/reports/{id}/fail` | 标记报表失败 | 命令（状态变更） |
| `POST` | `/api/analytics/reports/{id}/cancel` | 取消报表 | 命令（状态变更） |
| `POST` | `/api/analytics/reports/{id}/regenerate` | 重新生成报表 | 命令（状态变更） |

**设计要点：**

1. **CQRS 体现**：POST 是命令（写），GET 是查询（读）；所有状态变更操作使用 POST 动词
2. **查询过滤**：`GET /reports` 通过可选的 `type` 和 `status` 查询参数实现过滤

```java
@GetMapping("/reports")
public ResponseEntity<ApiResponse<List<ReportDTO>>> getReports(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String status) {
    if (type != null) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getReportsByType(type)));
    }
    if (status != null) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getReportsByStatus(status)));
    }
    return ResponseEntity.ok(ApiResponse.success(analyticsService.getAllReports()));
}
```

3. **路径变量注入**：`completeReport`、`failReport`、`cancelReport`、`regenerateReport` 方法的 `reportId` 从 `@PathVariable` 获取，请求体中的 `reportId` 被忽略，由 Controller 重新构造 Command 对象

```java
@PostMapping("/reports/{id}/complete")
public ResponseEntity<ApiResponse<ReportDTO>> completeReport(
        @PathVariable String id,
        @Valid @RequestBody CompleteReportCommand request) {
    CompleteReportCommand command = new CompleteReportCommand(
            id,                           // 从 URL 路径获取
            request.getTotalRecords(),
            request.getDataSummary(),
            request.getReportData()
    );
    ReportDTO report = analyticsService.completeReport(command);
    return ResponseEntity.ok(ApiResponse.success(report));
}
```

4. **统一响应**：所有端点返回 `ApiResponse<T>` 信封
5. **Swagger 注解**：`@Tag` 和 `@Operation` 生成 OpenAPI 文档
6. **参数验证**：`@Valid @RequestBody` 触发 Bean Validation

---

### 7.2 `GlobalExceptionHandler.java` —— 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReportNotFoundException.class)        → 404 Not Found
    @ExceptionHandler(InvalidOperationException.class)      → 409 Conflict
    @ExceptionHandler(DomainException.class)                → 根据 errorCode 映射
    @ExceptionHandler(MethodArgumentNotValidException.class) → 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)        → 400 Bad Request
    @ExceptionHandler(Exception.class)                      → 500 Internal Server Error
}
```

**DDD 定位**：**跨切面（Cross-Cutting Concern）**，在接口层统一处理所有异常。

**错误码 → HTTP 状态映射逻辑：**

```java
private HttpStatus mapToHttpStatus(String errorCode) {
    return switch (errorCode) {
        case "REPORT_NOT_FOUND" -> HttpStatus.NOT_FOUND;      // 404
        case "INVALID_OPERATION" -> HttpStatus.CONFLICT;      // 409
        default -> HttpStatus.BAD_REQUEST;                    // 400
    };
}
```

**异常处理优先级（从具体到通用）：**

| 优先级 | 异常类型 | HTTP 状态 | 说明 |
|--------|----------|-----------|------|
| 1 | `ReportNotFoundException` | 404 | 具体异常优先处理 |
| 2 | `InvalidOperationException` | 409 | 具体异常优先处理 |
| 3 | `DomainException` | 根据映射 | 兜底处理所有领域异常 |
| 4 | `MethodArgumentNotValidException` | 400 | Bean Validation 验证失败 |
| 5 | `IllegalArgumentException` | 400 | 枚举转换失败等 |
| 6 | `Exception` | 500 | 未知异常，不泄露内部信息 |

**验证错误消息格式**：将所有字段错误拼接为一条消息：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_ERROR", message));
}
```

**安全设计**：通用 `Exception` 处理器返回固定消息 "An unexpected error occurred"，不泄露堆栈信息。

---

## 8. 层间调用流程

以 **"创建报表"** 为例，展示一次完整请求的层间调用链：

```
HTTP POST /api/analytics/reports
  │  Body: { "reportType": "CIRCULATION_REPORT", "reportPeriod": "MONTHLY",
  │          "reportDate": "2026-05-30", "generatedBy": "admin" }
  ▼
┌──────────────────────────────────────────────────────────────┐
│ AnalyticsController.createReport(CreateReportCommand)         │  ← interfaces 层
│   @Valid 触发 Bean Validation（@NotNull 检查）                │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ AnalyticsApplicationService.createReport(CreateReportCommand) │  ← application 层
│   1. ReportType.valueOf("CIRCULATION_REPORT")  String → 枚举  │
│   2. ReportPeriod.valueOf("MONTHLY")           String → 枚举  │
│   3. 调用 analyticsService.createReport(...)                  │
│   4. ReportDTO.from(report)                    实体 → DTO     │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ AnalyticsService.createReport(type, period, date, ...)        │  ← domain 层（服务）
│   1. AnalyticsReport.create(...)              工厂方法创建     │
│      → 生成 ReportId                                         │
│      → status = GENERATING                                   │
│      → generatedAt = now()                                   │
│   2. reportRepository.save(report)           持久化           │
│   3. eventPublisher.publish(ReportCreatedEvent)               │
└──────┬──────────────────────────────┬────────────────────────┘
       │                              │
       ▼                              ▼
┌───────────────────┐  ┌──────────────────────────────────────┐
│ AnalyticsReport   │  │ DomainEventPublisher                   │  ← shared 基础设施
│ Repository        │  │   → applicationEventPublisher          │    Spring 本地事件
│ (Spring Data JPA  │  │     .publishEvent(ReportCreatedEvent)  │
│  自动实现)        │  │                                        │
└─────────┬─────────┘  └──────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────────────┐
│ PostgreSQL (library_analytics)                                │  ← 数据库
│ INSERT INTO analytics_reports                                 │
│   (id, report_type, report_period, report_date, status,      │
│    generated_at, generated_by, version, created_at)           │
│ VALUES ('uuid...', 'CIRCULATION_REPORT', 'MONTHLY',           │
│   '2026-05-30', 'GENERATING', '2026-05-30T10:00:00', ...)    │
└──────────────────────────────────────────────────────────────┘
```

以 **"完成报表"** 为例的状态变更流程：

```
HTTP POST /api/analytics/reports/{id}/complete
  │  Body: { "totalRecords": 1500, "dataSummary": "...", "reportData": "..." }
  ▼
┌──────────────────────────────────────────────────────────────┐
│ AnalyticsController.completeReport(id, request)               │
│   → 构造 CompleteReportCommand(id, totalRecords, ...)         │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ AnalyticsApplicationService.completeReport(command)           │
│   → ReportId.of(command.getReportId())         String → ID   │
│   → analyticsService.completeReport(reportId, ...)            │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ AnalyticsService.completeReport(reportId, totalRecords, ...)  │
│   1. findOrThrow(reportId)                   查找报表        │
│   2. report.complete(totalRecords, summary, data)             │
│      → 校验 status == GENERATING                             │
│      → status = COMPLETED                                    │
│   3. reportRepository.save(report)                           │
│   4. eventPublisher.publish(ReportCompletedEvent)             │
└──────────────────────────────────────────────────────────────┘
```

---

## 9. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `AnalyticsApplication.java` | Bootstrap | 启动类 | Spring Boot 入口 |
| 2 | `config/JpaConfig.java` | Config | 配置 | 启用 JPA 审计 |
| 3 | `domain/model/AnalyticsReport.java` | Domain | **聚合根** | 报表生命周期管理 |
| 4 | `domain/model/TrendAnalysis.java` | Domain | **值对象** | 趋势计算与分析 |
| 5 | `domain/model/enums/ReportStatus.java` | Domain | 枚举 | 报表状态枚举 |
| 6 | `domain/model/enums/ReportType.java` | Domain | 枚举 | 报表类型枚举 |
| 7 | `domain/model/enums/ReportPeriod.java` | Domain | 枚举 | 报表周期枚举 |
| 8 | `domain/model/enums/DashboardType.java` | Domain | 枚举 | 仪表盘类型枚举 |
| 9 | `domain/event/ReportCreatedEvent.java` | Domain | 领域事件 | 报表已创建/重新生成 |
| 10 | `domain/event/ReportCompletedEvent.java` | Domain | 领域事件 | 报表已完成 |
| 11 | `domain/event/ReportFailedEvent.java` | Domain | 领域事件 | 报表生成失败 |
| 12 | `domain/event/ReportCancelledEvent.java` | Domain | 领域事件 | 报表已取消 |
| 13 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 14 | `domain/exception/ReportNotFoundException.java` | Domain | 异常 | 报表不存在 |
| 15 | `domain/exception/DashboardNotFoundException.java` | Domain | 异常 | 仪表盘不存在 |
| 16 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作 |
| 17 | `domain/repository/AnalyticsReportRepository.java` | Domain | 仓储接口 | 报表数据访问 |
| 18 | `domain/service/AnalyticsService.java` | Domain | **领域服务** | 报表业务编排 |
| 19 | `application/service/AnalyticsApplicationService.java` | Application | **应用服务** | 报表用例编排 |
| 20 | `application/command/CreateReportCommand.java` | Application | 命令 | 创建报表入参 |
| 21 | `application/command/CompleteReportCommand.java` | Application | 命令 | 完成报表入参 |
| 22 | `application/command/FailReportCommand.java` | Application | 命令 | 标记失败入参 |
| 23 | `application/command/CancelReportCommand.java` | Application | 命令 | 取消报表入参 |
| 24 | `application/command/RegenerateReportCommand.java` | Application | 命令 | 重新生成入参 |
| 25 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 26 | `application/dto/ReportDTO.java` | Application | DTO | 报表数据传输 |
| 27 | `interfaces/rest/AnalyticsController.java` | Interfaces | REST 控制器 | 报表 API |
| 28 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：28 个源文件**
