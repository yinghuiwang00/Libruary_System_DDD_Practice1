# 图书馆管理系统 —— 测试报告与业务功能手册

> **生成日期**: 2026-05-30
> **项目**: Library Management System (DDD Practice)
> **技术栈**: Java 17 + Spring Boot 3.2.5 + Maven 多模块

---

## 一、测试总览

| 模块 | 端口 | 源文件 | 测试文件 | 测试数 | 通过 | 失败 | 错误 | BDD 特性 | 构建状态 |
|------|------|--------|----------|--------|------|------|------|----------|----------|
| library-catalog | 8081 | 49 | 29 | 334 | 334 | 0 | 0 | 4 | ✅ SUCCESS |
| library-inventory | 8082 | 40 | 11 | 82 | 82 | 0 | 0 | 3 | ✅ SUCCESS |
| library-circulation | 8083 | 44 | 9 | 62 | 62 | 0 | 0 | 1 | ✅ SUCCESS |
| library-patron | 8084 | 34 | 8 | 156 | 156 | 0 | 0 | 1 | ✅ SUCCESS |
| library-payment | 8085 | 34 | 8 | 138 | 138 | 0 | 0 | 1 | ✅ SUCCESS |
| library-analytics | 8086 | 28 | 8 | 133 | 133 | 0 | 0 | 1 | ✅ SUCCESS |
| library-notification | 8087 | 29 | 4 | 109 | 109 | 0 | 0 | 1 | ✅ SUCCESS |
| library-shared | - | 18 | 2 | 84 | 84 | 0 | 0 | - | ✅ SUCCESS |
| **合计** | - | **276** | **79** | **1098** | **1098** | **0** | **0** | **12** | **全部通过** |

**覆盖率（已有 JaCoCo 报告的模块）：**

| 模块 | 行覆盖率 | 分支覆盖率 |
|------|----------|------------|
| library-catalog | 80.7% (634/786) | 78.1% (178/228) |
| library-inventory | 83.5% (583/698) | 70.8% (109/154) |

---

## 二、各模块业务功能与 API 使用

---

### 2.1 library-catalog —— 编目上下文

**端口**: 8081 | **基础路径**: `/api/catalog/`

**业务简介**：管理图书的核心元数据——图书、作者、出版社、分类。图书拥有完整的生命周期（草稿→发布→取消发布→删除），支持多作者关联和树形分类体系。

#### 图书管理 (`/api/catalog/books`)

| 方法 | 路径 | 功能 | 请求体示例 |
|------|------|------|-----------|
| `POST` | `/api/catalog/books` | 创建图书（默认 DRAFT 状态） | `{"isbn":"9787111407010","title":"设计模式"}` |
| `GET` | `/api/catalog/books` | 获取所有图书 | - |
| `GET` | `/api/catalog/books/{id}` | 按 ID 获取图书 | - |
| `PUT` | `/api/catalog/books/{id}` | 更新图书基本信息 | `{"title":"新标题"}` |
| `POST` | `/api/catalog/books/{id}/publish` | 发布图书（需有作者+出版社+分类） | - |
| `POST` | `/api/catalog/books/{id}/unpublish` | 取消发布 | - |
| `DELETE` | `/api/catalog/books/{id}` | 删除图书（不能删除已发布的） | - |
| `GET` | `/api/catalog/books/search?title=xxx&status=PUBLISHED` | 多条件搜索（支持分页） | - |
| `POST` | `/api/catalog/books/{id}/authors?authorId=xxx&role=AUTHOR` | 添加作者 | - |
| `DELETE` | `/api/catalog/books/{id}/authors/{authorId}` | 移除作者 | - |
| `PUT` | `/api/catalog/books/{id}/publisher?publisherId=xxx` | 设置出版社 | - |
| `POST` | `/api/catalog/books/{id}/categories?categoryId=xxx` | 添加分类 | - |
| `DELETE` | `/api/catalog/books/{id}/categories/{categoryId}` | 移除分类 | - |

**图书生命周期：**
```
DRAFT ──publish──→ PUBLISHED ──unpublish──→ UNPUBLISHED
  │                                             │
  └──delete──→ DELETED              ──publish──→ PUBLISHED

发布条件：必须有 ≥1 个作者 + 1 个出版社 + 1 个分类
删除条件：不能是 PUBLISHED 状态
```

