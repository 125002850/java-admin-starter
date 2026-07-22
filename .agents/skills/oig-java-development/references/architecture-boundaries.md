# 架构边界与调用链

## 模块职责

| 模块 | 职责 | 约束 |
|---|---|---|
| `boot` | Spring Boot 启动、配置装配、Bean 扫描 | 不写业务逻辑 |
| `core` | 全局通用基础设施、与具体业务解耦的底层原生抽象 | 不放具体业务流程编排、不落具体业务表、不承载具体业务场景语义 |
| `mdm` | 主数据、平台型业务服务和跨业务复用能力 | 当前承载全局字典与导出中心，不承载仅属于单一业务域的实现 |
| `system` | 对象存储、SSO staff client 等外部系统集成 | 外部 SDK 只能出现在对应 provider 或 client 适配层 |
| `{biz}` | 其他具体业务域 | 只放本业务域实现；通过 AppService、SPI 或事件复用平台能力 |

模块目录名与 Maven `artifactId` 必须一致，统一使用仓库内语义名，不添加 `admin-` 或项目名前缀。仓库基线 Maven `groupId` 与 Java 根包统一使用 `com.oigit.admin`。

## 调用链路

强制调用链：

```text
Controller -> AppService -> Domain/Service -> Infra/Mapper
```

- `controller` 禁止绕过 `AppService` 直接调用 `service`、`infra` 或 `mapper`。
- `AppService` 是事务边界所在层。
- `AppService` 禁止直接面向 Web DTO 编写持久化逻辑，持久化访问统一下沉到 `service` / `infra`。
- 业务链路对外部厂商能力的调用必须通过 `system` 内对应 provider / client 适配层。

## 分包规范

标准业务模块包结构：

```text
com.oigit.admin.{module}
├── controller
│   └── dto
├── app
├── service
├── config
├── infra
│   ├── entity
│   ├── mapper
│   └── provider
└── enums
```

- 按业务领域分包，不按技术类型散乱拆包。
- 模块之间禁止直接依赖对方实现包。
- 需要同步返回值的跨模块调用，通过独立 `-api` 契约包完成。
- 不需要返回值的事后通知，通过 Spring `ApplicationEvent` 解耦。
- `service/query`、`openapi`、`convert` 只在确有需要时创建。

## 网关 SSO 与操作人上下文

- 网关在可信链路完成登录、Token/JWT 校验和权限判断；本仓库不实现本地登录、用户、角色、菜单或权限体系。
- 本仓库只消费网关透传的用户 header，不接收 `Authorization`、`Cookie`、`X-Tenant-Id`、角色或权限集合。
- `GatewayOperatorFilter` 从 `X-User-Id` 及用户展示 header 建立 `OperatorContext`；Controller 禁止自行解析 header 后层层透传。
- 业务代码通过 `OperatorContext.getOperatorId()`、`getOperatorName()`、`getOperatorPhone()`、`getOperatorRealName()` 读取当前操作人。
- `create_by` / `update_by` 由 MyBatis-Plus `MetaObjectHandler` 自动填充，Service 层不要手工赋值。
- 开发/测试环境缺失 `X-User-Id` 时，审计字段回退为 `0L`；生产环境依赖网关可信边界提供操作人。
- `sys_user_cache` 只缓存 SSO 用户展示信息，不构成本地 IAM 或授权数据源。

## 当前模块能力映射

- 全局字典能力位于 `mdm` 的 `mdm/dict` 包，接口统一暴露 `/api/mdm/dict/global/**`。
- 导出中心能力位于 `mdm` 的 `mdm/export` 包，接口统一暴露 `/api/mdm/export/**`。
- 员工信息查询能力位于 `system/staff`，接口统一暴露 `/api/staff/**`。
- 文件存储能力位于 `system/file`，接口统一暴露 `/api/file/storage/**`。

## 文件存储模块

- 文件能力位于 `system/file`，对外统一暴露 `/api/file/storage/**`。
- provider 通过 `platform.file.storage.type=local|qiniu|minio` 切换。
- `qiniu` / `minio` SDK 只允许出现在 `system` 的 file provider 适配层。
- 业务链路保持 `Controller -> AppService -> Service -> Provider`。
- 当前阶段对象元信息只存在于对象存储，不新增数据库表。
- 七牛真实网络集成测试使用 `qiniu-it` profile 和 `FILE_STORAGE_QINIU_*` 环境变量。
- MinIO 真实网络集成测试使用 `minio-it` profile 和 `FILE_STORAGE_MINIO_*` 环境变量。

## 导出与下载中心

- 与具体业务解耦的导出原生抽象放在 `core`，例如场景声明、handler SPI、renderer SPI、sink、file lifecycle。
- 外部文件落盘与访问能力放在 `system/file`，导出框架不得直接依赖厂商 SDK。
- 带明确业务语义、跨业务复用的下载中心与导出编排能力放在 `mdm/export`。
- 导出任务只在创建记录、成功落库和失败落库时开启短事务；查询、渲染、对象存储上传、临时 URL 和 ZIP 构建等文件或网络操作必须在事务外执行。
- 大文件渲染、对象存储上传和批量 ZIP 下载必须使用流式接口，禁止按总文件大小上限推导 JVM 内存安全。
- 具体业务导出实现放在具体业务模块，例如参数组装、数据查询、列定义、导出内容构建。
- 协作链路：业务模块声明导出场景并提供 handler，`mdm/export` 编排与记录生命周期，`system/file` 负责文件存储，`core` 负责抽象契约。

## 不提前做

- 不提前做微服务拆分、MQ、分布式事务。
- 不提前预建全量业务模块或 `-api` 契约包。
- 不提前创建没有业务支撑的主数据实体。
- 不提前建立为复用而复用的抽象基类体系。
- 每新增模块、实体、对象、接口，都必须有当前业务理由。
