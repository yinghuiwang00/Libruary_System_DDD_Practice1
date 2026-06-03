# 流通管理模块 (Circulation)

## 概述

Circulation bounded context 负责管理图书的借阅、归还、续借、召回、预约和逾期处理。端口 8083，API 前缀 `/api/circulation`。

该模块是图书馆系统的核心业务模块，围绕 `Loan`（借阅）和 `Hold`（预约）两个聚合根展开，通过 `CirculationPolicy`（流通策略）值对象控制借阅期限、续借次数上限和逾期罚金费率。

## API 一览

### 借阅管理 (LoanController)

| 方法   | 路径                                            | 说明             |
|--------|-------------------------------------------------|------------------|
| POST   | `/api/circulation/loans`                        | 借书             |
| POST   | `/api/circulation/loans/{loanId}/return`        | 还书             |
| POST   | `/api/circulation/loans/{loanId}/renew`         | 续借             |
| POST   | `/api/circulation/loans/{loanId}/recall`        | 召回（?reason=） |
| GET    | `/api/circulation/loans/{loanId}`               | 查询借阅详情     |
| GET    | `/api/circulation/patrons/{patronId}/loans`     | 查询读者当前借阅 |
| GET    | `/api/circulation/patrons/{patronId}/loans/history` | 查询读者借阅历史 |

### 预约管理 (HoldController)

| 方法   | 路径                                       | 说明                 |
|--------|--------------------------------------------|----------------------|
| POST   | `/api/circulation/holds`                   | 预约书籍             |
| DELETE | `/api/circulation/holds/{holdId}`          | 取消预约（?reason=） |
| GET    | `/api/circulation/holds/{holdId}`          | 查询预约详情         |
| GET    | `/api/circulation/patrons/{patronId}/holds` | 查询读者预约列表   |
| GET    | `/api/circulation/books/{bookId}/holds`    | 查询书籍预约队列     |

### 管理操作 (AdminController)

| 方法   | 路径                                    | 说明           |
|--------|-----------------------------------------|----------------|
| POST   | `/api/circulation/admin/process-overdue` | 处理逾期借阅  |

## 详细用法

### 借书

```bash
curl -X POST http://localhost:8083/api/circulation/loans \
  -H "Content-Type: application/json" \
  -d '{
    "copyId": "<copy-id>",
    "patronId": "<patron-id>",
    "bookId": "<book-id>"
  }'
```

请求体对应 `BorrowBookCommand`，三个字段均为必填（`@NotBlank`）。

成功响应（HTTP 201）：

```json
{
  "success": true,
  "data": {
    "id": "loan-uuid",
    "copyId": "copy-uuid",
    "patronId": "patron-uuid",
    "bookId": "book-uuid",
    "loanDate": "2026-06-01T10:00:00",
    "dueDate": "2026-07-31T10:00:00",
    "returnDate": null,
    "status": "ACTIVE",
    "renewalCount": 0,
    "fineAmount": null,
    "isRecalled": false,
    "createdAt": "2026-06-01T10:00:00"
  }
}
```

借阅期限由 `CirculationPolicy.loanPeriodDays` 决定，不同读者类型对应不同策略。

### 还书

```bash
curl -X POST http://localhost:8083/api/circulation/loans/{loanId}/return
```

成功后 `status` 变为 `RETURNED`，`returnDate` 填充当前时间。若归还日期晚于 `dueDate`，系统会自动计算逾期罚金并填入 `fineAmount`。

### 续借

```bash
curl -X POST http://localhost:8083/api/circulation/loans/{loanId}/renew
```

续借成功后 `dueDate` 延长一个 `loanPeriodDays` 周期，`renewalCount` 递增。续借次数受 `CirculationPolicy.maxRenewalsAllowed` 限制，超出后会返回业务异常。

### 召回

```bash
curl -X POST "http://localhost:8083/api/circulation/loans/{loanId}/recall?reason=Needed+for+course"
```

`reason` 为可选参数。召回后 `status` 变为 `RECALLED`，`dueDate` 可能被缩短为召回日期起 7 天（取原到期日与召回截止日中较早者）。

### 预约书籍

```bash
curl -X POST http://localhost:8083/api/circulation/holds \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": "<book-id>",
    "patronId": "<patron-id>",
    "pickupLibraryId": "<library-id>"
  }'
```

请求体对应 `PlaceHoldCommand`。`bookId` 和 `patronId` 必填（`@NotBlank`），`pickupLibraryId` 为可选字段，指定取书分馆。

