# main 分支移除 SSO/CIK 兼容能力设计

## 目标

从 `main` 分支彻底移除公司统一 SSO 员工目录兼容能力及 `oigit-appcik` 私有依赖，使基础项目可以在不配置公司 Maven 私服的情况下构建。远端 `origin/feature/sso` 不在本次改动范围内；实施前其提交为 `e0cae2a4560cfdb2deb5b057ad064c4e21eb841c`，完成后必须确认该引用未变化。

## 删除范围

- 删除 `system/src/main/java/com/oigit/admin/staff/` 下的 Controller、DTO、AppService、Domain 模型与网关、CI 客户端适配器和配置。
- 删除 `system/src/test/java/com/oigit/admin/staff/` 下对应测试。
- 删除 `/api/staff/**` 兼容接口；保留 `iam` 模块的 `/api/iam/staff/**` 本地员工管理接口。
- 从根 POM 和 `system/pom.xml` 删除 `oigit-appcik`；Apache HttpClient 4.x 仅由该 SDK 使用，因此同步删除。
- 删除 `platform.sso-staff`、`oigit.appcik` 以及对应环境变量配置。
- 从 `OpenApiConfig` 删除已不存在的 `com.oigit.admin.staff.dto` 扫描包。
- 清理 README、OIG 架构规范、能力分层文档和 ArchUnit 外部 SDK 白名单中仅服务于该兼容能力的描述。
- 保留并增强 OpenAPI 中不暴露 `/api/staff/**` 的回归断言，同时保留 `/api/iam/staff/**` 存在断言。

## 保持不变

- 本地 IAM 登录、员工、部门、角色、菜单和权限能力不变。
- 数据库结构与 Flyway 历史迁移不变。
- `system` 的字典、导出和文件存储能力不变。
- 不切换、不修改远端 `origin/feature/sso` 引用。

## 验证

1. 搜索运行时代码、POM、启动配置、测试、当前能力文档和 OIG 规范，确认不再残留 `com.oigit.appcik`、`sso-staff`、`SSO_STAFF`、`OIGIT_APPCIK`、`com.oigit.admin.staff`；设计/计划文档和历史 PRD 可作为明确允许项保留背景描述。
3. 使用空的临时 Maven 本地仓库和不包含公司镜像/私服的临时 settings 执行跨模块干净构建，证明构建不依赖用户 `~/.m2` 缓存或公司私服配置。
4. 执行全量测试，确认 `/api/staff/**` 不再暴露，且 `/api/iam/staff/**` 与其余启动能力不受影响。
5. 在独立 detached worktree 中重复上述隔离构建，满足删除性改造的完成标准。
6. 完成前再次核对 `origin/feature/sso` 仍指向实施前提交。
