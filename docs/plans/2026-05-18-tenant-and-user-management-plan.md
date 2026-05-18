# 多租户管理与用户生命周期接口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `demo-system` 增补平台级租户增删改接口，以及租户内用户注册、启用/禁用、修改密码、修改用户信息、删除接口，并补齐数据库、错误码、OpenAPI 与测试闭环。

**Architecture:** 租户管理落在全局表 `sys_tenant_global`，继续走平台级接口，不受租户拦截器影响；用户管理继续落在租户表 `sys_user`，统一通过 `X-Tenant-Id -> TenantResolver -> TenantContext` 收口租户上下文，避免在业务层自行信任请求体里的租户标识。删除全部采用逻辑删除；租户删除前必须校验无未删除用户；用户启停通过状态枚举控制，并回灌到登录链路。

**Tech Stack:** Java 17, Spring Boot 3.x, MyBatis-Plus, Flyway, Spring Security BCrypt, OpenAPI 3 + Knife4j, JUnit 5, MockMvc, H2

---

## 范围与前置假设

- 本期只实现用户明确提出的接口，不额外新增租户或用户的列表、详情、分页查询接口。
- 租户创建接口返回新建 `tenantId`；用户创建（注册）接口返回新建 `userId`，用于后续更新和删除。
- 用户管理接口定义为“当前租户内管理接口”，统一依赖 `X-Tenant-Id` header；登录接口继续沿用现有 `tenantId + username + password` 请求体模式。
- “修改用户信息”本期定义为修改 `displayName`、`mobile`、`email`，`username` 继续作为登录标识，不纳入本期变更范围。
- 删除保持逻辑删除语义，并沿用当前仓库的唯一键策略：被逻辑删除的租户名和用户名仍占用唯一键，不支持同名复用；如果架构上要求复用，需要单独设计唯一键与逻辑删除策略。
- 租户删除不做级联逻辑删用户；只要该租户下存在未删除用户，就拒绝删除租户。

## 接口草案

| 接口 | Header | 请求体 | 响应体 | 关键规则 |
|------|--------|--------|--------|----------|
| `POST /api/system/tenant/create` | 无 | `TenantCreateReqDTO` | `R<TenantCreateRspDTO>` | `tenantName` 唯一 |
| `POST /api/system/tenant/update` | 无 | `TenantUpdateReqDTO` | `R<Void>` | 仅允许修改租户名称 |
| `POST /api/system/tenant/delete` | 无 | `TenantDeleteReqDTO` | `R<Void>` | 存在未删除用户时拒绝删除 |
| `POST /api/system/user/create` | `X-Tenant-Id` 必填 | `UserRegisterReqDTO` | `R<UserRegisterRspDTO>` | 当前租户内用户名唯一，密码必须加密存储，且密码长度至少 6 位 |
| `POST /api/system/user/status/update` | `X-Tenant-Id` 必填 | `UserStatusUpdateReqDTO` | `R<Void>` | 只允许切换启用/禁用状态 |
| `POST /api/system/user/password/update` | `X-Tenant-Id` 必填 | `UserPasswordUpdateReqDTO` | `R<Void>` | 直接更新新密码，重新做 BCrypt 编码，且密码长度至少 6 位 |
| `POST /api/system/user/profile/update` | `X-Tenant-Id` 必填 | `UserProfileUpdateReqDTO` | `R<Void>` | 仅更新 `displayName/mobile/email` |
| `POST /api/system/user/delete` | `X-Tenant-Id` 必填 | `UserDeleteReqDTO` | `R<Void>` | 逻辑删除后用户不可再登录 |

## 文件结构与职责

### `demo-system` 模块

