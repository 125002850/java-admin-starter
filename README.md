# java-admin-starter

模块化单体架构的 Java 后端项目，基于 Spring Boot 3.x + MyBatis-Plus + MySQL 8。当前 `feature/sso` 分支由可信网关承接登录鉴权，应用只消费网关透传的操作人信息。

## 当前完成状态

- 当前工作分支：`feature/sso`
- boot/core/mdm 底座已完成（原登录/租户 system 业务模块已移除，鉴权与租户由网关 SSO 承接）
- `dev` profile 支持通过环境变量连接独立 MySQL 库 `basic_platform_sso`，未配置时回退到仓库根目录 `compose.yaml` 的本地 MySQL
- `system` 系统集成模块已落地，当前承载 `file` 子模块，支持 `local` / `qiniu` / `minio` 三种 provider，通过配置切换
- 动态查询 DSL 已在 `core` 落地，当前接入全局字典类型、字典项和导出记录分页场景
- `mdm` 当前无源码，保留为空模块（通用业务平台扩展预留空间）
- 顶层模块边界约定为：`boot` 负责启动，`core` 负责底层通用能力与原生抽象，`mdm` 承载通用业务服务，`system` 承载外部服务集成，`{biz}` 承载具体业务
- 模块目录名与 Maven `artifactId` 保持一致，统一使用仓库内语义名，不重复添加 `admin-` 或项目名前缀
- 仓库基线 Maven `groupId` 与 Java 根包统一使用 `com.oigit.admin`；初始化业务项目时由 `--package` 替换为目标命名空间

## 项目结构

```
java-admin-starter/
├── pom.xml                                          # 根 POM：依赖版本管理与 4 个模块聚合
├── README.md                                        # 项目说明文档
├── AGENTS.md                                        # AI 辅助开发配置
├── CLAUDE.md                                        # 另一套 AI 协作约束
├── lefthook.yml                                     # Git Hook 配置
├── compose.yaml                                     # 当前分支本地开发 MySQL 编排
├── scripts/
│   ├── init_template_project.py                     # 新项目初始化脚本
│   ├── check-migrations.sh                          # Flyway migration 约束检查脚本
│   └── start-dev.sh                                 # 检查 Docker/MySQL 并启动 dev 服务
│
├── docs/                                            # 项目文档
│   ├── README.md                                    # 当前文档索引与维护约束
│   ├── architecture/                                # 当前架构契约
│   └── dev/                                         # 可执行开发环境指南
│
├── boot/                                       # 启动模块：Spring Boot 装配、配置、启动入口
│   └── src/
│       ├── main/
│       │   ├── java/com/oigit/admin/boot/
│       │   │   ├── AdminBootApplication.java         #     启动类，扫描所有模块
│       │   │   └── config/                          #     OpenAPI 等启动层配置
│       │   └── resources/
│       │       ├── application.yml                  #     通用配置
│       │       ├── application-dev.yml              #     dev 环境
│       │       ├── application-test.yml             #     test 环境
│       │       ├── logback-spring.xml               #     日志配置
│       │       └── db/migration/                    #     Flyway SQL 迁移脚本
│       └── test/
│           ├── java/com/oigit/admin/boot/archunit/         #     分层/模块边界测试
│           ├── java/com/oigit/admin/boot/contract/         #     错误码契约测试
│           ├── java/com/oigit/admin/boot/file/             #     文件模块集成测试
│           ├── java/com/oigit/admin/boot/flyway/           #     迁移冒烟测试
│           ├── java/com/oigit/admin/boot/mybatis/          #     审计字段与 MyBatis 配置测试
│           ├── java/com/oigit/admin/boot/openapi/          #     OpenAPI 文档测试
│           ├── java/com/oigit/admin/boot/trace/            #     traceId 相关测试
│           ├── java/com/oigit/admin/boot/web/              #     Web/Validation 集成测试
│           └── resources/                           #     测试 profile 与 Mockito 配置
│
├── core/                                       # 全局共享基础设施与业务无关的底层抽象
│   └── src/
│       ├── main/java/com/oigit/admin/core/
│       │   ├── web/                                 #     R<T>、PageReqDTO、PageResult
│       │   ├── exception/                           #     错误码、BizException、全局异常处理
│       │   ├── validation/                          #     Bean Validation 集成
│       │   ├── jackson/                             #     Jackson 全局配置
│       │   ├── trace/                               #     TraceId 过滤器与 MDC
│       │   ├── operator/                            #     网关操作人上下文与过滤器
│       │   └── mybatis/                             #     MyBatis-Plus 配置、审计字段自动填充
│       └── test/java/com/oigit/admin/core/                 #     核心基础设施单元测试
│
├── system/                                     # 系统集成模块：对接对象存储、短信、邮件、支付等外部服务
│   └── src/
│       ├── main/java/com/oigit/admin/
│       │   ├── file/                                #     文件存储接口与 provider 适配
│       │   ├── dict/                                #     全局字典管理与导出场景 handler
│       │   ├── staff/                               #     SSO 员工查询
│       │   └── export/                              #     导出中心能力
│       └── test/java/com/oigit/admin/               #     file/dict/export/staff 模块测试
│
└── mdm/                                        # 通用业务服务模块（当前无源码，保留为空模块）
```

