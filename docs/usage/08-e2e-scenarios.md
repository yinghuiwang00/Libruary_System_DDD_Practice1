# 🔗 端到端应用场景

## 概述

本文档描述跨多个 bounded context 的完整业务流程。每个场景包含完整的 API 调用链，从前置条件到最终验证。

**端口约定**：

| 模块 | 端口 |
|------|------|
| Catalog | localhost:8081 |
| Inventory | localhost:8082 |
| Circulation | localhost:8083 |
| Patron | localhost:8084 |
| Payment | localhost:8085 |
| Analytics | localhost:8086 |
| Notification | localhost:8087 |

> **提示**: 所有示例中的 `$VARIABLE` 需替换为实际 API 响应中获取的 ID。Kafka 事件为异步传递，跨模块状态变更可能有短暂延迟。

---

## 场景 1: 📖 新书入库到可借阅

**流程**: 创建作者 → 创建出版商 → 创建分类 → 创建书籍 → 关联元数据 → 发布 → 创建图书馆 → 创建库存 → 添加副本

### Step 1: 创建作者

```bash
curl -X POST http://localhost:8081/api/catalog/authors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Robert C. Martin",
    "biography": "Software engineer and author",
    "birthDate": "1952-12-05",
    "nationality": "American"
  }'
```

> 记下 `id` → `AUTHOR_ID`

### Step 2: 创建出版商

```bash
curl -X POST http://localhost:8081/api/catalog/publishers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prentice Hall",
    "description": "Technical publishing",
    "address": "Upper Saddle River, NJ",
    "phone": "201-555-0100",
    "email": "info@ph.com",
    "website": "https://ph.com"
  }'
```

> 记下 `id` → `PUBLISHER_ID`

### Step 3: 创建分类

```bash
curl -X POST http://localhost:8081/api/catalog/categories \
  -H "Content-Type: application/json" \
  -d '{"name": "Software Engineering", "description": "Software design and patterns"}'
```

> 记下 `id` → `CATEGORY_ID`

### Step 4: 创建书籍

```bash
curl -X POST http://localhost:8081/api/catalog/books \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "9780132350884",
    "title": "Clean Code",
    "description": "A handbook of agile software craftsmanship",
    "publicationDate": "2008-08-01",
    "pageCount": 464,
    "language": "English"
  }'
```

> 记下 `id` → `BOOK_ID`

### Step 5: 关联元数据

```bash
curl -X POST "http://localhost:8081/api/catalog/books/$BOOK_ID/authors?authorId=$AUTHOR_ID&role=AUTHOR"

curl -X PUT "http://localhost:8081/api/catalog/books/$BOOK_ID/publisher?publisherId=$PUBLISHER_ID"

curl -X POST "http://localhost:8081/api/catalog/books/$BOOK_ID/categories?categoryId=$CATEGORY_ID"
```

### Step 6: 发布书籍

```bash
curl -X POST http://localhost:8081/api/catalog/books/$BOOK_ID/publish
```

### Step 7: 创建图书馆

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

> 记下 `id` → `LIBRARY_ID`

### Step 8: 创建库存并添加副本

```bash
curl -X POST http://localhost:8082/api/inventory/inventories \
  -H "Content-Type: application/json" \
  -d "{\"bookId\": \"$BOOK_ID\", \"libraryId\": \"$LIBRARY_ID\", \"initialCopies\": 0}"
```

> 记下 `id` → `INVENTORY_ID`

```bash
curl -X POST http://localhost:8082/api/inventory/inventories/$INVENTORY_ID/copies/batch \
  -H "Content-Type: application/json" \
  -d '{"copyCount": 3, "copyNumberPrefix": "CC-", "location": "Floor-2-Shelf-A", "condition": "NEW"}'
```

### ✅ 验证

```bash
curl http://localhost:8082/api/inventory/books/$BOOK_ID/overview
```

预期结果: `totalCopies=3, availableCopies=3`

---

## 场景 2: 🎓 读者注册到借书

**流程**: 注册会员 → 获取可用副本 → 借书 → 验证库存变更

### Step 1: 注册会员

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

> 记下 `id` → `PATRON_ID`

### Step 2: 获取可用副本

```bash
curl http://localhost:8082/api/inventory/books/$BOOK_ID/overview
```

> 找到一个 available 的 `copyId` → `COPY_ID`

### Step 3: 借书

```bash
curl -X POST http://localhost:8083/api/circulation/loans \
  -H "Content-Type: application/json" \
  -d "{\"copyId\": \"$COPY_ID\", \"patronId\": \"$PATRON_ID\", \"bookId\": \"$BOOK_ID\"}"
```

> 记下 `id` → `LOAN_ID`。注意 `dueDate`（60 天后）

### ✅ 验证

```bash
# 检查读者借阅
curl http://localhost:8083/api/circulation/patrons/$PATRON_ID/loans

# 检查库存变化（availableCopies 应减少 1）
curl http://localhost:8082/api/inventory/books/$BOOK_ID/overview
```