#### 作者管理 (`/api/catalog/authors`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/catalog/authors` | 创建作者 |
| `GET` | `/api/catalog/authors` | 获取所有作者 |
| `GET` | `/api/catalog/authors/{id}` | 按 ID 获取作者 |
| `PUT` | `/api/catalog/authors/{id}` | 更新作者信息 |
| `DELETE` | `/api/catalog/authors/{id}` | 删除作者 |

#### 分类管理 (`/api/catalog/categories`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/catalog/categories` | 创建根分类 |
| `POST` | `/api/catalog/categories/{parentId}/children` | 添加子分类 |
| `GET` | `/api/catalog/categories` | 获取所有分类（树形） |
| `GET` | `/api/catalog/categories/roots` | 仅获取根分类 |
| `GET` | `/api/catalog/categories/{id}` | 按 ID 获取分类 |
| `PUT` | `/api/catalog/categories/{id}` | 更新分类 |
| `DELETE` | `/api/catalog/categories/{id}` | 删除分类 |

#### 出版社管理 (`/api/catalog/publishers`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/catalog/publishers` | 创建出版社 |
| `GET` | `/api/catalog/publishers` | 获取所有出版社 |
| `GET` | `/api/catalog/publishers/{id}` | 按 ID 获取出版社 |
| `GET` | `/api/catalog/publishers/search?name=xxx` | 按名称搜索（分页） |
| `PUT` | `/api/catalog/publishers/{id}` | 更新出版社 |
| `DELETE` | `/api/catalog/publishers/{id}` | 删除出版社 |

**使用示例：**
```bash
# 1. 创建作者
curl -X POST http://localhost:8081/api/catalog/authors \
  -H "Content-Type: application/json" \
  -d '{"name":"Erich Gamma","nationality":"Swiss"}'

# 2. 创建出版社
curl -X POST http://localhost:8081/api/catalog/publishers \
  -H "Content-Type: application/json" \
  -d '{"name":"机械工业出版社"}'

# 3. 创建分类
curl -X POST http://localhost:8081/api/catalog/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"软件工程"}'

# 4. 创建图书
curl -X POST http://localhost:8081/api/catalog/books \
  -H "Content-Type: application/json" \
  -d '{"isbn":"9787111407010","title":"设计模式：可复用面向对象软件的基础","language":"zh"}'

# 5. 关联作者、出版社、分类（假设 ID 已知）
curl -X POST "http://localhost:8081/api/catalog/books/{bookId}/authors?authorId={authorId}&role=AUTHOR"
curl -X PUT "http://localhost:8081/api/catalog/books/{bookId}/publisher?publisherId={publisherId}"
curl -X POST "http://localhost:8081/api/catalog/books/{bookId}/categories?categoryId={categoryId}"

# 6. 发布图书
curl -X POST http://localhost:8081/api/catalog/books/{bookId}/publish
```

---

### 2.2 library-inventory —— 馆藏上下文

**端口**: 8082 | **基础路径**: `/api/inventory/`

**业务简介**：管理图书馆的分馆、馆藏副本和库存。支持多分馆体系、副本批量入库、借出/归还/损坏/丢失状态流转。

#### 库存管理 (`/api/inventory/inventories`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/inventory/inventories` | 创建库存记录 |
| `POST` | `/api/inventory/inventories/{inventoryId}/copies` | 添加单本副本 |
| `POST` | `/api/inventory/inventories/{inventoryId}/copies/batch` | 批量添加副本 |
| `POST` | `/api/inventory/copies/{copyId}/checkout` | 借出副本 |
| `POST` | `/api/inventory/copies/{copyId}/return` | 归还副本 |
| `POST` | `/api/inventory/copies/{copyId}/damage` | 标记损坏 |
| `POST` | `/api/inventory/copies/{copyId}/loss` | 标记丢失 |
| `GET` | `/api/inventory/inventories/{inventoryId}` | 查看某馆库存 |
| `GET` | `/api/inventory/books/{bookId}/overview` | 查看某书在各馆库存概览 |

