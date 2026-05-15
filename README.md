# java-demo

模块化单体架构的 Java 后端项目，基于 Spring Boot 3.x + MyBatis-Plus + MySQL 8。

## 项目结构

```
java-demo/
├── pom.xml                                          # 根 POM：依赖版本管理与模块聚合
├── README.md                                        # 项目说明文档
├── AGENTS.md                                        # AI 辅助开发配置
│
├── docs/                                            # 项目文档
│
├── demo-boot/                                       # 启动模块：Spring Boot 装配、配置、启动入口
│   ├── pom.xml                                      #   模块 POM，声明自身依赖
│   └── src/
│       ├── main/
│       │   ├── java/com/demo/boot/
│       │   │   └── DemoBootApplication.java         #     Spring Boot 启动类，扫描所有模块
│       │   └── resources/
│       │       ├── application.yml                  #     通用配置（跨环境共享）
│       │       ├── application-dev.yml              #     dev 环境配置（开发用）
│       │       └── application-test.yml             #     test 环境配置（测试用）
│       └── test/
│           └── java/com/demo/boot/
│               └── DemoBootApplicationTests.java    #     启动模块集成测试
│
├── demo-core/                                       # 全局共享基础设施（薄核心，不承载业务语义）
│   ├── pom.xml                                      #   模块 POM
│   └── src/
│       ├── main/java/com/demo/core/
│       │   └── web/                                 #   Web 层通用组件
│       │       ├── R.java                           #     统一响应体 {code, msg, data}
│       │       ├── PageReqDTO.java                  #     分页请求基类
│       │       └── PageResult.java                  #     分页响应体
│       └── test/java/com/demo/core/
│           └── web/
│               └── RTests.java                      #   R 响应体单元测试
│
├── demo-system/                                     # 系统底座模块：账号、权限、租户、审计
│   └── pom.xml                                      #   模块 POM（业务代码待实现）
│
└── demo-mdm/                                        # 主数据模块：字典、组织等基础数据
    └── pom.xml                                      #   模块 POM（业务代码待实现）
```

### 模块职责

| 模块 | 职责 | 约束 |
|------|------|------|
| `demo-boot` | Spring Boot 启动、配置装配、Bean 扫描 | 不写业务逻辑 |
| `demo-core` | `R<T>`、异常处理、分页、租户/用户上下文、MyBatis-Plus 配置、日志等 | 只放全局通用基础设施，不放业务语义 |
| `demo-system` | 租户、用户、角色、权限、登录鉴权 | 先做最小闭环 |
| `demo-mdm` | 字典、组织等主数据 | 只建当前业务真实需要的主数据 |
| `demo-{biz}` | 具体业务 | 有需求时创建，严格按分层链路落地 |

### 业务模块内部目录

```
com.demo.{module}
├── controller          # 输入输出、DTO 适配
├── app                 # AppService，事务边界与流程编排
├── service             # 领域服务 / 核心业务规则
├── infra
│   └── mapper          # 数据库访问，Mapper 统一归 infra
└── enums               # 模块私有枚举（按需创建）
```

以下目录只在需要时创建：`service/query`、`provider`、`openapi`、`convert`。

## 技术栈

| 领域 | 选型 | 版本约束 |
|------|------|----------|
| JDK | 17 | 固定 |
| 框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 锁定明确版本 |
| 数据库 | MySQL | 8 |
| 数据库迁移 | Flyway | 锁定明确版本 |
| 接口文档 | OpenAPI 3 + Knife4j | 锁定明确版本 |
| JSON | Jackson（Spring Boot 默认） | — |
| 连接池 | HikariCP（Spring Boot 默认） | — |
| 测试 | JUnit 5 + Spring Boot Test | — |
| 密码哈希 | BCrypt / Argon2 | 禁止明文或可逆存储 |

所有依赖版本统一锁定，禁止使用 `LATEST`、`RELEASE` 或动态范围。

## 工程规范

### 调用链路（强约束）

