# BDD 测试场景清单

> **生成日期**: 2026-05-30
> **总计**: 15 个 Feature 文件，36 个 Scenario

---

## 目录

- [1. library-catalog 编目上下文（4 Features, 4 Scenarios）](#1-library-catalog-编目上下文)
- [2. library-inventory 馆藏上下文（3 Features, 3 Scenarios）](#2-library-inventory-馆藏上下文)
- [3. library-circulation 借阅上下文（1 Feature, 7 Scenarios）](#3-library-circulation-借阅上下文)
- [4. library-patron 会员上下文（4 Features, 12 Scenarios）](#4-library-patron-会员上下文)
- [5. library-payment 支付上下文（1 Feature, 6 Scenarios）](#5-library-payment-支付上下文)
- [6. library-analytics 分析上下文（1 Feature, 6 Scenarios）](#6-library-analytics-分析上下文)
- [7. library-notification 通知上下文（1 Feature, 3 Scenarios）](#7-library-notification-通知上下文)
- [8. 统计汇总](#8-统计汇总)

---

## 1. library-catalog 编目上下文

### 1.1 图书创建

**Feature**: 图书创建
**用户故事**: 作为图书馆管理员，我想创建新的图书记录，以便管理图书馆的藏书信息

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功创建新图书 | 系统中不存在 ISBN 为 "9787111407010" 的图书 | 创建新书，标题为"领域驱动设计"，ISBN 为 "9787111407010"，分类为 SOFTWARE_ENGINEERING | 图书创建成功，状态为 DRAFT |

---

### 1.2 图书发布

**Feature**: 图书发布
**用户故事**: 作为图书馆管理员，我想将草稿状态的图书发布，以便读者可以搜索和借阅

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功发布草稿状态的图书 | 系统中存在一本状态为 DRAFT 的图书 | 发布该图书 | 图书状态变为 PUBLISHED |

---

### 1.3 图书查询

**Feature**: 图书查询
**用户故事**: 作为读者，我想通过 ID 查询图书，以便获取图书详细信息

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 通过 ID 成功查询已发布的图书 | 系统中存在一本 ISBN 为 "9787111407010" 且状态为 PUBLISHED 的图书 | 通过该图书的 ID 查询 | 返回图书信息，标题为"领域驱动设计" |

---

### 1.4 图书更新

**Feature**: 图书更新
**用户故事**: 作为图书馆管理员，我想更新已发布图书的信息，以便保持信息的准确性

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功更新已发布图书的标题和描述 | 系统中存在一本状态为 PUBLISHED 的图书 | 将标题更新为"领域驱动设计(修订版)" | 图书更新成功，标题为"领域驱动设计(修订版)" |

---

## 2. library-inventory 馆藏上下文

### 2.1 分馆管理

**Feature**: 分馆管理
**用户故事**: 作为图书馆管理员，我想管理图书馆分馆信息，以便支持多分馆馆藏管理

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功创建新分馆 | 系统中不存在编码为 "LIB-001" 的分馆 | 创建新分馆，编码 "LIB-001"，名称"总馆"，地址"中关村大街1号"，电话 "010-12345678" | 分馆创建成功，状态为 ACTIVE |

---

### 2.2 馆藏初始化

**Feature**: 馆藏初始化
**用户故事**: 作为图书馆管理员，我想为图书创建馆藏记录，以便管理各分馆的图书副本

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功创建馆藏并添加初始副本 | 系统中存在编码为 "LIB-001" 的分馆，且图书 "BOOK-001" 在该分馆尚无馆藏记录 | 为图书 "BOOK-001" 在该分馆创建馆藏，初始副本数为 2 | 馆藏创建成功，总副本数为 2，可用副本数为 2 |

---

### 2.3 副本借出与归还

**Feature**: 副本借出与归还
**用户故事**: 作为图书馆管理员，我想管理图书副本的借出和归还，以便跟踪副本的流通状态

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功借出并归还图书副本 | 系统中存在编码为 "LIB-001" 的分馆，图书 "BOOK-001" 在该分馆有馆藏，包含 2 个可用副本 | 借出一个副本 → 可用副本变为 1 → 归还该副本 → 可用副本变为 2 | 借出和归还均成功 |

---

## 3. library-circulation 借阅上下文

### 3.1 图书借出与归还

**Feature**: 图书借出与归还
**用户故事**: 作为图书馆管理员，我想管理图书的借出和归还，以便跟踪图书的流通状态

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功借出图书 | 读者 "PATRON-001" 想借阅图书 "BOOK-001" 的副本 "COPY-001" | 读者借出该图书 | 借出成功，状态为 ACTIVE |
| 2 | 成功归还图书 | 读者 "PATRON-002" 已借出图书 "BOOK-002" 的副本 "COPY-002" | 读者归还该图书 | 归还成功，状态为 RETURNED |
| 3 | 成功预约图书 | 图书 "BOOK-003" 当前不可借阅 | 读者 "PATRON-003" 预约该图书 | 预约成功，状态为 WAITING |
| 4 | 成功续借图书 | 读者 "PATRON-004" 已借出图书 "BOOK-004" 的副本 "COPY-004" | 读者续借该图书 | 续借成功，状态为 RENEWED，续借次数为 1，应还日期已延长 |
| 5 | 续借次数超过限制 | 读者 "PATRON-005" 已借出图书 "BOOK-005" 且已达到最大续借次数 | 读者尝试续借 | 续借失败 |
| 6 | 标记借阅为逾期 | 读者 "PATRON-006" 已借出图书 "BOOK-006" 且已逾期 | 系统处理逾期借阅 | 状态为 OVERDUE |
| 7 | 取消预约 | 读者 "PATRON-007" 已预约图书 "BOOK-007" | 读者取消该预约 | 预约已取消 |

---

## 4. library-patron 会员上下文

### 4.1 会员注册

**Feature**: Patron Registration
**用户故事**: As a library administrator, I want to register new patrons

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功注册新会员 | 不存在 email 为 "john@example.com" 的会员 | 以 firstName=John, lastName=Doe, email=john@example.com, patronType=STUDENT 注册 | 注册成功，状态为 ACTIVE |

---

### 4.2 会员生命周期管理

**Feature**: Patron Lifecycle Management
**用户故事**: As a library administrator, I want to manage patron lifecycle states

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 暂停并重新激活会员 | 会员 "Jane Smith" 已注册 | 暂停（原因：逾期未还）→ 状态变为 SUSPENDED → 重新激活（原因：已还书）→ 状态变为 ACTIVE | 暂停和激活均成功 |
| 2 | 终止会员 | 会员 "Bob" 已注册 | 终止会员（原因：毕业） | 状态为 TERMINATED |
| 3 | 延长会员有效期 12 个月 | 会员 "Alice" 已注册 | 延长会员 12 个月 | 有效期延长 12 个月 |
| 4 | 不能暂停已终止的会员 | 会员 "Charlie" 已注册并已终止 | 尝试暂停 | 操作失败 |
| 5 | 有未缴罚款的会员不能终止 | 会员 "Debt" 已注册且有 10.00 罚款 | 尝试终止 | 操作失败 |

---

### 4.3 会员罚款管理

**Feature**: Patron Fine Management
**用户故事**: As a library administrator, I want to manage patron fines

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 添加并缴纳罚款 | 会员 "Tom" 已注册 | 添加 25.00 罚款（逾期）→ 未缴罚款 25.00 → 缴纳 25.00 → 未缴罚款 0.00 | 罚款添加和缴纳均成功 |
| 2 | 罚款超阈值自动暂停 | 会员 "Sam" 已注册 | 添加 60.00 罚款（丢书）→ 状态变为 SUSPENDED → 缴纳 60.00 → 状态恢复 ACTIVE | 自动暂停和自动恢复均生效 |
| 3 | 豁免罚款 | 会员 "Lee" 已注册 | 添加 30.00 罚款 → 豁免 30.00 → 未缴罚款 0.00 | 豁免成功 |
| 4 | 豁免罚款后自动恢复 | 会员 "Grace" 已注册 | 添加 55.00 罚款 → 状态 SUSPENDED → 豁免 10.00 → 状态恢复 ACTIVE | 豁免触发了自动恢复 |
| 5 | 罚款未缴清不能手动恢复 | 会员 "HighFine" 已注册 | 添加 60.00 罚款 → 状态 SUSPENDED → 尝试手动恢复 → 操作失败，状态仍为 SUSPENDED | 手动恢复被阻止 |

---

### 4.4 会员类型变更

**Feature**: Patron Type Change
**用户故事**: As a library administrator, I want to change a patron's type

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | STUDENT → FACULTY | 会员 "Prof. Wang" 以 STUDENT 类型注册 | 变更类型为 FACULTY | 类型变为 FACULTY |
| 2 | STUDENT → STAFF | 会员 "Scientist" 以 STUDENT 类型注册 | 变更类型为 STAFF | 类型变为 STAFF |

---

## 5. library-payment 支付上下文

### 5.1 支付生命周期

**Feature**: Payment Lifecycle

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 成功处理罚款支付 | 为会员 "patron-123" 创建了 25.00 的支付单 | 处理支付（事务号 TXN-001）→ 完成支付 | 状态为 COMPLETED，金额 25.00 |
| 2 | 创建并取消支付 | 为会员 "patron-456" 创建了 50.00 的支付单 | 取消支付（原因：重复支付） | 状态为 CANCELLED |
| 3 | 处理中失败 | 为会员 "patron-789" 创建了 100.00 的支付单 | 处理支付 → 支付失败（原因：余额不足） | 状态为 FAILED |
| 4 | 申请并完成全额退款 | 会员 "patron-refund" 有一笔 75.00 的已完成支付 | 申请 75.00 退款 → 状态 PENDING → 处理退款 → 完成退款 | 退款金额 75.00 |
| 5 | 多次部分退款 | 会员 "patron-partial" 有一笔 100.00 的已完成支付 | 申请 30.00 退款 → 完成 → 申请 20.00 退款 → 完成 | 累计退款 50.00 |
| 6 | 按会员查询支付列表 | 会员 "patron-list" 有两笔支付（10.00 + 20.00） | 查询该会员的支付 | 看到 2 笔支付 |

---

## 6. library-analytics 分析上下文

### 6.1 分析报告生成

**Feature**: Analytics Report Generation
**用户故事**: As a library administrator, I want to generate analytics reports

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 生成并完成流通报告 | 分析服务可用 | 请求 CIRCULATION_REPORT（月度）→ 状态 GENERATING → 完成（100 条记录）→ 状态 COMPLETED | 报告有 100 条记录 |
| 2 | 生成并失败报告 | 分析服务可用 | 请求 INVENTORY_REPORT（周度）→ 状态 GENERATING → 失败（"数据源不可用"）→ 状态 FAILED | 报告失败 |
| 3 | 生成、取消、重新生成 | 分析服务可用 | 请求 PATRON_REPORT（季度）→ 状态 GENERATING → 取消 → 状态 CANCELLED → 重新生成 → 状态 GENERATING | 重新生成成功 |
| 4 | 列出所有报告 | 分析服务可用 | 请求 2 份报告 → 列出全部 | 看到 2 份报告 |
| 5 | 按类型过滤报告 | 分析服务可用 | 请求 CIRCULATION_REPORT + INVENTORY_REPORT → 按 CIRCULATION_REPORT 过滤 | 看到 1 份报告，类型为 CIRCULATION_REPORT |
| 6 | 按状态过滤报告 | 分析服务可用 | 请求并完成 1 份 → 请求另 1 份（未完成）→ 按 COMPLETED 过滤 | 看到 1 份报告，状态为 COMPLETED |

---

## 7. library-notification 通知上下文

### 7.1 通知生命周期

**Feature**: Notification Lifecycle

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | 发送并送达邮件通知 | 为会员 "patron-001" 创建了通知 | 通过 EMAIL 发送 → 状态 SENDING → 送达 → 状态 DELIVERED → 标记已读 → 状态 READ | 完整生命周期 PENDING → SENDING → DELIVERED → READ |
| 2 | 处理失败并重试通知 | 为会员 "patron-002" 创建了通知 | 通过 EMAIL 发送 → 失败（"SMTP error"）→ 状态 FAILED → 重试 → 状态 PENDING | 失败后可重试 |
| 3 | 调度并发送通知 | 为会员 "patron-003" 创建了通知 | 调度为 2 小时后发送 → 状态 SCHEDULED → 发送 → 状态 SENDING | 调度后可发送 |

---

## 8. 统计汇总

### 8.1 按模块统计

| 模块 | Feature 数 | Scenario 数 | 语言 |
|------|-----------|------------|------|
| library-catalog | 4 | 4 | 中文 |
| library-inventory | 3 | 3 | 中文 |
| library-circulation | 1 | 7 | 中文 |
| library-patron | 4 | 12 | 英文 |
| library-payment | 1 | 6 | 英文 |
| library-analytics | 1 | 6 | 英文 |
| library-notification | 1 | 3 | 英文 |
| **合计** | **15** | **41** | - |

### 8.2 按场景类型统计

| 类型 | Scenario 数 | 占比 |
|------|------------|------|
| 主流程（Happy Path） | 33 | 80.5% |
| 异常/边界场景 | 8 | 19.5% |
| — 续借超限 | 1 | |
| — 罚款未缴不能恢复 | 1 | |
| — 不能暂停已终止会员 | 1 | |
| — 有罚款不能终止 | 1 | |
| — 支付失败 | 1 | |
| — 报告生成失败 | 1 | |
| — 通知发送失败+重试 | 1 | |
| — 逾期标记 | 1 | |

### 8.3 业务覆盖矩阵

| 业务领域 | 创建 | 查询 | 更新 | 状态流转 | 异常处理 |
|----------|------|------|------|----------|----------|
| 图书 | ✅ | ✅ | ✅ | ✅ 发布/取消发布 | — |
| 作者 | — | — | — | — | — |
| 出版社 | — | — | — | — | — |
| 分类 | — | — | — | — | — |
| 分馆 | ✅ | — | — | — | — |
| 馆藏 | ✅ | — | — | — | — |
| 副本 | — | — | — | ✅ 借出/归还 | — |
| 借阅 | ✅ | — | — | ✅ 借出/归还/续借/逾期 | ✅ 续借超限 |
| 预约 | ✅ | — | — | ✅ 预约/取消 | — |
| 会员注册 | ✅ | — | — | — | — |
| 会员生命周期 | — | — | ✅ | ✅ 暂停/激活/终止 | ✅ 不可暂停已终止/有罚款不可终止 |
| 会员类型变更 | — | — | ✅ | — | — |
| 罚款 | ✅ | — | — | ✅ 缴纳/豁免/自动暂停/自动恢复 | ✅ 未缴清不可手动恢复 |
| 支付 | ✅ | ✅ | — | ✅ 处理/完成/取消/失败 | ✅ 支付失败 |
| 退款 | ✅ | ✅ | — | ✅ 全额/部分退款 | — |
| 分析报告 | ✅ | ✅ | — | ✅ 完成/失败/取消/重新生成 | ✅ 报告生成失败 |
| 通知 | ✅ | — | — | ✅ 调度/发送/送达/已读/失败/重试 | ✅ 发送失败+重试 |
