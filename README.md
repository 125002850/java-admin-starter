# java-admin-starter

模块化单体架构的 Java 后端项目，基于 Spring Boot 3.x + MyBatis-Plus + MySQL 8。当前 `main` 分支内置本地 IAM 管理后台基础能力，不依赖网关 SSO 承接登录鉴权。

## 当前完成状态

- 当前工作分支：`main`
- `admin-boot` / `admin-core` / `admin-iam` / `admin-system` 底座已完成。
- `dev` profile 支持通过 `JAVA_ADMIN_STARTER_DATASOURCE_*` 环境变量连接独立 MySQL；未配置时回退到仓库根目录 `compose.yaml` 的本地 MySQL。
- `admin-iam` 已落地本地登录、JWT、refresh token 轮换、员工、部门、角色、菜单/按钮权限、数据权限、登录日志和操作日志。
- `admin-system` 承载后台系统管理与平台能力：全局字典、导出中心、SSO 员工查询兼容接口、文件存储。
- 文件存储支持 `local` / `qiniu` / `minio` 三种 provider，通过配置切换。
- 动态查询 DSL 已在 `admin-core` 落地，当前接入全局字典类型、字典项和导出记录分页场景。
- 顶层模块边界约定为：`admin-boot` 负责启动，`admin-core` 负责底层通用能力与原生抽象，`admin-iam` 承载本地身份权限，`admin-system` 承载系统管理与平台能力，`admin-{biz}` 承载具体业务。

## 架构总览

```
                         HTTP / Bearer JWT
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  admin-boot                                   │
│                       Spring Boot 启动 · 配置装配 · Bean 扫描                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────┐  ┌───────────────────────────────────────┐  │
│  │          admin-iam            │  │             admin-system               │  │
│  │       本地身份与权限          │  │       系统管理与平台能力               │  │
│  │  auth / staff / dept / role  │  │  dict / export / file / staff compat │  │
│  │  menu / permission / logs    │  │  local / qiniu / minio providers     │  │
│  └──────────────┬──────────────┘  └───────────────────┬───────────────────┘  │
│                 │                                      │                      │
│                 └──────────────┬───────────────────────┘                      │
│                                ▼                                              │
│                         ┌────────────────┐                                    │
│                         │   admin-core    │                                    │
│                         │ R / Exception  │                                    │
│                         │ Trace / Query  │                                    │
│                         │ Export SPI     │                                    │
│                         │ MyBatis config │                                    │
│                         └────────────────┘                                    │
│                                                                              │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │
                ┌───────────────┴────────────────┐
                ▼                                ▼
          ┌──────────┐                    ┌──────────────┐
          │ MySQL 8  │                    │ 对象存储      │
          │ Flyway   │                    │ Local/Qiniu/ │
          │ tables   │                    │ MinIO        │
          └──────────┘                    └──────────────┘
```

**模块依赖方向**：

```
admin-boot
   ├──▶ admin-iam ─────▶ admin-core
   │          └───────▶ MySQL 8
   │
   └──▶ admin-system ──▶ admin-core
              ├──────▶ MySQL 8
              └──────▶ 对象存储（Local / Qiniu / MinIO）
```

**模块内调用链路**：

```
Controller → AppService → Service → Mapper / Provider
  (DTO)       (事务边界)    (领域逻辑)   (持久化/外部)
```

## 项目结构