- Create: `demo-system/src/main/java/com/demo/system/controller/TenantController.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/UserController.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantCreateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantCreateRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantDeleteReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserRegisterReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserRegisterRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserStatusUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserPasswordUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserProfileUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserDeleteReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/app/TenantAppService.java`
- Create: `demo-system/src/main/java/com/demo/system/app/UserAppService.java`
- Create: `demo-system/src/main/java/com/demo/system/service/TenantService.java`
- Create: `demo-system/src/main/java/com/demo/system/service/UserService.java`
- Create: `demo-system/src/main/java/com/demo/system/infra/entity/SysTenantGlobalEntity.java`
- Modify: `demo-system/src/main/java/com/demo/system/infra/entity/SysUserEntity.java`
- Create: `demo-system/src/main/java/com/demo/system/infra/mapper/SysTenantGlobalMapper.java`
- Modify: `demo-system/src/main/java/com/demo/system/infra/mapper/SysUserMapper.java`
- Modify: `demo-system/src/main/java/com/demo/system/service/AuthService.java`
- Create: `demo-system/src/main/java/com/demo/system/enums/TenantErrorCode.java`
- Create: `demo-system/src/main/java/com/demo/system/enums/UserErrorCode.java`
- Create: `demo-system/src/main/java/com/demo/system/enums/UserStatusEnum.java`
- Modify: `demo-system/src/main/java/com/demo/system/enums/AuthErrorCode.java`

### `demo-boot` 模块

- Create: `demo-boot/src/main/resources/db/migration/V5__add_tenant_user_management.sql`
- Modify: `demo-boot/src/main/java/com/demo/boot/config/OpenApiConfig.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`（通常只跑回归，不需要改文件；若断言提示需要补说明，再决定是否修改）

### 文档与回归测试

- Modify: `README.md`
- Create: `demo-system/src/test/java/com/demo/system/TenantManagementTests.java`
- Create: `demo-system/src/test/java/com/demo/system/UserManagementTests.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`

## 数据模型调整

### `sys_tenant_global`

- 保持平台级全局表，不新增 `tenant_id`。
- 新增唯一约束：`tenant_name` 唯一，避免出现重名租户。
- Java 实体新增 `IdType.ASSIGN_ID`，与 `demo-mdm` 现有实体风格对齐，避免创建接口无法生成主键。

### `sys_user`

- `id` 改为 `IdType.ASSIGN_ID`，避免注册接口手工分配主键。
- 新增 `status` 字段，建议 `tinyint not null default 1`，由 `UserStatusEnum` 统一解释为启用/禁用。
- 新增 `display_name varchar(64) null`、`mobile varchar(32) null`、`email varchar(128) null`，作为本期“用户信息”的最小闭环。
- 新增唯一约束：`(tenant_id, username)`，把“同租户用户名唯一”下沉到数据库；应用层仍保留重复键翻译，处理并发写入和脏数据回归。

## 业务规则

- 租户名称全局唯一。
- 租户删除前先检查 `sys_user` 中该租户是否还有未删除用户；有则抛 `BizException(TenantErrorCode.TENANT_HAS_USERS)`。
- 用户注册只允许在已存在且未删除的租户下进行；若租户不存在，抛 `BizException(TenantErrorCode.TENANT_NOT_FOUND)`。
- 用户名在当前租户内唯一，不限制跨租户重名。
- 禁用用户不能登录；登录时返回明确业务错误码，不回落成“用户名或密码错误”。
- 注册和改密的密码长度至少 6 位，先通过 DTO 校验拦截。
- 改密只改目标用户的新密码，不做“旧密码校验”；这是因为当前仓库尚未接入登录态用户上下文。若后续要做自助改密，应拆新接口。
- 修改用户信息不允许变更 `tenantId`、`username`、`status`、`password`。

## 错误码约束

- `AuthErrorCode` 继续使用 `2001xxx` 号段。
- `TenantErrorCode` 统一使用 `2002xxx` 号段。
- `UserErrorCode` 统一使用 `2003xxx` 号段。

## 测试约束

- 任何测试只要手工调用 `TenantContext.setTenantId(...)`，都必须在 `try/finally` 或测试类 `@AfterEach` 中执行 `TenantContext.clear()`，禁止依赖测试执行顺序回收 `ThreadLocal`。
- 仅通过 `MockMvc + X-Tenant-Id` header 走 `TenantFilter` 的测试，不需要额外手工清理 `TenantContext`。

### Task 1: 锁定表结构与接口契约