### 模块职责

| 模块 | 职责 | 约束 |
|------|------|------|
| `boot` | Spring Boot 启动、配置装配、Bean 扫描 | 不写业务逻辑 |
| `core` | 全局通用基础设施，以及与具体业务解耦的底层原生抽象（如通用 SPI、上下文、通用配置） | 不放具体业务流程编排、不落具体业务表、不承载具体业务场景语义 |
| `system` | 系统集成能力，负责对象存储、短信、邮件、支付等外部服务适配 | 只做外部系统/厂商能力适配；业务侧禁止直接依赖厂商 SDK；如需落库，应仅保存集成能力自身必要的元数据 |
| `mdm` | 通用业务服务，承载“有明确业务语义、但不从属于单一业务域”的共享业务能力 | 可承载主数据、平台型业务服务、跨业务复用的统一能力；不承载仅属于单一业务域的实现 |
| `{biz}` | 其余具体业务 | 只放本业务域实现；如需导出等能力，应复用 `core/system/mdm` 提供的基础抽象与平台服务 |

### 顶层模块边界原则

- `boot` 只负责启动和装配，不吸收业务语义。
- `core` 只放与具体业务解耦的底层能力；可复用的原生抽象优先沉淀在这里，而不是散落到业务模块。
- `system` 只负责对接外部服务；文件存储属于这一层，不上移到 `core`。
- `mdm` 用于沉淀通用业务服务，而不局限于“字典”这一类主数据；跨业务复用、带明确业务语义的平台能力优先落在这里。
- `{biz}` 只承载具体业务域实现；与平台共享能力的边界应通过 `AppService`、SPI 或事件解耦，而不是反向把业务细节塞进 `core/system/mdm`。

### 业务模块内部目录

```
com.oigit.admin.{module}
├── controller          # 输入输出适配
│   └── dto             # ReqDTO / RspDTO
├── app                 # AppService，事务边界与流程编排
├── service             # 领域服务 / 核心业务规则
├── config              # 模块私有配置（按需创建）
├── infra
│   ├── entity          # 持久化实体（按需创建）
│   ├── mapper          # 数据库访问，Mapper 统一归 infra
│   └── provider        # 三方能力适配（按需创建）
└── enums               # 模块私有枚举（按需创建）
```

补充说明：

- `src/test/java` 与 `src/test/resources` 默认随模块创建，用于模块级单测、冒烟测试和测试专用配置。
- 以下目录只在需要时创建：`service/query`、`openapi`、`convert`。

## 模板项目初始化

