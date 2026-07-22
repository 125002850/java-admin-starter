# main 分支本地 IAM 管理后台基础能力 PRD

## 1. 文档信息

| 项目 | 内容 |
|------|------|
| 文档状态 | Draft |
| 编写日期 | 2026-07-08 |
| 适用分支 | `main` |
| 不适用分支 | `feature/sso` |
| 目标系统 | `java-admin-starter` 管理后台后端基础框架 |
| 相关前端 | `tanstack-start-admin`，前端 PRD 后续单独输出 |

## 2. 背景

当前项目定位为管理后台基础框架，但历史上存在两类不同形态：

- `main` 分支：应作为可独立运行的本地权限版管理后台底座。
- `feature/sso` 分支：登录、员工、部门、角色、菜单、权限均由 SSO 管理，本项目只承载业务能力。

本 PRD 只定义 `main` 分支需要补齐的本地 IAM 能力。`main` 分支应完全隔离 SSO 概念，不依赖网关透传身份、SSO 用户、SSO 角色、SSO 菜单或 SSO 权限。

## 3. 产品目标

1. 在 `main` 分支恢复并完善管理后台本地登录鉴权能力。
2. 补齐员工、部门、角色、菜单、按钮权限、数据权限、登录日志和操作日志。
3. 提供后端接口级权限校验能力，而不是只依赖前端菜单展示控制。
4. 形成可复用的基础框架，使后续业务模块可以接入统一用户上下文、RBAC 权限和数据权限。
5. 保持 `main` 与 `feature/sso` 的职责隔离，避免本地 IAM 逻辑污染 SSO 分支。

## 4. 非目标

本期不做以下能力：

- 不做 SSO、OAuth2、OIDC、CAS、LDAP 或第三方登录。
- 不做多租户。多租户未来通过独立分支设计。
- 不支持手机号或邮箱登录，第一版只支持用户名登录。
- 不做角色互斥、deny 权限、岗位、组织负责人、审批流。
- 不做 access token 实时黑名单。
- 不限制多设备登录数量。
- 不做服务端 CSRF token 防护。`main` 第一版采用 stateless JWT + Bearer Authorization，不使用 Cookie session 承载登录态。
- 不做密码历史和密码复用限制。第一版只校验当前密码复杂度，后续如需合规策略再扩展。
- 不输出前端页面级 PRD。前端 PRD 后续基于本文继续拆解。

## 5. 分支边界

### 5.1 `main` 分支

`main` 是本地 IAM 版管理后台基础框架，内置以下能力：

- 登录认证。
- JWT access token 与 refresh token。
- 员工管理。
- 部门管理。
- 角色管理。
- 菜单和按钮权限管理。
- 后端接口权限校验。
- 数据权限。
- 登录日志。
- 操作日志。
- 字典、文件、导出中心、动态查询等通用后台能力。

`main` 分支不应依赖：

- SSO 服务。
- 网关透传 `X-User-Id`。
- 网关透传角色或权限。
- 外部统一菜单权限中心。

### 5.2 `feature/sso` 分支

`feature/sso` 是外置身份权限版：

- 登录由 SSO 负责。
- 员工、部门、角色、菜单、权限由 SSO 管理。
- 后端只消费可信链路透传的操作人信息。
- 本地不提供 IAM 管理能力。

### 5.3 共享能力

两个分支可以共享：

- `core` 中的统一响应、异常、校验、Jackson、MyBatis、动态查询、导出基础抽象。
- `system` 中的全局字典、导出中心、文件存储等系统级平台能力与外部系统集成能力。

## 6. 模块定位

### 6.1 新增模块

新增 `iam` 模块，承载本地身份与权限能力。

模块职责：

- 登录认证。
- 员工、部门、角色、菜单、权限管理。
- refresh token 生命周期。
- RBAC 权限解析与校验。
- 数据权限上下文与查询辅助能力。
- 登录日志。
- 操作日志。

### 6.2 不放入 `system`

IAM 不放入 `system`。

原因：

- `system` 面向系统级平台能力和外部服务集成。
- IAM 涉及认证、token、安全上下文、权限校验和日志审计，是安全基础能力，不只是主数据。
- 将 IAM 放入 `system` 会导致系统平台模块承担安全网关职责，模块边界不清晰。

## 7. 角色与使用对象

| 角色 | 说明 |
|------|------|
| 超级管理员 | 维护系统初始化数据、员工、部门、角色、菜单、权限和日志 |
| 普通管理员 | 按角色授权使用管理后台 |
| 业务用户 | 登录后台并使用被授权的业务功能 |
| 后端开发者 | 在新业务模块中接入权限校验、操作日志和数据权限 |
| 前端开发者 | 根据菜单树、按钮权限和当前用户信息渲染前端后台 |

## 8. 核心术语