**Files:**
- Create: `demo-boot/src/main/resources/db/migration/V5__add_tenant_user_management.sql`
- Modify: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`
- Create: `demo-system/src/test/java/com/demo/system/TenantManagementTests.java`
- Create: `demo-system/src/test/java/com/demo/system/UserManagementTests.java`

- [ ] **Step 1: 先补失败测试，锁定这批需求的外部契约**

新增或调整以下测试目标：

- `FlywaySmokeTests` 先断言存在 `V5` 成功记录，并校验 `sys_tenant_global` 有租户名称唯一约束、`sys_user` 有 `status/display_name/mobile/email` 字段和 `(tenant_id, username)` 唯一约束。
- `OpenApiDocumentationTests` 先断言根文档包含 `/api/system/tenant/create`、`/api/system/user/create`、`/api/system/user/status/update`、`/api/system/user/password/update`、`/api/system/user/profile/update`、`/api/system/user/delete`。
- `TenantManagementTests` 先定义租户创建成功、租户名重复失败、存在未删除用户时删除失败的接口期望。
- `UserManagementTests` 先定义注册成功、同租户重复用户名失败、跨租户重名允许、缺少 `X-Tenant-Id` 返回 `400`、禁用后登录失败、修改密码后旧密码失效、修改资料成功、删除后登录失败的期望。
- `AuthFlowTests` 先补“禁用用户登录返回显式错误码”的期望。

- [ ] **Step 2: 运行红灯，确认需求尚未实现**

Run: `mvn -q -pl demo-system -am test -Dtest=TenantManagementTests,UserManagementTests,AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，租户/用户管理接口、用户状态控制尚不存在。

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests,OpenApiDocumentationTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，尚无 `V5` migration，新接口也未出现在 OpenAPI 文档中。

- [ ] **Step 3: 编写 `V5` migration，先把表结构演进落稳**

`V5__add_tenant_user_management.sql` 需要覆盖以下 DDL：

- 给 `sys_tenant_global` 增加 `tenant_name` 唯一约束。
- 给 `sys_user` 增加 `status`、`display_name`、`mobile`、`email` 字段。
- 给 `sys_user` 增加 `(tenant_id, username)` 唯一约束。
- 所有新增字段保持 MySQL 8 语法，并兼容当前 Flyway 约束。

注意：

- 不要回改 `V1` 到 `V4`。
- 在加唯一约束前必须先执行存量查重，至少跑下面两条 SQL：

```sql
select tenant_name, count(*) as cnt
from sys_tenant_global
where deleted = 0
group by tenant_name
having cnt > 1;

select tenant_id, username, count(*) as cnt
from sys_user
where deleted = 0
group by tenant_id, username
having cnt > 1;
```

- 如果查出重复数据，先停下清理脏数据，再执行加唯一约束的 migration；不要在 `V5` 里静默挑选保留记录。
- `AuthFlowTests` 里的本地建表可以保留“无唯一约束”的最小测试模型，以继续覆盖 `AuthService` 的脏数据防御分支；真实 schema 约束由 `FlywaySmokeTests` 负责验收。

- [ ] **Step 4: 跑 migration 与契约回归，确认新基线成立**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，`V5` 应用成功，新增列与唯一约束生效。

- [ ] **Step 5: Commit**

Commit message: `feat: add tenant and user management schema baseline`

### Task 2: 新增平台级租户增删改接口

**Files:**
- Create: `demo-system/src/main/java/com/demo/system/controller/TenantController.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantCreateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantCreateRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/TenantDeleteReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/app/TenantAppService.java`
- Create: `demo-system/src/main/java/com/demo/system/service/TenantService.java`
- Create: `demo-system/src/main/java/com/demo/system/infra/entity/SysTenantGlobalEntity.java`
- Create: `demo-system/src/main/java/com/demo/system/infra/mapper/SysTenantGlobalMapper.java`
- Create: `demo-system/src/main/java/com/demo/system/enums/TenantErrorCode.java`
- Create: `demo-system/src/test/java/com/demo/system/TenantManagementTests.java`

- [ ] **Step 1: 先把租户接口行为锁成失败测试**

