# java-demo

模块化单体架构的 Java 后端项目，基于 Spring Boot 3.x + MyBatis-Plus + MySQL 8。

## 当前完成状态

- boot/core/mdm 底座已完成（system 模块已移除，鉴权与租户由网关 SSO 承接）
- 首个业务模块待单独规划

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
│   └── src/
│       ├── main/
│       │   ├── java/com/demo/boot/
│       │   │   └── DemoBootApplication.java         #     启动类，扫描所有模块
│       │   └── resources/
│       │       ├── application.yml                  #     通用配置
│       │       ├── application-dev.yml              #     dev 环境
│       │       └── application-test.yml             #     test 环境
│       └── test/
│
├── demo-core/                                       # 全局共享基础设施（薄核心，不承载业务语义）
│   └── src/main/java/com/demo/core/
│       ├── web/                                     #   R<T>、PageReqDTO、PageResult
│       ├── exception/                               #   错误码、BizException、全局异常处理
│       ├── validation/                              #   Bean Validation 集成
│       ├── jackson/                                 #   Jackson 全局配置
│       ├── trace/                                   #   TraceId 过滤器与 MDC
│       ├── operator/                                #   网关操作人上下文与过滤器
│       └── mybatis/                                 #   MyBatis-Plus 配置、审计字段自动填充
│
└── demo-mdm/                                        # 主数据模块：全局字典
    └── src/main/java/com/demo/mdm/
        ├── controller/                              #   全局字典接口 + DTO
        ├── app/                                     #   DictAppService（事务边界）
        ├── service/                                 #   字典领域服务
        └── infra/
            ├── entity/                              #   数据库实体
            └── mapper/                              #   MyBatis Mapper
```

### 模块职责

| 模块 | 职责 | 约束 |
|------|------|------|
| `demo-boot` | Spring Boot 启动、配置装配、Bean 扫描 | 不写业务逻辑 |
| `demo-core` | `R<T>`、异常处理、分页、网关操作人上下文、MyBatis-Plus 配置、日志等 | 只放全局通用基础设施，不放业务语义 |
| `demo-mdm` | 全局字典主数据 | 只保留全局字典，不区分租户 |
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
| 鉴权 | 网关 SSO 透传 `X-User-Id` | 本仓库不做登录/Token 校验 |

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
- Controller 必须补齐 OpenAPI 文档注解：类上使用 `@Tag`，方法上使用 `@Operation`，保证接口分组、摘要、说明完整。
- `ReqDTO` / `RspDTO` 必须补齐 `@Schema` 注解，为关键字段提供含义说明和示例值。
- 分页：`PageReqDTO` 和 `PageResult<T>`。
- 全局异常统一转换为标准 `R<T>` 响应。
- 日期格式：`yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd`。

### 错误码规范

- 成功响应固定为 `code = 200`、`msg = ok`。
- 默认失败响应使用 `CommonErrorCode.FAILED(500, "操作失败")`。
- 参数错误、未登录、无权限、资源不存在、限流使用公共 HTTP 语义码：`400 / 401 / 403 / 404 / 429`。
- 模块私有业务码使用独立号段，例如 `demo-system` 当前使用 `2001xxx`。
- `BizException` 只接受 `ErrorCode`，禁止业务代码散落裸错误码、裸失败文案。

### 数据规范

- 所有业务表必须包含：`create_time`、`update_time`、`create_by`、`update_by`、`deleted`。
- `create_time` / `update_time` 禁止在业务 `service` 中手工赋值；建表时应提供 `default current_timestamp`，并由 MyBatis-Plus `MetaObjectHandler` 统一兜底填充。
- `create_by` / `update_by` 通过 MyBatis-Plus `MetaObjectHandler` 自动填充，优先从网关操作人上下文（`OperatorContext`）读取 `X-User-Id`，缺失时回退 `0L`。
- 逻辑删除字段统一为 `deleted`。
- 本仓库不再要求业务表包含 `tenant_id`，不再校验 `X-Tenant-Id` 请求头。多租户隔离由网关 SSO 和基础设施层承接。

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

启动后可访问接口文档：

- `http://127.0.0.1:8080/doc.html`
- `http://127.0.0.1:8080/v3/api-docs`
- `http://127.0.0.1:8080/v3/api-docs/mdm-dict`

## 数据库初始化

```bash
# Flyway 自动迁移，启动时执行
# 或手动执行
mvn flyway:migrate
```

### 数据库迁移约束

- 版本化迁移文件统一放在 `demo-boot/src/main/resources/db/migration/`，命名必须匹配 `V*__*.sql`，例如 `V2__add_user_table.sql`。
- 新增版本化迁移不得复用已有版本号；例如仓库里已有 `V1__init_platform_tables.sql` 时，下一条必须新增为 `V2__*.sql` 或更高版本。
- 当前仓库采用更严格策略：提交阶段只允许新增符合 `V*__*.sql` 规则的版本化迁移文件，禁止修改、删除、重命名已存在的 `V*.sql`。
- 迁移脚本一旦进入历史，应通过新增下一版本脚本演进，例如 `V2__add_xxx.sql`，不要回改 `V1__*.sql`。
- Flyway migration 的 SQL 方言必须以真实目标库 **MySQL 8** 为准，不能只以 H2 可执行为准；H2 通过不代表 MySQL 8 兼容。
- 涉及列默认值、时间字段、`alter table` 之类 DDL 时，必须优先使用 MySQL 8 语法。例如修改默认值应写成 `alter table ... modify column ... default ...`，不要写 H2 可过但 MySQL 8 会失败的 `alter column ... set default ...`。
- 一旦 migration 在本地或测试库执行失败，Flyway 会在 `flyway_schema_history` 留下 `success = false` 记录，后续启动会被 `validate` 阶段直接拦截；修复 SQL 后需要先 `repair` 或清理失败记录，再重新执行迁移。
- 对逻辑删除表新增唯一约束时，必须先明确约束作用范围；如果唯一索引列中不包含 `deleted`，那它约束的是整张表，包含已逻辑删除行，迁移前查重也必须按同样语义检查，不能只筛 `deleted = 0`。
- 本仓库不再使用租户模型；平台级表由网关 SSO 和基础设施层管理。

### Lefthook 启用

```bash
# 本地安装 Lefthook CLI 后执行
lefthook install

# 可手动验证 pre-commit 规则
lefthook run pre-commit
```

当前仓库的 `pre-commit` 会执行 [scripts/check-migrations.sh](/Users/youdingte/studys/java-demo/scripts/check-migrations.sh:1)，拦截对历史版本化 migration 的修改。

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
至少保证一套真实 MySQL 8 环境验证通过，不能只跑 H2 测试后就提交。

## 禁止行为

- 禁止绕过 AppService 直接在 Controller 调用 Mapper。
- 禁止 Web DTO、Entity、内部调用 DTO 混用。
- 禁止引入 `VO`、`BO`、`DO`、`Command` 等额外对象层，除非文档明确更新。
- 禁止使用动态依赖版本。
- 禁止提前创建空模块、空包、空接口。