| 术语 | 定义 |
|------|------|
| 员工 | 后台本地用户主体，使用 `staff` 命名 |
| 部门 | 员工组织归属，也是数据权限计算基础 |
| 角色 | 权限集合，绑定菜单、按钮权限和数据权限范围 |
| 菜单 | 前端路由或导航节点 |
| 按钮权限 | 页面内操作权限点，对应后端接口权限码 |
| 权限标识 | 如 `iam:staff:create`，用于后端注解校验和前端按钮控制 |
| 数据权限 | 限定用户可访问哪些组织或本人数据 |
| access token | 短期 JWT，用于接口访问 |
| refresh token | 长期随机令牌，用于刷新 access token |

## 9. 总体业务规则

1. 所有接口响应统一使用 `R.ok(...)` 或 `R.fail(...)`。
2. 业务异常统一使用 `BizException(ErrorCode)`。
3. 所有业务接口统一使用 `POST`。
4. URL 格式遵守 `/api/{模块名}/{资源名}/{动作}`。
5. Controller 必须补齐 OpenAPI `@Tag` 和 `@Operation`。
6. ReqDTO 和 RspDTO 必须补齐 `@Schema`。
7. `main` 分支的当前操作人来自本地 JWT 登录态，不来自 SSO header。
8. 审计字段 `create_by`、`update_by` 来自当前登录员工 ID；无登录上下文的初始化或测试场景可以回退为 `0L`。
9. 密码、token 等敏感信息禁止明文落库、禁止明文返回、禁止明文写入日志。

### 9.1 认证架构决策

`main` 分支采用 Spring Security 作为认证过滤链基础设施。

架构决策：

1. 引入 `spring-boot-starter-security`。
2. 使用 Spring Security `SecurityFilterChain` 组织认证、匿名访问、异常处理和受保护接口。
3. 使用 JWT 认证过滤器解析 access token，并将当前员工身份写入 `SecurityContextHolder`。
4. JWT 认证过滤器可基于 `OncePerRequestFilter` 实现，并注册到 Spring Security filter chain 中。
5. 不使用 `HandlerInterceptor` 承担认证职责。
6. 不启用 Spring Method Security 作为第一版权限模型。
7. `@RequiresPermission` 使用自定义 AOP advice 实现，读取 `SecurityContextHolder` 中的当前员工权限快照。
8. 未登录和无权限响应必须由统一认证异常处理组件转换为标准 `R.fail(...)`。

### 9.2 JWT 内容决策

access token 使用最小 JWT claims。

必须包含：

- `sub`：员工 ID。
- `jti`：access token 唯一 ID。
- `iat`：签发时间。
- `exp`：过期时间。

不得包含：

- 角色集合。
- 权限标识集合。
- 菜单树。
- 数据权限范围。

规则：

1. 每次请求只从 JWT 读取员工身份，不从 JWT 信任角色和权限。
2. 认证过滤器通过员工 ID 加载当前员工状态、角色、权限和数据权限快照。
3. 员工被禁用或删除后，后续请求必须在权限快照加载阶段被拒绝。
4. 权限变更不依赖等待 access token 过期生效。
5. 前端 workspace tab 是否实时关闭不是安全边界；后端接口级权限校验必须在每次受保护请求中按最新员工状态、角色、菜单权限和数据权限执行。

### 9.3 权限快照缓存与失效

第一版允许通过本地缓存降低每次请求的权限查询成本，但必须定义失效策略。

规则：

1. 权限快照缓存键为员工 ID。
2. 员工状态变更、员工删除、员工角色分配变更时，必须清理该员工权限快照。
3. 角色状态变更、角色删除、角色菜单变更、角色数据权限变更时，必须清理所有持有该角色的员工权限快照。
4. 菜单状态变更、菜单删除、权限标识变更时，必须清理受影响角色下所有员工权限快照。
5. 如果第一版不引入缓存，则每次请求直接加载权限快照，上述失效规则自然满足。
6. 如果部署为多实例，必须使用可跨实例失效的缓存方案，或明确权限变更在缓存 TTL 内可能延迟生效。

### 9.4 权限变更一致性边界

P0 接受前端 workspace 最终一致，不要求权限变更后立即关闭已打开 tab。

后端边界：

1. `@RequiresPermission` 必须在每次接口调用时按最新权限快照判断。
2. 员工禁用、员工删除、密码修改、管理员重置密码必须失效该员工全部 refresh token。
3. 员工角色分配、角色菜单分配、角色数据权限分配、菜单状态变更默认不强制失效 refresh token，但必须清理权限快照缓存，使下一次接口权限校验实时生效。
4. 如果实施阶段引入“高危权限变更”策略，可以额外失效受影响员工 refresh token，但不是 P0 必需项。
5. 旧前端 UI 继续展示不构成授权，任何失权操作必须由后端返回 403。
6. access token TTL 必须收紧到 15 到 30 分钟范围，默认 30 分钟，可配置。

### 9.5 逻辑删除与唯一约束决策

IAM 新表的唯一约束必须兼容逻辑删除后的同名复用。

规则：