```
java-admin-starter/
├── pom.xml                                          # 根 POM：依赖版本管理与模块聚合
├── README.md                                        # 项目说明文档
├── AGENTS.md                                        # AI agent 基础规范
├── CLAUDE.md                                        # Claude Code 配置
├── lefthook.yml                                     # Git Hook 配置
├── compose.yaml                                     # 本地开发 MySQL 编排
├── .agents/
│   └── skills/
│       └── oig-java-development/                    # AI Java 开发规范 skill 与按需加载 references
│
├── scripts/
│   └── check-migrations.sh                          # Flyway migration 约束检查脚本
│
├── docs/
│   └── prd/                                         # 当前产品需求基线
│
├── admin-boot/                                       # 启动模块：Spring Boot 装配、配置、启动入口
│   └── src/
│       ├── main/
│       │   ├── java/com/example/admin/boot/
│       │   │   ├── AdminBootApplication.java         # 启动类
│       │   │   └── config/                          # OpenAPI 等启动层配置
│       │   └── resources/
│       │       ├── application.yml                  # 通用配置
│       │       ├── application-dev.yml              # dev 环境
│       │       ├── application-test.yml             # test 环境
│       │       ├── logback-spring.xml               # 日志配置
│       │       └── db/migration/                    # Flyway SQL 迁移脚本
│       └── test/
│           ├── java/com/example/admin/boot/                  # 启动层契约、OpenAPI、Flyway 与 IAM 集成测试
│           └── resources/                           # 测试 profile 与 Mockito 配置
│
├── admin-core/                                       # 全局共享基础设施与业务无关的底层抽象
│   └── src/main/java/com/example/admin/core/
│       ├── web/                                     # R<T>、PageReqDTO、PageResult
│       ├── exception/                               # 错误码、BizException、全局异常处理
│       ├── validation/                              # Bean Validation 集成
│       ├── jackson/                                 # Jackson 全局配置
│       ├── trace/                                   # TraceId 过滤器与 MDC
│       ├── operator/                                # 操作人上下文与可选网关过滤器
│       ├── mybatis/                                 # MyBatis-Plus 配置、审计字段自动填充
│       ├── export/                                  # 导出框架 SPI 与通用模型
│       └── query/                                   # 动态查询 DSL 框架
│
├── admin-iam/                                        # 本地 IAM 模块：认证、员工、部门、角色、菜单、权限和日志
│   └── src/main/java/com/example/admin/iam/
│       ├── controller/                              # IAM HTTP 接口
│       ├── dto/                                     # IAM ReqDTO / RspDTO
│       ├── app/                                     # 事务边界与流程编排
│       ├── service/                                 # 权限快照、密码、Token、数据权限等核心规则
│       ├── security/                                # Spring Security、JWT filter、权限切面
│       ├── infra/                                   # Entity 与 Mapper
│       └── enums/                                   # IAM 错误码和业务枚举
│
└── admin-system/                                     # 系统管理与平台能力模块
    └── src/main/java/com/example/admin/
        ├── dict/                                    # 全局字典
        ├── export/                                  # 导出中心与导出记录生命周期
        ├── file/                                    # 文件存储与 local/qiniu/minio provider
        └── staff/                                   # SSO staff 查询兼容能力（默认关闭）
```

## 模块职责

| 模块 | 职责 | 约束 |
|------|------|------|
| `admin-boot` | Spring Boot 启动、配置装配、Bean 扫描 | 不写业务逻辑 |
| `admin-core` | 全局通用基础设施，以及与具体业务解耦的底层原生抽象 | 不放具体业务流程编排、不落具体业务表、不承载具体业务场景语义 |
| `admin-iam` | 本地身份与权限能力，负责认证、员工、部门、角色、菜单、权限、数据权限和审计日志 | 不依赖 SSO；不放通用主数据或第三方 provider |
| `admin-system` | 后台系统管理与平台能力，负责全局字典、导出中心、员工兼容查询、文件存储等能力 | 外部厂商 SDK 只能出现在对应 provider 适配层 |
| `admin-{biz}` | 其余具体业务模块 | 只放本业务域实现；如需导出、文件、字典等能力，应复用 `core/system` 提供的基础抽象与平台服务 |

## 技术栈

| 领域 | 选型 | 版本约束 |
|------|------|----------|
| JDK | 17 | 固定 |
| 框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 锁定明确版本 |
| 数据库 | MySQL | 8 |
| 数据库迁移 | Flyway | 锁定明确版本 |
| 接口文档 | OpenAPI 3 + Knife4j | 锁定明确版本 |
| JSON | Jackson（Spring Boot 默认） | - |
| 连接池 | HikariCP（Spring Boot 默认） | - |
| 测试 | JUnit 5 + Spring Boot Test + ArchUnit | - |
| 鉴权 | 本地 IAM + Spring Security + JWT | access token 只承载员工身份，权限快照每次请求加载 |

所有依赖版本统一锁定，禁止使用 `LATEST`、`RELEASE` 或动态范围。

## 工程规范

详细开发规范已拆分到本仓库 skill：`.agents/skills/oig-java-development/`。后续写代码或提交前，先读取 `.agents/skills/oig-java-development/SKILL.md`，再按改动类型读取对应 `references/`。

核心入口：

