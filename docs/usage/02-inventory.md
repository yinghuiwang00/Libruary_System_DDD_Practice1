# 📦 库存管理模块 (Inventory)

> **端口**: 8082 | **API 前缀**: `/api/inventory` | **数据库**: `library_inventory`

## 概述

Inventory bounded context 负责管理图书馆分馆、书籍副本和库存数量。它跟踪每本物理副本的状态（可用/借出/损坏/遗失），并在库存不足时发出预警。

---

## API 一览

### 图书馆管理 (LibraryController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/inventory/libraries` | 创建图书馆 |
| GET | `/api/inventory/libraries` | 查询所有图书馆 |
| GET | `/api/inventory/libraries/active` | 查询活跃图书馆 |
| GET | `/api/inventory/libraries/{libraryId}` | 查询图书馆详情 |
| PUT | `/api/inventory/libraries/{libraryId}` | 更新图书馆信息 |
| POST | `/api/inventory/libraries/{libraryId}/deactivate` | 停用图书馆 |
| POST | `/api/inventory/libraries/{libraryId}/activate` | 启用图书馆 |

### 库存管理 (InventoryController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/inventory/inventories` | 创建库存 |
| POST | `/api/inventory/inventories/{inventoryId}/copies` | 添加单个副本 |
| POST | `/api/inventory/inventories/{inventoryId}/copies/batch` | 批量添加副本 |
| GET | `/api/inventory/inventories/{inventoryId}` | 查询库存详情 |
| GET | `/api/inventory/books/{bookId}/overview` | 查询书籍各馆库存概况 |

### 副本状态变更

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/inventory/copies/{copyId}/checkout` | 借出副本 |
| POST | `/api/inventory/copies/{copyId}/return` | 归还副本 |
| POST | `/api/inventory/copies/{copyId}/damage` | 报告损坏 |
| POST | `/api/inventory/copies/{copyId}/loss` | 报告遗失 |

---

## 详细用法

### 创建图书馆并添加库存（典型流程）

#### 1. 创建图书馆

```bash
curl -X POST http://localhost:8082/api/inventory/libraries \
  -H "Content-Type: application/json" \
  -d '{
    "name": "City Central Library",
    "address": "123 Main St",
    "city": "Shanghai",
    "postalCode": "200000",
    "phone": "021-555-0100",
    "email": "info@citylib.cn"
  }'
```

响应：

```json
{
  "success": true,
  "data": {
    "id": "ee857b28-61af-4fff-ac93-d2a508933c38",
    "name": "City Central Library",
    "code": "LIB-001",
    "city": "Shanghai",
    "address": "123 Main St",
    "isActive": true
  }
}
```

> 记下 `id`，作为 `LIBRARY_ID`。

#### 2. 创建库存

```bash
curl -X POST http://localhost:8082/api/inventory/inventories \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": "<BOOK_ID from Catalog>",
    "libraryId": "<LIBRARY_ID>",
    "initialCopies": 0
  }'
```

> 记下 `id`，作为 `INVENTORY_ID`。

#### 3. 添加单个副本

```bash
curl -X POST http://localhost:8082/api/inventory/inventories/$INVENTORY_ID/copies \
  -H "Content-Type: application/json" \
  -d '{
    "copyNumber": "COPY-001",
    "location": "Floor-2-Shelf-A",
    "condition": "NEW"
  }'
```

> 记下返回的 `id`，作为 `COPY_ID`。

#### 4. 批量添加副本

```bash
curl -X POST http://localhost:8082/api/inventory/inventories/$INVENTORY_ID/copies/batch \
  -H "Content-Type: application/json" \
  -d '{
    "copyCount": 5,
    "copyNumberPrefix": "CC-",
    "location": "Floor-2-Shelf-A",
    "condition": "NEW"
  }'
```

一次性添加 5 个副本，编号为 CC-1、CC-2、CC-3、CC-4、CC-5。

### 查看库存

```bash
# 查看某个库存详情
curl http://localhost:8082/api/inventory/inventories/$INVENTORY_ID

# 查看书籍在所有图书馆的库存概况
curl http://localhost:8082/api/inventory/books/$BOOK_ID/overview
```

### 副本状态变更

```bash
# 借出（配合 Circulation 模块使用）
curl -X POST http://localhost:8082/api/inventory/copies/$COPY_ID/checkout

# 归还
curl -X POST http://localhost:8082/api/inventory/copies/$COPY_ID/return

# 报告损坏
curl -X POST http://localhost:8082/api/inventory/copies/$COPY_ID/damage \
  -H "Content-Type: application/json" \
  -d '{"description": "Cover torn, pages 10-15 water damaged"}'

# 报告遗失
curl -X POST http://localhost:8082/api/inventory/copies/$COPY_ID/loss \
  -H "Content-Type: application/json" \
  -d '{"reason": "Missing for 30 days, presumed lost"}'
```

### 图书馆管理

```bash
# 查询活跃图书馆
curl http://localhost:8082/api/inventory/libraries/active

# 更新图书馆信息
curl -X PUT http://localhost:8082/api/inventory/libraries/$LIBRARY_ID \
  -H "Content-Type: application/json" \
  -d '{
    "name": "City Central Library - Renovated",
    "address": "456 New Main St",
    "city": "Shanghai",
    "postalCode": "200000",
    "phone": "021-555-0200",
    "email": "info@citylib.cn"
  }'

# 停用图书馆
curl -X POST http://localhost:8082/api/inventory/libraries/$LIBRARY_ID/deactivate

# 重新启用
curl -X POST http://localhost:8082/api/inventory/libraries/$LIBRARY_ID/activate
```

---

## 业务规则

- **副本状态流转**:
  - 正常循环: `AVAILABLE` → `BORROWED` → `AVAILABLE`
  - 损坏修复: `AVAILABLE` → `DAMAGED` → `UNDER_REPAIR` → `AVAILABLE`
  - 终态: `LOST`（遗失）、`REMOVED`（移除）
- **副本条件**: `NEW`（全新）、`GOOD`（良好）、`DAMAGED`（已损坏）
- **库存预警**: 可用副本低于阈值时触发 `LowStockAlertEvent`
- **删除限制**: 有活跃借阅的副本不能删除
- **图书馆编码**: 自动生成（如 LIB-001、LIB-002）

---

## Kafka 事件

### 发布事件（8 个）

| 事件 | 触发时机 |
|------|---------|
| CopyAddedEvent | 添加单个副本 |
| CopiesBatchAddedEvent | 批量添加副本 |
| CopyBorrowedEvent | 副本借出 |
| CopyReturnedEvent | 副本归还 |
| CopyDamagedEvent | 副本损坏报告 |
| CopyLostEvent | 副本遗失报告 |
| InventoryCreatedEvent | 创建库存 |
| LowStockAlertEvent | 库存低于阈值 |

### 消费事件（2 个）

| 事件 | 来源 | 处理逻辑 |
|------|------|---------|
| BookPublishedEvent | Catalog | 新书发布时准备库存 |
| BookBorrowedEvent / BookReturnedEvent | Circulation | 同步副本可用数量 |

---

## 返回 [README](../../README.md)
