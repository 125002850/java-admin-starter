---
name: oig-java-development
description: Use when modifying OIG Java/Spring Boot backend repositories, including java-admin-starter foundation code, REST APIs, DTOs, enums, MyBatis/Flyway persistence, dynamic query DSL scenes, module boundaries, file/export capabilities, tests, or developer documentation.
---

# OIG Java Development

## Overview

本 skill 是 OIG Java/Spring Boot 后端项目的开发操作手册，当前覆盖 `java-admin-starter` 基础仓库，并以当前 `main` 分支基座约束为准。它把 README 中的项目开发规范拆成可按需加载的 reference，避免 README 变成超长规范清单。

## Required Workflow

1. 先阅读仓库根目录 `AGENTS.md` 和 `README.md`，确认当前模块状态、启动方式和硬约束。
2. 修改任何文件前，用 3 个要点列出计划；计划、规格说明和实现文档默认使用中文。
3. 根据改动类型读取下方对应 reference，按其中的强约束实现。
4. 代码变更必须保持统一响应 `R.ok(...)` / `R.fail(...)`，业务异常必须使用 `BizException(ErrorCode)`。
5. 涉及数据库结构变更时，只能新增更高版本 Flyway migration，禁止修改、删除、重命名历史 `V*__*.sql`；经明确批准的模块目录整体重命名，只允许原样搬迁 migration 路径，不得改变历史 migration 的文件名或内容。
6. 汇报前必须运行验证命令并说明结果；命令失败时先读日志、分析根因，再修复或说明阻塞。

## Reference Selection

| 改动类型 | 必读 reference |
|---|---|
| 模块边界、包结构、调用链、网关操作人、文件/导出架构 | `references/architecture-boundaries.md` |
| Controller、DTO、OpenAPI、错误码、枚举、数据模型、命名 | `references/api-and-modeling-contracts.md` |
| 新增或修改分页查询、条件查询、OpenAPI 动态查询 schema | `references/dynamic-query-dsl.md` |
| 新增表、字段、索引、约束、Flyway 脚本 | `references/database-migrations.md` |
| 选择验证命令、跨模块测试、完成前检查 | `references/verification.md` |

## Default Checks

- Controller 不得绕过 AppService。
- Web DTO、Entity、内部调用对象不得混用。
- 依赖版本不得使用 `LATEST`、`RELEASE` 或动态范围。
- `java-admin-starter` 基线 Maven `groupId` 与 Java 根包统一使用 `com.oigit.admin`；初始化后的业务项目使用其目标包名，不保留基线命名空间。
- 模块目录名与 Maven `artifactId` 必须一致，使用 `boot`、`core`、`iam`、`system` 或实际业务域名，不重复添加 `admin-` 或项目名前缀。
- 不提前创建空模块、空包、空接口；每个新增模块、实体、对象、接口都必须有当前业务理由。
- 分页查询默认走动态查询 DSL；不要在请求 DTO 上散落平铺查询字段。