`TenantManagementTests` 至少覆盖：

- `create` 成功后返回 `tenantId`，并落库到 `sys_tenant_global`。
- 重复 `tenantName` 返回 `R.fail(...)`，错误码来自 `TenantErrorCode.TENANT_NAME_DUPLICATED`。
- `update` 可以修改租户名，重复名失败。
- `delete` 在租户下仍有未删除用户时返回 `TenantErrorCode.TENANT_HAS_USERS`。
- `delete` 成功时只把 `deleted` 改为 `1`，不做物理删除。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-system -am test -Dtest=TenantManagementTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前仓库没有租户管理控制器、服务和错误码。

- [ ] **Step 3: 实现租户 controller/app/service/mapper 全链路**

实现要求：

- `TenantController` 路径使用 `/api/system/tenant`，全部 `POST`，统一 `R.ok(...)` / `R.ok()`。
- `TenantAppService` 负责事务边界；`TenantService` 负责唯一名校验、逻辑删除校验和业务异常翻译。
- `SysTenantGlobalEntity` 使用 `@TableName("sys_tenant_global")` 和 `@TableId(type = IdType.ASSIGN_ID)`。
- `SysTenantGlobalMapper` 直接继承 `BaseMapper` 即可，因为 `sys_tenant_global` 已在租户忽略名单里。
- `TenantDeleteReqDTO` 按 ID 删除，不新增“批量删除”。
- 错误码建议最少覆盖：`TENANT_NOT_FOUND(2002001)`、`TENANT_NAME_DUPLICATED(2002002)`、`TENANT_HAS_USERS(2002003)`。
- 删除租户前用 `SysUserMapper` 新增的自定义方法统计指定租户下未删除用户数；该统计必须 `@InterceptorIgnore(tenantLine = "true")`，并显式带 `tenant_id = #{tenantId} and deleted = 0` 条件，因为手写 `@Select` 不会自动补逻辑删除过滤。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-system -am test -Dtest=TenantManagementTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，租户创建、修改、删除和异常路径全部符合契约。

- [ ] **Step 5: Commit**

Commit message: `feat: add tenant management apis`

### Task 3: 新增租户内用户创建（注册）接口

**Files:**
- Create: `demo-system/src/main/java/com/demo/system/controller/UserController.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserRegisterReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserRegisterRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/app/UserAppService.java`
- Create: `demo-system/src/main/java/com/demo/system/service/UserService.java`
- Modify: `demo-system/src/main/java/com/demo/system/infra/entity/SysUserEntity.java`
- Modify: `demo-system/src/main/java/com/demo/system/infra/mapper/SysUserMapper.java`
- Create: `demo-system/src/main/java/com/demo/system/enums/UserErrorCode.java`
- Create: `demo-system/src/main/java/com/demo/system/enums/UserStatusEnum.java`
- Create: `demo-system/src/test/java/com/demo/system/UserManagementTests.java`

- [ ] **Step 1: 写失败测试，先锁定注册接口和租户 header 语义**

`UserManagementTests` 中先覆盖：

- 带 `X-Tenant-Id` 的 `create` 能创建用户，并返回 `userId`。
- 同租户重复用户名失败，错误码使用 `UserErrorCode.USERNAME_DUPLICATED`。
- 不同租户允许相同用户名。
- 缺少 `X-Tenant-Id` 时返回 `400`，复用现有租户上下文错误语义。
- 密码入库后不是明文，`BCrypt` 可匹配原始密码。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-system -am test -Dtest=UserManagementTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前没有用户注册接口，也没有用户资料字段和状态字段。

- [ ] **Step 3: 实现注册链路**

实现要求：

