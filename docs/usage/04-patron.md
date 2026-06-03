# 读者管理模块 (Patron)

## 概述

Patron bounded context 负责管理图书馆会员，包括注册、信息管理、会员类型、状态变更和罚款管理。端口 8084，API 前缀 `/api/patrons`。

该模块围绕 `Patron`（会员）聚合根展开，管理会员的全生命周期：从注册、信息更新、类型变更，到状态管理（暂停/恢复/终止）、罚款记录和会籍续期。

## API 一览

### 会员管理 (PatronController)

| 方法 | 路径                                   | 说明         |
|------|----------------------------------------|--------------|
| POST | `/api/patrons`                         | 注册新会员   |
| GET  | `/api/patrons/{id}`                    | 查询会员     |
| GET  | `/api/patrons`                         | 查询所有会员 |
| PUT  | `/api/patrons/{id}`                    | 更新会员信息 |
| PUT  | `/api/patrons/{id}/type`               | 变更会员类型 |
| POST | `/api/patrons/{id}/suspend`            | 暂停会员     |
| POST | `/api/patrons/{id}/reactivate`         | 恢复会员     |
| POST | `/api/patrons/{id}/terminate`          | 终止会员     |

### 罚款管理

| 方法 | 路径                                | 说明     |
|------|-------------------------------------|----------|
| POST | `/api/patrons/{id}/fines`           | 添加罚款 |
| POST | `/api/patrons/{id}/fines/pay`       | 缴纳罚款 |
| POST | `/api/patrons/{id}/fines/waive`     | 减免罚款 |

### 会籍管理

| 方法 | 路径                                         | 说明     |
|------|----------------------------------------------|----------|
| POST | `/api/patrons/{id}/extend-membership`        | 续期会籍 |

## 详细用法

### 注册会员

```bash
curl -X POST http://localhost:8084/api/patrons \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Zhang",
    "lastName": "San",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "address": "456 Library Road",
    "city": "Beijing",
    "postalCode": "100000",
    "patronType": "STUDENT"
  }'
```

请求体对应 `RegisterPatronCommand`。`firstName`、`lastName`、`email`（需符合邮箱格式）和 `patronType` 为必填字段。

成功响应（HTTP 201）：

```json
{
  "success": true,
  "data": {
    "id": "patron-uuid",
    "firstName": "Zhang",
    "lastName": "San",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "address": "456 Library Road",
    "city": "Beijing",
    "postalCode": "100000",
    "patronType": "STUDENT",
    "status": "ACTIVE",
    "memberSince": "2026-06-01",
    "membershipExpiry": "2027-06-01",
    "currentLoans": 0,
    "outstandingFines": 0.00,
    "totalBorrowed": 0,
    "maxLoans": 5,
    "loanPeriodDays": 30,
    "createdAt": "2026-06-01T10:00:00",
    "updatedAt": "2026-06-01T10:00:00"
  }
}
```

`maxLoans` 和 `loanPeriodDays` 由会员类型对应的借阅权限（`BorrowingPrivilege`）决定。

### 更新会员信息

```bash
curl -X PUT http://localhost:8084/api/patrons/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Zhang",
    "lastName": "San Updated",
    "email": "zhangsan_new@example.com",
    "phone": "13900139000"
  }'
```

请求体中只需传递需要更新的字段，未传递的字段保持不变。对应 `UpdatePatronCommand`。

### 变更会员类型

```bash
curl -X PUT http://localhost:8084/api/patrons/{id}/type \
  -H "Content-Type: application/json" \
  -d '{"patronType": "FACULTY"}'
```

类型变更后，借阅配额（`maxLoans`）和借阅期限（`loanPeriodDays`）会随之调整。

### 暂停/恢复/终止

```bash
# 暂停
curl -X POST http://localhost:8084/api/patrons/{id}/suspend \
  -H "Content-Type: application/json" \
  -d '{"reason": "Overdue fines exceed limit"}'

# 恢复
curl -X POST http://localhost:8084/api/patrons/{id}/reactivate \
  -H "Content-Type: application/json" \
  -d '{"reason": "Fines paid"}'

# 终止
curl -X POST http://localhost:8084/api/patrons/{id}/terminate \
  -H "Content-Type: application/json" \
  -d '{"reason": "Graduated"}'
```