- 架构边界、模块职责、调用链、操作人与文件/导出架构：`.agents/skills/oig-java-development/references/architecture-boundaries.md`
- API、错误码、命名、枚举、数据规范与对象模型：`.agents/skills/oig-java-development/references/api-and-modeling-contracts.md`
- 分页查询和条件查询：`.agents/skills/oig-java-development/references/dynamic-query-dsl.md`
- Flyway 数据库迁移：`.agents/skills/oig-java-development/references/database-migrations.md`
- 验证命令、跨模块测试与完成标准：`.agents/skills/oig-java-development/references/verification.md`

README 只保留项目说明、架构概览、启动方式和关键入口，避免开发规范与项目介绍混在一起。

## 启动方式

```bash
# 1. 可选：启动本地 MySQL fallback
docker compose up -d

# 2. 可选：使用远程独立库 java_admin_starter_iam
export JAVA_ADMIN_STARTER_DATASOURCE_URL='jdbc:mysql://192.168.186.154:32425/java_admin_starter_iam?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true'
export JAVA_ADMIN_STARTER_DATASOURCE_USERNAME=oig
export JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD='<从本地安全配置读取>'

# 3. 先在仓库根目录构建并安装启动模块依赖的兄弟模块
mvn -pl admin-boot -am install -DskipTests

# 4. 以 dev profile 启动应用
cd admin-boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

未设置 `JAVA_ADMIN_STARTER_DATASOURCE_*` 时，本地 fallback 数据库映射如下：

| 项目 | 值 |
|------|----|
| Compose 项目标识 | `java-admin-starter-main-iam` |
| MySQL 端口 | `3307` |
| 数据库名 | `java_admin_starter_iam` |
| 用户名 | `root` |
| 密码 | `root` |

启动后可访问接口文档：

- `http://127.0.0.1:8080/doc.html`
- `http://127.0.0.1:8080/v3/api-docs`
- `http://127.0.0.1:8080/v3/api-docs/iam`
- `http://127.0.0.1:8080/v3/api-docs/mdm-dict`
- `http://127.0.0.1:8080/v3/api-docs/file-storage`
- `http://127.0.0.1:8080/v3/api-docs/mdm-export`

当前基座业务端点范围：

- `/api/iam/auth/**`
- `/api/iam/staff/**`
- `/api/iam/dept/**`
- `/api/iam/role/**`
- `/api/iam/menu/**`
- `/api/iam/log/**`
- `/api/mdm/dict/global/**`
- `/api/mdm/export/**`
- `/api/file/storage/**`

除登录、刷新、健康检查、接口文档和本地文件访问外，业务接口默认要求 Bearer token。

明确不包含：

- `track-bench-postloan` 模块
- `com.example.admin.postloan.*` / `com.trackbench.postloan.*`
- `tb_track_*`、客户工作台、订单、库存、贷后跟踪、附件业务表
- `/api/postloan/**`

## 数据库初始化

Flyway 自动迁移，应用启动时执行。推荐使用独立库验证本地 IAM 基座：

```bash
mysql --protocol=tcp -h 192.168.186.154 -P 32425 -u oig -p \
  -e "create database if not exists java_admin_starter_iam character set utf8mb4 collate utf8mb4_general_ci;"
```

如需单独执行迁移，可在启动模块下运行：

```bash
cd admin-boot
mvn flyway:migrate
```

常用本地数据库命令：

```bash
docker compose ps
docker compose logs -f mysql
docker compose down
docker compose down -v
```

`down -v` 会删除当前分支对应的 MySQL 数据卷，只适合在确认要清空本地开发数据时使用。

## 数据库迁移

- 版本化迁移文件统一放在 `admin-boot/src/main/resources/db/migration/`。
- 当前历史迁移已演进到 `V9__add_local_iam_tables.sql`。
- 历史 `V*__*.sql` 一旦提交，禁止修改、删除、重命名；结构变更通过新增下一版本 migration 演进。
- 仓库 `pre-commit` 通过 `scripts/check-migrations.sh` 拦截历史版本化 migration 的修改。
- 完整迁移规范见 `.agents/skills/oig-java-development/references/database-migrations.md`。

## Lefthook 启用

```bash
# 本地安装 Lefthook CLI 后执行
lefthook install

# 可手动验证 pre-commit 规则
lefthook run pre-commit
```

## 完成标准

完成标准与跨模块测试流程见 `.agents/skills/oig-java-development/references/verification.md`。每次修改后必须运行并汇报实际验证命令；涉及数据库迁移时必须验证 Flyway 脚本，且至少保证一套真实 MySQL 8 环境通过。