1. `deleted = 0` 表示未删除。
2. 已删除记录的 `deleted` 必须写入行级唯一值，优先使用当前行 ID。
3. 禁止对逻辑删除表直接建立 `unique(username)`、`unique(staff_code)`、`unique(role_code)` 这类不包含 `deleted` 的唯一索引。
4. 员工用户名唯一索引建议为 `(username, deleted)`。
5. 员工工号唯一索引建议为 `(staff_code, deleted)`。
6. 角色编码唯一索引建议为 `(role_code, deleted)`。
7. 角色名称唯一索引建议为 `(role_name, deleted)`。
8. 菜单权限标识唯一索引建议为 `(permission_code, deleted)`，但空权限标识需要单独处理，避免多个目录或菜单因空值冲突。
9. 如果沿用 MyBatis-Plus 逻辑删除，必须确认删除 SQL 能将 `deleted` 更新为行级唯一值；不能使用固定值 `1` 或低精度时间戳导致唯一索引冲突。

## 10. 登录认证需求

### 10.1 登录

接口：

- `POST /api/iam/auth/login`

登录凭证：

- `username`
- `password`

规则：

1. 第一版只支持用户名登录。
2. 用户名匹配本地员工账号。
3. 密码使用 BCrypt 校验。
4. 禁用员工不能登录。
5. 删除员工不能登录。
6. 登录成功返回 access token、refresh token、过期时间、当前员工基础信息、权限标识集合、菜单树摘要信息。
7. 如果员工处于必须修改密码状态，登录仍可成功，但响应中必须返回 `mustChangePassword = true`。
8. `mustChangePassword = true` 时，后端只允许访问修改本人密码、退出登录、获取当前用户信息等最小接口。
9. 登录成功和失败都必须记录登录日志。
10. 登录失败必须有最小响应延迟，默认 500ms，可配置。
11. 第一版不内置 CAPTCHA，但必须记录失败次数，为后续验证码或风控策略预留扩展点。

### 10.2 刷新 token

接口：

- `POST /api/iam/auth/refresh`

规则：

1. 使用 refresh token 换取新的 access token 和 refresh token。
2. refresh token 必须轮换，旧 refresh token 使用后立即失效。
3. refresh token 只在数据库中存储哈希值，不存明文。
4. refresh token 默认有效期 14 天，必须可配置。
5. access token 默认有效期 30 分钟，建议配置范围 15 到 30 分钟。
6. 员工被禁用、删除或密码已变更时，refresh token 不得继续刷新。
7. refresh token 表必须记录服务端生成的会话 ID、设备 ID、IP、User-Agent、签发时间、过期时间、撤销时间。
8. refresh token 无效、过期、已撤销或重放时，刷新接口必须执行与登录失败一致的最小响应延迟，默认 500ms，可配置。
9. refresh token 刷新失败必须记录安全日志，用于后续限流、风控或会话异常分析。

### 10.3 退出登录

接口：

- `POST /api/iam/auth/logout`

规则：

1. 退出登录只失效当前会话的 refresh token。
2. 本期不做 access token 实时黑名单。
3. 已签发 access token 最多在短 TTL 内自然过期。
4. 当前会话由客户端提交的 refresh token 识别，不要求客户端自行生成 device ID。
5. 服务端可以生成 device ID 并随 refresh token 记录，用于日志审计和后续会话管理。

### 10.4 获取当前用户

接口：

- `POST /api/iam/auth/me`

返回：

- 当前员工基础信息。
- 部门信息。
- 角色集合。
- 权限标识集合。
- 可访问菜单树。
- 数据权限摘要。
- 是否必须修改密码。

### 10.5 修改本人密码

接口：

- `POST /api/iam/auth/password/change`

规则：

1. 需要提供旧密码和新密码。
2. 新密码必须满足密码策略。
3. 修改成功后，失效该员工全部 refresh token。
4. 修改成功后，返回新的 access token 和 refresh token，避免用户修改密码后被强制退出当前流程。

## 11. Token 策略

| 项目 | 规则 |
|------|------|
| access token | JWT，默认 30 分钟，建议 15 到 30 分钟，可配置 |
| refresh token | 随机高强度字符串，默认 14 天，可配置 |
| refresh token 存储 | 仅存哈希 |
| refresh token 轮换 | 每次刷新都轮换 |
| 多设备登录 | 第一版不限制 |
| 当前会话退出 | 客户端提交 refresh token，后端失效对应会话 |
| 修改密码 | 失效员工全部 refresh token |
| 禁用员工 | 失效员工全部 refresh token |
| 删除员工 | 失效员工全部 refresh token |
| 角色/权限变更 | 默认只清理权限快照缓存，不强制失效 refresh token |
| access token 黑名单 | 第一版不做 |
| JWT 权限载荷 | 不写入角色、权限、菜单或数据权限 |

## 12. 密码策略

第一版密码规则：

1. 长度 8 到 32 位。
2. 必须包含大写字母。
3. 必须包含小写字母。
4. 必须包含数字。
5. 必须包含特殊字符。
6. 密码使用 BCrypt 存储。
7. 管理员重置密码后，员工下次登录必须修改密码。
8. 初始管理员账号首次登录必须修改密码。