`reason` 参数为可选。暂停和恢复操作可循环执行，但终止操作不可逆（终态）。

### 罚款管理

```bash
# 添加罚款
curl -X POST http://localhost:8084/api/patrons/{id}/fines \
  -H "Content-Type: application/json" \
  -d '{"amount": 15.00, "reason": "Book overdue 15 days"}'

# 缴纳罚款
curl -X POST http://localhost:8084/api/patrons/{id}/fines/pay \
  -H "Content-Type: application/json" \
  -d '{"amount": 15.00}'

# 减免罚款
curl -X POST http://localhost:8084/api/patrons/{id}/fines/waive \
  -H "Content-Type: application/json" \
  -d '{"amount": 10.00, "reason": "First offense waiver"}'
```

- 添加罚款（`AddFineCommand`）：`amount` 和 `reason` 均必填
- 缴纳罚款（`PayFineCommand`）：仅需 `amount`
- 减免罚款（`WaiveFineCommand`）：`amount` 和 `reason` 均必填

### 会籍续期

```bash
curl -X POST http://localhost:8084/api/patrons/{id}/extend-membership \
  -H "Content-Type: application/json" \
  -d '{"months": 12, "reason": "Annual renewal"}'
```

`months` 为必填字段，指定续期月数。`reason` 为可选。成功后 `membershipExpiry` 相应延长。

## 业务规则

### 会员类型

枚举类 `PatronType`，不同类型有不同的借阅配额：

| 类型        | 说明     |
|-------------|----------|
| `STUDENT`   | 学生     |
| `FACULTY`   | 教职员工 |
| `STAFF`     | 行政人员 |
| `ALUMNI`    | 校友     |
| `COMMUNITY` | 社区成员 |

### 会员状态

枚举类 `MembershipStatus`，状态流转如下：

```
ACTIVE     -->  SUSPENDED    （暂停，可恢复）
SUSPENDED  -->  ACTIVE       （恢复，可循环）
ACTIVE/SUSPENDED  -->  TERMINATED  （终止，不可逆）
```

### 关键约束

- **暂停会员时不能借书**：`SUSPENDED` 和 `TERMINATED` 状态的会员 `canBorrow()` 返回 false
- **罚款自动暂停**：罚款超过一定额度时自动触发暂停
- **会籍到期**：到期后需要续期才能继续借书
- **不同类型配额**：不同会员类型的 `maxLoans` 和 `loanPeriodDays` 不同

## Kafka 事件

### 发布事件（6 个）

发布到 `library.patron.events` topic：

| 事件名称                  | 触发时机     |
|---------------------------|--------------|
| `PatronRegisteredEvent`   | 新会员注册   |
| `PatronUpdatedEvent`      | 会员信息更新 |
| `PatronTypeChangedEvent`  | 会员类型变更 |
| `PatronSuspendedEvent`    | 会员暂停     |
| `PatronReactivatedEvent`  | 会员恢复     |
| `PatronTerminatedEvent`   | 会员终止     |

### 消费事件

**来自 Circulation**（`CirculationEventConsumer`，topic: `library.circulation.events`）：

| 事件名称              | 处理逻辑                           |
|-----------------------|------------------------------------|
| `BookBorrowedEvent`   | 更新借阅计数（`currentLoans` 增加） |
| `BookReturnedEvent`   | 更新借阅计数（`currentLoans` 减少） |
| `FineIncurredEvent`   | 更新罚款余额（`outstandingFines`）  |

**来自 Payment**（`PaymentEventConsumer`，topic: `library.payment.events`）：

| 事件名称                 | 处理逻辑                         |
|--------------------------|----------------------------------|
| `PaymentCompletedEvent`  | 更新罚款状态（`outstandingFines`） |

其他未知事件类型以 DEBUG 级别忽略，不会抛出异常（防止 poison pill）。
