# 📚 图书管理模块 (Catalog)

> **端口**: 8081 | **API 前缀**: `/api/catalog` | **数据库**: `library_catalog`

## 概述

Catalog bounded context 负责管理书籍的元数据信息，包括书籍、作者、出版商和分类。它是整个图书馆系统的数据基础——所有书籍必须先在 Catalog 中创建和发布，才能进入库存和流通环节。

---

## API 一览

### 书籍管理 (BookController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/catalog/books` | 创建书籍 |
| GET | `/api/catalog/books/{id}` | 查询书籍详情 |
| GET | `/api/catalog/books` | 查询所有书籍 |
| PUT | `/api/catalog/books/{id}` | 更新书籍信息 |
| POST | `/api/catalog/books/{id}/publish` | 发布书籍 |
| POST | `/api/catalog/books/{id}/unpublish` | 下架书籍 |
| DELETE | `/api/catalog/books/{id}` | 删除书籍 |
| GET | `/api/catalog/books/search` | 搜索书籍（分页+过滤） |
| POST | `/api/catalog/books/{id}/authors` | 添加作者关联 |
| DELETE | `/api/catalog/books/{id}/authors/{authorId}` | 移除作者关联 |
| PUT | `/api/catalog/books/{id}/publisher` | 设置出版商 |
| POST | `/api/catalog/books/{id}/categories` | 添加分类关联 |
| DELETE | `/api/catalog/books/{id}/categories/{categoryId}` | 移除分类关联 |

### 作者管理 (AuthorController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/catalog/authors` | 创建作者 |
| GET | `/api/catalog/authors/{id}` | 查询作者详情 |
| GET | `/api/catalog/authors` | 查询所有作者 |
| PUT | `/api/catalog/authors/{id}` | 更新作者信息 |
| DELETE | `/api/catalog/authors/{id}` | 删除作者 |

### 出版商管理 (PublisherController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/catalog/publishers` | 创建出版商 |
| GET | `/api/catalog/publishers/{id}` | 查询出版商详情 |
| GET | `/api/catalog/publishers` | 查询所有出版商 |
| PUT | `/api/catalog/publishers/{id}` | 更新出版商信息 |
| GET | `/api/catalog/publishers/search` | 按名称搜索出版商（分页） |
| DELETE | `/api/catalog/publishers/{id}` | 删除出版商 |

### 分类管理 (CategoryController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/catalog/categories` | 创建根分类 |
| POST | `/api/catalog/categories/{parentId}/children` | 创建子分类 |
| PUT | `/api/catalog/categories/{id}` | 更新分类 |
| GET | `/api/catalog/categories/{id}` | 查询分类详情 |
| GET | `/api/catalog/categories` | 查询所有分类 |
| GET | `/api/catalog/categories/roots` | 查询根分类 |
| DELETE | `/api/catalog/categories/{id}` | 删除分类 |

---

## 详细用法

### 创建完整书籍（典型流程）

#### 1. 创建作者

```bash
curl -X POST http://localhost:8081/api/catalog/authors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Martin Fowler",
    "biography": "Author of Refactoring, pioneer of enterprise architecture",
    "birthDate": "1963-12-18",
    "nationality": "British"
  }'
```

响应：

```json
{
  "success": true,
  "data": {
    "id": "78448d1d-256b-4514-b100-7c4aff9a5558",
    "name": "Martin Fowler",
    "biography": "Author of Refactoring, pioneer of enterprise architecture",
    "birthDate": "1963-12-18",
    "nationality": "British"
  }
}
```

> 记下响应中的 `id`，后续步骤作为 `AUTHOR_ID` 使用。

#### 2. 创建出版商

```bash
curl -X POST http://localhost:8081/api/catalog/publishers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Addison-Wesley",
    "description": "Premier technology publisher",
    "address": "Boston, MA",
    "phone": "617-555-0100",
    "email": "info@aw.com",
    "website": "https://aw.com"
  }'
```

> 记下 `id`，作为 `PUBLISHER_ID`。

#### 3. 创建分类

```bash
curl -X POST http://localhost:8081/api/catalog/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Software Engineering",
    "description": "Software design and patterns"
  }'
```

> 记下 `id`，作为 `CATEGORY_ID`。

#### 4. 创建子分类（可选）

```bash
curl -X POST http://localhost:8081/api/catalog/categories/$CATEGORY_ID/children \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Refactoring",
    "description": "Code refactoring techniques"
  }'
```

#### 5. 创建书籍

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

> 记下 `id`，作为 `BOOK_ID`。此时书籍状态为 `DRAFT`。

#### 6. 关联元数据

```bash
# 添加作者
curl -X POST "http://localhost:8081/api/catalog/books/$BOOK_ID/authors?authorId=$AUTHOR_ID&role=AUTHOR"

# 设置出版商
curl -X PUT "http://localhost:8081/api/catalog/books/$BOOK_ID/publisher?publisherId=$PUBLISHER_ID"

# 添加分类
curl -X POST "http://localhost:8081/api/catalog/books/$BOOK_ID/categories?categoryId=$CATEGORY_ID"
```

#### 7. 发布书籍

```bash
curl -X POST http://localhost:8081/api/catalog/books/$BOOK_ID/publish
```

状态变更：`DRAFT` → `PUBLISHED`

### 搜索书籍

```bash
# 按标题搜索
curl "http://localhost:8081/api/catalog/books/search?title=Clean&page=0&size=10"

# 按状态过滤
curl "http://localhost:8081/api/catalog/books/search?status=PUBLISHED&page=0&size=10"

# 组合搜索
curl "http://localhost:8081/api/catalog/books/search?title=Clean&status=PUBLISHED&language=English&page=0&size=10"
```

### 更新和下架

```bash
# 更新书籍信息
curl -X PUT http://localhost:8081/api/catalog/books/$BOOK_ID \
  -H "Content-Type: application/json" \
  -d '{"title": "Clean Code: A Handbook of Agile Software Craftsmanship", "description": "Updated description"}'

# 下架书籍
curl -X POST http://localhost:8081/api/catalog/books/$BOOK_ID/unpublish
```

### 删除书籍

```bash
curl -X DELETE http://localhost:8081/api/catalog/books/$BOOK_ID
```

### 搜索出版商

```bash
curl "http://localhost:8081/api/catalog/publishers/search?name=Addison&page=0&size=10"
```

---

## 业务规则

- **书籍状态流转**: `DRAFT` → `PUBLISHED` → `UNPUBLISHED` → `DELETED`
- **发布前置条件**: 书籍必须至少有一个作者、一个出版商、一个分类才能发布
- **ISBN 校验**: 支持 ISBN-10 和 ISBN-13 格式校验
- **作者角色**: `AUTHOR`, `CO_AUTHOR`, `EDITOR`, `TRANSLATOR`, `CONTRIBUTOR`
- **分类结构**: 支持树形结构（parent-child），可多层嵌套
- **删除规则**: 删除书籍为逻辑删除，状态标记为 `DELETED`

---

## Kafka 事件

### 发布事件

| 事件 | 触发时机 |
|------|---------|
| BookCreatedEvent | 书籍创建时 |
| BookPublishedEvent | 书籍发布时 |
| BookUpdatedEvent | 书籍信息更新时 |
| BookDeletedEvent | 书籍删除时 |

### 消费事件

无（Catalog 是事件的源头，不消费其他模块的事件）

---

## 返回 [README](../../README.md)