#### 图书馆管理 (`/api/inventory/libraries`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/inventory/libraries` | 注册新分馆 |
| `GET` | `/api/inventory/libraries` | 获取所有分馆 |
| `GET` | `/api/inventory/libraries/active` | 获取活跃分馆 |
| `GET` | `/api/inventory/libraries/{libraryId}` | 按 ID 获取分馆 |
| `PUT` | `/api/inventory/libraries/{libraryId}` | 更新分馆信息 |
| `POST` | `/api/inventory/libraries/{libraryId}/activate` | 激活分馆 |
| `POST` | `/api/inventory/libraries/{libraryId}/deactivate` | 停用分馆 |

**副本状态流转：**
```
AVAILABLE ──checkout──→ BORROWED ──return──→ AVAILABLE
    │                       │
    │                  ──damage──→ DAMAGED
    │
    └──loss──→ LOST
```

**使用示例：**
```bash
# 1. 注册分馆
curl -X POST http://localhost:8082/api/inventory/libraries \
  -H "Content-Type: application/json" \
  -d '{"name":"总馆","address":"北京市朝阳区"}'

# 2. 创建库存
curl -X POST http://localhost:8082/api/inventory/inventories \
  -H "Content-Type: application/json" \
  -d '{"libraryId":"{libraryId}","bookId":"{bookId}"}'

# 3. 批量添加副本
curl -X POST http://localhost:8082/api/inventory/inventories/{inventoryId}/copies/batch \
  -H "Content-Type: application/json" \
  -d '{"copyIds":["copy1","copy2","copy3"]}'

# 4. 借出副本
curl -X POST http://localhost:8082/api/inventory/copies/{copyId}/checkout
```

---

### 2.3 library-circulation —— 借阅上下文

**端口**: 8083 | **基础路径**: `/api/circulation/`

**业务简介**：处理图书借阅、归还、续借、催还、预约（Hold）的核心业务流程。包含罚款计算和借阅策略（CirculationPolicy）。

#### 借阅管理 (`/api/circulation/loans`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/circulation/loans` | 创建借阅记录 |
| `POST` | `/api/circulation/loans/{loanId}/return` | 归还图书 |
| `POST` | `/api/circulation/loans/{loanId}/renew` | 续借 |
| `POST` | `/api/circulation/loans/{loanId}/recall` | 催还 |
| `GET` | `/api/circulation/loans/{loanId}` | 查看借阅详情 |
| `GET` | `/api/circulation/patrons/{patronId}/loans` | 查看会员当前借阅 |
| `GET` | `/api/circulation/patrons/{patronId}/loans/history` | 查看会员借阅历史 |

**借阅生命周期：**
```
ACTIVE ──return──→ RETURNED
  │
  ├──renew──→ RENEWED (延长借期)
  │
  └──recall──→ RECALLED (被催还，需尽快归还)

逾期时自动生成罚款（Fine）
```

#### 预约管理 (`/api/circulation/holds`)

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/circulation/holds` | 预约图书 |
| `DELETE` | `/api/circulation/holds/{holdId}` | 取消预约 |
| `GET` | `/api/circulation/holds/{holdId}` | 查看预约详情 |
| `GET` | `/api/circulation/patrons/{patronId}/holds` | 查看会员的预约 |
| `GET` | `/api/circulation/books/{bookId}/holds` | 查看图书的预约队列 |

**预约生命周期：**
```
PENDING ──fulfill──→ FULFILLED (书到了，已取书)
  │
  ├──cancel──→ CANCELLED
  │
  └──超时──→ EXPIRED
```

**使用示例：**
```bash
# 1. 借书
curl -X POST http://localhost:8083/api/circulation/loans \
  -H "Content-Type: application/json" \
  -d '{"patronId":"p001","bookId":"b001","copyId":"c001","libraryId":"l001"}'

# 2. 续借
curl -X POST http://localhost:8083/api/circulation/loans/{loanId}/renew

# 3. 归还
curl -X POST http://localhost:8083/api/circulation/loans/{loanId}/return

# 4. 预约图书
curl -X POST http://localhost:8083/api/circulation/holds \
  -H "Content-Type: application/json" \
  -d '{"patronId":"p001","bookId":"b001"}'