敏感处理：

- 密码字段不得出现在响应 DTO 中。
- 日志中的 `password`、`oldPassword`、`newPassword` 必须脱敏。

## 13. 员工管理需求

### 13.1 员工模型

核心字段：

- 员工 ID。
- 用户名 `username`。
- 员工工号 `staffCode`。
- 员工姓名 `staffName`。
- 所属部门 ID。
- 手机号。
- 邮箱。
- 头像。
- 状态：启用、禁用。
- 是否必须修改密码。
- 备注。
- 审计字段。

唯一性：

- 未删除员工的 `username` 全局唯一。
- 未删除员工的 `staffCode` 全局唯一。

### 13.2 员工接口

建议接口：

- `POST /api/iam/staff/page`
- `POST /api/iam/staff/detail`
- `POST /api/iam/staff/create`
- `POST /api/iam/staff/update`
- `POST /api/iam/staff/delete`
- `POST /api/iam/staff/status/update`
- `POST /api/iam/staff/password/reset`
- `POST /api/iam/staff/roles/assign`

规则：

1. 创建员工时必须指定部门。
2. 创建员工时可以指定角色集合。
3. 创建员工时设置初始密码，或由系统生成初始密码。实现阶段需要固定一种策略。
4. 禁用员工后，该员工不能登录，全部 refresh token 失效。
5. 删除员工采用逻辑删除。
6. 删除员工后，该员工不能登录，全部 refresh token 失效。
7. 重置密码后，该员工全部 refresh token 失效，并设置必须修改密码。
8. 员工分页列表必须接入数据权限。

### 13.3 超级管理员员工删除规则

系统不按用户名硬保护 `admin` 员工。

规则：

1. 初始 `admin` 员工可以删除。
2. 删除任何绑定 `SUPER_ADMIN` 角色的员工前，必须校验删除后系统中仍至少存在一个启用且未删除的 `SUPER_ADMIN` 员工。
3. 如果删除会导致系统没有可用超级管理员，必须拒绝删除。

## 14. 部门管理需求

### 14.1 部门模型

核心字段：

- 部门 ID。
- 父部门 ID。
- 部门名称。
- 部门编码。
- 排序。
- 状态：启用、禁用。
- 备注。
- 审计字段。

### 14.2 部门接口

建议接口：

- `POST /api/iam/dept/tree`
- `POST /api/iam/dept/detail`
- `POST /api/iam/dept/create`
- `POST /api/iam/dept/update`
- `POST /api/iam/dept/delete`
- `POST /api/iam/dept/status/update`

规则：

1. 部门以树形结构组织。
2. 内置根部门“总部”。
3. 同一父部门下，未删除部门名称不能重复。
4. 同一父部门下，未删除部门编码不能重复。
5. 存在子部门时，不允许删除。
6. 存在未删除员工时，不允许删除。
7. 禁用部门后，该部门不可作为新员工归属部门。
8. 数据权限计算依赖部门树。

## 15. 角色管理需求

### 15.1 角色模型

核心字段：

- 角色 ID。
- 角色编码。
- 角色名称。
- 排序。
- 状态：启用、禁用。
- 数据权限范围。
- 备注。
- 审计字段。

唯一性：

- 未删除角色的角色编码全局唯一。
- 未删除角色的角色名称全局唯一。

### 15.2 角色接口

建议接口：

- `POST /api/iam/role/page`
- `POST /api/iam/role/detail`
- `POST /api/iam/role/create`
- `POST /api/iam/role/update`
- `POST /api/iam/role/delete`
- `POST /api/iam/role/status/update`
- `POST /api/iam/role/menus/assign`
- `POST /api/iam/role/data-scope/assign`

规则：

1. 角色可绑定菜单和按钮权限。
2. 角色可配置数据权限范围。
3. 禁用角色后，该角色不再参与登录后的权限计算。
4. 删除角色前必须解除员工绑定，或由接口明确支持同步解绑。第一版建议存在员工绑定时拒绝删除。

### 15.3 内置超级管理员角色

内置角色：

- 编码：`SUPER_ADMIN`
- 名称：超级管理员

规则：

1. `SUPER_ADMIN` 角色不可删除。
2. `SUPER_ADMIN` 角色不可禁用。
3. `SUPER_ADMIN` 角色默认拥有全部菜单、按钮权限和全部数据权限。
4. `SUPER_ADMIN` 角色不允许被收窄数据权限。
5. 允许修改展示名称或备注，但不允许修改角色编码和系统保护属性。

## 16. 菜单与权限需求

### 16.1 菜单模型

菜单类型：

- `DIR`：目录。
- `MENU`：菜单。
- `BUTTON`：按钮或权限点。

核心字段：

- 菜单 ID。
- 父菜单 ID。
- 菜单名称。
- 菜单类型。
- 路由路径。
- 前端组件路径。
- 图标。
- 排序。
- 是否隐藏。
- 是否缓存。
- 状态：启用、禁用。
- 权限标识 `permissionCode`。
- 备注。
- 审计字段。

