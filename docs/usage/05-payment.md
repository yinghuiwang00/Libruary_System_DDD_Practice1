# 💰 支付管理模块 (Payment)

> **端口**: 8085 | **API 前缀**: `/api/payments` | **数据库**: `library_payment`

## 概述

Payment bounded context 负责管理罚款支付和退款处理。支持多种支付方式和支付类型，提供完整的支付生命周期管理和退款流程。

---

## API 一览

### 支付管理 (PaymentController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/payments` | 创建支付 |
| POST | `/api/payments/{id}/process` | 处理支付 |
| POST | `/api/payments/{id}/complete` | 完成支付 |
| POST | `/api/payments/{id}/fail` | 标记支付失败 |
| POST | `/api/payments/{id}/cancel` | 取消支付 |
| GET | `/api/payments/{id}` | 查询支付详情 |
| GET | `/api/payments?patronId=xxx` | 按读者查询支付列表 |
| POST | `/api/payments/{id}/refunds` | 请求退款 |
| GET | `/api/payments/{id}/refunds` | 查询退款列表 |
| POST | `/api/payments/refunds/{id}/process` | 处理退款 |
| POST | `/api/payments/refunds/{id}/complete` | 完成退款 |

---

## 详细用法

### 创建并完成支付（典型流程）

#### 1. 创建支付

```bash
curl -X POST http://localhost:8085/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "patronId": "<PATRON_ID>",
    "paymentType": "FINE",
    "amount": 15.00,
    "paymentMethod": "CREDIT_CARD",
    "description": "Overdue fine for Clean Code"
  }'
```

响应：

```json
{
  "success": true,
  "data": {
    "id": "pay-001",
    "patronId": "...",
    "paymentType": "FINE",
    "amount": 15.00,
    "paymentMethod": "CREDIT_CARD",
    "status": "PENDING",
    "referenceNumber": "REF-20260603-001"
  }
}
```

> 记下 `id` 作为 `PAYMENT_ID`。状态为 `PENDING`。

#### 2. 处理支付

```bash
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/process \
  -H "Content-Type: application/json" \
  -d '{"externalTransactionId": "TXN-12345"}'
```

状态变更：`PENDING` → `PROCESSING`

#### 3. 完成支付

```bash
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/complete
```

状态变更：`PROCESSING` → `COMPLETED`

### 查询支付

```bash
# 查询单笔支付
curl http://localhost:8085/api/payments/$PAYMENT_ID

# 按读者查询所有支付
curl "http://localhost:8085/api/payments?patronId=$PATRON_ID"
```

### 支付失败和取消

```bash
# 标记支付失败
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/fail \
  -H "Content-Type: application/json" \
  -d '{"reason": "Card declined"}'

# 取消待处理支付
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Duplicate payment"}'
```

### 退款流程

#### 1. 请求退款

```bash
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/refunds \
  -H "Content-Type: application/json" \
  -d '{"amount": 15.00, "reason": "Fine was applied in error"}'
```

> 记下退款 `id` 作为 `REFUND_ID`。

#### 2. 处理退款

```bash
curl -X POST http://localhost:8085/api/payments/refunds/$REFUND_ID/process \
  -H "Content-Type: application/json" \
  -d '{"externalRefundId": "REF-TXN-12345"}'
```

#### 3. 完成退款

```bash
curl -X POST http://localhost:8085/api/payments/refunds/$REFUND_ID/complete \
  -H "Content-Type: application/json" \
  -d '{"refundMethod": "CREDIT_CARD"}'
```

#### 4. 查询退款

```bash
curl http://localhost:8085/api/payments/$PAYMENT_ID/refunds
```

---

## 业务规则

- **支付类型**: `FINE`（罚款）、`FEE`（费用）、`REPLACEMENT`（替换费）、`DONATION`（捐赠）
- **支付方式**: `CASH`、`CREDIT_CARD`、`DEBIT_CARD`、`ONLINE_TRANSFER`
- **支付状态流转**:
  - 成功: `PENDING` → `PROCESSING` → `COMPLETED`
  - 失败: `PENDING` → `PROCESSING` → `FAILED`
  - 取消: `PENDING` → `CANCELLED`
  - 退款: `COMPLETED` → `REFUNDED`
- **退款限制**: 只有 `COMPLETED` 状态的支付才能退款
- **可退金额**: 不超过原支付金额

---

## Kafka 事件

### 发布事件（6 个）

| 事件 | 触发时机 |
|------|---------|
| PaymentCreatedEvent | 支付创建 |
| PaymentCompletedEvent | 支付完成 |
| PaymentFailedEvent | 支付失败 |
| PaymentCancelledEvent | 支付取消 |
| RefundRequestedEvent | 退款请求 |
| RefundCompletedEvent | 退款完成 |

### 消费事件（1 个）

| 事件 | 来源 | 处理逻辑 |
|------|------|---------|
| FineIncurredEvent | Circulation | 自动创建待支付罚款记录 |

---

## 返回 [README](../../README.md)
