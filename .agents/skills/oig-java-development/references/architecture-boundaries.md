# 架构边界与调用链

## 模块职责

| 模块 | 职责 | 约束 |
|---|---|---|
| `boot` | Spring Boot 启动、配置装配、Bean 扫描 | 不写业务逻辑 |
| `core` | 全局通用基础设施、与具体业务解耦的底层原生抽象 | 不放具体业务流程编排、不落具体业务表、不承载具体业务场景语义 |
| `iam` | 本地身份、鉴权、权限、员工、部门、角色、菜单和审计日志 | 不依赖 `system` 实现包；认证入口使用 Bearer JWT |
| `system` | 全局字典、导出中心、文件存储和员工兼容查询 | 依赖 `core`；外部 SDK 只能出现在对应 provider 或 client 适配层 |
| `{biz}` | 其他具体业务域 | 只放本业务域实现；通过 AppService、SPI 或事件复用平台能力 |

## 命名空间

- `java-admin-starter` 仓库基线 Maven `groupId` 与 Java 根包固定为 `com.oigit.admin`。
- 包声明、源码目录、组件扫描、Mapper 扫描、ArchUnit 规则和内部 Maven 依赖必须使用同一根命名空间。
- 通过 `scripts/init_template_project.py --package` 生成业务项目时，必须把基线命名空间完整替换为目标包名，生成结果不得残留 `com.oigit.admin`。

## 模块命名

- 模块目录名与 Maven `artifactId` 必须保持一致。
- 基础模块固定使用 `boot`、`core`、`iam`、`system`，具体业务模块直接使用业务域名，例如 `order`、`inventory`。
- 不重复添加 `admin-` 或项目名前缀；管理后台命名空间由仓库名、Maven `groupId` 和 Java 包名表达。
- 只有同一 reactor 确实同时承载多个端且存在同名业务模块时，才允许使用 `admin-`、`customer-` 等端标识消除歧义。

## 调用链路

强制调用链：

```text
Controller -> AppService -> Domain
                           ^
                  Infra implements Repository
                           |
                  IService/ServiceImpl -> Mapper
```

- `controller` 禁止绕过 `AppService` 直接调用 `domain`、`infra` 或 `mapper`。
- `AppService` 是事务边界所在层。
- `AppService` 禁止依赖 Entity、Mapper、MyBatis-Plus Service 或具体 Repository 实现。
- `domain` 只包含领域模型、业务规则、领域服务和 Repository 接口，不依赖 Spring、MyBatis、Controller 或 Infra。
- MyBatis-Plus `IService/ServiceImpl` 属于持久化基础设施，只能位于 `infra/persistence/service`。
- 业务链路对外部厂商能力的调用必须在 Domain 定义 gateway，由 `system` 内对应 provider / client 适配层实现；AppService 不得直接依赖厂商 SDK 或具体 adapter。

## 分包规范

标准业务模块包结构：

```text
com.oigit.admin.{module}
├── controller
├── dto
│   ├── req
│   │   └── query
│   └── rsp
├── app
├── domain
│   ├── model
│   ├── repository
│   └── service
├── infra
│   ├── persistence
│   │   ├── entity
│   │   ├── mapper
│   │   ├── service
│   │   │   └── impl
│   │   └── repository
│   ├── query
│   ├── provider/client
│   ├── translation
│   ├── export
│   └── config
└── enums
```

- 按业务领域分包，不按技术类型散乱拆包。
- DTO 与 `controller`、`app` 同级，作为应用用例输入输出对象供两层共用；请求对象放 `dto.req`，响应对象放 `dto.rsp`，动态查询请求放 `dto.req.query`；DTO 不得依赖 Controller、App、Domain 或 Infra 实现。
- 模块之间禁止直接依赖对方实现包。
- 需要同步返回值的跨模块调用，通过独立 `-api` 契约包完成。
- 跨模块公开的接口、DTO、事件和枚举放 `-api`；仅实现内部使用的枚举留在 impl 的 `enums`，Entity、Mapper 和实现类不得进入 API 契约。
- 不需要返回值的事后通知，通过 Spring `ApplicationEvent` 解耦。
- 没有真实领域规则时不创建空 Domain Service，AppService 可以直接依赖 Domain Repository 接口。

