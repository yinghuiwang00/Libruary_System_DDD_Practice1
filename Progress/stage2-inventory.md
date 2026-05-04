# Stage 2: Inventory Context (馆藏上下文) - 完成总结

## 概览

Inventory Context 实现了图书馆多分馆馆藏管理的核心功能，包括分馆管理、馆藏记录创建、图书副本管理及借还流程。

## 完成日期

2026-05-04

## 代码统计

- **新增文件**: ~50个Java源文件 + 3个Cucumber feature文件 + 1个SQL migration
- **代码行数**: ~3000行
- **测试总数**: 65个 (55 unit + 7 integration + 3 Cucumber BDD)
- **测试通过率**: 100%

## 各层实现总结

### Domain Layer (领域层)

| 组件 | 文件 | 说明 |
|------|------|------|
| Aggregate Root | `CopyInventory` | 馆藏聚合根，管理副本集合，跟踪可用数量 |
| Entity | `Library` | 分馆实体，含联系信息、运营状态 |
| Entity | `BookCopy` | 副本实体，完整状态机 (7种状态转换) |
| Value Object | `Location` | 位置值对象，含楼层/区域/书架信息 |
| Domain Service | `InventoryManagementService` | 馆藏管理领域服务，协调聚合操作 |
| Domain Events | 4个事件 | CopyAdded/Borrowed/Returned, InventoryCreated |
| Exceptions | 6个异常 | 含错误码 (INV-001 ~ INV-006) |
| Repositories | 3个接口 | CopyInventory/BookCopy/Library |

### Application Layer (应用层)

| 组件 | 文件 | 说明 |
|------|------|------|
| Application Service | `InventoryApplicationService` | 馆藏应用服务，编排领域操作 |
| Application Service | `LibraryApplicationService` | 分馆CRUD服务 |
| Commands | 4个命令 | CreateInventory/AddCopy/BatchAdd/CreateLibrary |
| DTOs | 4个DTO | LibraryDTO/BookCopyDTO/CopyInventoryDTO/ApiResponse |

### Interface Layer (接口层)

| 组件 | 文件 | 说明 |
|------|------|------|
| Controller | `InventoryController` | 馆藏和副本操作REST API |
| Controller | `LibraryController` | 分馆管理REST API |
| Exception Handler | `GlobalExceptionHandler` | 全局异常处理 |

### Infrastructure Layer (基础设施层)

| 组件 | 文件 | 说明 |
|------|------|------|
| Config | `JpaConfig` | JPA审计配置 |
| Database | `V2__Create_Inventory_Schema.sql` | 3张表 + 索引 |
| Application | `InventoryApplication` | Spring Boot启动类 |

### Shared Module (共享模块)

| 组件 | 文件 | 说明 |
|------|------|------|
| ID Classes | 3个ID | LibraryId/CopyId/CopyInventoryId |

## 测试覆盖

### Unit Tests (55个)
- `LocationTest`: 7个 - 值对象创建、验证、相等性
- `LibraryTest`: 10个 - 实体创建、更新、激活/停用
- `BookCopyTest`: 22个 - 完整状态机转换、验证、边界情况
- `CopyInventoryTest`: 16个 - 副本增删、可用性跟踪、低库存

### Integration Tests (7个)
- `InventoryIntegrationTest`: 7个 - 完整API流程（创建分馆、创建馆藏、借出、归还、报损、防重复）

### Functional Tests (3个 Cucumber BDD)
- `library-management.feature`: 分馆创建 happy path
- `inventory-creation.feature`: 馆藏初始化 happy path
- `copy-checkout-return.feature`: 借还流程 happy path

## 关键设计决策

1. **CopyInventory作为聚合根** - 管理一组BookCopy，确保一致性边界
2. **BookCopy状态机** - 7种状态 (AVAILABLE/BORROWED/RESERVED/DAMAGED/UNDER_REPAIR/LOST/REMOVED)
3. **乐观锁** - 所有聚合使用@Version防止并发冲突
4. **领域事件** - 副本增删、借还均发布事件，为跨上下文集成做准备
5. **低库存阈值** - availableCopies <= 2 标记为低库存

## 已知限制

- 事件发布仅在同一JVM内，尚未集成消息队列
- 外部服务集成（如ISBN验证）留待后续阶段
- 缓存策略待实现
