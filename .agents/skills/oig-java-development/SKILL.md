---
name: oig-java-development
description: Use when modifying OIG Java/Spring Boot backend repositories, including java-admin-starter foundation code, REST APIs, DTOs, enums, MyBatis/Flyway persistence, dynamic query DSL scenes, module boundaries, file/export capabilities, tests, or developer documentation.
---

# OIG Java Development

## Overview

本 skill 用于 `java-admin-starter` 的 Java/Spring Boot 开发。入口保留共同约束，具体接口、迁移和验证规则按改动类型加载 reference。

## Required Workflow

1. 先阅读仓库根目录 `AGENTS.md` 和 `README.md`，确认当前模块状态、启动方式和硬约束。
2. 修改任何文件前，用 3 个要点列出计划；计划、规格说明和实现文档默认使用中文。
3. 根据改动类型读取下方对应 reference，按规则的适用范围实现；当前任务已读内容可复用。
4. 按 `references/verification.md` 选择验证，运行后汇报实际结果；命令失败时先读日志、分析根因，再修复或说明阻塞。

## 规则强度

- **硬约束**：适用范围内的“必须 / 禁止 / 不得”，包括分层、响应、身份来源和迁移历史保护。
- **默认选择**：未有具体业务要求时采用，不据此改变已有公开契约。
- **兼容例外**：仅适用于 reference 列明的现有 API；内部分层、类名或目录调整保持 HTTP 路径、JSON 字段、schema 名称和 `operationId`，例外不扩展到新接口。

## Reference Selection

| 改动类型 | 必读 reference |
|---|---|
| 模块边界、包结构、调用链、网关操作人、文件/导出架构 | `references/architecture-boundaries.md` |
| Controller、DTO、OpenAPI、错误码、枚举、数据模型、命名 | `references/api-and-modeling-contracts.md` |
| 新增分页或组合查询、维护 IAM 兼容分页、OpenAPI 动态查询 schema | `references/dynamic-query-dsl.md` |
| 新增表、字段、索引、约束、Flyway 脚本 | `references/database-migrations.md` |
| 运行配置、选择验证命令、跨模块测试、完成前检查 | `references/verification.md` |

## 硬约束速查

- 响应统一使用 `R.ok(...)` / `R.fail(...)`，业务异常使用 `BizException(ErrorCode)`。
- Controller 不得绕过 AppService。
- 业务模块实现包统一使用 `controller/dto/app/domain/infra/enums`；DTO 与 Controller 同级，供 Controller 和 AppService 共用，其中请求对象放 `dto.req`、响应对象放 `dto.rsp`、动态查询请求放 `dto.req.query`；MyBatis-Plus `IService/ServiceImpl` 只能位于 `infra/persistence/service`。
- App/Domain 不得依赖 Entity、Mapper、MyBatis-Plus Service 或 Infra 实现；Domain Repository 接口由 Infra 实现。
- Web DTO、Domain Model、Entity 不得混用。
- HTTP 完成日志由 `core/logging` 统一输出；身份只采信认证请求属性，IP 经 `core.operator.ClientIpResolver` 的可信代理实现解析，无实现时使用 TCP 对端地址。
- 历史版本化 SQL 不得修改、删除或重命名；新增版本必须高于当前最大版本。当前初始化基线不构成再次 squash 的例外，详见迁移 reference。
- 依赖版本不得使用 `LATEST`、`RELEASE` 或动态范围。
- `java-admin-starter` 基线 Maven `groupId` 与 Java 根包统一使用 `com.oigit.admin`；初始化后的业务项目使用其目标包名，不保留基线命名空间。
- 模块目录名与 Maven `artifactId` 必须一致，使用 `boot`、`core`、`iam`、`system` 或实际业务域名，不重复添加 `admin-` 或项目名前缀。
- 不提前创建空模块、空包、空接口；每个新增模块、实体、对象、接口都必须有当前业务理由。
- 新增分页和新增可组合条件查询必须使用动态查询 DSL；现有 IAM 员工、角色、登录日志和操作日志分页保留已发布平铺字段，范围见 `references/dynamic-query-dsl.md`。