```
Controller → AppService → Domain/Service → Infra/Mapper
```

- `controller` 禁止绕过 `AppService` 直接调用 `service`、`infra` 或 `mapper`。
- `AppService` 禁止直接面向 Web DTO 编写持久化逻辑，持久化访问统一下沉到 `service` / `infra`。
- `AppService` 是事务边界所在层。

### 分包规范

- 按业务领域分包，不按技术类型散乱拆包。
- 模块之间禁止直接依赖对方实现包。
- 需要同步返回值的跨模块调用，通过独立 `-api` 契约包完成。
- 不需要返回值的事后通知，通过 Spring `ApplicationEvent` 解耦。

### API 规范

- 统一响应结构：`{"code":200,"msg":"ok","data":...}`，响应对象命名为 `R`。
- 所有接口统一使用 `POST`。
- URL 格式：`/api/{模块名}/{资源名}/{动作}`。
- 请求对象：`XxxReqDTO`，响应对象：`XxxRspDTO`，禁止复用数据库实体。
- 分页：`PageReqDTO` 和 `PageResult<T>`。
- 全局异常统一转换为标准 `R<T>` 响应。
- 日期格式：`yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd`。

### 数据规范

- 所有业务表必须包含：`create_time`、`update_time`、`create_by`、`update_by`、`deleted`。
- `create_time` / `update_time` 由数据库自动维护。
- `create_by` / `update_by` 通过 MyBatis-Plus `MetaObjectHandler` 自动填充。
- 逻辑删除字段统一为 `deleted`。
- 所有业务数据表必须包含 `tenant_id`，多租户隔离通过 `TenantLineInnerInterceptor` 统一实现。
- 平台级全局表如不参与租户隔离，必须显式标注为"非租户表"并加入 `TenantLineHandler` 忽略清单，禁止只靠人工约定绕过。

### 对象模型

第一阶段只保留三类核心对象：

| 对象 | 用途 |
|------|------|
| `Entity` | 数据库持久化对象 |
| `ReqDTO` / `RspDTO` | Web 层请求/响应对象 |
| `XxxQuery` | 复杂查询场景（按需引入） |

不提前引入 `VO`、`BO`、`DO`、`Param`、`Form`、`Command` 等多套近义对象。

### 扩展能力（按需引入）

- **文件存储**：先做本地实现，有需要再补 MinIO，通过配置切换。
- **翻译引擎**：ID 转名称走翻译机制，不在 SQL 中写大量 `LEFT JOIN`。
- **导出**：使用独立 `ExportDTO`，不污染接口响应对象。
- **状态枚举**：影响后端逻辑分支用 `Enum`，仅用于展示/筛选用 `Dict`。
- **操作日志**：首个真实业务模块接入后再补全链路。

### 开发环境

- 提供 `dev`、`test` 两套 profile。
- 开发态配置最小 CORS。
- 日志中必须输出 `traceId`。
- 健康检查端点可访问。

## 启动方式

```bash
# 以 dev profile 启动
cd demo-boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 数据库初始化

```bash
# Flyway 自动迁移，启动时执行
# 或手动执行
mvn flyway:migrate
```

## 不提前做的事

- 微服务拆分、MQ、分布式事务
- 全量业务模块 / `-api` 契约包预建
- 大而全的动态查询 DSL、翻译类型体系
- 没有业务支撑的主数据实体
- 为复用而复用的抽象基类体系

每新增一个模块、实体、对象、接口，都必须有当前业务理由。

## 完成标准

每次修改后至少执行：

- `mvn test`
- `mvn compile`

如果改动涉及数据库迁移，需要验证 Flyway 脚本可执行。

## 禁止行为

- 禁止绕过 AppService 直接在 Controller 调用 Mapper。
- 禁止 Web DTO、Entity、内部调用 DTO 混用。
- 禁止引入 `VO`、`BO`、`DO`、`Command` 等额外对象层，除非文档明确更新。
- 禁止使用动态依赖版本。
- 禁止提前创建空模块、空包、空接口。
