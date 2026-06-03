# 🔔 通知管理模块 (Notification)

> **端口**: 8087 | **API 前缀**: `/api/notifications` | **数据库**: `library_notification`

## 概述

Notification bounded context 负责管理各类通知的创建、发送和状态追踪。支持多渠道（Email/SMS/Push/站内）和多优先级，是系统与用户沟通的核心模块。它消费其他模块的事件来触发自动通知（到期提醒、逾期通知、预约就绪等）。

---

## API 一览

### 通知管理 (NotificationController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/notifications` | 创建通知 |
| GET | `/api/notifications` | 查询通知（可按 recipientId/status/type 过滤） |
| GET | `/api/notifications/{id}` | 查询通知详情 |
| POST | `/api/notifications/{id}/schedule` | 调度通知（定时发送） |
| POST | `/api/notifications/{id}/send` | 立即发送通知 |
| POST | `/api/notifications/{id}/deliver` | 标记已送达 |
| PUT | `/api/notifications/{id}/read` | 标记已读 |
| POST | `/api/notifications/{id}/fail` | 标记发送失败 |
| POST | `/api/notifications/{id}/retry` | 重试失败通知 |
| POST | `/api/notifications/{id}/cancel` | 取消通知 |

---

## 详细用法

### 创建并发送通知（典型流程）

#### 1. 创建通知

```bash
curl -X POST http://localhost:8087/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "<PATRON_ID>",
    "type": "DUE_DATE",
    "message": "Your book \"Clean Code\" is due in 3 days. Please return it on time.",
    "subject": "Due Date Reminder",
    "priority": "NORMAL",
    "scheduledDelivery": "2026-06-05T09:00:00"
  }'
```

响应：

```json
{
  "success": true,
  "data": {
    "id": "notif-001",
    "recipientId": "...",
    "type": "DUE_DATE",
    "subject": "Due Date Reminder",
    "status": "PENDING",
    "priority": "NORMAL",
    "channel": "EMAIL"
  }
}
```

> 记下 `id` 作为 `NOTIFICATION_ID`。

#### 2. 调度通知（定时发送）

```bash
curl -X POST http://localhost:8087/api/notifications/$NOTIFICATION_ID/schedule \
  -H "Content-Type: application/json" \
  -d '{"scheduledAt": "2026-06-05T09:00:00"}'
```

状态变更：`PENDING` → `SCHEDULED`

#### 3. 发送通知

```bash
curl -X POST http://localhost:8087/api/notifications/$NOTIFICATION_ID/send
```

状态变更：`SCHEDULED` → `SENDING`

#### 4. 标记已送达

```bash
curl -X POST http://localhost:8087/api/notifications/$NOTIFICATION_ID/deliver
```

状态变更：`SENDING` → `DELIVERED`

#### 5. 标记已读

```bash
curl -X PUT http://localhost:8087/api/notifications/$NOTIFICATION_ID/read
```

状态变更：`DELIVERED` → `READ`

### 查询通知

```bash
# 按读者查询
curl "http://localhost:8087/api/notifications?recipientId=$PATRON_ID"

# 按状态查询
curl "http://localhost:8087/api/notifications?status=DELIVERED"

# 按类型查询
curl "http://localhost:8087/api/notifications?type=DUE_DATE"

# 组合查询
curl "http://localhost:8087/api/notifications?recipientId=$PATRON_ID&status=UNREAD&type=OVERDUE"
```

### 失败重试

```bash
# 标记发送失败
curl -X POST http://localhost:8087/api/notifications/$NOTIFICATION_ID/fail \
  -H "Content-Type: application/json" \
  -d '{"reason": "SMTP server unavailable"}'

# 重试发送
curl -X POST http://localhost:8087/api/notifications/$NOTIFICATION_ID/retry
```

### 取消通知

```bash
curl -X POST http://localhost:8087/api/notifications/$NOTIFICATION_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Book already returned, reminder no longer needed"}'
```

---

## 业务规则

- **通知类型**: `DUE_DATE`（到期提醒）、`OVERDUE`（逾期通知）、`HOLD_READY`（预约就绪）、`FINE`（罚款通知）、`GENERAL`（通用）、`SYSTEM`（系统通知）
- **优先级**: `LOW` → `NORMAL` → `HIGH` → `URGENT`
- **渠道**: `EMAIL`、`SMS`、`PUSH`、`IN_APP`
- **通知状态流转**:
  - 正常: `PENDING` → `SCHEDULED` → `SENDING` → `DELIVERED` → `READ`
  - 直接发送: `PENDING` → `SENDING` → `DELIVERED` → `READ`
  - 失败: `SENDING` → `FAILED` → (retry) → `SENDING`
  - 取消: `PENDING`/`SCHEDULED` → `CANCELLED`
- **重试限制**: 失败通知可重试，有最大重试次数
- **自动触发**: 系统会根据 Kafka 事件自动创建通知（无需手动）

---

## Kafka 事件

### 发布事件（4 个）

| 事件 | 触发时机 |
|------|---------|
| NotificationCreatedEvent | 通知创建 |
| NotificationDeliveredEvent | 通知送达 |
| NotificationReadEvent | 通知已读 |
| NotificationFailedEvent | 通知发送失败 |

### 消费事件（4 个）

| 事件 | 来源 | 触发的通知 |
|------|------|-----------|
| DueDateReminderEvent | Circulation | 到期提醒通知 |
| OverdueNoticeEvent | Circulation | 逾期通知 |
| HoldPlacedEvent / HoldFulfilledEvent | Circulation | 预约确认/就绪通知 |
| PaymentCompletedEvent | Payment | 支付确认通知 |

---

## 返回 [README](../../README.md)