- `UserController` 路径使用 `/api/system/user/create`，创建请求不再接收 `tenantId`，统一从 `TenantContext.requireTenantId()` 读取当前租户。
- `UserRegisterReqDTO` 至少包含 `username`、`password`、`displayName`、`mobile`、`email`，并补齐 `@Schema` 与必要的校验注解；`password` 至少加 `@Size(min = 6)`。
- `UserService.register(...)` 先校验租户存在，再校验当前租户内用户名是否重复，最后使用 `PasswordEncoder` 编码密码并插入。
- `SysUserEntity` 增加 `status/displayName/mobile/email` 字段，主键改为 `IdType.ASSIGN_ID`。
- 注册时默认写入 `UserStatusEnum.ENABLED`，`deleted = 0`。
- 并发重复注册要 catch `DuplicateKeyException` 并翻译成 `BizException(UserErrorCode.USERNAME_DUPLICATED)`。
- 错误码建议最少覆盖：`USER_NOT_FOUND(2003001)`、`USERNAME_DUPLICATED(2003002)`。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-system -am test -Dtest=UserManagementTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，注册成功、重复用户名失败、跨租户重名允许、密码加密存储成立。

- [ ] **Step 5: Commit**

Commit message: `feat: add tenant scoped user registration`

### Task 4: 新增用户启用/禁用并接入登录链路

**Files:**
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserStatusUpdateReqDTO.java`
- Modify: `demo-system/src/main/java/com/demo/system/app/UserAppService.java`
- Modify: `demo-system/src/main/java/com/demo/system/service/UserService.java`
- Modify: `demo-system/src/main/java/com/demo/system/service/AuthService.java`
- Modify: `demo-system/src/main/java/com/demo/system/infra/mapper/SysUserMapper.java`
- Modify: `demo-system/src/main/java/com/demo/system/enums/AuthErrorCode.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`
- Modify: `demo-system/src/test/java/com/demo/system/UserManagementTests.java`

- [ ] **Step 1: 先写失败测试，锁定禁用用户的管理和登录表现**

新增或调整测试：

- `UserManagementTests` 覆盖启用 -> 禁用 -> 再启用的接口行为，并断言数据库 `status` 正确切换。
- `AuthFlowTests` 覆盖禁用用户登录返回显式错误码，建议使用 `AuthErrorCode.USER_DISABLED`。
- `UserManagementTests` 覆盖跨租户 header 不能修改其他租户用户状态，防止绕过租户拦截。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-system -am test -Dtest=UserManagementTests,AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前 `sys_user` 没有状态字段，登录链路也不会拦截禁用用户。

- [ ] **Step 3: 实现状态流转与认证拦截**

实现要求：

- 新增 `UserStatusUpdateReqDTO`，请求字段建议为 `id` 和 `enabled`，应用层转成 `UserStatusEnum`，避免在 service 里散落魔法值。
- `UserService.updateStatus(...)` 只允许更新当前租户下的用户，找不到则抛 `UserErrorCode.USER_NOT_FOUND`。
- `SysUserMapper.selectForLogin(...)` 需要查询 `status` 字段；`AuthService.authenticate(...)` 在密码校验前先判断是否禁用，并抛 `BizException(AuthErrorCode.USER_DISABLED)`。
- `AuthErrorCode.USER_DISABLED` 放在 `2001xxx` 号段内继续编排，不要跨到租户或用户管理号段。
- 登录查询仍保留重复用户防御逻辑，即便真实 schema 已加唯一键，也要能显式报告脏数据问题。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-system -am test -Dtest=UserManagementTests,AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，状态切换成功，禁用用户被拦截，重新启用后可恢复登录。

- [ ] **Step 5: Commit**

Commit message: `feat: add user enable disable flow`

### Task 5: 新增用户改密、改资料、删除接口

**Files:**
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserPasswordUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserProfileUpdateReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/UserDeleteReqDTO.java`
- Modify: `demo-system/src/main/java/com/demo/system/app/UserAppService.java`
- Modify: `demo-system/src/main/java/com/demo/system/service/UserService.java`
- Modify: `demo-system/src/main/java/com/demo/system/infra/mapper/SysUserMapper.java`
- Modify: `demo-system/src/test/java/com/demo/system/UserManagementTests.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`

- [ ] **Step 1: 先补失败测试**

`UserManagementTests` 至少覆盖：