```

---

### 2.4 library-patron —— 会员上下文

**端口**: 8084 | **基础路径**: `/api/patrons/`

**业务简介**：管理图书馆会员的注册、会员类型、借阅权限、账户状态和罚款记录。支持自动状态恢复（罚款缴清后自动解除暂停）。

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/patrons` | 注册新会员 |
| `GET` | `/api/patrons` | 获取所有会员 |
| `GET` | `/api/patrons/{id}` | 查看会员详情 |
| `PUT` | `/api/patrons/{id}` | 更新会员信息 |
| `PUT` | `/api/patrons/{id}/type` | 变更会员类型（REGULAR/PREMIUM/STUDENT/VIP） |
| `POST` | `/api/patrons/{id}/suspend` | 暂停会员 |
| `POST` | `/api/patrons/{id}/reactivate` | 重新激活会员 |
| `POST` | `/api/patrons/{id}/terminate` | 终止会员 |
| `POST` | `/api/patrons/{id}/extend-membership` | 延长会员有效期 |
| `POST` | `/api/patrons/{id}/fines` | 记录罚款 |
| `POST` | `/api/patrons/{id}/fines/pay` | 缴纳罚款（自动检测状态恢复） |
| `POST` | `/api/patrons/{id}/fines/waive` | 豁免罚款 |

**会员类型与权限：**

| 类型 | 最大借阅数 | 借阅天数 | 最大预约数 | 可续借次数 |
|------|-----------|----------|-----------|-----------|
| REGULAR | 5 | 30 | 3 | 2 |
| STUDENT | 10 | 60 | 5 | 3 |
| PREMIUM | 15 | 45 | 8 | 4 |
| VIP | 20 | 90 | 10 | 5 |

**会员状态：**
```
ACTIVE ──suspend──→ SUSPENDED ──reactivate──→ ACTIVE
  │                     │
  └──terminate──→ TERMINATED   ──terminate──→ TERMINATED
```

**使用示例：**
```bash
# 1. 注册会员
curl -X POST http://localhost:8084/api/patrons \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","email":"zhang@example.com","phone":"13800000000","type":"REGULAR"}'

# 2. 升级为高级会员
curl -X PUT http://localhost:8084/api/patrons/{id}/type \
  -H "Content-Type: application/json" \
  -d '{"type":"PREMIUM"}'

# 3. 记录罚款
curl -X POST http://localhost:8084/api/patrons/{id}/fines \
  -H "Content-Type: application/json" \
  -d '{"amount":5.00,"reason":"逾期归还"}'

# 4. 缴纳罚款
curl -X POST http://localhost:8084/api/patrons/{id}/fines/pay \
  -H "Content-Type: application/json" \
  -d '{"amount":5.00}'
```

---

### 2.5 library-payment —— 支付上下文

**端口**: 8085 | **基础路径**: `/api/payments/`

**业务简介**：处理罚款缴纳、退款等支付流程。支持完整的支付状态机和退款工作流。

#### 支付管理

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/payments` | 创建支付单 |
| `POST` | `/api/payments/{id}/process` | 开始处理支付 |
| `POST` | `/api/payments/{id}/complete` | 完成支付 |
| `POST` | `/api/payments/{id}/fail` | 支付失败 |
| `POST` | `/api/payments/{id}/cancel` | 取消支付 |
| `GET` | `/api/payments/{id}` | 查看支付详情 |
| `GET` | `/api/payments` | 查看所有支付（可按 patronId/状态 过滤） |
| `POST` | `/api/payments/{id}/refunds` | 申请退款 |
| `POST` | `/api/payments/refunds/{id}/process` | 处理退款 |
| `POST` | `/api/payments/refunds/{id}/complete` | 完成退款 |
| `GET` | `/api/payments/{id}/refunds` | 查看支付关联的退款 |

**支付状态机：**
```
PENDING ──process──→ PROCESSING ──complete──→ COMPLETED
  │                     │
  └──cancel──→ CANCELLED    └──fail──→ FAILED
```

**退款状态机：**
```
REQUESTED ──process──→ PROCESSING ──complete──→ COMPLETED
                          │
                          └──fail──→ FAILED
```

**使用示例：**
```bash
# 1. 创建支付单
curl -X POST http://localhost:8085/api/payments \
  -H "Content-Type: application/json" \
  -d '{"patronId":"p001","amount":5.00,"paymentMethod":"CREDIT_CARD"}'

