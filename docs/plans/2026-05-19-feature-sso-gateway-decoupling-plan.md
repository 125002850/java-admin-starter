# feature/sso 去租户去鉴权改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前基础架构从“项目内置租户、用户、登录鉴权”收缩为“仅承载业务能力，由网关接入 SSO 服务，本仓库不实现任何鉴权、租户、用户功能”。

**Architecture:** 推荐做法是一次性完成“能力切断 + 语义清理”：删除 `demo-system` 模块，先将 `demo-mdm` 收敛为纯全局字典，再移除 `demo-core` 中的租户上下文与多租户拦截器，避免出现中间提交无法编译。网关与 SSO 的接入不在本仓库落认证代码，只在文档中明确“网关透传操作人信息 -> 应用内审计字段填充”的正向契约；应用只消费可信头做审计，不承担登录、鉴权或租户判定。

**Tech Stack:** Java 17, Spring Boot 3.x, MyBatis-Plus, Flyway, OpenAPI 3 + Knife4j, JUnit 5, H2, Maven

---

## 1. 现状判断

- `demo-system` 当前承载了 `/api/system/auth/**`、`/api/system/tenant/**`、`/api/system/user/**` 三组接口，以及 `sys_tenant_global`、`sys_user` 的实体、Mapper、服务和测试。
- `demo-core` 当前内置了 `TenantContext`、`TenantFilter`、`TenantResolver`、`TenantLineInnerInterceptor`，并把 `X-Tenant-Id` 作为基础设施层约束。
- `demo-core` 的 `CommonMetaObjectHandler` 当前固定以 `0L` 填充 `create_by` / `update_by`，尚未定义接入 SSO 之后的操作人来源。
- `demo-mdm` 当前同时支持“租户字典”和“全局字典”；租户字典依赖 `TenantContext.requireTenantId()`，数据库结构由 `mdm_dict_type` / `mdm_dict_item` 两张带 `tenant_id` 的表支撑。
- `README.md`、OpenAPI 分组、Flyway 测试和契约测试都默认项目自身承担租户、用户和登录能力；如果只删接口不删底层语义，仓库会继续误导后续开发。

## 2. 方案比较

### 方案 A：仅删除 `/api/system/**` 接口，保留租户基础设施

- 优点：代码改动最小，短期通过编译最快。
- 缺点：`demo-core` 仍要求 `X-Tenant-Id`，`demo-mdm` 仍是租户模型，数据库仍保留 `sys_user` / `sys_tenant_global`，与“项目不做租户/用户/鉴权”的目标矛盾。

### 方案 B：删除 `demo-system`，保留 `demo-core` 多租户与 `demo-mdm` 租户字典

- 优点：比方案 A 干净，系统模块确实退出。
- 缺点：本质上仍然保留了租户基础设施，只是把“租户管理接口”挪走；后续业务仍会自然依赖 `tenant_id` 和 `TenantContext`，架构方向仍然偏离目标。

### 方案 C：完整去租户化与去鉴权化，仅保留业务与全局主数据

- 优点：目标最一致，仓库语义最清晰，后续业务模块不会再误用本地租户/用户模型；还能顺手消除当前 `TenantFilterTests` 带来的全仓 `mvn test` 阻塞点。
- 缺点：涉及 `pom`、Flyway、`demo-core`、`demo-mdm`、测试和 README 的联动修改，属于一次完整收缩改造。

**推荐方案：** 采用方案 C。用户给出的目标是“只通过网关接入 SSO 服务，本项目不做任何关于鉴权以及租户/用户相关的功能”，继续保留租户基础设施会直接违背这个目标。

## 3. 范围与关键假设