项目名、输出路径和 Java 根包名均为必填参数。输出目录名可以与项目名不同；项目名会写入根 POM、子模块坐标及应用配置。

```bash
python3 scripts/init_template_project.py \
  --project-name sample-service \
  --project-path ~/work/generated-project \
  --package com.acme.sample
```

目标目录必须为空，且不能位于当前模板仓库内部。脚本会复制模板、迁移包路径，并初始化一个不含历史提交、初始分支固定为 `main` 的新 Git 仓库。生成项目继续使用 `boot`、`core`、`mdm`、`system` 等语义模块名，不添加项目名前缀；同时保留 `.agents/skills` 与 `.claude/skills` 软链，但不会复制本地 IDE、环境变量或 Claude 私有配置。

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
| 测试 | JUnit 5 + Spring Boot Test + ArchUnit | — |
| 鉴权 | 网关 SSO 透传 `X-User-Id` | 本仓库不做登录/Token 校验 |

所有依赖版本统一锁定，禁止使用 `LATEST`、`RELEASE` 或动态范围。

## 工程规范

- 详细开发规范位于 `.agents/skills/oig-java-development/`。
- 每个独立开发任务首次修改文件前，先读取一次 `SKILL.md`，再按改动类型读取对应 `references/`。
- 当前任务中，已读取的 `SKILL.md` 和对应 `reference` 可直接复用，无需在每次文件修改或提交前重复读取。
- 仅在新增改动类型、当前任务修改了对应规范文件，或当前上下文已无法保留相关规范内容时，补读或重新读取。

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
- 模块私有业务码使用独立号段，例如 `mdm` 当前使用 `3001xxx`。
- `BizException` 只接受 `ErrorCode`，禁止业务代码散落裸错误码、裸失败文案。

### 数据规范

- 所有业务表必须包含：`create_time`、`update_time`、`create_by`、`update_by`、`deleted`。
- `create_time` / `update_time` 禁止在业务 `service` 中手工赋值；建表时应提供 `default current_timestamp`，并由 MyBatis-Plus `MetaObjectHandler` 统一兜底填充。
- `create_by` / `update_by` 通过 MyBatis-Plus `MetaObjectHandler` 自动填充，优先从网关操作人上下文（`OperatorContext`）读取 `X-User-Id`，缺失时回退 `0L`。
- 逻辑删除字段统一为 `deleted`，未删除值为 `0`，删除值使用数据库时间戳表达式，避免软删后唯一索引冲突。
- 本仓库不再要求业务表包含 `tenant_id`，不再校验 `X-Tenant-Id` 请求头。多租户隔离由网关 SSO 和基础设施层承接。
- 本分支不提供本地登录、用户、角色、菜单、权限表；SSO 透传用户展示信息仅缓存到 `sys_user_cache`，不作为本地用户体系。

### 对象模型

第一阶段只保留三类核心对象：

| 对象 | 用途 |
|------|------|
| `Entity` | 数据库持久化对象 |
| `ReqDTO` / `RspDTO` | Web 层请求/响应对象 |
| `XxxQuery` | 复杂查询场景（按需引入） |

不提前引入 `VO`、`BO`、`DO`、`Param`、`Form`、`Command` 等多套近义对象。

### 扩展能力（按需引入）

- **文件存储**：当前已落地 `system` 下的 `file` 子模块，默认本地实现，支持通过配置切换到七牛或 MinIO。
- **导出基础抽象**：如需引入 handler、renderer、sink、file lifecycle 等与具体业务解耦的原生抽象，优先放在 `core`。
- **平台型业务能力**：如需沉淀跨业务复用、带明确业务语义的统一服务，优先放在 `mdm`。
- **翻译引擎**：ID 转名称走翻译机制，不在 SQL 中写大量 `LEFT JOIN`。
- **导出**：使用独立 `ExportDTO`，不污染接口响应对象。
- **状态枚举**：影响后端逻辑分支用 `Enum`，仅用于展示/筛选用 `Dict`。
- **操作日志**：首个真实业务模块接入后再补全链路。