规则：

1. `DIR` 用于导航分组。
2. `MENU` 用于可访问页面。
3. `BUTTON` 用于页面内操作权限和后端接口权限。
4. `BUTTON` 必须配置 `permissionCode`。
5. `MENU` 可以配置 `permissionCode`，用于页面访问权限。
6. 禁用菜单不进入前端菜单树，也不参与权限计算。
7. 删除菜单前必须先删除子节点，或接口明确支持级联删除。第一版建议不做级联删除。

### 16.2 菜单接口

建议接口：

- `POST /api/iam/menu/tree`
- `POST /api/iam/menu/detail`
- `POST /api/iam/menu/create`
- `POST /api/iam/menu/update`
- `POST /api/iam/menu/delete`
- `POST /api/iam/menu/status/update`

## 17. RBAC 权限需求

### 17.1 权限模型

采用社区成熟 RBAC 模型：

```text
员工 -> 员工角色关系 -> 角色 -> 角色菜单关系 -> 菜单/按钮权限
```

规则：

1. 员工可以绑定多个角色。
2. 角色可以绑定多个菜单和按钮权限。
3. 员工最终权限为所有启用角色权限的并集。
4. 禁用角色不参与权限计算。
5. 禁用菜单不参与权限计算。
6. `SUPER_ADMIN` 角色绕过权限校验。
7. 第一版不支持 deny 权限。

### 17.2 后端接口权限校验

要求：

1. 后端必须支持接口级权限校验。
2. 建议提供注解，例如 `@RequiresPermission("iam:staff:create")`。
3. 权限不足时返回统一无权限错误。
4. 未登录访问受保护接口时返回统一未登录错误。
5. `SUPER_ADMIN` 角色默认通过所有权限校验。
6. 权限标识由后端定义，前端只消费和展示。

示例权限标识：

| 功能 | 权限标识 |
|------|----------|
| 员工查询 | `iam:staff:query` |
| 员工新增 | `iam:staff:create` |
| 员工编辑 | `iam:staff:update` |
| 员工删除 | `iam:staff:delete` |
| 员工重置密码 | `iam:staff:password:reset` |
| 部门管理 | `iam:dept:manage` |
| 角色管理 | `iam:role:manage` |
| 菜单管理 | `iam:menu:manage` |
| 登录日志查询 | `iam:log:login:query` |
| 操作日志查询 | `iam:log:operation:query` |

## 18. 数据权限需求

### 18.1 数据范围

角色支持以下数据权限范围：

| 数据范围 | 说明 |
|----------|------|
| `ALL` | 全部数据 |
| `DEPT_AND_CHILD` | 本部门及子部门 |
| `DEPT_ONLY` | 本部门 |
| `SELF` | 仅本人 |
| `CUSTOM_DEPT` | 自定义部门集合 |

### 18.2 多角色合并规则

多个角色叠加时，数据权限取并集。

规则：

1. 任一启用角色拥有 `ALL`，最终数据范围为 `ALL`。
2. `DEPT_AND_CHILD` 包含当前员工所在部门及所有子部门。
3. `DEPT_ONLY` 包含当前员工所在部门。
4. `CUSTOM_DEPT` 包含角色绑定的自定义部门集合。
5. `SELF` 包含当前员工本人数据。
6. 多个非 `ALL` 范围合并时，部门集合和本人条件取并集。
7. `SUPER_ADMIN` 默认为 `ALL`。

### 18.3 接入方式

第一版要求：

1. 提供 `DataScopeContext`。
2. 提供 `@DataScope` 注解，但注解作用在 AppService 或查询服务方法，不作用在 Mapper 方法。
3. 提供查询辅助组件，将当前用户数据权限转为可组合的 `LambdaQueryWrapper` 条件。
4. 对接动态查询时，数据权限条件必须通过 `MybatisPlusQueryExecutor` 的 `preApply` 参数预先写入 wrapper，再叠加前端传入的 DSL 条件。
5. 第一版不做复杂 SQL 自动拦截器，避免误伤动态查询、复杂 JOIN 和导出场景。
6. 员工分页列表必须接入数据权限，作为标准示例。
7. 后续业务模块通过统一注解和 helper 主动接入。
8. 动态查询 DSL 必须能与数据权限条件组合。

## 19. 初始化数据需求

系统内置初始化数据：

| 类型 | 内容 |
|------|------|
| 根部门 | 总部 |
| 初始员工 | `admin` |
| 初始密码 | `Admin@123456` |
| 初始角色 | `SUPER_ADMIN` |
| 初始菜单 | 系统管理、员工管理、部门管理、角色管理、菜单管理、字典管理、文件管理、导出中心 |

规则：

1. `admin` 员工默认绑定 `SUPER_ADMIN` 角色。
2. `admin` 首次登录必须修改密码。
3. 初始化数据应通过 Flyway migration 或明确的启动初始化机制创建。
4. 初始化过程必须幂等。
5. 生产环境不得依赖明文默认密码长期运行。
6. 系统必须始终至少保留一个启用且未删除的 `SUPER_ADMIN` 员工。