- 本仓库不新增任何 SSO SDK、JWT 解析、Token 校验、Session 管理、权限注解或用户上下文代码。
- “通过网关接入 SSO 服务”是系统边界，不是本仓库功能；网关路由、鉴权链和 SSO 联调只在文档中沉淀，不在本仓库增加实现。
- 网关在认证成功后，可透传 `X-User-Id`（审计必需，数值型）和 `X-User-Name`（可选，仅用于日志/排障）；本仓库只集中读取这两个头，不透传 `X-Tenant-Id`，也不接收角色/权限头用于业务判定。
- `create_by` / `update_by` 的目标策略必须在本期落清楚：`CommonMetaObjectHandler` 优先读取统一的“网关操作人上下文”，缺失时回退到 `0L`，以支持本地 dev 无网关直连场景。
- 默认假设当前分支上的租户字典数据可以废弃，或者可以在删除前人工迁移到全局字典；如果必须保留租户字典中的历史数据，需要在删除表前增加一次性数据归并 migration。
- 默认假设 `demo-mdm` 后续只保留全局字典，不再提供任何 `tenant_id` 维度的数据隔离。如果后续业务确实需要“按组织/公司/业务单元隔离”的数据模型，应在具体业务模块中显式建模，不能恢复平台级 `tenant` 基础设施。
- Flyway 仍遵守“只新增、不回改历史版本”的仓库约束，不能回改 `V1` 到 `V5`。

## 4. 目标状态

- 根 `pom.xml` 不再聚合 `demo-system` 模块。
- `demo-boot` 不再依赖 `demo-system`，OpenAPI 只保留 `mdm-dict` 分组。
- `demo-core` 不再存在 `tenant` 包，不再注册 `TenantLineInnerInterceptor`，并新增仅用于审计的网关操作人上下文，统一承接 `X-User-Id` / `X-User-Name`。
- `demo-mdm` 只保留全局字典接口 `/api/mdm/dict/global/**`；所有租户字典代码、测试和表结构被删除。
- 数据库不再存在 `sys_tenant_global`、`sys_user`、`mdm_dict_type`、`mdm_dict_item` 这四张仅为本地租户/用户模型服务的表。
- `CommonMetaObjectHandler` 不再依赖本地鉴权/租户能力；它统一从网关操作人上下文读取 `create_by` / `update_by`，在本地无头调用时回退为 `0L`。
- README、计划文档和测试基线不再描述或校验本地鉴权、租户上下文、`X-Tenant-Id`、`tenant_id`、`/api/system/**`。

## 5. 影响文件与职责

### 根模块与启动模块

- Modify: `pom.xml`
- Modify: `demo-boot/pom.xml`
- Modify: `demo-boot/src/main/java/com/demo/boot/config/OpenApiConfig.java`
- Create: `demo-boot/src/main/resources/db/migration/V6__remove_local_auth_and_tenant_schema.sql`
- Create: `demo-boot/src/test/java/com/demo/boot/architecture/ModuleBoundaryTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/mybatis/AuditFieldMetadataTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`

### `demo-core`

- Delete: `demo-core/src/main/java/com/demo/core/tenant/TenantContext.java`
- Delete: `demo-core/src/main/java/com/demo/core/tenant/TenantFilter.java`
- Delete: `demo-core/src/main/java/com/demo/core/tenant/TenantResolver.java`
- Delete: `demo-core/src/main/java/com/demo/core/tenant/HeaderTenantResolver.java`
- Delete: `demo-core/src/main/java/com/demo/core/tenant/TenantIgnoreTables.java`
- Delete: `demo-core/src/main/java/com/demo/core/tenant/MissingTenantContextException.java`
- Delete: `demo-core/src/main/java/com/demo/core/tenant/InvalidTenantHeaderException.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/OperatorContext.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/GatewayOperatorFilter.java`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/CommonMetaObjectHandler.java`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/MybatisPlusConfig.java`
- Modify: `demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java`
- Delete: `demo-core/src/test/java/com/demo/core/tenant/TenantFilterTests.java`
- Delete: `demo-core/src/test/java/com/demo/core/tenant/TenantContextTests.java`
- Delete: `demo-core/src/test/java/com/demo/core/tenant/HeaderTenantResolverTests.java`
- Delete: `demo-core/src/test/java/com/demo/core/tenant/TenantIgnoreTablesTests.java`
- Create: `demo-core/src/test/java/com/demo/core/operator/GatewayOperatorFilterTests.java`
- Create: `demo-core/src/test/java/com/demo/core/mybatis/CommonMetaObjectHandlerTests.java`
- Modify: `demo-core/src/test/java/com/demo/core/mybatis/MybatisPlusConfigTests.java`

