# DDD 实现说明文档索引

> 本目录包含企业级图书馆管理系统中 DDD（领域驱动设计）实现模式的完整说明文档。
> 所有文档基于实际源码编写，反映项目当前实现状态。

## 📖 文档目录

### 跨领域模式（01-04）

| 编号 | 文件 | 内容 |
|------|------|------|
| 01 | [DDD分层架构实现.md](01-DDD分层架构实现.md) | 四层架构（domain/application/infrastructure/interfaces）的实现方式、依赖方向、各层文件统计 |
| 02 | [聚合根模式实现.md](02-聚合根模式实现.md) | 15 个聚合根的统一模式、16 个强类型 ID、值对象体系、乐观锁机制 |
| 03 | [事件驱动架构实现.md](03-事件驱动架构实现.md) | Kafka 双发模式、7 个发布者、12 个消费者、19 个事件处理器、上下文间通信图 |
| 04 | [测试策略实现.md](04-测试策略实现.md) | 四层测试金字塔、146 个测试类、37 个 BDD Feature、E2E 测试架构 |

### 限界上下文详解（05-12）

| 编号 | 文件 | 上下文 | 端口 | 源文件数 | 核心聚合根 |
|------|------|--------|------|---------|-----------|
| 05 | [Catalog编目上下文.md](05-Catalog编目上下文.md) | library-catalog | 8081 | 49 | Book, Author, Category, Publisher |
| 06 | [Inventory库存上下文.md](06-Inventory库存上下文.md) | library-inventory | 8082 | 46 | CopyInventory, BookCopy, Library |
| 07 | [Circulation流通上下文.md](07-Circulation流通上下文.md) | library-circulation | 8083 | 48 | Loan, Hold, Fine |
| 08 | [Patron读者上下文.md](08-Patron读者上下文.md) | library-patron | 8084 | 41 | Patron |
| 09 | [Payment支付上下文.md](09-Payment支付上下文.md) | library-payment | 8085 | 37 | Payment, Refund |
| 10 | [Analytics分析上下文.md](10-Analytics分析上下文.md) | library-analytics | 8086 | 33 | AnalyticsReport |
| 11 | [Notification通知上下文.md](11-Notification通知上下文.md) | library-notification | 8087 | 41 | Notification |
| 12 | [Shared共享内核.md](12-Shared共享内核.md) | library-shared | - | 23 | (无，提供基类和值对象) |

### 跨上下文集成（13）

| 编号 | 文件 | 内容 |
|------|------|------|
| 13 | [跨上下文集成实现.md](13-跨上下文集成实现.md) | 8 个集成事件流详解、Kafka Topic 配置、E2E 测试覆盖、容错设计 |

## 🏗️ 项目统计

| 指标 | 数值 |
|------|------|
| Maven 模块 | 10 |
| 主代码 Java 文件 | 318 |
| 测试 Java 文件 | 146 |
| BDD Feature 文件 | 37 |
| 聚合根 | 15 |
| 领域事件 | 46 |
| Kafka 消费者 | 12 |
| REST Controller | 12 |
| 领域异常类 | 38 |

## 📐 每个上下文文档的标准结构

每个限界上下文的文档都包含以下章节：

1. **模块概览** — 职责、端口、基础路径
2. **DDD 分层架构总览** — 完整文件树
3. **引导与配置层** — Application 类、JPA 配置
4. **领域层** — 聚合根、值对象、枚举、领域事件、领域异常、仓储接口、领域服务
5. **应用层** — 应用服务、命令对象、查询对象、DTO、事件处理器
6. **基础设施层** — Kafka 消费者/发布者、持久化实现
7. **接口层** — REST Controller、全局异常处理器
8. **层间调用流程** — 完整请求链路示例
9. **文件清单速查表** — 按层分类的完整文件列表

## 🔗 相关文档

- **架构设计**: `Architecture_Design/` — 限界上下文设计文档（02-08）、Spring 实现指南（10）、测试计划（15）
- **开发计划**: `DEVELOPMENT_PLAN.md` — 详细任务分解与进度跟踪
- **项目说明**: `CLAUDE.md` — 项目概览、构建命令、开发规范

> 文档数据截至 2026-05-31，基于实际源码统计