# 2. 处理支付
curl -X POST http://localhost:8085/api/payments/{id}/process

# 3. 完成支付
curl -X POST http://localhost:8085/api/payments/{id}/complete

# 4. 申请退款
curl -X POST http://localhost:8085/api/payments/{id}/refunds \
  -H "Content-Type: application/json" \
  -d '{"amount":5.00,"reason":"误缴"}'
```

---

### 2.6 library-analytics —— 分析上下文

**端口**: 8086 | **基础路径**: `/api/analytics/`

**业务简介**：生成和管理统计分析报告，包括借阅趋势分析、热门图书统计、会员活跃度等。支持报告的异步生成和重新生成。

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/analytics/reports` | 创建分析报告 |
| `GET` | `/api/analytics/reports` | 查看报告列表（可按类型/状态过滤） |
| `GET` | `/api/analytics/reports/{id}` | 查看报告详情 |
| `POST` | `/api/analytics/reports/{id}/complete` | 标记报告完成 |
| `POST` | `/api/analytics/reports/{id}/fail` | 标记报告生成失败 |
| `POST` | `/api/analytics/reports/{id}/cancel` | 取消报告生成 |
| `POST` | `/api/analytics/reports/{id}/regenerate` | 重新生成报告 |

**报告类型**：`BORROWING_TREND`（借阅趋势）、`POPULAR_BOOKS`（热门图书）、`PATRON_ACTIVITY`（会员活跃度）、`LIBRARY_PERFORMANCE`（图书馆绩效）

**报告状态机：**
```
GENERATING ──complete──→ COMPLETED
    │           │
    ├──fail──→ FAILED     ──regenerate──→ GENERATING
    │
    └──cancel──→ CANCELLED
```

**使用示例：**
```bash
# 1. 创建借阅趋势报告
curl -X POST http://localhost:8086/api/analytics/reports \
  -H "Content-Type: application/json" \
  -d '{"type":"BORROWING_TREND","startDate":"2026-01-01","endDate":"2026-05-30"}'

# 2. 查看所有已完成报告
curl "http://localhost:8086/api/analytics/reports?type=BORROWING_TREND&status=COMPLETED"
```

---

### 2.7 library-notification —— 通知上下文

**端口**: 8087 | **基础路径**: `/api/notifications/`

**业务简介**：管理到期提醒、预约通知、系统公告等多渠道通知。支持 EMAIL、SMS、PUSH、IN_APP 四种通知渠道，以及通知的调度、发送、重试等完整生命周期。

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/notifications` | 创建通知 |
| `GET` | `/api/notifications` | 查看通知列表（可按收件人/渠道/状态过滤） |
| `GET` | `/api/notifications/{id}` | 查看通知详情 |
| `POST` | `/api/notifications/{id}/schedule` | 调度通知 |
| `POST` | `/api/notifications/{id}/send` | 发送通知 |
| `POST` | `/api/notifications/{id}/deliver` | 确认送达 |
| `PUT` | `/api/notifications/{id}/read` | 标记已读 |
| `POST` | `/api/notifications/{id}/fail` | 标记发送失败 |
| `POST` | `/api/notifications/{id}/retry` | 重试发送 |
| `POST` | `/api/notifications/{id}/cancel` | 取消通知 |

**通知渠道**：`EMAIL`、`SMS`、`PUSH`、`IN_APP`

**通知状态机：**
```
PENDING ──schedule──→ SCHEDULED ──send──→ SENDING ──deliver──→ DELIVERED ──read──→ READ
                       │                    │
                       │                    └──fail──→ FAILED ──retry──→ SENDING
                       │
                       └──cancel──→ CANCELLED
```

**使用示例：**
```bash
# 1. 创建到期提醒
curl -X POST http://localhost:8087/api/notifications \
  -H "Content-Type: application/json" \
  -d '{"recipientId":"p001","channel":"EMAIL","subject":"图书到期提醒", \
       "content":"您借阅的《设计模式》将于3天后到期","type":"DUE_REMINDER"}'