### `demo-system`

- Delete: `demo-system/pom.xml`
- Delete: `demo-system/src/main/java/com/demo/system/**`
- Delete: `demo-system/src/test/java/com/demo/system/**`
- Delete: `demo-system/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

### `demo-mdm`

- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/TenantDictController.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeCreateReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeUpdateReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeDeleteReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeListReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeRspDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictItemCreateReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictItemUpdateReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictItemDeleteReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictListReqDTO.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/DictTypeEntity.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/DictItemEntity.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/mapper/DictTypeMapper.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/mapper/DictItemMapper.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/app/DictAppService.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/service/DictServiceDuplicateKeyTests.java`

### 文档

- Modify: `README.md`
- Create: `docs/plans/2026-05-19-feature-sso-gateway-decoupling-plan.md`
- Create: `docs/architecture/` 下的网关-SSO 边界说明文档（如目录当前不存在，则在 `docs/` 下新增同义文档）

## 6. 分阶段任务

### Task 1: 冻结收缩边界与验收口径

**Files:**
- Modify: `README.md`
- Create: `docs/architecture/2026-05-19-gateway-sso-boundary.md`
- Create: `docs/plans/2026-05-19-feature-sso-gateway-decoupling-plan.md`

- [ ] **Step 1: 先明确这次改造的边界文档**

文档里必须明确以下结论：

- 本仓库不再提供登录、租户管理、用户管理、用户资料、用户密码等任何能力。
- 本仓库不再校验 `X-Tenant-Id`，也不再要求业务表必须包含 `tenant_id`。
- SSO 接入点在网关，不在本仓库；本仓库只接受“已被网关放行的业务请求”。
- 当前仍保留的主数据能力只有“全局字典”，不再区分租户字典。
- 网关认证成功后，应用侧仅接收 `X-User-Id`（审计必需）和 `X-User-Name`（可选，日志用），不接收 `X-Tenant-Id`，也不消费角色/权限头做业务判定。
- 可信边界必须写清楚：这些头只在“网关到应用”的私有链路上可信；应用自身不做登录态校验，也不对外暴露“随便带头即可冒充用户”的调用方式。
- 本地 dev 无网关时，缺失操作人头不会阻断请求；`CommonMetaObjectHandler` 回退 `create_by` / `update_by = 0L`。
- 必须明确 breaking change 迁移表：`POST /api/mdm/dict/items/by-type` 下线，调用方统一迁移到 `POST /api/mdm/dict/global/items/by-type`；当前仓库内无该旧接口的真实业务调用方，只有测试覆盖，因此本计划不引入运行期兼容 shim。

- [ ] **Step 2: 为文档先写失败式检查项**

执行前先列出验收命令。注意：`rg` 只作为文档级 sanity check，不是最终 gate；最终 gate 在 Task 6 通过 ArchUnit + OpenAPI + 全量测试完成。

Run: `rg -n "/api/system|X-Tenant-Id|TenantContext|TenantResolver|TenantFilter|sys_user|sys_tenant_global" demo-boot demo-core demo-mdm README.md`

Expected: 改造完成后，主源码与 README 中不再出现这些语义；允许只在 Flyway 删除脚本和历史文档中出现。

Run: `mvn -q test`

Expected: PASS，且不再出现 `TenantFilterTests` 的 Mockito 自附着失败。

- [ ] **Step 3: Commit**

Commit message: `docs: define sso gateway decoupling scope`

### Task 2: 删除 `demo-system` 模块与 `/api/system/**` 外部契约

