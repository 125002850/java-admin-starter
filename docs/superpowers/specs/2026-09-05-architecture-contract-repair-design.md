# 架构与工程契约修复设计

## 目标与范围

修复架构评审确认的访问日志可信来源、架构规则失效、迁移契约漂移、IAM 分层不一致和开发规范歧义。用户已在评审后确认执行修复。以当前已移除 SSO CIK 的提交为基础，不恢复兼容模块。

## 设计约束

- 保留模块化单体和现有 HTTP 路径、JSON 字段、operationId；统一使用 R 与 ErrorCode。
- 访问日志 userId 只读取已认证的请求属性；客户端 IP 通过 core 定义的 `ClientIpResolver.resolveClientIp(HttpServletRequest)` 端口读取，由 IAM 的可信代理解析实现提供。没有实现时仅使用 TCP 对端地址。现有 GatewayOperatorFilter 只校验头格式，不能据此写入认证身份属性；网关头不覆盖 JWT 身份。网关模式依赖部署侧隔离和可信入口，未来认证适配才能写入该属性。
- IAM App/Domain 不依赖 Entity、Mapper、MyBatis 或具体 Infra。Domain 使用模型、Repository/gateway 端口及实际业务规则；持久化适配和查询位于 infra/persistence。Web DTO 按 req/rsp 拆分，保持序列化与 OpenAPI 名称稳定。安全过滤器/密码与 JWT 框架适配位于 infra，纯快照数据不依赖 Web DTO。
- 已发布的 IAM 平铺分页请求作为列明的兼容例外保留；新增分页和新增可组合筛选场景使用 DSL。此次不进行前端协议迁移。
- 架构规则覆盖 IAM 及后续业务能力，针对禁止路径规则增加故意违规与合规样例，验证规则能够失败和通过。
- 迁移测试从当前资源获取版本集合并校验执行成功。当前基线首条 CREATE TABLE 语法错误通过单次、完整内容匹配的 7 处 AFTER 删除修正，文件名、版本、结构和种子数据保持原契约。删除 squash 自动放行，历史基线合并仅作为已发生的初始化历史记录，后续全部追加迁移；检查新增版本必须大于基线。CI 同时检查 PR 合并结果相对目标分支的迁移变更。
- skills 明确硬约束、默认选择和已有 API 兼容例外；写操作批量 ids 仅约束适合批量的动作；运行配置按影响面验证；每项关键约束对应可运行检查。

## 验收

1. 伪造身份/转发头不能覆盖认证身份或未受信任的 TCP 来源；可信代理请求在 HTTP 与 IAM 日志得到同一 IP。
2. IAM 分层规则覆盖真实编译后的类；权限、认证、数据范围、审计批量翻译和 OpenAPI 契约回归通过。
3. 路径限制负例被规则拒绝，普通合法路径通过。
4. 当前 SQL 基线可初始化，测试不再假设已删除的版本链；迁移保护脚本拒绝历史改动、重复/倒退版本和再次 squash。
5. 在当前 reactor 完成测试，并在干净独立 checkout 复验；使用一次性 MySQL 8 数据库验证实际初始化与 Flyway 契约，默认 H2 测试不能替代此项。失败需区分实现问题和外部环境问题。

## IAM 简化补充（2026-09-06）

用户在过度设计评审后确认收敛以下三处：

- App 直接使用现有 Spring `PasswordEncoder`，删除仅转发同名方法的 `PasswordHasher` 和 `BCryptPasswordHasher`。Domain 继续不依赖 Spring。
- 用 App 层不可变 `AuthenticationOptions` 保存刷新令牌有效天数和失败延迟毫秒数，在现有 `IamSecurityConfig` 中从 `IamProperties` 装配。删除配置 gateway/adapter，把延迟处理放回 `AuthAppService`；保留负值截断、零延迟和线程中断恢复。
- 登录、操作日志改为仅包含查询/写入所需字段的 record，删除操作日志未使用的反向映射。Entity 仍负责持久化审计与默认值；登录失败日志保留独立事务，其他可更新模型保留乐观锁版本。

保持 Repository 隔离、权限快照事务边界、HTTP/DTO/OpenAPI 契约。补充日志完整字段与认证配置的行为回归，并在无旧编译产物的副本运行完整验证。
