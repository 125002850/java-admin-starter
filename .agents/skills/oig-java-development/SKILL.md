---
name: oig-java-development
description: Apply the java-admin-starter feature/sso gateway-SSO conventions when modifying its Spring Boot foundation, REST APIs, DTOs, enums, MyBatis/Flyway persistence, dynamic query scenes, module boundaries, file/export capabilities, tests, or developer documentation.
---

# OIG Java Development

## Overview

本 skill 是 `java-admin-starter` 的开发操作手册，只描述当前 `feature/sso` 网关 SSO 基座。入口文件保留工作流和 reference 路由，具体规范按改动类型加载。

## Required Workflow

1. 先完整阅读仓库根目录 `AGENTS.md` 和 `README.md`，确认当前模块状态、启动方式和硬约束；发生冲突时以 `AGENTS.md`、`README.md` 和当前分支真实代码为准，并修正失配的 reference。
2. 修改任何文件前，用 3 个要点列出计划；计划、规格说明和实现文档默认使用中文。
3. 根据改动类型读取下方对应 reference，按其中的强约束实现。
4. 代码变更必须保持统一响应 `R.ok(...)` / `R.fail(...)`，业务异常必须使用 `BizException(ErrorCode)`。
5. 涉及数据库结构变更时，只能新增更高版本 Flyway migration，禁止修改、删除、重命名历史 `V*__*.sql`。
6. 汇报前按 `references/verification.md` 运行验证命令并说明结果；命令失败时先读日志、分析根因，再修复或说明阻塞。

## Reference Selection

| 改动类型 | 必读 reference |
|---|---|
| 模块边界、包结构、调用链、网关操作人、文件/导出架构 | `references/architecture-boundaries.md` |
| Controller、DTO、OpenAPI、错误码、枚举、数据模型、命名 | `references/api-and-modeling-contracts.md` |
| 新增或修改分页查询、条件查询、OpenAPI 动态查询 schema | `references/dynamic-query-dsl.md` |
| 新增表、字段、索引、约束、Flyway 脚本 | `references/database-migrations.md` |
| 选择验证命令、跨模块测试、完成前检查 | `references/verification.md` |

## Default Checks

- 模块目录名必须与 Maven `artifactId` 一致，使用 `boot`、`core`、`mdm`、`system`、`{biz}` 等语义名，不添加 `admin-` 或项目名前缀。
- 仓库基线 `groupId` 与 Java 根包使用 `com.oigit.admin`；生成新项目时由初始化参数替换命名空间。
- Controller 不得绕过 AppService。
- Web DTO、Entity、内部调用对象不得混用。
- 依赖版本不得使用 `LATEST`、`RELEASE` 或动态范围。
- 不提前创建空模块、空包、空接口；每个新增模块、实体、对象、接口都必须有当前业务理由。
- 新增数据库分页查询默认走动态查询 DSL；已有公开 DTO 字段和 `operationId` 先按契约稳定性处理，不做机械重构。