---

## 场景 3: ⏰ 借书 → 逾期 → 罚款 → 缴费

**流程**: 借书 → 处理逾期 → 添加罚款 → 创建支付 → 完成支付 → 缴纳罚款

### Step 1: 借书（使用场景 2 的借阅，或新建一个）

### Step 2: 处理逾期

```bash
curl -X POST http://localhost:8083/api/circulation/admin/process-overdue
```

> 标记逾期借阅，触发 `FineIncurredEvent` 和 `OverdueNoticeEvent`

### Step 3: 给读者添加罚款

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/fines \
  -H "Content-Type: application/json" \
  -d '{"amount": 15.00, "reason": "Book overdue 15 days"}'
```

### Step 4: 创建支付

```bash
curl -X POST http://localhost:8085/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "patronId": "'$PATRON_ID'",
    "paymentType": "FINE",
    "amount": 15.00,
    "paymentMethod": "CREDIT_CARD",
    "description": "Overdue fine payment"
  }'
```

> 记下 `id` → `PAYMENT_ID`

### Step 5: 处理并完成支付

```bash
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/process \
  -H "Content-Type: application/json" \
  -d '{"externalTransactionId": "TXN-OVERDUE-001"}'

curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/complete
```

### Step 6: 缴纳罚款

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/fines/pay \
  -H "Content-Type: application/json" \
  -d '{"amount": 15.00}'
```

### ✅ 验证

```bash
# 支付记录应为 COMPLETED
curl "http://localhost:8085/api/payments?patronId=$PATRON_ID"

# 读者 outstandingFines 应为 0
curl http://localhost:8084/api/patrons/$PATRON_ID
```

---

## 场景 4: 📋 预约 → 到书通知 → 取书

**流程**: 所有副本被借出 → 另一读者预约 → 原读者归还 → 预约就绪 → 通知 → 取书

### Step 1: 预约书籍（所有副本被借出时）

```bash
curl -X POST http://localhost:8083/api/circulation/holds \
  -H "Content-Type: application/json" \
  -d '{
    "copyId": "'$COPY_ID'",
    "patronId": "'$PATRON_2_ID'",
    "bookId": "'$BOOK_ID'"
  }'
```

> 记下 `id` → `HOLD_ID`，`queuePosition` = 1

### Step 2: 查看预约队列

```bash
curl http://localhost:8083/api/circulation/books/$BOOK_ID/holds
```

### Step 3: 原读者归还 → 触发预约就绪

```bash
curl -X POST http://localhost:8083/api/circulation/loans/$LOAN_ID/return
```

> 触发 `BookReturnedEvent` → Hold 状态变为 `READY_FOR_PICKUP` → 发送通知

### ✅ 验证

```bash
# 预约状态应为 READY_FOR_PICKUP
curl http://localhost:8083/api/circulation/holds/$HOLD_ID

# 读者应收到预约就绪通知
curl "http://localhost:8087/api/notifications?recipientId=$PATRON_2_ID&type=HOLD_READY"
```

---

## 场景 5: 🔧 损坏报告 → 罚款 → 退款

**流程**: 报告损坏 → 添加罚款 → 支付 → 发现误判 → 退款

### Step 1: 报告副本损坏

```bash
curl -X POST http://localhost:8082/api/inventory/copies/$COPY_ID/damage \
  -H "Content-Type: application/json" \
  -d '{"description": "Cover torn, pages 10-15 water damaged"}'
```

### Step 2: 添加替换费罚款

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/fines \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00, "reason": "Book copy damage replacement fee"}'
```

### Step 3: 创建并完成支付

```bash
curl -X POST http://localhost:8085/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "patronId": "'$PATRON_ID'",
    "paymentType": "REPLACEMENT",
    "amount": 50.00,
    "paymentMethod": "ONLINE_TRANSFER",
    "description": "Damage replacement fee"
  }'
```

> 记下 `id` → `PAYMENT_ID`

```bash
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/process \
  -H "Content-Type: application/json" \
  -d '{"externalTransactionId": "TXN-DAMAGE-001"}'

curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/complete
```

### Step 4: 请求退款（发现是正常磨损）

```bash
curl -X POST http://localhost:8085/api/payments/$PAYMENT_ID/refunds \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00, "reason": "Damage assessment reversed - normal wear and tear"}'
```

> 记下退款 `id` → `REFUND_ID`

### Step 5: 处理并完成退款

```bash
curl -X POST http://localhost:8085/api/payments/refunds/$REFUND_ID/process \
  -H "Content-Type: application/json" \
  -d '{"externalRefundId": "REF-DAMAGE-001"}'

curl -X POST http://localhost:8085/api/payments/refunds/$REFUND_ID/complete \
  -H "Content-Type: application/json" \
  -d '{"refundMethod": "ONLINE_TRANSFER"}'
