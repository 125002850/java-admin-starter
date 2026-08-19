# main 分支移除 SSO/CIK 兼容能力设计

## 目标

从 `main` 分支彻底移除公司统一 SSO 员工目录兼容能力及 `oigit-appcik` 私有依赖，使基础项目可以在不配置公司 Maven 私服的情况下构建。`sso` 分支不在本次改动范围内。

## 删除范围

- 删除 `system/src/main/java/com/oigit/admin/staff/` 下的 Controller、DTO、AppService、Domain 模型与网关、CI 客户端适配器和配置。
- 删除 `system/src/test/java/com/oigit/admin/staff/` 下对应测试。
- 删除 `/api/staff/**` 兼容接口；保留 `iam` 模块的 `/api/iam/staff/**` 本地员工管理接口。
- 从根 POM 和 `system/pom.xml` 删除 `oigit-appcik`；Apache HttpClient 4.x 仅由该 SDK 使用，因此同步删除。
- 删除 `platform.sso-staff`、`oigit.appcik` 以及对应环境变量配置。
- 清理 README、架构规范、架构测试白名单、OpenAPI 测试中仅服务于该兼容能力的描述和断言。

## 保持不变

- 本地 IAM 登录、员工、部门、角色、菜单和权限能力不变。
- 数据库结构与 Flyway 历史迁移不变。
- `system` 的字典、导出和文件存储能力不变。
- 不切换、不修改 `sso` 分支。

## 验证

1. 全仓搜索确认不再残留 `appcik`、`sso-staff`、`SSO_STAFF`、`OIGIT_APPCIK`、`/api/staff` 和被删除包引用。
2. 执行跨模块干净构建，确认不再解析 `oigit-appcik` 私有制品。
3. 执行全量测试，确认本地 IAM 和其余启动能力不受影响。
4. 在独立 detached worktree 中复验干净构建，满足删除性改造的完成标准。