# 2. 调度并发送
curl -X POST http://localhost:8087/api/notifications/{id}/schedule
curl -X POST http://localhost:8087/api/notifications/{id}/send
```

---

### 2.8 library-shared —— 共享内核

**端口**: 无（工具模块）| **不独立运行**

**业务简介**：所有 bounded context 共享的领域基础概念，包括：

| 类别 | 内容 |
|------|------|
| **ID 值对象** | `AggregateId` 基类 + `BookId`, `AuthorId`, `PublisherId`, `CategoryId`, `LibraryId`, `CopyId`, `CopyInventoryId`, `PatronId`, `LoanId`, `HoldId`, `FineId`, `ReportId`, `NotificationId`, `PaymentId`, `RefundId` |
| **领域事件** | `DomainEvent` 基类（eventId, eventType, occurredAt, version） + `DomainEventPublisher` 接口 |

**使用方式**：其他模块通过 Maven 依赖引入，直接使用强类型 ID 和事件基类。

---

## 三、跨模块业务流程

### 3.1 典型借阅流程

```
读者借书完整流程（跨 4 个模块）：

1. [catalog]  确认图书存在且已发布
2. [inventory] 确认有可用副本 → checkout
3. [patron]   验证会员状态、借阅权限、是否超额
4. [circulation] 创建 Loan 记录 → 返回借书成功
```

### 3.2 逾期罚款流程

```
1. [circulation] 检测到 Loan 逾期 → 计算罚款 → 发布事件
2. [patron]      接收事件 → 记录罚款到会员账户 → 可能暂停会员
3. [payment]     会员缴纳罚款 → 支付完成
4. [patron]      检测罚款已缴清 → 自动恢复会员状态
5. [notification] 发送罚款通知/缴费确认
```

### 3.3 通知触发流程

```
1. [circulation] 图书即将到期 → 发布 DomainEvent
2. [notification] 消费事件 → 创建到期提醒 → 调度 → 发送邮件/短信
3. [analytics]   消费事件 → 更新借阅统计数据
```

---

## 四、API 通用约定

### 4.1 统一响应格式

所有接口返回统一的 JSON 信封：

```json
// 成功
{
  "success": true,
  "data": { ... },
  "error": null,
  "errorCode": null
}

// 失败
{
  "success": false,
  "data": null,
  "error": "Book not found: xxx",
  "errorCode": "BOOK_NOT_FOUND"
}
```

### 4.2 常见错误码

| 错误码 | HTTP 状态 | 含义 |
|--------|----------|------|
| `*_NOT_FOUND` | 404 | 资源不存在 |
| `DUPLICATE_*` | 409 | 资源重复 |
| `INVALID_ISBN` | 400 | ISBN 无效 |
| `INVALID_OPERATION` | 400 | 状态不允许的操作 |
| `VALIDATION_ERROR` | 400 | 请求参数验证失败 |
| `INVALID_ARGUMENT` | 400 | 参数非法 |

### 4.3 API 文档

各模块启动后访问 Swagger UI：

| 模块 | Swagger 地址 |
|------|-------------|
| Catalog | http://localhost:8081/swagger-ui.html |
| Inventory | http://localhost:8082/swagger-ui.html |
| Circulation | http://localhost:8083/swagger-ui.html |
| Patron | http://localhost:8084/swagger-ui.html |
| Payment | http://localhost:8085/swagger-ui.html |
| Analytics | http://localhost:8086/swagger-ui.html |
| Notification | http://localhost:8087/swagger-ui.html |

---

## 五、快速启动指南

### 5.1 启动基础设施

确保 PostgreSQL、Redis、Kafka 已运行（参见 CLAUDE.md 中的连接信息）。

### 5.2 启动所有服务

```bash
# 在不同终端分别启动
cd library-catalog && mvn spring-boot:run        # 端口 8081
cd library-inventory && mvn spring-boot:run       # 端口 8082
cd library-circulation && mvn spring-boot:run     # 端口 8083
cd library-patron && mvn spring-boot:run          # 端口 8084
cd library-payment && mvn spring-boot:run         # 端口 8085
cd library-analytics && mvn spring-boot:run       # 端口 8086
cd library-notification && mvn spring-boot:run    # 端口 8087
```

### 5.3 运行全部测试

```bash
# 在项目根目录
mvn clean test           # 运行所有模块的测试
mvn clean verify         # 运行测试 + 生成覆盖率报告
```