### 开发环境

- 提供 `dev`、`test` 两套 profile。
- 开发态配置最小 CORS。
- 日志中必须输出 `traceId`。
- 健康检查端点可访问。

### 网关-SSO 边界摘要

- 网关到应用的可信链路负责完成登录、Token/JWT 校验、权限判断；本仓库只消费透传 header，不实现本地登录态。
- 生产环境写操作的操作人 ID 来自 `X-User-Id`；`X-User-Name`、`X-User-Phone`、`X-Real-Name` 和 `X-User-Code` 只用于展示、缓存或排障，不参与鉴权。
- 本仓库不接收 `X-Tenant-Id`、`X-User-Roles`、`X-User-Permissions`、`Authorization`、`Cookie`。
- 开发/测试环境无网关时允许缺失 `X-User-Id`，审计字段 `create_by` / `update_by` 回退为 `0L`；如需模拟操作人，可手工加 `X-User-Id: 1`。

详细契约见 [网关 SSO 边界](docs/architecture/2026-05-19-gateway-sso-boundary.md)。

### 文件存储模块摘要

- 模块路径：`system`，当前文件能力代码位于 `system/src/main/java/com/oigit/admin/file`，后续 `sms`、`email`、`pay` 等第三方服务可作为同级包继续落在该模块下；对外统一暴露 `/api/file/storage/**`，不复用任何厂商控制器或返回模型。
- 当前接口：
  - `/api/file/storage/object/upload`
  - `/api/file/storage/object/delete`
  - `/api/file/storage/object/temp-url/fetch`
  - `/api/file/storage/direct-upload/credential/fetch`
- provider 切换：`platform.file.storage.type=local|qiniu|minio`。
- `local` 模式默认通过 `/local-files/**` 暴露文件访问；`dev` profile 下本地文件根目录固定到 `${user.home}/.java-admin-starter/uploads`，避免临时目录被系统清理。
- `qiniu` 模式只在 `system` 的 `file` provider 适配层内依赖七牛 SDK；业务链路仍保持 `Controller -> AppService -> Service -> Provider`。
- `minio` 模式只在 `system` 的 `file` provider 适配层内依赖 MinIO Java SDK，当前支持服务端上传、删除和临时下载地址；直传凭证暂未开放。
- 文件对象元信息只存在于对象存储，不做数据库持久化；导出中心和 SSO 用户展示缓存通过 Flyway migration 维护平台表。
- 文件模块额外支持字节数组上传、对象下载和批量临时 URL，供导出中心复用。
- 七牛真实网络集成测试为手动 gate：使用 `qiniu-it` profile，并通过环境变量注入 `FILE_STORAGE_QINIU_*` 配置。
- MinIO 真实网络集成测试为手动 gate：使用 `minio-it` profile，并通过环境变量注入 `FILE_STORAGE_MINIO_*` 配置。

### SSO 员工查询摘要

- `system` 下 `staff` 子模块对接可选 `oigit-appcik` SSO staff client。
- 对外接口为 `POST /api/staff/list-all`，返回 SSO 员工展示数据，不落本地用户表。
- 真实 SSO 地址通过 `OIGIT_APPCIK_SSO_SERVER_ADDR` / `oigit.appcik.sso.server-addr` 配置。

### 导出与下载中心架构原则