**Files:**
- Modify: `pom.xml`
- Modify: `demo-boot/pom.xml`
- Modify: `demo-boot/src/main/java/com/demo/boot/config/OpenApiConfig.java`
- Delete: `demo-system/**`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`

- [ ] **Step 1: 先把外部契约锁成红灯**

调整测试期望：

- `/v3/api-docs` 不再包含 `/api/system/auth/login`、`/api/system/tenant/create`、`/api/system/user/create` 等路径。
- `/v3/api-docs` 仍可访问，且保留 `mdm-dict` 分组。
- `ErrorCodeContractTests` 的源码扫描根目录不再包含 `demo-system/src/main/java`。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-boot -am test -Dtest=OpenApiDocumentationTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前仓库仍暴露 `/api/system/**`，且扫描范围仍包含 `demo-system`。

- [ ] **Step 3: 执行模块删除**

改动要求：

- 根 `pom.xml` 移除 `<module>demo-system</module>`。
- `demo-boot/pom.xml` 移除对 `demo-system` 的依赖。
- `OpenApiConfig` 删除 `system-auth`、`system-tenant`、`system-user` 三个分组 Bean。
- 物理删除 `demo-system` 模块目录，而不是只从 Maven 聚合中摘除，避免死代码继续误导仓库。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-boot -am test -Dtest=OpenApiDocumentationTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，OpenAPI 中只剩业务能力，源码契约测试不再扫描 `demo-system`。

- [ ] **Step 5: Commit**

Commit message: `refactor: remove local auth tenant and user module`

### Task 3: 先收敛 `demo-mdm`，清除对 `TenantContext` 的编译依赖

**Files:**
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/TenantDictController.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/Dict*.java` 中全部租户字典 DTO
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/DictTypeEntity.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/DictItemEntity.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/mapper/DictTypeMapper.java`
- Delete: `demo-mdm/src/main/java/com/demo/mdm/infra/mapper/DictItemMapper.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/app/DictAppService.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/service/DictServiceDuplicateKeyTests.java`

- [ ] **Step 1: 先锁定新的 MDM 能力范围**

测试目标改为：

- 只校验 `/api/mdm/dict/global/**` 相关能力。
- 不再要求任何请求头携带 `X-Tenant-Id`。
- 不再校验租户字典的增删改查、租户字典唯一约束和租户上下文错误响应。
- 删除旧接口 `/api/mdm/dict/items/by-type` 后，测试和文档都必须只保留迁移目标 `/api/mdm/dict/global/items/by-type`。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-mdm -am test`

Expected: FAIL，当前 `demo-mdm` 仍然包含租户字典控制器、实体、Mapper、服务分支和对应测试。

- [ ] **Step 3: 执行 MDM 收缩**

改动要求：

- `DictService` 先删除所有依赖 `TenantContext`、`tenantId`、`mdm_dict_type`、`mdm_dict_item` 的方法和分支，再处理 `demo-core` 的 tenant 包删除，避免中间提交无法编译。
- `DictAppService` 删除租户字典编排方法，仅保留全局字典编排方法。
- 租户字典 DTO、实体、Mapper 和 `TenantDictController` 全部物理删除。
- `GlobalDictController` 如果代码无需变化，则不要在实现里制造无意义修改；该文件不作为必改项。
- 旧接口 `/api/mdm/dict/items/by-type` 不保留兼容 shim；调用迁移完全通过 Task 1 的边界文档和 README 明确声明。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-mdm -am test`

Expected: PASS，`demo-mdm` 不再依赖 `demo-core.tenant`，且旧租户字典接口全部退出。

- [ ] **Step 5: Commit**

Commit message: `refactor: converge mdm to global dictionary only`

### Task 4: 删除 `demo-core` 租户基础设施，并补齐网关操作人审计上下文

**Files:**
- Delete: `demo-core/src/main/java/com/demo/core/tenant/**`
- Create: `demo-core/src/main/java/com/demo/core/operator/OperatorContext.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/GatewayOperatorFilter.java`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/CommonMetaObjectHandler.java`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/MybatisPlusConfig.java`
- Modify: `demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java`
- Delete: `demo-core/src/test/java/com/demo/core/tenant/**`
- Create: `demo-core/src/test/java/com/demo/core/operator/GatewayOperatorFilterTests.java`
- Create: `demo-core/src/test/java/com/demo/core/mybatis/CommonMetaObjectHandlerTests.java`
- Modify: `demo-core/src/test/java/com/demo/core/mybatis/MybatisPlusConfigTests.java`

- [ ] **Step 1: 先锁定新的基础设施契约**

测试需要改为：

- `MybatisPlusConfigTests` 只校验保留 `PaginationInnerInterceptor` 和逻辑删除配置，不再期望 `TenantLineInnerInterceptor`。
- `CommonMetaObjectHandlerTests` 必须覆盖：有 `X-User-Id` 时写入该值；无头时回退 `0L`。
- `GatewayOperatorFilterTests` 必须覆盖：读取 `X-User-Id` / `X-User-Name`，请求结束后清理上下文；头缺失时不拦请求；头存在但 `X-User-Id` 非数字时返回 `400`，暴露网关契约错误。
- 不再存在 `TenantFilterTests`、`TenantContextTests`、`HeaderTenantResolverTests`、`TenantIgnoreTablesTests`。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-core test -Dtest=MybatisPlusConfigTests,GatewayOperatorFilterTests,CommonMetaObjectHandlerTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前 `demo-core` 仍然内置租户拦截器，且还没有网关操作人上下文与对应测试实现。

- [ ] **Step 3: 执行基础设施收缩**

改动要求：

- 删除整个 `com.demo.core.tenant` 包，并从 `MybatisPlusConfig` 移除 `TenantLineHandler` / `TenantLineInnerInterceptor`。
- 新增轻量 `OperatorContext` 与 `GatewayOperatorFilter`，集中读取 `X-User-Id` / `X-User-Name`，只服务审计字段与日志，不承担认证或鉴权逻辑。
- `GatewayOperatorFilter` 只信任来自网关私有入口的头；本地 dev 缺头时允许请求继续，不构成本地鉴权能力。
- `CommonMetaObjectHandler` 必须显式改造：优先写入当前操作人 ID；缺失时保持 `0L`，并在计划/文档中明确这是“无网关本地开发回退值”。
- `GlobalExceptionHandler` 删除对 `MissingTenantContextException` 的专门处理；如果新增的网关头校验需要错误响应，必须围绕新契约单独处理，不能复用租户异常语义。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-core test -Dtest=MybatisPlusConfigTests,GatewayOperatorFilterTests,CommonMetaObjectHandlerTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，且 `CommonMetaObjectHandler` 的 `create_by` / `update_by` 来源在无网关和有网关两种场景下都被显式覆盖。

- [ ] **Step 5: Commit**

Commit message: `refactor: replace tenant context with gateway operator audit context`

### Task 5: 用新增 migration 清理本地租户/用户/租户字典表

**Files:**
- Create: `demo-boot/src/main/resources/db/migration/V6__remove_local_auth_and_tenant_schema.sql`
- Modify: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/mybatis/AuditFieldMetadataTests.java`

- [ ] **Step 1: 先锁定新的 schema 契约**

`FlywaySmokeTests` 需要改为：

- `hasSuccessfulMigration("6")` 为真。
- 不再要求 `sys_tenant_global`、`sys_user`、`mdm_dict_type`、`mdm_dict_item` 存在。
- 只校验 `sys_dict_type_global`、`sys_dict_item_global` 仍存在并保留审计字段与唯一约束。
- `AuditFieldMetadataTests` 只校验仍然保留的实体类，不再引用 `SysUserEntity`、`DictTypeEntity`、`DictItemEntity`。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests,AuditFieldMetadataTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前 schema 和实体基线仍然包含租户/用户表与实体。

- [ ] **Step 3: 编写 migration**

`V6__remove_local_auth_and_tenant_schema.sql` 至少包含：

- 删除 `sys_user`
- 删除 `sys_tenant_global`
- 删除 `mdm_dict_item`
- 删除 `mdm_dict_type`

执行前要先确认是否需要数据归档：

- 如果当前环境没有真实数据，可以直接 drop。
- 如果已有必须保留的租户字典数据，先补一条前置 migration，把数据人工映射并迁入全局字典表，再执行 drop；不要在删除脚本里做不可审计的隐式归并。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests,AuditFieldMetadataTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，Flyway 与实体元数据契约都只反映“全局字典 + 通用底座”的新状态。

- [ ] **Step 5: Commit**

Commit message: `refactor: remove local auth tenant and tenant-dict schema`

### Task 6: README、契约搜索与全仓验证收口

**Files:**
- Modify: `README.md`
- Modify: `demo-boot/pom.xml`
- Create: `demo-boot/src/test/java/com/demo/boot/architecture/ModuleBoundaryTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`

- [ ] **Step 1: 更新 README，并补 bytecode 级边界守卫**

至少要更新这些部分：

- 模块结构：删除 `demo-system` 描述，重写 `demo-core` 和 `demo-mdm` 的职责。
- 模块职责表：去掉租户、用户、登录鉴权描述。
- API 文档链接：删除 `system-auth`、`system-tenant`、`system-user`。
- 数据规范：删除“所有业务表必须包含 `tenant_id`”与 `X-Tenant-Id` 兼容输入说明，并补上 `create_by` / `update_by` 的新来源说明。
- 完成标准：保留 `mvn test` 作为最终验收基线。
- 在 `demo-boot/pom.xml` 增加 ArchUnit 测试依赖，并新增 `ModuleBoundaryTests`：
  - 禁止主源码依赖 `com.demo.core.tenant..`
  - 禁止存在 `/api/system/**` 控制器
  - 禁止 `demo-mdm` 暴露 `/api/mdm/dict/**` 的非 global 路径

- [ ] **Step 2: 跑收口检查**

Run: `rg -n "/api/system|X-Tenant-Id|TenantContext|TenantResolver|TenantFilter|sys_user|sys_tenant_global" demo-boot demo-core demo-mdm README.md`

Expected: 除删除脚本、历史文档或计划文档外，主代码与 README 中不再出现这些语义；该命令只做补充检查，不代替测试 gate。

Run: `mvn -q -pl demo-boot -am test -Dtest=ModuleBoundaryTests,OpenApiDocumentationTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，bytecode 级边界守卫、OpenAPI 契约和源码约束同时成立。

Run: `mvn -q test`

Expected: PASS，全仓测试通过。

- [ ] **Step 3: Commit**

Commit message: `docs: align readme with gateway sso architecture`

## 7. 风险与检查点

- **风险 1：租户字典历史数据是否需要保留。**
  处理方式：在执行 Task 5 前先做数据盘点；若需保留，先单独设计“租户字典到全局字典”的一次性归并规则，再执行删除。执行 V6 前先对 `sys_user` / `sys_tenant_global` / `mdm_dict_type` / `mdm_dict_item` 做数据备份（`mysqldump` 或等效手段），确保误删可恢复。

- **风险 2：后续业务是否仍需要请求级身份信息。**
  处理方式：本期只实现最小审计契约：`X-User-Id` + 可选 `X-User-Name`。如果后续需要更多身份信息，必须继续通过网关统一透传，并沿用单一上下文入口，禁止业务模块各自直接读 header。

- **风险 3：OpenAPI 与 README 同步不彻底。**
  处理方式：把 `OpenApiDocumentationTests`、`rg` 契约搜索和 README 更新放到同一阶段收口，避免代码删了但文档仍旧宣称支持本地认证。

- **风险 4：仅从 Maven 聚合中移除 `demo-system`，但目录残留导致误导。**
  处理方式：明确要求物理删除模块目录，并让 `ErrorCodeContractTests` 的源码扫描不再包含该模块。

- **风险 5：旧接口 `/api/mdm/dict/items/by-type` 的外部调用方未提前迁移。**
  处理方式：Task 1 的边界文档和 README 必须显式发布迁移表，说明该接口下线且迁移目标为 `/api/mdm/dict/global/items/by-type`；合并前需要确认外部调用方已完成切换。

## 8. 完成标准

- 根模块和启动模块均不再依赖 `demo-system`。
- 主源码中不再存在 `tenant` 基础设施、`/api/system/**` 接口和 `X-Tenant-Id` 约束。
- `CommonMetaObjectHandler` 的 `create_by` / `update_by` 来源已明确：优先读取网关操作人上下文，缺失时回退 `0L`。
- `demo-mdm` 只保留全局字典能力，且不依赖任何租户上下文。
- Flyway 最新版本已清理本地租户/用户/租户字典表，且未回改历史 migration。
- `README.md` 已与目标架构一致。
- `ModuleBoundaryTests`、`OpenApiDocumentationTests`、`ErrorCodeContractTests` 与 `mvn -q test` 共同通过。
- `mvn -q test` 通过。