## 20. 登录日志需求

登录日志记录范围：

- 登录成功。
- 登录失败。
- refresh token 成功。
- refresh token 失败。
- 退出登录。

核心字段：

- 日志 ID。
- 员工 ID。
- 用户名。
- 登录结果。
- 失败原因。
- IP。
- User-Agent。
- access token ID 或会话标识摘要。
- 操作时间。

规则：

1. 密码不得记录。
2. token 不得明文记录。
3. 登录失败时，如果员工不存在，也要记录用户名和失败原因摘要。
4. 登录日志查询接口必须做权限校验。

建议接口：

- `POST /api/iam/log/login/page`
- `POST /api/iam/log/login/detail`

## 21. 操作日志需求

### 21.1 记录方式

第一版采用注解方式记录操作日志，但注解参数不得散落裸字符串。

示例：

```java
@OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.CREATE)
```

约定：

1. `module` 必须使用枚举或集中定义的常量。
2. `action` 必须使用枚举或集中定义的常量。
3. 展示文案由枚举或常量集中维护，避免接口代码中散落不可审计的字符串。
4. 业务方法执行后，由 AOP 组装操作日志事件并通过 `ApplicationEventPublisher` 发布。
5. 操作日志监听器负责异步落库，使用独立有界线程池。
6. 日志落库失败只记录应用日志，不向主业务抛出异常。
7. 第一版不要求重试队列或 DLQ，但必须保证异常被捕获并可排查。

### 21.2 记录字段

核心字段：

- 日志 ID。
- 操作人 ID。
- 操作人用户名。
- 操作人姓名。
- 模块。
- 操作动作。
- 请求路径。
- HTTP 方法。
- 请求参数摘要。
- 响应结果摘要。
- 是否成功。
- 错误信息。
- IP。
- User-Agent。
- 耗时。
- 操作时间。

### 21.3 脱敏规则

以下字段必须脱敏：

- `password`
- `oldPassword`
- `newPassword`
- `accessToken`
- `refreshToken`
- `token`
- `authorization`

### 21.4 操作日志接口

建议接口：

- `POST /api/iam/log/operation/page`
- `POST /api/iam/log/operation/detail`

规则：

1. 操作日志查询必须做权限校验。
2. 操作日志记录失败不得影响主业务流程。
3. 操作日志应记录统一响应结果，而不是吞掉异常。
4. 操作日志异步线程池必须配置核心线程数、最大线程数、队列长度和拒绝策略。

## 22. API 范围汇总

### 22.1 认证

| 接口 | 说明 |
|------|------|
| `POST /api/iam/auth/login` | 登录 |
| `POST /api/iam/auth/refresh` | 刷新 token |
| `POST /api/iam/auth/logout` | 退出登录 |
| `POST /api/iam/auth/me` | 当前用户信息 |
| `POST /api/iam/auth/password/change` | 修改本人密码 |

### 22.2 员工

| 接口 | 说明 |
|------|------|
| `POST /api/iam/staff/page` | 员工分页 |
| `POST /api/iam/staff/detail` | 员工详情 |
| `POST /api/iam/staff/create` | 新增员工 |
| `POST /api/iam/staff/update` | 编辑员工 |
| `POST /api/iam/staff/delete` | 删除员工 |
| `POST /api/iam/staff/status/update` | 启用或禁用员工 |
| `POST /api/iam/staff/password/reset` | 重置员工密码 |
| `POST /api/iam/staff/roles/assign` | 分配员工角色 |

### 22.3 部门

| 接口 | 说明 |
|------|------|
| `POST /api/iam/dept/tree` | 部门树 |
| `POST /api/iam/dept/detail` | 部门详情 |
| `POST /api/iam/dept/create` | 新增部门 |
| `POST /api/iam/dept/update` | 编辑部门 |
| `POST /api/iam/dept/delete` | 删除部门 |
| `POST /api/iam/dept/status/update` | 启用或禁用部门 |

### 22.4 角色

| 接口 | 说明 |
|------|------|
| `POST /api/iam/role/page` | 角色分页 |
| `POST /api/iam/role/detail` | 角色详情 |
| `POST /api/iam/role/create` | 新增角色 |
| `POST /api/iam/role/update` | 编辑角色 |
| `POST /api/iam/role/delete` | 删除角色 |
| `POST /api/iam/role/status/update` | 启用或禁用角色 |
| `POST /api/iam/role/menus/assign` | 分配菜单权限 |
| `POST /api/iam/role/data-scope/assign` | 分配数据权限 |

### 22.5 菜单

| 接口 | 说明 |
|------|------|
| `POST /api/iam/menu/tree` | 菜单树 |
| `POST /api/iam/menu/detail` | 菜单详情 |
| `POST /api/iam/menu/create` | 新增菜单 |
| `POST /api/iam/menu/update` | 编辑菜单 |
| `POST /api/iam/menu/delete` | 删除菜单 |
| `POST /api/iam/menu/status/update` | 启用或禁用菜单 |