## 本地 IAM 与操作人上下文

- 当前 `main` 分支由本地 IAM 完成登录、JWT 校验和权限判断；除公开接口外，业务接口要求 Bearer JWT。
- 操作人默认从认证上下文读取；可选网关过滤器开启后才消费 `X-User-Id`、`X-User-Name`。
- `Authorization` 由 JWT filter 处理；不要在 Controller 解析 Token 或透传权限集合。
- 开发/测试环境无认证上下文时，审计字段回退为 `0L`。
- 业务代码获取当前操作人时调用 `OperatorContext.getOperatorId()`、`getOperatorName()`、`getOperatorPhone()`、`getOperatorRealName()`，禁止 Controller 解析 header 后层层透传。
- `create_by` / `update_by` 由 MyBatis-Plus `MetaObjectHandler` 自动从 `OperatorContext` 填充，Service 层不要手工赋值。


## HTTP 请求日志

- HTTP 完成日志统一由 `core/logging` 的过滤器输出，业务模块不得自行打印完整请求体或响应体。
- 过滤器顺序必须保证 `TraceIdFilter` 先于 `HttpRequestLoggingFilter`，并覆盖本地 IAM 或可选网关过滤器的拒绝响应。
- 请求/响应正文必须限长，并按字段名递归脱敏；multipart、二进制、文件访问、健康检查和 OpenAPI 资源不得打印正文或完成日志。
- Spring Web 的日志级别不得开启到会输出反序列化请求/响应对象的 DEBUG，避免绕过统一脱敏。
- HTTP 请求日志不代替业务操作审计；单据状态变化和关键字段前后值由业务域另行记录。

## 当前模块能力映射

- 全局字典能力位于 `system` 的 `dict` 包，接口统一暴露 `/api/system/dict/global/**`。
- 导出中心能力位于 `system` 的 `export` 包，接口统一暴露 `/api/mdm/export/**`。
- 员工信息查询能力位于 `system/staff`，接口统一暴露 `/api/staff/**`。
- 文件存储能力位于 `system/file`，接口统一暴露 `/api/file/storage/**`。

## 文件存储模块

- 文件能力位于 `system/file`，对外统一暴露 `/api/file/storage/**`。
- provider 通过 `platform.file.storage.type=local|qiniu|minio` 切换。
- `qiniu` / `minio` SDK 只允许出现在 `system` 的 file provider 适配层。
- 当前文件适配链路为 `Controller -> FileAppService -> FileStorageGateway <- Infra Provider`；对象键生成与校验由 Domain Policy 承担，AppService 不依赖具体 provider。
- 当前阶段对象元信息只存在于对象存储，不新增数据库表。
- 七牛真实网络集成测试使用 `qiniu-it` profile 和 `FILE_STORAGE_QINIU_*` 环境变量。
- MinIO 真实网络集成测试使用 `minio-it` profile 和 `FILE_STORAGE_MINIO_*` 环境变量。

## 导出与下载中心

- 与具体业务解耦的导出原生抽象放在 `core`，例如场景声明、handler SPI、renderer SPI、sink、file lifecycle。
- 外部文件落盘与访问能力放在 `system/file`，导出框架不得直接依赖厂商 SDK。
- 带明确业务语义、跨业务复用的下载中心与导出编排能力放在 `system/export`。
- 具体业务导出实现放在具体业务模块，例如参数组装、数据查询、列定义、导出内容构建。
- 协作链路：业务模块声明导出场景并提供 handler，`system/export` 编排与记录生命周期，`system/file` 负责文件存储，`core` 负责抽象契约。

## 不提前做

- 不提前做微服务拆分、MQ、分布式事务。
- 不提前预建全量业务模块或 `-api` 契约包。
- 不提前创建没有业务支撑的主数据实体。
- 不提前建立为复用而复用的抽象基类体系。
- 每新增模块、实体、对象、接口，都必须有当前业务理由。