成功响应（HTTP 201）：

```json
{
  "success": true,
  "data": {
    "id": "hold-uuid",
    "bookId": "book-uuid",
    "patronId": "patron-uuid",
    "requestDate": "2026-06-01T10:00:00",
    "expirationDate": null,
    "status": "WAITING",
    "queuePosition": 1,
    "fulfilledCopyId": null,
    "availableUntilDate": null,
    "pickupLibraryId": "library-uuid",
    "createdAt": "2026-06-01T10:00:00"
  }
}
```

### 取消预约

```bash
curl -X DELETE "http://localhost:8083/api/circulation/holds/{holdId}?reason=No+longer+needed"
```

`reason` 为可选参数。取消后 `status` 变为 `CANCELLED`。

### 查询书籍预约队列

```bash
curl http://localhost:8083/api/circulation/books/{bookId}/holds
```

返回该书所有预约记录，按 `queuePosition` 排序，可用于查看排队情况。

### 处理逾期

```bash
curl -X POST http://localhost:8083/api/circulation/admin/process-overdue
```

批量扫描所有活跃借阅，将超过 `dueDate` 的记录标记为 `OVERDUE`，计算逾期罚金，并发布 `OverdueNoticeEvent` 和 `FineIncurredEvent`。返回当前所有逾期借阅列表。

## 业务规则

### 借阅状态

枚举类 `LoanStatus`，定义了以下状态：

```
ACTIVE  -->  RETURNED     （正常归还）
ACTIVE  -->  OVERDUE      （超过 dueDate 未还）
ACTIVE  -->  RECALLED     （被召回）
ACTIVE  -->  RENEWED      （续借成功，实际状态回到 ACTIVE 并延长 dueDate）
ACTIVE  -->  CANCELLED    （取消）
ACTIVE  -->  LOST         （书籍丢失）
```

### 预约状态

枚举类 `HoldStatus`，定义了以下状态：

```
WAITING            -->  READY_FOR_PICKUP   （有可用副本分配）
WAITING            -->  CANCELLED           （读者主动取消）
WAITING            -->  EXPIRED             （预约过期）
READY_FOR_PICKUP   -->  FULFILLED           （读者取书）
READY_FOR_PICKUP   -->  EXPIRED_NOT_PICKED_UP （超时未取）
```

### 关键约束

- **借阅期限**：由 `CirculationPolicy.loanPeriodDays` 控制，不同读者类型可配置不同天数
- **续借限制**：受 `CirculationPolicy.maxRenewalsAllowed` 约束，有最大续借次数
- **逾期罚金**：按天计算，费率由 `CirculationPolicy.dailyFineRate` 决定，上限为 `maxFineAmount`
- **召回缩短**：召回后 dueDate 可能缩短（取原到期日与召回截止日的较早者）
- **预约排队**：预约支持排队机制（`queuePosition`），先到先得
- **预约超期**：预约到书后若未在规定时间内取书，状态变为 `EXPIRED_NOT_PICKED_UP`

## Kafka 事件

### 发布事件（14 个）

由 `CirculationDomainEventPublisher` 发布到 `library.circulation.events` topic：

| 事件名称                      | 触发时机               |
|-------------------------------|------------------------|
| `BookBorrowedEvent`           | 借书成功               |
| `BookReturnedEvent`           | 还书成功               |
| `LoanRenewedEvent`            | 续借成功               |
| `LoanRecalledEvent`           | 召回成功               |
| `LoanCancelledEvent`          | 借阅取消               |
| `HoldPlacedEvent`             | 预约创建               |
| `HoldFulfilledEvent`          | 预约可取（分配到副本） |
| `HoldPickedUpEvent`           | 读者取走预约书籍       |
| `HoldCancelledEvent`          | 预约取消               |
| `HoldExpiredEvent`            | 预约过期               |
| `HoldExpiredNotPickedUpEvent` | 预约到书后未取         |
| `FineIncurredEvent`           | 产生逾期罚金           |
| `OverdueNoticeEvent`          | 发送逾期通知           |
| `DueDateReminderEvent`        | 到期前提醒             |

### 消费事件

由 `PatronEventConsumer` 从 `library.patron.events` topic 消费：

| 事件名称                | 处理逻辑                         |
|-------------------------|----------------------------------|
| `PatronSuspendedEvent`  | 读者被暂停时，阻止其继续借阅/预约 |

其他未知事件类型以 DEBUG 级别忽略，不会抛出异常（防止 poison pill）。
