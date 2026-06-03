# 📊 数据分析模块 (Analytics)

> **端口**: 8086 | **API 前缀**: `/api/analytics` | **数据库**: `library_analytics`

## 概述

Analytics bounded context 负责生成图书馆运营报表和数据分析。支持按流通、库存、会员、财务、使用情况等维度生成报表，并提供报表生命周期管理。

---

## API 一览

### 报表管理 (AnalyticsController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/analytics/reports` | 创建报表 |
| GET | `/api/analytics/reports` | 查询报表（可按 type/status 过滤） |
| GET | `/api/analytics/reports/{id}` | 查询报表详情 |
| POST | `/api/analytics/reports/{id}/complete` | 完成报表（填充数据） |
| POST | `/api/analytics/reports/{id}/fail` | 标记报表失败 |
| POST | `/api/analytics/reports/{id}/cancel` | 取消报表 |
| POST | `/api/analytics/reports/{id}/regenerate` | 重新生成报表 |

---

## 详细用法

### 创建报表

```bash
curl -X POST http://localhost:8086/api/analytics/reports \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CIRCULATION",
    "name": "Monthly Circulation Report",
    "description": "Monthly borrowing and returning statistics",
    "scheduledBy": "admin"
  }'
```

响应：

```json
{
  "success": true,
  "data": {
    "id": "report-001",
    "type": "CIRCULATION",
    "name": "Monthly Circulation Report",
    "status": "PENDING",
    "scheduledBy": "admin"
  }
}
```

> 记下 `id` 作为 `REPORT_ID`。

### 查询报表

```bash
# 查询所有报表
curl http://localhost:8086/api/analytics/reports

# 按类型过滤
curl "http://localhost:8086/api/analytics/reports?type=CIRCULATION"

# 按状态过滤
curl "http://localhost:8086/api/analytics/reports?status=COMPLETED"

# 组合过滤
curl "http://localhost:8086/api/analytics/reports?type=CIRCULATION&status=COMPLETED"
```

### 完成报表

```bash
curl -X POST http://localhost:8086/api/analytics/reports/$REPORT_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "totalRecords": 1500,
    "dataSummary": "Total 1500 loans in June, 12% increase from May",
    "reportData": "{\"totalLoans\": 1500, \"returns\": 1420, \"overdue\": 80, \"avgLoanDays\": 21}"
  }'
```

状态变更：`PENDING` → `COMPLETED`

### 标记报表失败

```bash
curl -X POST http://localhost:8086/api/analytics/reports/$REPORT_ID/fail \
  -H "Content-Type: application/json" \
  -d '{"errorMessage": "Data source unavailable, retry later"}'
```

### 取消报表

```bash
curl -X POST http://localhost:8086/api/analytics/reports/$REPORT_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "No longer needed for this quarter"}'
```

### 重新生成报表

```bash
curl -X POST http://localhost:8086/api/analytics/reports/$REPORT_ID/regenerate \
  -H "Content-Type: application/json" \
  -d '{"regeneratedBy": "admin"}'
```

对已完成或失败的报表重新生成，状态回到 `PENDING`。

---

## 业务规则

- **报表类型**: `CIRCULATION`（流通）、`INVENTORY`（库存）、`PATRON`（会员）、`FINANCIAL`（财务）、`USAGE`（使用情况）
- **报表周期**: `DAILY`、`WEEKLY`、`MONTHLY`、`QUARTERLY`、`YEARLY`
- **报表状态流转**:
  - 成功: `PENDING` → `GENERATING` → `COMPLETED`
  - 失败: `PENDING` → `GENERATING` → `FAILED`
  - 取消: `PENDING` → `CANCELLED`
- **重新生成**: 已完成或失败的报表可以重新生成

---

## Kafka 事件

### 发布事件（4 个）

| 事件 | 触发时机 |
|------|---------|
| ReportCreatedEvent | 报表创建 |
| ReportCompletedEvent | 报表完成 |
| ReportFailedEvent | 报表失败 |
| ReportCancelledEvent | 报表取消 |

### 消费事件（2 个）

| 事件 | 来源 | 处理逻辑 |
|------|------|---------|
| BookPublishedEvent / CopyAddedEvent | Catalog / Inventory | 触发库存报表数据更新 |
| BookBorrowedEvent / BookReturnedEvent | Circulation | 触发流通报表数据更新 |

---

## 返回 [README](../../README.md)