### 22.6 日志

| 接口 | 说明 |
|------|------|
| `POST /api/iam/log/login/page` | 登录日志分页 |
| `POST /api/iam/log/login/detail` | 登录日志详情 |
| `POST /api/iam/log/operation/page` | 操作日志分页 |
| `POST /api/iam/log/operation/detail` | 操作日志详情 |

## 23. 概念数据模型

建议核心表：

| 表名 | 说明 |
|------|------|
| `sys_staff` | 员工 |
| `sys_dept` | 部门 |
| `sys_role` | 角色 |
| `sys_menu` | 菜单与按钮权限 |
| `sys_staff_role` | 员工角色关系 |
| `sys_role_menu` | 角色菜单关系 |
| `sys_role_data_scope_dept` | 角色自定义数据权限部门 |
| `sys_refresh_token` | refresh token |
| `sys_login_log` | 登录日志 |
| `sys_operation_log` | 操作日志 |

命名约束：

1. `main` 分支本地用户主体统一使用 `staff` 术语。
2. 后端包名、类名、DTO、接口路径和数据库表名都应使用 `staff`，不得在 IAM 新代码中混用 `user` 表示本地员工。
3. 历史 `sys_user_cache` 属于 SSO 透传用户缓存语义，不作为 `main` 分支 IAM 员工表；当前 schema 已移除该表。
4. `main` 本地 IAM 不保留 SSO 专用用户缓存代码，避免与 `sys_staff` 语义冲突。

`sys_refresh_token` 至少包含：

- token ID。
- 员工 ID。
- refresh token 哈希。
- 服务端会话 ID。
- 服务端设备 ID。
- IP。
- User-Agent。
- 签发时间。
- 过期时间。
- 最近使用时间。
- 撤销时间。
- 撤销原因。

所有业务表必须遵守仓库数据规范：

- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

逻辑删除字段统一为 `deleted`。

## 24. 错误码范围

建议 `iam` 使用独立错误码号段。

| 领域 | 建议号段 |
|------|----------|
| 认证 | `2001xxx` |
| 员工 | `2002xxx` |
| 部门 | `2003xxx` |
| 角色 | `2004xxx` |
| 菜单权限 | `2005xxx` |
| 数据权限与日志 | `2006xxx` |

必须覆盖的错误：

- 未登录。
- 无权限。
- 用户名或密码错误。
- 员工已禁用。
- 员工不存在。
- 员工已删除。
- refresh token 无效。
- refresh token 已过期。
- 密码不符合策略。
- 必须修改密码。
- 用户名重复。
- 员工工号重复。
- 部门不存在。
- 部门下存在子部门。
- 部门下存在员工。
- 角色不存在。
- 角色编码重复。
- 菜单不存在。
- 权限标识重复。
- 不能删除 `SUPER_ADMIN` 角色。
- 系统必须至少保留一个启用的超级管理员员工。

## 25. 安全要求

1. 密码只允许 BCrypt 哈希存储。
2. refresh token 只允许哈希存储。
3. access token 使用 JWT 签名，密钥必须可配置。
4. JWT 密钥不得硬编码到源码。
5. 登录、刷新、退出、改密必须记录安全相关日志。
6. 操作日志必须脱敏敏感字段。
7. 所有受保护接口必须经过认证过滤器。
8. 所有配置了权限标识的接口必须经过权限校验。
9. OpenAPI 文档不得暴露默认生产密码以外的敏感配置。
10. 登录失败必须使用统一错误提示，避免暴露用户名是否存在。
11. 登录失败必须执行最小响应延迟，默认 500ms，可配置。
12. 必须记录登录失败次数，为后续验证码、限流或账号保护策略提供依据。
13. refresh token 刷新失败必须执行与登录失败一致的最小响应延迟，默认 500ms，可配置。
14. refresh token 刷新失败必须记录安全日志。
15. stateless Bearer token 模式下不启用服务端 CSRF token；如果未来改为 Cookie session，必须重新设计 CSRF 防护。

### 25.1 401 / 403 语义

后端必须稳定区分认证失败和授权失败：

1. `401` 只表示未登录、access token 缺失/非法/过期且无法通过 refresh 恢复，或 refresh token 无效/过期/已撤销/重放。
2. `403` 表示已认证但无权限，包括 `@RequiresPermission` 不通过、员工处于必须改密状态却访问非允许接口、数据权限不允许访问目标资源。
3. 员工禁用或删除后，access token 请求应被认证过滤链拒绝；响应可以使用 `401` 并清理前端会话，也可以使用带明确错误码的 `403`。P0 推荐使用 `401`，促使前端清 session 回登录。
4. 强制改密限制使用 `403`，错误码必须能让前端跳转强制改密页，而不是清 session。
5. 普通业务无权限使用 `403`，前端不得因此登出。

## 26. 与现有能力的关系

### 26.1 动态查询