- 改密成功后，新密码可登录、旧密码不可登录。
- 修改资料后，`display_name/mobile/email` 持久化成功。
- 删除后 `deleted = 1`，同租户通过 ID 再查不到该用户。
- 删除后登录返回“用户名或密码错误”而不是 `500`。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-system -am test -Dtest=UserManagementTests,AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前没有改密、改资料、删除接口，也没有相应 service 方法。

- [ ] **Step 3: 实现用户后续生命周期接口**

实现要求：

- `updatePassword` 仅更新当前租户下指定用户的新密码，并重新通过 `PasswordEncoder` 编码。
- `UserPasswordUpdateReqDTO` 的新密码字段至少加 `@Size(min = 6)`。
- `updateProfile` 仅更新 `displayName/mobile/email`，不允许顺手修改用户名或租户归属。
- `delete` 使用逻辑删除，直接更新 `deleted = 1`，不做物理删除。
- 所有接口都要沿用“当前租户内按 ID 操作”的模式，禁止 `@InterceptorIgnore` 绕开租户拦截器。
- 改密和删除后，`selectForLogin(...)` 仍只查询 `deleted = 0` 用户，保证登录链路自然收敛。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-system -am test -Dtest=UserManagementTests,AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，改密、改资料、删除及登录回归全部通过。

- [ ] **Step 5: Commit**

Commit message: `feat: add user lifecycle management apis`

### Task 6: 补齐 OpenAPI、README 与总回归

**Files:**
- Modify: `demo-boot/src/main/java/com/demo/boot/config/OpenApiConfig.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `README.md`
- Modify: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`（只跑不改；如失败再补）

- [ ] **Step 1: 先补失败测试，锁定文档分组和公开路径**

更新 `OpenApiDocumentationTests` 目标：

- 根文档 `/v3/api-docs` 包含全部新接口路径，用户创建路径按 `/api/system/user/create` 断言。
- 新增分组文档，建议是 `/v3/api-docs/system-tenant` 和 `/v3/api-docs/system-user`；断言各自只包含自己的路径，不串组。
- DTO schema 中展示 `UserRegisterReqDTO`、`UserStatusUpdateReqDTO`、`TenantCreateReqDTO` 等字段说明。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-boot -am test -Dtest=OpenApiDocumentationTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前 OpenAPI 只有 `system-auth` 和 `mdm-dict` 分组。

- [ ] **Step 3: 实现文档与验收配置**

实现要求：

- `OpenApiConfig` 保留 `system-auth`，新增 `system-tenant` 与 `system-user` 两个分组。
- `README.md` 的接口文档入口补充 `/v3/api-docs/system-tenant` 和 `/v3/api-docs/system-user`。
- 所有 controller、ReqDTO、RspDTO 补齐 `@Tag`、`@Operation`、`@Schema`，避免出现裸字段。

- [ ] **Step 4: 执行总回归**

Run: `mvn -q test`

Expected: PASS

Run: `mvn -q compile`

Expected: PASS

- [ ] **Step 5: Commit**

Commit message: `docs: expose tenant and user management apis`

## 风险与待确认项

- `username` 是否允许后续变更：本计划默认不允许。如果首席架构师希望用户名可改，需要补充唯一性、登录兼容和审计要求。
- “修改密码”是否需要校验旧密码：本计划默认是管理口径，不校验旧密码。如果要做自助改密，需要等认证上下文落地后另起接口。
- 删除后用户名不可复用：这是跟当前唯一键和逻辑删除策略一致的最小方案；如果业务要求复用，需要额外设计逻辑删除值和唯一索引。
- 本计划没有新增列表/详情接口；如果后续需要配管理端页面，应单独补一个“租户/用户查询能力”计划，不要在执行阶段顺手扩 scope。

## 覆盖检查

- 多租户增删改：由 Task 2 覆盖。
- 用户注册：由 Task 3 覆盖。
- 用户启用/禁用：由 Task 4 覆盖。
- 修改密码：由 Task 5 覆盖。
- 修改用户信息：由 Task 5 覆盖。
- 删除用户：由 Task 5 覆盖。
- 数据库迁移、OpenAPI、README、全量回归：由 Task 1 和 Task 6 覆盖。
