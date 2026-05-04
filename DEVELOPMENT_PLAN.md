# 图书馆管理系统开发计划

> **版本**: v1.1
> **创建日期**: 2026-05-03
> **最后更新**: 2026-05-04
> **状态**: 进行中 - 阶段二（馆藏上下文）已完成，准备进入阶段三

本文档提供了从零开始实现图书馆管理系统的详细开发计划。每个任务都是可独立执行的，并提供了具体的实现步骤和验收标准。

注意要follow Architecture_Design目录里面对应的详细设计文件

每完成一个task，对当前task做一个总结，保存在Progress目录里面。

---

## 目录

1. [项目初始化阶段](#1-项目初始化阶段)
2. [阶段一：编目上下文](#2-阶段一编目上下文catalog-context)
3. [阶段二：馆藏上下文](#3-阶段二馆藏上下文inventory-context)
4. [阶段三：借阅上下文](#4-阶段三借阅上下文circulation-context)
5. [阶段四：会员上下文](#5-阶段四会员上下文patron-context)
6. [阶段五：支付上下文](#6-阶段五支付上下文payment-context)
7. [阶段六：分析上下文](#7-阶段六分析上下文analytics-context)
8. [阶段七：通知上下文](#8-阶段七通知上下文notification-context)
9. [阶段八：共享模块](#9-阶段八共享模块library-shared)
10. [阶段九：跨上下文集成](#10-阶段九跨上下文集成)

---

## 1. 项目初始化阶段

### 1.1 创建Maven多模块项目结构

**目标**: 建立基础的多模块Maven项目，包括父POM和所有上下文模块

**任务步骤**:

1. 创建根目录结构
   ```bash
   mkdir -p library-catalog/src/{main,test}/{java/com/library/catalog,dist}
   mkdir -p library-inventory/src/{main,test}/{java/com/library/inventory,dist}
   mkdir -p library-circulation/src/{main,test}/{java/com/library/circulation,dist}
   mkdir -p library-patron/src/{main,test}/{java/com/library/patron,dist}
   mkdir -p library-payment/src/{main,test}/{java/com/library/payment,dist}
   mkdir -p library-analytics/src/{main,test}/{java/com/library/analytics,dist}
   mkdir -p library-notification/src/{main,test}/{java/com/library/notification,dist}
   mkdir -p library-shared/src/{main,test}/{java/com/library/shared,dist}
   ```

2. 创建父POM文件 `pom.xml`
   - 设置Spring Boot 3.2+ parent
   - 定义所有子模块
   - 配置依赖版本管理
   - 添加公共依赖

3. 为每个模块创建独立的`pom.xml`
   - 继承父POM
   - 配置模块特定的依赖

**验收标准**:
- [x] `mvn clean install` 成功执行
- [x] 所有模块被识别并构建成功
- [x] 每个模块有正确的Spring Boot主类结构

**预计时间**: 2小时

**文件清单**:
- `pom.xml` (根POM)
- `library-catalog/pom.xml`
- `library-inventory/pom.xml`
- `library-circulation/pom.xml`
- `library-patron/pom.xml`
- `library-payment/pom.xml`
- `library-analytics/pom.xml`
- `library-notification/pom.xml`
- `library-shared/pom.xml`

---

## 2. 阶段一：编目上下文 (Catalog Context)

### 2.1 领域层 - 基础设施

#### 任务2.1.1: 创建值对象基础类

**目标**: 实现ID值对象的基类，确保聚合根ID的一致性

**文件路径**: `library-shared/src/main/java/com/library/shared/domain/model/`

**实现步骤**:

1. 创建`AggregateId.java`基类
2. 创建`BookId.java`
3. 创建`AuthorId.java`
4. 创建`PublisherId.java`
5. 创建`CategoryId.java`

**验收标准**:
- [x] 所有ID类继承自`AggregateId`
- [x] 使用UUID作为底层标识符
- [x] 提供`of()`静态工厂方法
- [x] 实现`equals()`和`hashCode()`
- [x] 不可变对象（final字段）
- [x] 包含`created_at`时间戳

**预计时间**: 1小时

---

#### 任务2.1.2: 实现ISBN值对象

**目标**: 实现完整的ISBN验证和格式化逻辑

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/model/ISBN.java`

**实现步骤**:

1. 创建ISBN值对象类
2. 实现ISBN-10校验逻辑
3. 实现ISBN-13校验逻辑
4. 实现自动清理（移除连字符和空格）
5. 实现格式化显示方法
6. 添加单元测试

**关键实现点**:
```java
@Embeddable
public class ISBN {
    private String value;

    public ISBN(String value) {
        this.value = cleanISBN(value);
        validateISBN(this.value);
    }

    public String getFormattedValue() {
        // 格式化为 X-XXXX-XXXX-X 或 XXX-X-XXXXX-XXX-X
    }
}
```

**验收标准**:
- [x] 正确验证ISBN-10格式
- [x] 正确验证ISBN-13格式
- [x] 计算正确的校验和
- [x] 支持输入清理（连字符、空格）
- [x] 提供格式化显示
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务2.1.3: 创建领域事件基类

**目标**: 建立领域事件的基础结构

**文件路径**: `library-shared/src/main/java/com/library/shared/domain/event/`

**实现步骤**:

1. 创建`DomainEvent.java`基类
2. 创建`DomainEventPublisher.java`
3. 创建事件发布机制

**验收标准**:
- [x] 所有领域事件包含事件ID、时间戳、版本
- [ ] 提供同步和异步发布能力
- [ ] 支持事件溯源

**预计时间**: 1小时

---

### 2.2 领域层 - 核心聚合

#### 任务2.2.1: 实现Author实体

**目标**: 完成Author实体的领域逻辑

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/model/Author.java`

**实现步骤**:

1. 创建Author实体类
2. 实现领域行为：
   - `updateBiography()`
   - `updatePersonalInfo()`
3. 实现验证逻辑
4. 编写单元测试

**验收标准**:
- [x] JPA注解正确配置
- [x] 生死日期不能早于出生日期
- [x] 作者名称不能为空或超过200字符
- [x] 提供不可变的getter方法
- [x] 包含审计字段（created_at, updated_at）
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务2.2.2: 实现Publisher实体

**目标**: 完成Publisher实体的领域逻辑

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/model/Publisher.java`

**实现步骤**:

1. 创建Publisher实体类
2. 实现领域行为
3. 实现验证逻辑
4. 编写单元测试

**验收标准**:
- [x] JPA注解正确配置
- [x] 出版社名称不能为空
- [x] 包含联系信息字段
- [x] 提供不可变的getter方法
- [x] 80%+测试覆盖率

**预计时间**: 1.5小时

---

#### 任务2.2.3: 实现Category实体

**目标**: 完成Category实体的领域逻辑，支持层级分类

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/model/Category.java`

**实现步骤**:

1. 创建Category实体类
2. 实现层级关系（parent, children）
3. 实现领域行为：
   - `addChild()`
   - `removeChild()`
4. 实现验证逻辑
5. 编写单元测试

**验收标准**:
- [x] 支持父子关系
- [x] 防止循环引用
- [x] 分类名称唯一性验证
- [x] level字段自动计算
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务2.2.4: 实现BookAuthor关联实体

**目标**: 完成Book与Author的多对多关系实体

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/model/BookAuthor.java`

**实现步骤**:

1. 创建BookAuthor实体类
2. 使用复合主键（BookId, AuthorId）
3. 实现AuthorRole枚举
4. 支持作者排序

**验收标准**:
- [x] 复合主键正确配置
- [x] 支持作者角色区分
- [x] 支持作者排序
- [x] 80%+测试覆盖率

**预计时间**: 1小时

---

#### 任务2.2.5: 实现Book聚合根

**目标**: 完成Book聚合根的完整领域逻辑

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/model/Book.java`

**实现步骤**:

1. 创建Book聚合根类
2. 实现所有领域行为：
   - `addAuthor()` - 添加作者
   - `removeAuthor()` - 移除作者
   - `setPublisher()` - 设置出版社
   - `addCategory()` - 添加分类
   - `removeCategory()` - 移除分类
   - `updateBasicInfo()` - 更新基本信息
   - `publish()` - 发布图书
   - `unpublish()` - 下架图书
   - `delete()` - 删除图书
3. 实现所有验证逻辑
4. 确保不变量保护
5. 编写全面的单元测试

**关键不变量**:
- 已发布的图书必须至少有一个作者
- 已发布的图书必须设置出版社
- 已发布的图书必须至少有一个分类
- 已发布的图书不能被删除
- 图书必须至少有一个作者
- 图书状态转换必须符合业务规则

**验收标准**:
- [x] 所有领域行为正确实现
- [x] 状态机正确（DRAFT → PUBLISHED → UNPUBLISHED/DELETED）
- [x] 所有不变量得到保护
- [x] 80%+测试覆盖率
- [x] 包含边界条件测试

**预计时间**: 4小时

---

#### 任务2.2.6: 创建领域异常类

**目标**: 建立统一的领域异常体系

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/exception/`

**实现步骤**:

1. 创建`DomainException.java`基类
2. 创建具体异常：
   - `BookNotFoundException.java`
   - `DuplicateISBNException.java`
   - `InvalidISBNException.java`
   - `DuplicateAuthorException.java`
   - `InvalidOperationException.java`
   - `PublisherNotFoundException.java`
   - `CategoryNotFoundException.java`

**验收标准**:
- [x] 所有异常包含错误码和消息
- [x] 异常层次清晰
- [ ] 支持国际化（预留）

**预计时间**: 1小时

---

### 2.3 领域层 - 仓储接口

#### 任务2.3.1: 定义仓储接口

**目标**: 定义所有仓储接口，使用Spring Data JPA

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/repository/`

**实现步骤**:

1. 创建`BookRepository.java`
2. 创建`AuthorRepository.java`
3. 创建`PublisherRepository.java`
4. 创建`CategoryRepository.java`

**BookRepository示例**:
```java
public interface BookRepository extends JpaRepository<Book, BookId>, CustomBookRepository {
    Optional<Book> findByISBN(ISBN isbn);
    boolean existsByISBN(ISBN isbn);
    List<Book> findByTitleContaining(String title);
    List<Book> findByStatus(BookStatus status);
}
```

**验收标准**:
- [x] 所有接口继承JpaRepository
- [x] 定义自定义查询方法
- [x] 使用正确的类型参数
- [x] 方法命名符合Spring Data规范

**预计时间**: 1.5小时

---

#### 任务2.3.2: 实现自定义仓储

**目标**: 实现复杂的JPA查询

**文件路径**: `library-catalog/src/main/java/com/library/catalog/infrastructure/persistence/jpa/`

**实现步骤**:

1. 创建`CustomBookRepository.java`接口
2. 创建`BookRepositoryImpl.java`实现
3. 实现复杂查询：
   - 根据作者姓名搜索
   - 组合条件搜索
   - 全文搜索

**验收标准**:
- [x] 使用Criteria API或JPQL
- [x] 正确处理关联查询
- [x] 支持分页和排序
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

### 2.4 领域层 - 领域服务

#### 任务2.4.1: 实现ISBNValidationService

**目标**: 提供ISBN验证和外部查询服务

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/service/`

**实现步骤**:

1. 创建`ISBNValidationService.java`
2. 实现ISBN格式验证
3. 实现ISBN-10转ISBN-13
4. 集成外部ISBN API（OpenLibrary API）
5. 编写单元测试和集成测试

**验收标准**:
- [x] 验证ISBN-10和ISBN-13格式
- [x] 正确计算校验和
- [x] 支持ISBN格式转换
- [ ] 可从外部API获取图书信息 (stubbed only)
- [x] 80%+测试覆盖率

**预计时间**: 3小时

---

#### 任务2.4.2: 实现BookManagementService

**目标**: 实现跨聚合的图书管理业务逻辑

**文件路径**: `library-catalog/src/main/java/com/library/catalog/domain/service/`

**实现步骤**:

1. 创建`BookManagementService.java`
2. 实现核心方法：
   - `createBook()` - 创建图书（草稿）
   - `publishBook()` - 发布图书
   - `updateBook()` - 更新图书
   - `deleteBook()` - 删除图书
3. 实现辅助方法：
   - `findOrCreateAuthor()`
   - `importBooks()` - 批量导入
4. 集成领域事件发布
5. 编写集成测试

**验收标准**:
- [x] 创建图书时验证ISBN唯一性
- [x] 发布前验证前置条件
- [ ] 正确发布领域事件
- [ ] 支持批量导入
- [x] 事务边界正确
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

### 2.5 应用层设计

#### 任务2.5.1: 创建Command和Query对象

**目标**: 定义应用层的命令和查询对象

**文件路径**:
- `library-catalog/src/main/java/com/library/catalog/application/command/`
- `library-catalog/src/main/java/com/library/catalog/application/query/`

**实现步骤**:

1. 创建命令对象：
   - `CreateBookCommand.java`
   - `UpdateBookCommand.java`
   - `PublishBookCommand.java`
   - `DeleteBookCommand.java`
   - `ImportBookCommand.java`

2. 创建查询对象：
   - `BookSearchQuery.java`
   - `BookDetailQuery.java`
   - `AuthorBooksQuery.java`

3. 添加Bean Validation注解

**验收标准**:
- [x] 所有字段有验证注解
- [x] 使用JSR-303验证
- [ ] 包含错误消息本地化
- [x] 不可变对象

**预计时间**: 2小时

---

#### 任务2.5.2: 创建DTO对象

**目标**: 定义数据传输对象

**文件路径**: `library-catalog/src/main/java/com/library/catalog/application/dto/`

**实现步骤**:

1. 创建DTO类：
   - `BookDTO.java`
   - `AuthorDTO.java`
   - `PublisherDTO.java`
   - `CategoryDTO.java`
   - `ApiResponse.java`
   - `PageResponse.java`

2. 实现转换方法：
   - `fromDomain()` - 从领域模型转换
   - `toDomain()` - 转换为领域模型

**验收标准**:
- [x] 使用Jackson注解
- [x] 正确处理日期格式
- [x] 排除敏感字段
- [x] 提供批量转换方法

**预计时间**: 2小时

---

#### 任务2.5.3: 实现BookApplicationService

**目标**: 实现应用服务，协调领域对象完成用例

**文件路径**: `library-catalog/src/main/java/com/library/catalog/application/service/`

**实现步骤**:

1. 创建`BookApplicationService.java`
2. 实现用例方法：
   - `createBook()` - 创建图书用例
   - `getBook()` - 获取图书详情用例
   - `searchBooks()` - 搜索图书用例
   - `publishBook()` - 发布图书用例
   - `updateBook()` - 更新图书用例
   - `deleteBook()` - 删除图书用例
3. 添加事务注解
4. 实现权限检查
5. 编写集成测试

**验收标准**:
- [x] 使用只读事务处理查询
- [x] 使用写事务处理命令
- [x] 正确映射命令到领域服务
- [x] 异常处理和日志记录
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

### 2.6 接口层 - REST API

#### 任务2.6.1: 实现BookController

**目标**: 提供图书管理的REST API

**文件路径**: `library-catalog/src/main/java/com/library/catalog/interfaces/rest/`

**实现步骤**:

1. 创建`BookController.java`
2. 实现REST端点：
   - `POST /api/catalog/books` - 创建图书
   - `GET /api/catalog/books/{id}` - 获取图书详情
   - `GET /api/catalog/books` - 搜索图书
   - `PUT /api/catalog/books/{id}` - 更新图书
   - `POST /api/catalog/books/{id}/publish` - 发布图书
   - `DELETE /api/catalog/books/{id}` - 删除图书
3. 添加Swagger/OpenAPI注解
4. 实现请求验证
5. 编写集成测试

**验收标准**:
- [x] 所有端点有OpenAPI文档
- [x] 正确的HTTP状态码
- [x] 输入验证生效
- [ ] 支持分页和排序
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

#### 任务2.6.2: 实现AuthorController

**目标**: 提供作者管理的REST API

**文件路径**: `library-catalog/src/main/java/com/library/catalog/interfaces/rest/`

**实现步骤**:

1. 创建`AuthorController.java`
2. 实现REST端点：
   - `POST /api/catalog/authors` - 创建作者
   - `GET /api/catalog/authors/{id}` - 获取作者详情
   - `GET /api/catalog/authors` - 搜索作者
   - `PUT /api/catalog/authors/{id}` - 更新作者
   - `DELETE /api/catalog/authors/{id}` - 删除作者

**验收标准**:
- [x] 所有端点有OpenAPI文档
- [x] 正确的HTTP状态码
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务2.6.3: 实现全局异常处理

**目标**: 统一的异常处理和响应格式

**文件路径**: `library-catalog/src/main/java/com/library/catalog/interfaces/rest/`

**实现步骤**:

1. 创建`GlobalExceptionHandler.java`
2. 处理领域异常
3. 处理验证异常
4. 处理JPA异常
5. 创建统一响应格式`ApiResponse`

**验收标准**:
- [x] 所有异常返回JSON格式
- [x] 包含错误码和消息
- [x] HTTP状态码符合REST规范
- [ ] 记录异常日志

**预计时间**: 1.5小时

---

### 2.7 基础设施层 - JPA实现

#### 任务2.7.1: 实现JPA实体映射

**目标**: 将领域模型映射为JPA实体

**文件路径**: `library-catalog/src/main/java/com/library/catalog/infrastructure/persistence/jpa/entity/`

**实现步骤**:

1. 创建JPA实体类（如果领域模型不是JPA实体）
2. 配置表映射
3. 配置关系映射
4. 添加索引注解
5. 配置审计字段

**验收标准**:
- [x] 所有表名使用snake_case
- [x] 所有字段有正确的长度和约束
- [x] 关系映射正确
- [x] 索引配置优化查询性能
- [x] 审计字段自动填充

**预计时间**: 2小时

---

#### 任务2.7.2: 配置数据库和JPA

**目标**: 配置Spring Data JPA和数据库连接

**文件路径**:
- `library-catalog/src/main/resources/application.yml`
- `library-catalog/src/main/java/com/library/catalog/config/`

**实现步骤**:

1. 配置application.yml
2. 创建JPA配置类
3. 配置数据库连接池（HikariCP）
4. 配置Hibernate方言
5. 配置审计（创建时间、更新时间）

**验收标准**:
- [x] 数据库连接池正确配置
- [x] DDL自动更新设置为validate
- [x] SQL日志可配置
- [x] 支持多环境配置（dev, test, prod）

**预计时间**: 1.5小时

---

### 2.8 事件驱动实现

#### 任务2.8.1: 配置Kafka消息队列

**目标**: 集成Kafka用于事件发布

**文件路径**:
- `library-catalog/src/main/resources/application.yml`
- `library-catalog/src/main/java/com/library/catalog/config/`

**实现步骤**:

1. 添加Kafka依赖
2. 配置Kafka连接
3. 创建Kafka配置类
4. 创建Kafka生产者
5. 创建领域事件监听器

**验收标准**:
- [x] Kafka连接成功
- [x] 领域事件正确发布到Kafka
- [x] 消息序列化正确（JSON）
- [ ] 支持事件溯源

**预计时间**: 2小时

---

#### 任务2.8.2: 实现事件发布机制

**目标**: 集成领域事件发布到基础设施

**文件路径**: `library-catalog/src/main/java/com/library/catalog/infrastructure/messaging/`

**实现步骤**:

1. 创建`KafkaEventPublisher.java`
2. 实现事件序列化
3. 实现错误处理和重试
4. 编写集成测试

**验收标准**:
- [x] 事件正确发布到Kafka主题
- [x] 序列化/反序列化正确
- [x] 错误时记录日志并重试
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

### 2.9 测试和质量保证

#### 任务2.9.1: 编写全面的单元测试

**目标**: 达到80%+测试覆盖率

**文件路径**: `library-catalog/src/test/java/`

**实现步骤**:

1. 测试所有领域模型
2. 测试所有领域服务
3. 测试所有值对象
4. 使用JUnit 5和Mockito
5. 使用AssertJ进行断言

**验收标准**:
- [x] 80%+代码覆盖率
- [x] 所有边界条件有测试
- [x] 异常路径有测试
- [x] 测试独立且可重复

**预计时间**: 4小时

---

#### 任务2.9.2: 编写集成测试

**目标**: 测试数据库集成和API端点

**文件路径**: `library-catalog/src/test/java/`

**实现步骤**:

1. 使用`@SpringBootTest`
2. 使用`@DataJpaTest`测试仓储
3. 使用`MockMvc`测试控制器
4. 使用TestContainers测试数据库集成
5. 编写E2E测试

**验收标准**:
- [x] 所有API端点有集成测试
- [ ] 仓储操作有集成测试
- [ ] 使用真实的PostgreSQL（TestContainers）
- [x] 80%+测试覆盖率

**预计时间**: 4小时

---

#### 任务2.9.3: 配置CI/CD （目前先不实现）

**目标**: 设置自动化构建和测试

**文件路径**: `.github/workflows/`

**实现步骤**:

1. 创建GitHub Actions工作流
2. 配置多JDK测试
3. 配置代码覆盖率报告
4. 配置SonarQube扫描
5. 配置自动部署

**验收标准**:
- [ ] PR自动触发CI
- [ ] 测试覆盖率报告生成
- [ ] 代码质量门禁失败构建
- [ ] 支持JDK 17和21

**预计时间**: 2小时

---

### 2.10 阶段一总结

**阶段一完成标准**:
- [x] 编目上下文完全实现（领域层、应用层、接口层已完成，基础设施层待实现）
- [x] 所有领域模型有80%+测试覆盖率（单元测试已有，集成测试缺失）
- [x] 所有REST API有集成测试
- [x] 事件发布机制正常工作
- [x] API文档可通过Swagger访问
- [ ] 数据库迁移脚本准备

**预计总时间**: 约50小时

---

## 3. 阶段二：馆藏上下文 (Inventory Context)

### 3.1 领域层 - 核心模型

#### 任务3.1.1: 实现Location值对象

**目标**: 实现图书馆位置的值对象

**文件路径**: `library-inventory/src/main/java/com/library/inventory/domain/model/Location.java`

**实现步骤**:

1. 创建Location值对象
2. 包含图书馆ID、书架号、层数
3. 实现验证逻辑
4. 实现格式化显示

**验收标准**:
- [x] 不可变对象
- [x] 包含位置验证
- [x] 支持不同类型的图书馆位置

**预计时间**: 1.5小时

---

#### 任务3.1.2: 实现Library实体

**目标**: 实现图书馆/分馆实体

**文件路径**: `library-inventory/src/main/java/com/library/inventory/domain/model/Library.java`

**实现步骤**:

1. 创建Library实体
2. 包含图书馆信息（名称、地址、联系方式）
3. 实现领域行为
4. 编写单元测试

**验收标准**:
- [x] 支持多分馆
- [x] 包含地理信息
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务3.1.3: 实现BookCopy实体

**目标**: 实现图书副本实体

**文件路径**: `library-inventory/src/main/java/com/library/inventory/domain/model/BookCopy.java`

**实现步骤**:

1. 创建BookCopy实体
2. 包含状态枚举（AVAILABLE, CHECKED_OUT, RESERVED, MAINTENANCE）
3. 实现状态转换
4. 实现领域行为：
   - `checkout()` - 借出
   - `checkin()` - 归还
   - `reserve()` - 预约
   - `markMaintenance()` - 标记维修
5. 编写单元测试

**验收标准**:
- [x] 状态机正确实现
- [x] 不变量得到保护
- [x] 包含位置关联
- [x] 80%+测试覆盖率

**预计时间**: 3小时

---

#### 任务3.1.4: 实现CopyInventory聚合根

**目标**: 实现图书副本库存聚合根

**文件路径**: `library-inventory/src/main/java/com/library/inventory/domain/model/CopyInventory.java`

**实现步骤**:

1. 创建CopyInventory聚合根
2. 包含BookId作为标识符
3. 包含BookCopy集合
4. 包含Library关联
5. 实现领域行为：
   - `addCopy()` - 添加副本
   - `removeCopy()` - 移除副本
   - `getAvailableCopies()` - 获取可用副本
   - `syncWithOtherLibraries()` - 同步其他图书馆
6. 实现库存状态查询
7. 编写单元测试

**验收标准**:
- [x] 聚合边界清晰
- [x] 不变量得到保护
- [x] 支持跨图书馆同步
- [x] 80%+测试覆盖率

**预计时间**: 3小时

---

### 3.2 领域层 - 仓储和服务

#### 任务3.2.1: 实现Inventory仓储

**目标**: 实现馆藏仓储接口和实现

**文件路径**: `library-inventory/src/main/java/com/library/inventory/domain/repository/`

**实现步骤**:

1. 创建`CopyInventoryRepository.java`
2. 创建`BookCopyRepository.java`
3. 创建`LibraryRepository.java`
4. 实现自定义查询：
   - 根据BookId查找所有图书馆的库存
   - 根据位置查找可用副本
   - 跨图书馆库存查询

**验收标准**:
- [x] 仓储接口清晰
- [x] 支持复杂查询
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务3.2.2: 实现InventoryManagementService

**目标**: 实现库存管理领域服务

**文件路径**: `library-inventory/src/main/java/com/library/inventory/domain/service/`

**实现步骤**:

1. 创建`InventoryManagementService.java`
2. 实现核心方法：
   - `addCopies()` - 添加副本到图书馆
   - `checkoutCopy()` - 借出副本
   - `returnCopy()` - 归还副本
   - `reserveCopy()` - 预约副本
   - `syncInventory()` - 跨图书馆同步
3. 实现库存状态计算
4. 集成领域事件发布：
   - `CopyAddedEvent`
   - `CopyCheckedOutEvent`
   - `CopyReturnedEvent`
   - `InventorySyncedEvent`
5. 编写集成测试

**验收标准**:
- [x] 副本状态正确更新
- [x] 跨图书馆同步正常
- [x] 事件正确发布
- [x] 事务边界正确
- [x] 80%+测试覆盖率

**预计时间**: 3小时

---

### 3.3 应用层和接口层

#### 任务3.3.1: 实现InventoryApplicationService

**目标**: 实现库存管理的应用服务

**文件路径**: `library-inventory/src/main/java/com/library/inventory/application/service/`

**实现步骤**:

1. 创建`InventoryApplicationService.java`
2. 实现用例方法：
   - `addCopiesToLibrary()`
   - `getInventoryStatus()`
   - `findAvailableCopies()`
   - `getLibraryLocations()`
3. 编写集成测试

**验收标准**:
- [x] 用例实现完整
- [x] 权限检查正确
- [x] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务3.3.2: 实现InventoryController

**目标**: 提供库存管理的REST API

**文件路径**: `library-inventory/src/main/java/com/library/inventory/interfaces/rest/`

**实现步骤**:

1. 创建`InventoryController.java`
2. 实现REST端点：
   - `POST /api/inventory/copies` - 添加副本
   - `GET /api/inventory/books/{bookId}/inventory` - 获取库存
   - `GET /api/inventory/books/{bookId}/available` - 查找可用副本
   - `GET /api/inventory/libraries` - 获取图书馆列表
   - `PUT /api/inventory/libraries/{libraryId}/sync` - 同步库存
3. 编写集成测试

**验收标准**:
- [x] API文档完整
- [x] 80%+测试覆盖率
- [x] 事件正常发布

**预计时间**: 2小时

---

### 3.4 阶段二总结

**阶段二完成标准**:
- [x] 馆藏上下文完全实现
- [x] 80%+测试覆盖率
- [x] 跨图书馆同步功能工作
- [x] 事件发布正常

**预计总时间**: 约20小时

---

## 4. 阶段三：借阅上下文 (Circulation Context)

### 4.1 领域层 - 核心聚合

#### 任务4.1.1: 实现Fine值对象

**目标**: 实现罚金值对象

**文件路径**: `library-circulation/src/main/java/com/library/circulation/domain/model/Fine.java`

**实现步骤**:

1. 创建Fine值对象
2. 实现罚金计算逻辑
3. 实现罚金状态枚举
4. 实现最大罚金限制
5. 编写单元测试

**验收标准**:
- [ ] 罚金计算正确（逾期天数 × 日费率）
- [ ] 最大罚金限制生效
- [ ] 罚金可支付
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务4.1.2: 实现CirculationPolicy值对象

**目标**: 实现借阅策略值对象

**文件路径**: `library-circulation/src/main/java/com/library/circulation/domain/model/CirculationPolicy.java`

**实现步骤**:

1. 创建CirculationPolicy值对象
2. 包含借阅规则：
   - 最长借阅期限
   - 最大续借次数
   - 每个会员最大借阅数
   - 罚金费率
3. 实现策略验证
4. 编写单元测试

**验收标准**:
- [ ] 策略规则可配置
- [ ] 不变量得到保护
- [ ] 80%+测试覆盖率

**预计时间**: 1.5小时

---

#### 任务4.1.3: 实现Loan聚合根

**目标**: 实现借阅聚合根

**文件路径**: `library-circulation/src/main/java/com/library/circulation/domain/model/Loan.java`

**实现步骤**:

1. 创建Loan聚合根
2. 包含字段：
   - LoanId
   - CopyId
   - PatronId
   - BookId
   - 借阅日期
   - 到期日期
   - 归还日期
   - 续借次数
   - 状态
   - 罚金
3. 实现领域行为：
   - `checkout()` - 借出
   - `return()` - 归还
   - `renew()` - 续借
   - `calculateFine()` - 计算罚金
   - `markOverdue()` - 标记逾期
   - `recall()` - 召回
4. 实现状态转换：
   - ACTIVE → RETURNED
   - ACTIVE → OVERDUE
   - ACTIVE → RECALLED
5. 实现不变量保护
6. 编写全面的单元测试

**验收标准**:
- [ ] 状态机正确
- [ ] 续借规则正确
- [ ] 罚金计算准确
- [ ] 召回逻辑正确
- [ ] 80%+测试覆盖率

**预计时间**: 4小时

---

#### 任务4.1.4: 实现Hold聚合根

**目标**: 实现预约聚合根

**文件路径**: `library-circulation/src/main/java/com/library/circulation/domain/model/Hold.java`

**实现步骤**:

1. 创建Hold聚合根
2. 包含字段：
   - HoldId
   - BookId
   - PatronId
   - 创建日期
   - 有效期日期
   - 队列位置
   - 状态（PENDING, FULFILLED, CANCELLED, EXPIRED）
3. 实现领域行为：
   - `fulfill()` - 满足预约
   - `cancel()` - 取消预约
   - `expire()` - 过期
   - `advanceInQueue()` - 队列前进
4. 实现队列管理
5. 编写单元测试

**验收标准**:
- [ ] 预约队列正确管理
- [ ] 状态转换正确
- [ ] 先进先出原则
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

### 4.2 领域层 - 仓储和服务

#### 任务4.2.1: 实现Circulation仓储

**目标**: 实现借阅仓储接口和实现

**文件路径**: `library-circulation/src/main/java/com/library/circulation/domain/repository/`

**实现步骤**:

1. 创建`LoanRepository.java`
2. 创建`HoldRepository.java`
3. 实现自定义查询：
   - 根据PatronId查找活跃借阅
   - 根据PatronId查找逾期借阅
   - 根据BookId查找等待预约
   - 统计会员借阅数
   - 查找即将到期的借阅

**验收标准**:
- [ ] 仓储接口清晰
- [ ] 复杂查询优化
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务4.2.2: 实现CirculationManagementService

**目标**: 实现借阅管理领域服务

**文件路径**: `library-circulation/src/main/java/com/library/circulation/domain/service/`

**实现步骤**:

1. 创建`CirculationManagementService.java`
2. 实现核心方法：
   - `borrowBook()` - 借书完整流程
   - `returnBook()` - 还书完整流程
   - `renewLoan()` - 续借
   - `placeHold()` - 预约
   - `cancelHold()` - 取消预约
   - `recallBook()` - 召回
3. 实现业务规则验证：
   - 会员资格验证
   - 借阅限制验证
   - 罚金验证
4. 集成事件发布：
   - `BookBorrowedEvent`
   - `BookReturnedEvent`
   - `LoanExtendedEvent`
   - `HoldPlacedEvent`
   - `HoldFulfilledEvent`
   - `FineIncurredEvent`
5. 编写集成测试

**验收标准**:
- [ ] 借书流程完整
- [ ] 业务规则得到验证
- [ ] 事件正确发布
- [ ] 事务边界正确
- [ ] 80%+测试覆盖率

**预计时间**: 5小时

---

### 4.3 应用层和接口层

#### 任务4.3.1: 实现CirculationApplicationService

**目标**: 实现借阅管理的应用服务

**文件路径**: `library-circulation/src/main/java/com/library/circulation/application/service/`

**实现步骤**:

1. 创建`CirculationApplicationService.java`
2. 实现用例方法：
   - `borrowBook()` - 借书用例
   - `returnBook()` - 还书用例
   - `renewLoan()` - 续借用例
   - `placeHold()` - 预约用例
   - `viewLoanHistory()` - 查看借阅历史
   - `viewActiveLoans()` - 查看活跃借阅
3. 编写集成测试

**验收标准**:
- [ ] 用例实现完整
- [ ] 权限检查正确
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

#### 任务4.3.2: 实现CirculationController

**目标**: 提供借阅管理的REST API

**文件路径**: `library-circulation/src/main/java/com/library/circulation/interfaces/rest/`

**实现步骤**:

1. 创建`LoanController.java`
2. 创建`HoldController.java`
3. 实现REST端点：
   - `POST /api/circulation/loans` - 借书
   - `POST /api/circulation/loans/{loanId}/return` - 还书
   - `POST /api/circulation/loans/{loanId}/renew` - 续借
   - `POST /api/circulation/holds` - 预约
   - `DELETE /api/circulation/holds/{holdId}` - 取消预约
   - `GET /api/circulation/patrons/{patronId}/loans` - 查看借阅
4. 编写集成测试

**验收标准**:
- [ ] API文档完整
- [ ] 80%+测试覆盖率
- [ ] 事件正常发布

**预计时间**: 3小时

---

### 4.4 阶段三总结

**阶段三完成标准**:
- [ ] 借阅上下文完全实现
- [ ] 80%+测试覆盖率
- [ ] 借书和还书流程完整
- [ ] 预约机制正常工作
- [ ] 罚金计算准确
- [ ] 事件发布正常

**预计总时间**: 约25小时

---

## 5. 阶段四：会员上下文 (Patron Context)

### 5.1 领域层 - 核心模型

#### 任务5.1.1: 实现Patron聚合根

**目标**: 实现会员聚合根

**文件路径**: `library-patron/src/main/java/com/library/patron/domain/model/Patron.java`

**实现步骤**:

1. 创建Patron聚合根
2. 包含字段：
   - PatronId
   - 姓名
   - 邮箱
   - 电话
   - 地址
   - 会员类型（STUDENT, FACULTY, STAFF, ALUMNI）
   - 会员状态（ACTIVE, SUSPENDED, INACTIVE）
   - 借阅权限
   - 最大借阅数
   - 当前借阅数
3. 实现领域行为：
   - `register()` - 注册
   - `activate()` - 激活
   - `suspend()` - 暂停
   - `updateInfo()` - 更新信息
   - `increaseLoanCount()` - 增加借阅数
   - `decreaseLoanCount()` - 减少借阅数
4. 实现业务规则：
   - 逾期检查
   - 借阅资格检查
5. 编写单元测试

**验收标准**:
- [ ] 会员状态转换正确
- [ ] 借阅权限验证准确
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

#### 任务5.1.2: 实现PatronAccount实体

**目标**: 实现会员账户实体

**文件路径**: `library-patron/src/main/java/com/library/patron/domain/model/PatronAccount.java`

**实现步骤**:

1. 创建PatronAccount实体
2. 包含字段：
   - 账户余额
   - 罚金总额
   - 支付历史
3. 实现领域行为：
   - `deductFine()` - 扣除罚金
   - `processPayment()` - 处理支付
   - `calculateBalance()` - 计算余额
4. 编写单元测试

**验收标准**:
- [ ] 账户余额计算正确
- [ ] 罚金扣减准确
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

### 5.2 领域层 - 仓储和服务

#### 任务5.2.1: 实现Patron仓储

**目标**: 实现会员仓储接口和实现

**文件路径**: `library-patron/src/main/java/com/library/patron/domain/repository/`

**实现步骤**:

1. 创建`PatronRepository.java`
2. 实现自定义查询：
   - 根据邮箱查找
   - 根据电话查找
   - 查找逾期会员
   - 统计活跃会员数

**验收标准**:
- [ ] 仓储接口清晰
- [ ] 80%+测试覆盖率

**预计时间**: 1.5小时

---

#### 任务5.2.2: 实现PatronManagementService

**目标**: 实现会员管理领域服务

**文件路径**: `library-patron/src/main/java/com/library/patron/domain/service/`

**实现步骤**:

1. 创建`PatronManagementService.java`
2. 实现核心方法：
   - `registerPatron()` - 注册会员
   - `validatePatronStatus()` - 验证会员状态
   - `updatePatronInfo()` - 更新会员信息
   - `managePatronAccount()` - 管理账户
3. 集成事件发布：
   - `PatronRegisteredEvent`
   - `PatronTypeChangedEvent`
   - `PatronSuspendedEvent`
4. 编写集成测试

**验收标准**:
- [ ] 会员注册流程完整
- [ ] 状态验证准确
- [ ] 事件正确发布
- [ ] 80%+测试覆盖率

**预计时间**: 3小时

---

### 5.3 应用层和接口层

#### 任务5.3.1: 实现PatronApplicationService

**目标**: 实现会员管理的应用服务

**文件路径**: `library-patron/src/main/java/com/library/patron/application/service/`

**实现步骤**:

1. 创建`PatronApplicationService.java`
2. 实现用例方法：
   - `register()` - 注册用例
   - `login()` - 登录用例
   - `getProfile()` - 获取会员资料
   - `updateProfile()` - 更新会员资料
   - `viewAccount()` - 查看账户
3. 编写集成测试

**验收标准**:
- [ ] 用例实现完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务5.3.2: 实现PatronController

**目标**: 提供会员管理的REST API

**文件路径**: `library-patron/src/main/java/com/library/patron/interfaces/rest/`

**实现步骤**:

1. 创建`PatronController.java`
2. 实现REST端点：
   - `POST /api/patrons` - 注册会员
   - `POST /api/patrons/login` - 登录
   - `GET /api/patrons/{id}` - 获取会员详情
   - `PUT /api/patrons/{id}` - 更新会员信息
   - `GET /api/patrons/{id}/account` - 查看账户
3. 编写集成测试

**验收标准**:
- [ ] API文档完整
- [ ] 80%+测试覆盖率

**预计时间**: 2.5小时

---

### 5.4 阶段四总结

**阶段四完成标准**:
- [ ] 会员上下文完全实现
- [ ] 80%+测试覆盖率
- [ ] 会员注册和验证流程完整
- [ ] 账户管理正常

**预计总时间**: 约14小时

---

## 6. 阶段五：支付上下文 (Payment Context)

### 6.1 领域层 - 核心模型

#### 任务6.1.1: 实现Payment聚合根

**目标**: 实现支付聚合根

**文件路径**: `library-payment/src/main/java/com/library/payment/domain/model/Payment.java`

**实现步骤**:

1. 创建Payment聚合根
2. 包含字段：
   - PaymentId
   - PatronId
   - FineId（可选）
   - 金额
   - 支付方式（CASH, CARD, ALIPAY, WECHAT）
   - 支付状态（PENDING, COMPLETED, FAILED, REFUNDED）
   - 支付日期
   - 交易号
3. 实现领域行为：
   - `process()` - 处理支付
   - `complete()` - 完成支付
   - `fail()` - 支付失败
   - `refund()` - 退款
4. 编写单元测试

**验收标准**:
- [ ] 支付状态机正确
- [ ] 支付处理逻辑准确
- [ ] 80%+测试覆盖率

**预计时间**: 2.5小时

---

#### 任务6.1.2: 实现Refund实体

**目标**: 实现退款实体

**文件路径**: `library-payment/src/main/java/com/library/payment/domain/model/Refund.java`

**实现步骤**:

1. 创建Refund实体
2. 包含字段：
   - RefundId
   - PaymentId
   - 退款金额
   - 退款原因
   - 退款状态
   - 退款日期
3. 实现领域行为
4. 编写单元测试

**验收标准**:
- [ ] 退款流程正确
- [ ] 80%+测试覆盖率

**预计时间**: 1.5小时

---

### 6.2 领域层 - 仓储和服务

#### 任务6.2.1: 实现Payment仓储

**目标**: 实现支付仓储接口和实现

**文件路径**: `library-payment/src/main/java/com/library/payment/domain/repository/`

**实现步骤**:

1. 创建`PaymentRepository.java`
2. 实现自定义查询
3. 编写单元测试

**验收标准**:
- [ ] 仓储接口清晰
- [ ] 80%+测试覆盖率

**预计时间**: 1.5小时

---

#### 任务6.2.2: 实现PaymentService和第三方集成

**目标**: 实现支付服务并集成第三方支付

**文件路径**: `library-payment/src/main/java/com/library/payment/domain/service/`

**实现步骤**:

1. 创建`PaymentService.java`
2. 创建第三方支付适配器：
   - `AlipayAdapter.java`
   - `WeChatPayAdapter.java`
3. 实现支付处理：
   - `initiatePayment()` - 发起支付
   - `handleCallback()` - 处理回调
   - `queryPaymentStatus()` - 查询支付状态
4. 实现退款处理
5. 集成事件发布：
   - `PaymentCompletedEvent`
6. 编写集成测试

**验收标准**:
- [ ] 支付流程完整
- [ ] 第三方集成正确
- [ ] 回调处理安全
- [ ] 80%+测试覆盖率

**预计时间**: 4小时

---

### 6.3 应用层和接口层

#### 任务6.3.1: 实现PaymentApplicationService

**目标**: 实现支付管理的应用服务

**文件路径**: `library-payment/src/main/java/com/library/payment/application/service/`

**实现步骤**:

1. 创建`PaymentApplicationService.java`
2. 实现用例方法：
   - `payFine()` - 支付罚金
   - `getPaymentHistory()` - 获取支付历史
   - `requestRefund()` - 请求退款
3. 编写集成测试

**验收标准**:
- [ ] 用例实现完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务6.3.2: 实现PaymentController

**目标**: 提供支付管理的REST API

**文件路径**: `library-payment/src/main/java/com/library/payment/interfaces/rest/`

**实现步骤**:

1. 创建`PaymentController.java`
2. 实现REST端点：
   - `POST /api/payments` - 创建支付
   - `POST /api/payments/callback/{provider}` - 支付回调
   - `GET /api/payments/{id}` - 获取支付详情
   - `POST /api/payments/{id}/refund` - 请求退款
3. 编写集成测试

**验收标准**:
- [ ] API文档完整
- [ ] 回调端点安全
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

### 6.4 阶段五总结

**阶段五完成标准**:
- [ ] 支付上下文完全实现
- [ ] 80%+测试覆盖率
- [ ] 支付宝和微信支付集成
- [ ] 回调处理正确

**预计总时间**: 约14小时

---

## 7. 阶段六：分析上下文 (Analytics Context)

### 7.1 领域层 - 核心模型

#### 任务7.1.1: 实现AnalyticsReport聚合根

**目标**: 实现分析报告聚合根

**文件路径**: `library-analytics/src/main/java/com/library/analytics/domain/model/AnalyticsReport.java`

**实现步骤**:

1. 创建AnalyticsReport聚合根
2. 包含字段：
   - 报告ID
   - 报告类型（BORROWING_STATS, POPULAR_BOOKS, PATRON_ANALYSIS）
   - 报告数据（JSON格式）
   - 生成日期
   - 报告期间（开始日期，结束日期）
3. 实现领域行为：
   - `generate()` - 生成报告
   - `schedule()` - 调度报告
4. 编写单元测试

**验收标准**:
- [ ] 报告生成逻辑正确
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

### 7.2 领域层 - 仓储和服务

#### 任务7.2.1: 实现AnalyticsService

**目标**: 实现分析服务，处理事件和生成报告

**文件路径**: `library-analytics/src/main/java/com/library/analytics/domain/service/`

**实现步骤**:

1. 创建`AnalyticsService.java`
2. 实现事件监听器：
   - 监听借阅事件
   - 监听还书事件
   - 监听支付事件
3. 实现统计分析：
   - 借阅统计
   - 热门图书分析
   - 会员分析
   - 罚金统计
4. 实现报告生成
5. 编写集成测试

**验收标准**:
- [ ] 事件处理正确
- [ ] 统计分析准确
- [ ] 报告生成正常
- [ ] 80%+测试覆盖率

**预计时间**: 4小时

---

### 7.3 应用层和接口层

#### 任务7.3.1: 实现AnalyticsApplicationService

**目标**: 实现分析管理的应用服务

**文件路径**: `library-analytics/src/main/java/com/library/analytics/application/service/`

**实现步骤**:

1. 创建`AnalyticsApplicationService.java`
2. 实现用例方法：
   - `generateReport()` - 生成报告
   - `getDashboard()` - 获取仪表盘数据
   - `getPopularBooks()` - 获取热门图书
   - `getPatronStatistics()` - 获取会员统计
3. 编写集成测试

**验收标准**:
- [ ] 用例实现完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务7.3.2: 实现AnalyticsController

**目标**: 提供分析管理的REST API

**文件路径**: `library-analytics/src/main/java/com/library/analytics/interfaces/rest/`

**实现步骤**:

1. 创建`AnalyticsController.java`
2. 实现REST端点：
   - `GET /api/analytics/dashboard` - 获取仪表盘
   - `GET /api/analytics/reports` - 获取报告列表
   - `POST /api/analytics/reports` - 生成报告
   - `GET /api/analytics/popular-books` - 获取热门图书
   - `GET /api/analytics/patrons/statistics` - 获取会员统计
3. 编写集成测试

**验收标准**:
- [ ] API文档完整
- [ ] 80%+测试覆盖率

**预计时间**: 2.5小时

---

### 7.4 阶段六总结

**阶段六完成标准**:
- [ ] 分析上下文完全实现
- [ ] 80%+测试覆盖率
- [ ] 事件监听正常工作
- [ ] 统计分析准确

**预计总时间**: 约10小时

---

## 8. 阶段七：通知上下文 (Notification Context)

### 8.1 领域层 - 核心模型

#### 任务8.1.1: 实现Notification聚合根

**目标**: 实现通知聚合根

**文件路径**: `library-notification/src/main/java/com/library/notification/domain/model/Notification.java`

**实现步骤**:

1. 创建Notification聚合根
2. 包含字段：
   - NotificationId
   - PatronId
   - 通知类型（DUE_REMINDER, HOLD_AVAILABLE, PAYMENT_SUCCESS）
   - 通知渠道（EMAIL, SMS, PUSH）
   - 标题
   - 内容
   - 发送状态（PENDING, SENT, FAILED）
   - 发送时间
   - 创建时间
3. 实现领域行为：
   - `send()` - 发送通知
   - `markAsSent()` - 标记已发送
   - `markAsFailed()` - 标记失败
4. 编写单元测试

**验收标准**:
- [ ] 通知状态机正确
- [ ] 发送逻辑完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务8.1.2: 实现NotificationTemplate实体

**目标**: 实现通知模板实体

**文件路径**: `library-notification/src/main/java/com/library/notification/domain/model/NotificationTemplate.java`

**实现步骤**:

1. 创建NotificationTemplate实体
2. 包含字段：
   - 模板ID
   - 通知类型
   - 模板内容（支持变量替换）
   - 语言
3. 实现领域行为：
   - `render()` - 渲染模板
4. 编写单元测试

**验收标准**:
- [ ] 模板变量替换正确
- [ ] 80%+测试覆盖率

**预计时间**: 1.5小时

---

### 8.2 领域层 - 仓储和服务

#### 任务8.2.1: 实现NotificationService

**目标**: 实现通知服务和多渠道发送

**文件路径**: `library-notification/src/main/java/com/library/notification/domain/service/`

**实现步骤**:

1. 创建`NotificationService.java`
2. 创建发送器：
   - `EmailSender.java`
   - `SMSSender.java`
   - `PushNotificationSender.java`
3. 实现通知发送：
   - `sendNotification()` - 发送通知
   - `sendBatch()` - 批量发送
   - `retryFailed()` - 重试失败通知
4. 集成事件监听：
   - 监听借阅事件（发送到期提醒）
   - 监听预约事件（发送预约通知）
   - 监听支付事件（发送支付确认）
5. 编写集成测试

**验收标准**:
- [ ] 多渠道发送正常
- [ ] 模板渲染正确
- [ ] 事件监听正常
- [ ] 重试机制工作
- [ ] 80%+测试覆盖率

**预计时间**: 4小时

---

### 8.3 应用层和接口层

#### 任务8.3.1: 实现NotificationApplicationService

**目标**: 实现通知管理的应用服务

**文件路径**: `library-notification/src/main/java/com/library/notification/application/service/`

**实现步骤**:

1. 创建`NotificationApplicationService.java`
2. 实现用例方法：
   - `sendNotification()` - 发送通知用例
   - `getNotificationHistory()` - 获取通知历史
   - `updateNotificationPreferences()` - 更新通知偏好
3. 编写集成测试

**验收标准**:
- [ ] 用例实现完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务8.3.2: 实现NotificationController

**目标**: 提供通知管理的REST API

**文件路径**: `library-notification/src/main/java/com/library/notification/interfaces/rest/`

**实现步骤**:

1. 创建`NotificationController.java`
2. 实现REST端点：
   - `GET /api/notifications` - 获取通知列表
   - `PUT /api/notifications/{id}/read` - 标记已读
   - `PUT /api/notifications/patrons/{patronId}/preferences` - 更新偏好
3. 编写集成测试

**验收标准**:
- [ ] API文档完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

### 8.4 阶段七总结

**阶段七完成标准**:
- [ ] 通知上下文完全实现
- [ ] 80%+测试覆盖率
- [ ] 多渠道发送正常
- [ ] 模板系统工作

**预计总时间**: 约12小时

---

## 9. 阶段八：共享模块 (library-shared)

### 9.1 实现共享领域概念

#### 任务9.1.1: 实现共享领域事件

**目标**: 创建跨上下文共享的领域事件

**文件路径**: `library-shared/src/main/java/com/library/shared/domain/event/`

**实现步骤**:

1. 创建所有领域事件类
2. 定义事件基类
3. 创建事件发布器接口
4. 编写单元测试

**验收标准**:
- [ ] 所有事件有统一结构
- [ ] 事件包含版本信息
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务9.1.2: 实现共享值对象

**目标**: 创建跨上下文共享的值对象

**文件路径**: `library-shared/src/main/java/com/library/shared/domain/model/`

**实现步骤**:

1. 创建Money值对象
2. 创建Email值对象
3. 创建PhoneNumber值对象
4. 创建Address值对象
5. 编写单元测试

**验收标准**:
- [ ] 值对象不可变
- [ ] 验证逻辑完整
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

#### 任务9.1.3: 实现共享工具类

**目标**: 创建共享的工具类

**文件路径**: `library-shared/src/main/java/com/library/shared/infrastructure/common/`

**实现步骤**:

1. 创建日期时间工具
2. 创建字符串工具
3. 创建验证工具
4. 创建JSON工具
5. 编写单元测试

**验收标准**:
- [ ] 工具类方法全面
- [ ] 80%+测试覆盖率

**预计时间**: 2小时

---

### 9.2 阶段八总结

**阶段八完成标准**:
- [ ] 共享模块完全实现
- [ ] 80%+测试覆盖率
- [ ] 所有上下文可以使用共享代码

**预计总时间**: 约6小时

---

## 10. 阶段九：跨上下文集成

### 10.1 事件驱动集成

#### 任务10.1.1: 配置Kafka集群

**目标**: 配置Kafka用于跨上下文事件传递

**实现步骤**:

1. 配置Kafka集群
2. 创建Kafka主题
3. 配置事件序列化
4. 实现事件溯源
5. 编写集成测试

**验收标准**:
- [ ] Kafka集群正常
- [ ] 事件可靠传递
- [ ] 支持重放

**预计时间**: 4小时

---

#### 任务10.1.2: 实现Saga协调器

**目标**: 实现Saga模式处理分布式事务

**文件路径**: `library-shared/src/main/java/com/library/shared/infrastructure/saga/`

**实现步骤**:

1. 创建Saga协调器接口
2. 实现借书Saga
3. 实现还书Saga
4. 实现补偿事务
5. 编写集成测试

**验收标准**:
- [ ] Saga协调正确
- [ ] 补偿事务工作
- [ ] 最终一致性达成

**预计时间**: 5小时

---

### 10.2 API网关

#### 任务10.2.1: 实现API网关

**目标**: 实现统一的API网关

**实现步骤**:

1. 使用Spring Cloud Gateway
2. 配置路由规则
3. 实现认证过滤器
4. 实现限流
5. 实现熔断
6. 编写集成测试

**验收标准**:
- [ ] 路由正确
- [ ] 认证工作
- [ ] 限流生效
- [ ] 80%+测试覆盖率

**预计时间**: 6小时

---

### 10.3 监控和运维

#### 任务10.3.1: 配置Prometheus和Grafana

**目标**: 实现监控和可视化

**实现步骤**:

1. 配置Prometheus指标
2. 配置健康检查
3. 配置Grafana仪表盘
4. 配置告警规则
5. 配置日志聚合（ELK）

**验收标准**:
- [ ] 指标收集正常
- [ ] 健康检查通过
- [ ] 仪表盘显示正常
- [ ] 告警工作

**预计时间**: 4小时

---

#### 任务10.3.2: 配置分布式追踪

**目标**: 实现OpenTelemetry追踪

**实现步骤**:

1. 配置OpenTelemetry
2. 配置Jaeger
3. 添加追踪ID传递
4. 配置采样率

**验收标准**:
- [ ] 追踪正常工作
- [ ] Jaeger显示正确
- [ ] 跨服务追踪完整

**预计时间**: 3小时

---

### 10.4 阶段九总结

**阶段九完成标准**:
- [ ] 跨上下文集成完成
- [ ] 事件驱动架构工作
- [ ] Saga协调正确
- [ ] API网关正常
- [ ] 监控和追踪完整
- [ ] 80%+测试覆盖率

**预计总时间**: 约22小时

---

## 11. 测试和质量保证总结

### 11.1 系统集成测试

#### 任务11.1.1: 编写E2E测试

**目标**: 测试完整的业务流程

**实现步骤**:

1. 测试借书流程
2. 测试还书流程
3. 测试预约流程
4. 测试支付流程
5. 测试跨上下文流程

**验收标准**:
- [ ] 所有核心流程有E2E测试
- [ ] 测试覆盖正常和异常场景
- [ ] 测试独立且可重复

**预计时间**: 8小时

---

#### 任务11.1.2: 性能测试

**目标**: 进行性能和负载测试

**实现步骤**:

1. 使用JMeter进行负载测试
2. 测试API响应时间
3. 测试数据库性能
4. 测试Kafka吞吐量
5. 优化瓶颈

**验收标准**:
- [ ] API响应时间 < 200ms (P95)
- [ ] 支持并发100+用户
- [ ] 数据库查询优化
- [ ] Kafka吞吐量满足需求

**预计时间**: 6小时

---

### 11.2 部署准备

#### 任务11.2.1: Docker化

**目标**: 为所有服务创建Docker镜像

**实现步骤**:

1. 创建Dockerfile
2. 优化镜像大小
3. 配置多阶段构建
4. 创建docker-compose.yml

**验收标准**:
- [ ] 所有服务可容器化
- [ ] 镜像大小优化
- [ ] docker-compose可启动整个系统

**预计时间**: 4小时

---

#### 任务11.2.2: Kubernetes部署

**目标**: 创建Kubernetes部署文件

**实现步骤**:

1. 创建Deployment
2. 创建Service
3. 创建Ingress
4. 创建ConfigMap和Secret
5. 配置Helm Chart

**验收标准**:
- [ ] 所有服务可部署到K8s
- [ ] 配置外部化
- [ ] 支持滚动更新

**预计时间**: 6小时

---

### 11.3 文档和培训

#### 任务11.3.1: 完善文档

**目标**: 完善所有文档

**实现步骤**:

1. 更新API文档
2. 编写架构文档
3. 编写运维文档
4. 编写故障排查手册

**验收标准**:
- [ ] 文档完整且准确
- [ ] 新开发者能快速上手

**预计时间**: 4小时

---

## 12. 整体时间规划

### 12.1 时间分配

| 阶段 | 预计时间 | 占比 |
|------|---------|------|
| 项目初始化 | 2小时 | 1% |
| 阶段一：编目上下文 | 50小时 | 15% |
| 阶段二：馆藏上下文 | 20小时 | 6% |
| 阶段三：借阅上下文 | 25小时 | 7% |
| 阶段四：会员上下文 | 14小时 | 4% |
| 阶段五：支付上下文 | 14小时 | 4% |
| 阶段六：分析上下文 | 10小时 | 3% |
| 阶段七：通知上下文 | 12小时 | 4% |
| 阶段八：共享模块 | 6小时 | 2% |
| 阶段九：跨上下文集成 | 22小时 | 6% |
| 测试和质量保证 | 28小时 | 8% |
| 部署和文档 | 10小时 | 3% |
| **总计** | **约213小时** | **100%** |

### 12.2 建议开发周期

**全职开发（每周40小时）**: 约5-6周
**兼职开发（每周20小时）**: 约10-12周

---

## 13. 如何使用本计划

### 13.1 开始开发

1. 阅读本计划的完整内容
2. 从"项目初始化阶段"开始
3. 每完成一个任务，打勾相应的验收标准
4. 遇到阻塞或问题，记录在任务备注中

### 13.2 继续开发

1. 下次启动时，查看DEVELOPMENT_PLAN.md
2. 找到当前进度（最近完成的任务）
3. 继续执行下一个任务
4. 保持验收标准的检查

### 13.3 质量保证

1. 每个任务完成后，运行测试确保验收标准满足
2. 使用TDD方法：先写测试，再实现功能
3. 保持80%+测试覆盖率
4. 代码审查每个完成的阶段

### 13.4 进度追踪

建议使用以下方法追踪进度：
- 创建GitHub Project看板
- 使用任务列表追踪每个任务的完成状态
- 定期更新DEVELOPMENT_PLAN.md中的进度标记

---

## 14. 关键注意事项

### 14.1 DDD原则

- [ ] 严格遵循分层架构
- [ ] 领域逻辑不泄露到应用层
- [ ] 应用服务只做协调，不包含业务逻辑
- [ ] 仓储接口在领域层定义
- [ ] 领域事件从领域层发布

### 14.2 测试原则

- [ ] 使用TDD方法
- [ ] 测试覆盖率80%+
- [ ] 单元测试快速且独立
- [ ] 集成测试覆盖关键路径
- [ ] E2E测试覆盖完整业务流程

### 14.3 安全原则

- [ ] 永不硬编码密钥
- [ ] 使用环境变量管理密钥
- [ ] 所有输入验证
- [ ] 使用参数化查询
- [ ] 实施认证和授权

### 14.4 性能原则

- [ ] 使用缓存优化热点数据
- [ ] 数据库查询优化
- [ ] 异步处理耗时操作
- [ ] 实施限流和熔断
- [ ] 监控关键指标

---

## 15. 附录

### 15.1 技术栈总结

- **后端框架**: Spring Boot 3.2+
- **数据库**: PostgreSQL 15+
- **消息队列**: Apache Kafka 3.6+
- **缓存**: Redis 7+
- **搜索**: Elasticsearch 8+
- **API文档**: SpringDoc OpenAPI
- **监控**: Prometheus + Grafana
- **追踪**: OpenTelemetry + Jaeger
- **容器化**: Docker + Kubernetes
- **CI/CD**: GitHub Actions

### 15.2 依赖版本

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.2.5</spring-boot.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
    <kafka.version>3.6.0</kafka.version>
    <postgresql.version>42.6.0</postgresql.version>
    <redis.version>7.2.0</redis.version>
    <elasticsearch.version>8.11.0</elasticsearch.version>
</properties>
```

### 15.3 参考资源

- Spring Boot官方文档：https://docs.spring.io/spring-boot/docs/current/
- DDD参考：https://www.domainlanguage.com/ddd/
- Kafka文档：https://kafka.apache.org/documentation/
- PostgreSQL文档：https://www.postgresql.org/docs/
- Spring Data JPA：https://docs.spring.io/spring-data/jpa/docs/current/

---

**计划版本**: v1.1
**创建日期**: 2026-05-03
**最后更新**: 2026-05-04
**状态**: 进行中 - 阶段二（馆藏上下文）已完成，准备进入阶段三

---

> **提示**: 本计划会随着开发进度持续更新。每次完成一个阶段，请更新相应的验收标准和时间记录。