员工、角色、日志等分页查询可以复用动态查询 DSL。

要求：

- 动态查询条件必须与数据权限条件组合。
- 数据权限条件不得被前端传入条件绕过。
- 前端传入排序字段必须经过场景白名单映射。

### 26.2 导出中心

后续 IAM 列表导出应复用现有导出中心。

第一版 PRD 不强制实现 IAM 导出，但设计时不得阻断后续接入。

### 26.3 字典

展示型状态可以复用全局字典。

影响后端逻辑分支的状态仍使用 Enum，例如员工启用/禁用、菜单类型、数据权限范围。

### 26.4 文件存储

员工头像可以复用文件存储能力。

第一版员工头像字段只保存文件 ID 或访问标识，具体前端上传流程在前端 PRD 中定义。

## 27. 文档与分支清理要求

`main` 分支文档需要与本 PRD 对齐：

1. README 应描述本地 IAM 版后台基础框架。
2. README 不应将 SSO 作为 `main` 默认鉴权方式。
3. SSO 边界文档只适用于 `feature/sso`，不得误导 `main`。
4. 历史计划文档可以保留，但新增文档必须明确适用分支。

## 28. 验收标准

### 28.1 功能验收

1. 初始管理员可以登录。
2. 初始管理员首次登录被要求修改密码。
3. 可以创建部门、员工、角色、菜单和按钮权限。
4. 可以给员工分配角色。
5. 可以给角色分配菜单和按钮权限。
6. 普通员工只能访问被授权接口。
7. 无权限访问受保护接口返回统一无权限响应。
8. 未登录访问受保护接口返回统一未登录响应。
9. 禁用员工不能登录，refresh token 全部失效。
10. 删除员工不能登录，refresh token 全部失效。
11. 重置密码后，该员工必须修改密码，旧 refresh token 失效。
12. 员工分页列表受到数据权限限制。
13. `SUPER_ADMIN` 拥有全部接口权限和全部数据权限。
14. 删除 `SUPER_ADMIN` 员工时，系统必须至少保留一个启用且未删除的 `SUPER_ADMIN` 员工。
15. 登录日志记录成功和失败场景。
16. 操作日志记录被注解标记的后台操作。
17. 操作日志敏感字段已脱敏。

### 28.2 工程验收

1. 新增模块遵守调用链路：Controller -> AppService -> Domain/Service -> Infra/Mapper。
2. API 响应统一使用 `R.ok(...)` 或 `R.fail(...)`。
3. 业务异常统一使用 `BizException(ErrorCode)`。
4. Controller、ReqDTO、RspDTO 补齐 OpenAPI 注解。
5. Flyway migration 只新增，不回改历史版本。
6. ArchUnit 模块边界测试通过。
7. OpenAPI 文档包含 `iam` 相关接口。
8. Controller 层 `@WebMvcTest` 通过，覆盖认证、权限、参数校验和统一响应。
9. Service 层单元测试通过，覆盖核心业务规则、异常分支和 token 生命周期。
10. `@RequiresPermission` AOP 切面测试通过，覆盖有权限、无权限、未登录和 `SUPER_ADMIN` 绕过场景。
11. 数据权限组合测试通过，覆盖 `ALL`、`DEPT_AND_CHILD`、`DEPT_ONLY`、`SELF`、`CUSTOM_DEPT` 和多角色并集。
12. 操作日志 AOP 与异步事件监听测试通过，覆盖成功、失败和脱敏。
13. Flyway 迁移测试通过，覆盖 IAM 表结构、唯一索引、初始化数据和逻辑删除策略。
14. OpenAPI 文档测试通过，确认 `iam` 接口和分组完整。
15. 全量 `mvn -q test` 通过。

`iam` 最小 ArchUnit 规则：

1. `core` 不得依赖 `iam`。
2. `iam` 可以依赖 `core`。
3. `iam` 不得依赖已删除的 `admin-mdm` 实现包。
4. `iam` 不得依赖 `system` 的实现包。
5. `boot` 可以依赖并装配 `iam`。
6. `system` 不得反向依赖 `iam` 实现包。
7. `iam` 的 Controller 不得直接依赖 Service、Mapper 或 Entity。
8. `iam` 的 AppService 是事务边界，不得在 Controller 中声明业务事务。

建议验收命令：

```bash
mvn -q test
```

建议文档和源码 sanity check：

```bash
rg -n "X-User-Id|GatewayOperator|SSO|sso" README.md core iam boot/src/main/java
```

预期：

- `main` 的运行时代码不应依赖 SSO 或网关操作人 header。
- 历史文档中出现 SSO 允许存在，但必须标明适用 `feature/sso`。

## 29. 开放问题

以下问题实现前需要进一步确认：

1. 创建员工时，初始密码由管理员输入，还是系统生成后返回一次。
2. 菜单初始化数据是否由后端维护完整默认树，还是只提供最小系统管理菜单。
3. 操作日志请求参数摘要的最大长度和截断策略。
4. 登录日志和操作日志是否需要保留周期和清理策略。