```

---

## 场景 6: 👤 完整的读者生命周期

**流程**: 注册 → 借书 → 逾期 → 罚款 → 缴费 → 暂停 → 恢复 → 续期 → 终止

### Step 1-3: 注册+借书+逾期+缴费

参考场景 2 和场景 3 的步骤。

### Step 4: 暂停会员

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/suspend \
  -H "Content-Type: application/json" \
  -d '{"reason": "Three overdue incidents in 30 days"}'
```

> 状态: `ACTIVE` → `SUSPENDED`。此时会员不能借书。

### Step 5: 恢复会员

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/reactivate \
  -H "Content-Type: application/json" \
  -d '{"reason": "Suspension period served, all fines cleared"}'
```

> 状态: `SUSPENDED` → `ACTIVE`

### Step 6: 会籍续期

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/extend-membership \
  -H "Content-Type: application/json" \
  -d '{"months": 12, "reason": "Annual renewal"}'
```

### Step 7: 终止会员（毕业）

```bash
curl -X POST http://localhost:8084/api/patrons/$PATRON_ID/terminate \
  -H "Content-Type: application/json" \
  -d '{"reason": "Graduated - membership no longer eligible"}'
```

> 状态: `ACTIVE` → `TERMINATED`。**不可逆操作**。

---

## 场景 7: 📉 库存不足预警

**流程**: 大量借出 → 库存低于阈值 → 触发 LowStockAlertEvent → 补充库存

### Step 1: 大量借出

假设某书在图书馆有 3 个副本，依次借出 2 个：

```bash
curl -X POST http://localhost:8083/api/circulation/loans \
  -H "Content-Type: application/json" \
  -d "{\"copyId\": \"$COPY_1\", \"patronId\": \"$PATRON_1\", \"bookId\": \"$BOOK_ID\"}"

curl -X POST http://localhost:8083/api/circulation/loans \
  -H "Content-Type: application/json" \
  -d "{\"copyId\": \"$COPY_2\", \"patronId\": \"$PATRON_2\", \"bookId\": \"$BOOK_ID\"}"
```

### Step 2: 检查库存

```bash
curl http://localhost:8082/api/inventory/books/$BOOK_ID/overview
```

> `availableCopies=1, totalCopies=3`。如果低于阈值，触发 `LowStockAlertEvent`。

### Step 3: 查看预警通知

```bash
curl "http://localhost:8087/api/notifications?type=SYSTEM"
```

### Step 4: 补充库存

```bash
curl -X POST http://localhost:8082/api/inventory/inventories/$INVENTORY_ID/copies/batch \
  -H "Content-Type: application/json" \
  -d '{"copyCount": 5, "copyNumberPrefix": "RESTOCK-", "location": "Floor-2-Shelf-A", "condition": "NEW"}'
```

### ✅ 验证

```bash
curl http://localhost:8082/api/inventory/books/$BOOK_ID/overview
```

预期: `availableCopies=6, totalCopies=8`

---

## 场景 8: 📊 数据分析报表生成

**流程**: 创建报表 → 查看状态 → 完成报表 → 重新生成

### Step 1: 创建流通报表

```bash
curl -X POST http://localhost:8086/api/analytics/reports \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CIRCULATION",
    "name": "Q2 2026 Circulation Report",
    "description": "Quarterly borrowing and returning statistics",
    "scheduledBy": "admin"
  }'
```

> 记下 `id` → `REPORT_ID`

### Step 2: 查看报表状态

```bash
curl http://localhost:8086/api/analytics/reports/$REPORT_ID
```

> 状态: `PENDING`

### Step 3: 完成报表

```bash
curl -X POST http://localhost:8086/api/analytics/reports/$REPORT_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "totalRecords": 2450,
    "dataSummary": "Total 2450 loans in Q2 2026, 12% increase from Q1",
    "reportData": "{\"totalLoans\": 2450, \"returns\": 2300, \"overdue\": 150, \"avgLoanDays\": 21}"
  }'
```

### Step 4: 查看所有已完成报表

```bash
curl "http://localhost:8086/api/analytics/reports?type=CIRCULATION&status=COMPLETED"
```

### Step 5: 重新生成（可选）

```bash
curl -X POST http://localhost:8086/api/analytics/reports/$REPORT_ID/regenerate \
  -H "Content-Type: application/json" \
  -d '{"regeneratedBy": "admin"}'
```

---

## 💡 使用建议

1. **变量替换**: 所有 `$VARIABLE` 需替换为实际 API 响应中的 ID
2. **异步事件**: 跨模块状态变更通过 Kafka 事件异步传递，可能有数秒延迟
3. **Swagger UI**: 每个模块都有交互式 API 文档 → `http://localhost:{port}/swagger-ui.html`
4. **环境准备**: 运行场景前确保所有模块已启动，Docker 中 PostgreSQL 和 Kafka 已运行
5. **数据清理**: Staging 测试会在 `@Before` 中自动清理数据，手动测试需自行清理

---

## 返回 [README](../../README.md)