- 与具体业务解耦的导出原生抽象放在 `core`，例如导出场景声明、导出 handler SPI、文件渲染器 SPI、导出结果落盘 sink、文件访问 lifecycle 等。
- 外部文件落盘与文件访问能力放在 `system`，导出框架如需写入对象存储，应通过 `system` 提供的文件能力完成，而不是直接依赖厂商 SDK。
- 带明确业务语义、跨业务复用的下载中心与导出编排能力放在 `system`（`export` 包），例如导出记录、状态流转、有效期管理、下载留痕、过期清理等。
- 导出提交只同步创建 `PROCESSING` 记录，查询、渲染和对象存储上传由异步任务执行；创建、成功和失败状态分别使用独立短事务落库。
- CSV 渲染、批量 ZIP 构建、源文件读取和对象存储上传均使用临时文件或流式接口，避免把源文件与压缩结果同时放入 JVM 堆。
- 具体业务导出实现放在 `{biz}`，例如某个业务的导出参数组装、数据查询、列定义与导出内容构建；业务模块通过 `core` 提供的 SPI 接入平台能力。
- 模块协作链路保持清晰：业务模块声明导出场景并提供 handler，`system` 的 `export` 包负责编排与记录生命周期，`system` 的 `file` 包负责文件存储，`core` 负责抽象契约。

## 启动方式

推荐使用统一启动脚本。未配置远程数据源时，脚本会检查 Docker 是否运行并启动本地 MySQL；配置了远程数据源时会跳过 Docker：

```bash
# 1. 可选：使用远程独立库 basic_platform_sso
export JAVA_ADMIN_STARTER_DATASOURCE_URL='jdbc:mysql://192.168.186.154:32425/basic_platform_sso?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true'
export JAVA_ADMIN_STARTER_DATASOURCE_USERNAME=oig
export JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD='<从本地安全配置读取>'

# 2. 检查并启动依赖，然后以 dev profile 启动应用
./scripts/start-dev.sh
```

也可以手动启动：

```bash
# 1. 可选：启动当前分支的本地 MySQL fallback
docker compose up -d

# 2. 在仓库根目录安装 boot 依赖的兄弟模块
mvn -pl boot -am install -DskipTests

# 3. 以 dev profile 启动应用
cd boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

未设置 `JAVA_ADMIN_STARTER_DATASOURCE_*` 时，当前分支本地 fallback 数据库映射如下：

| 项目 | 值 |
|------|----|
| Compose 项目标识 | `java-admin-starter-feature-sso` |
| MySQL 端口 | `3307` |
| 数据库名 | `java_admin_starter_sso` |
| 用户名 | `root` |
| 密码 | `root` |

推荐远程开发库：

| 项目 | 值 |
|------|----|
| MySQL 地址 | `192.168.186.154:32425` |
| 数据库名 | `basic_platform_sso` |
| 用户名 | `oig` |
| 密码 | 不写入仓库，通过 `JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD` 注入 |

启动后可访问接口文档：

- `http://127.0.0.1:8080/doc.html`
- `http://127.0.0.1:8080/v3/api-docs`
- `http://127.0.0.1:8080/v3/api-docs/mdm-dict`
- `http://127.0.0.1:8080/v3/api-docs/file-storage`
- `http://127.0.0.1:8080/v3/api-docs/mdm-export`
- `http://127.0.0.1:8080/v3/api-docs/staff`

当前基座公开端点范围：

- `/api/mdm/dict/global/**`
- `/api/mdm/export/**`
- `/api/file/storage/**`
- `/api/staff/list-all`

明确不包含：

- `track-bench-postloan` 模块
- `com.oigit.admin.postloan.*` / `com.trackbench.postloan.*`
- `tb_track_*`、客户工作台、订单、库存、贷后跟踪、附件业务表
- `/api/postloan/**`

## 数据库初始化

```bash
# Flyway 自动迁移，应用启动时执行。推荐新建独立库验证 SSO 分支基座：
mysql --protocol=tcp -h 192.168.186.154 -P 32425 -u oig -p \
  -e "create database if not exists basic_platform_sso character set utf8mb4 collate utf8mb4_general_ci;"

# 如需单独执行迁移，可在启动模块下运行：
cd boot
mvn flyway:migrate
```

常用数据库命令：

```bash
docker compose ps
docker compose logs -f mysql
docker compose down
docker compose down -v
```

`down -v` 会删除当前分支对应的 MySQL 数据卷，只适合在确认要清空本地开发数据时使用。更多说明见 [分支数据库说明](docs/dev/2026-05-19-branch-database.md)。

### 数据库迁移约束

- 版本化迁移文件统一放在 `boot/src/main/resources/db/migration/`，命名必须匹配 `V*__*.sql`，例如 `V7__add_xxx.sql`。
- 新增版本化迁移不得复用已有版本号；例如仓库里已有 `V1__init_platform_tables.sql` 时，下一条必须新增为 `V2__*.sql` 或更高版本。
- 当前仓库采用更严格策略：提交阶段只允许新增符合 `V*__*.sql` 规则的版本化迁移文件，禁止修改、删除、重命名已存在的文件名或内容；经批准的模块目录整体重命名只能原样搬迁迁移文件。
- 迁移脚本一旦进入历史，应通过新增下一版本脚本演进，例如 `V2__add_xxx.sql`，不要回改 `V1__*.sql`。
- Flyway migration 的 SQL 方言必须以真实目标库 **MySQL 8** 为准，不能只以 H2 可执行为准；H2 通过不代表 MySQL 8 兼容。
- 涉及列默认值、时间字段、`alter table` 之类 DDL 时，必须优先使用 MySQL 8 语法。例如修改默认值应写成 `alter table ... modify column ... default ...`，不要写 H2 可过但 MySQL 8 会失败的 `alter column ... set default ...`。
- 一旦 migration 在本地或测试库执行失败，Flyway 会在 `flyway_schema_history` 留下 `success = false` 记录，后续启动会被 `validate` 阶段直接拦截；修复 SQL 后需要先 `repair` 或清理失败记录，再重新执行迁移。
- 对逻辑删除表新增唯一约束时，必须先明确约束作用范围；如果唯一索引列中不包含 `deleted`，那它约束的是整张表，包含已逻辑删除行，迁移前查重也必须按同样语义检查，不能只筛 `deleted = 0`。
- 当前分支历史迁移已演进到 `V6__remove_tenant_auth_tables.sql`，租户/用户/本地鉴权表已从应用侧移除；后续只追加更高版本 migration，例如 `V8__backport_track_bench_foundation.sql`。
- `V8` 只补齐基础能力：字典状态/备注/排序/版本字段、导出记录版本字段、`sys_user_cache` 和通用字典数据；不迁入 `track-bench-postloan`、`tb_track_*`、客户工作台、订单、库存、贷后跟踪或 `/api/postloan/**`。

### Lefthook 启用

```bash
# 本地安装 Lefthook CLI 后执行
lefthook install

# 可手动验证 pre-commit 规则
lefthook run pre-commit
```

当前仓库的 `pre-commit` 会执行 [`scripts/check-migrations.sh`](scripts/check-migrations.sh)，拦截对历史版本化 migration 的修改。

## 不提前做的事

- 微服务拆分、MQ、分布式事务
- 全量业务模块 / `-api` 契约包预建
- 大而全的动态查询 DSL、翻译类型体系
- 没有业务支撑的主数据实体
- 为复用而复用的抽象基类体系

每新增一个模块、实体、对象、接口，都必须有当前业务理由。

## 完成标准

每次修改后至少执行：

- `mvn clean test`
- `mvn test`
- `mvn compile`

如果改动涉及数据库迁移，需要验证 Flyway 脚本可执行。
至少保证一套真实 MySQL 8 环境验证通过，不能只跑 H2 测试后就提交。
跨模块收缩或删除性改造，必须额外在 clean checkout / detached worktree 中复验，避免被脏工作区或增量编译结果误导。

## 禁止行为

- 禁止绕过 AppService 直接在 Controller 调用 Mapper。
- 禁止 Web DTO、Entity、内部调用 DTO 混用。
- 禁止引入 `VO`、`BO`、`DO`、`Command` 等额外对象层，除非文档明确更新。
- 禁止使用动态依赖版本。
- 禁止提前创建空模块、空包、空接口。

## todo list

- 日志
