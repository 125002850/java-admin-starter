# 架构与工程契约修复实现计划

> **For agentic workers:** 使用 superpowers:subagent-driven-development 与 dispatching-parallel-agents；各任务只修改约定文件，主代理负责集成与最终验证。

**Goal:** 修复已确认的架构缺陷，使实现、规范和自动检查一致。

**Architecture:** 保持模块化单体，通过 Domain 端口隔离 IAM 持久化，通过 core IP 解析端口统一访问日志可信来源。保留已发布接口契约，针对现有基线和规则补真实回归测试。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、ArchUnit、JUnit、Flyway、Git/Bash。

## 任务 1：日志可信来源

- [x] 在 core 日志测试添加伪造 header、认证属性优先、无认证属性、可信代理解析端口等回归用例，先确认失败。
- [x] 创建 `core/operator/ClientIpResolver.java`，定义 `String resolveClientIp(HttpServletRequest request)`；HttpRequestLoggingFilter 注入可选解析端口，缺省仅读取 remoteAddr。
- [x] 修复 HttpRequestLoggingFilter 身份来源。现有网关过滤器只有格式校验，不提升其头为认证身份，不覆盖 JWT 属性；保持正文脱敏、限长、排除和异常响应记录。
- [x] 更新 boot 日志测试与 HTTP 日志架构文档。IAM 实现桥接由 IAM 任务完成。
- [x] 验证：`mvn -pl core -am -Dtest=HttpRequestLoggingFilterTests,GatewayOperatorFilterTests test`；集成后运行 boot 日志和 IAM IP 测试。

## 任务 2：IAM 内部分层

- [x] 增加 IAM 分层 ArchUnit 回归规则并确认原实现违反约束；此任务测试放 IAM 模块自有测试目录，主代理维护 boot 通用规则。
- [x] 将现有业务服务拆为领域服务/Repository 端口和持久化实现，模型与 Entity 分离。App 使用领域输入与结果，查询包装器只出现在持久化适配。
- [x] 将嵌套 IAM DTO 拆入 req/rsp，迁移安全与配置适配到 infra，调整相关 boot 引用；保持对外 schema 名称与字段。
- [x] ClientRequestInfoResolver 实现 core 的 ClientIpResolver 端口，`resolveClientIp(request)` 返回已有可信代理解析得到的 ip；不修改 core 日志文件。
- [x] 维持员工部门/角色批量装配，保留业务异常、事务、权限和刷新令牌语义。
- [x] 验证：`mvn -pl iam -am test`；集成后运行 `IamAuthIntegrationTests,IamManagementIntegrationTests,IamPersistenceIntegrationTests,OpenApiDocumentationTests,ModuleBoundaryTests`。持久化回归覆盖版本传递、连续更新和旧版本冲突。

## 任务 3：规则、迁移和构建验证（主代理）

- [x] 为禁止路径规则建立正反例，修复否定条件方向，并推广业务层规则覆盖范围。
- [x] 更新 FlywaySmokeTests，校验当前资源版本集合与迁移状态；基线初始化语法问题按下述单次修复处理。
- [x] 为 check-migrations.sh 添加临时 Git 仓库回归测试，覆盖历史修改/删除/重命名、全部替换、重复及倒退版本与合法追加；删除 squash 自动豁免。
- [x] 提供 CI 调用的变更范围检查入口，添加运行全量 Maven 测试和脚本测试的 workflow。
- [x] 先读每次失败日志，修复当前基线阻塞；运行当前工作区 `mvn test` 和独立副本 `mvn clean verify`，确认实际测试数量。
- [x] SQL 语法修正落地后，在一次性 MySQL 8 数据库执行 FlywaySmokeTests 和初始化验证；不使用共享/生产库，不自动 repair 或 clean 数据库历史。

## 任务 4：规范与集成

- [x] 使用 skill-creator 收敛 OIG skill/reference：规则强度一致，列明 IAM 平铺分页兼容范围，限定批量 ids，运行配置按风险验证，记录可执行约束与 CI。
- [x] README 同步当前迁移基线、IAM 目录、构建与检查入口；保持历史设计文档可追溯。
- [x] 审核整体 diff，确认 SQL 只有记录的 7 处语法修正、无接口契约漂移、无私有依赖恢复。
- [x] 在独立干净 checkout 复验完成后的代码和脚本；记录实际命令、结果与限制，不提交或发布未经验证的结果。

## SQL 初始化修复

原已提交基线在 CREATE TABLE 中含 7 个非法 AFTER 子句；追加迁移无法修复首条语句先失败的问题。最小补丁及精确内容例外见 [基线语法修复记录](../specs/2026-09-05-migration-baseline-syntax-repair.md)。

先在独立副本中通过外部 Flyway 测试夹具验证，再按用户确认修复并继续执行的指示落地。正式源码在默认 classpath 迁移位置重新通过以下验证，不依赖候选夹具或替代配置。

## 最终验证记录（2026-09-06）

- 当前工作区：`mvn -B -ntp test`，core 60、iam 29、system 68、boot 121，共 278 项，失败、错误、跳过均为 0。
- 独立干净副本：`mvn -B -ntp clean verify`，同样 278 项全部通过，构建返回 0。
- 一次性 MySQL 8.0 空库：使用测试数据源环境变量运行 `mvn -B -ntp -pl boot -am -Dtest=FlywaySmokeTests test`，7 项全部通过；实际基线初始化及 Flyway validate 成功。
- 新增的五项持久化集成测试验证员工、部门、角色、菜单和刷新令牌的连续更新与旧版本冲突，48 个 DTO 和现有 Controller 的外部契约保持一致。
- 迁移脚本：当前工作区及独立副本分别运行 `bash scripts/tests/test_check_migrations.sh`，50 项全部通过；`--all`、真实 SQL 补丁的暂存检查及提交范围检查均通过。
- 模板初始化：`python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v`，2 项全部通过。
- 规范：OIG skill 的 `quick_validate.py` 校验通过；分页兼容、配置验证和创建请求三类规则场景复核通过。Bash 语法、workflow YAML 与 `git diff --check` 通过；未向远端推送或触发远端 CI。
- 临时 MySQL 容器及其测试数据卷已移除；未对任何已有数据库执行 repair、clean 或修改迁移历史。

## 任务 5：IAM 简化（2026-09-06）

本节按用户已确认的三项简化直接执行，前述验证记录对应简化前版本。

- [x] 在 `boot/src/test/java/com/oigit/admin/boot/iam/IamAuthIntegrationTests.java` 验证登录失败后日志仍落库、审计字段自动填充、刷新令牌有效期遵循配置；在同目录 `IamManagementIntegrationTests.java` 补全两类日志分页/详情字段断言。先对原实现运行行为基线：19 项集成测试及 7 项应用测试通过。
- [x] 修改 `iam/src/main/java/com/oigit/admin/iam/app/{AuthAppService,StaffAppService}.java` 直接注入 `PasswordEncoder`；删除 `domain/gateway/PasswordHasher.java` 与 `infra/security/BCryptPasswordHasher.java`。
- [x] 新增 `iam/src/main/java/com/oigit/admin/iam/app/AuthenticationOptions.java`，修改 `infra/config/IamSecurityConfig.java`、`app/{AuthAppService,RefreshTokenAppService}.java`；删除 `domain/gateway/AuthenticationSettings.java` 和 `infra/config/IamAuthenticationSettings.java`。应用单元测试验证负值/零/自定义 TTL 及失败延迟中断语义。
- [x] 将 `iam/src/main/java/com/oigit/admin/iam/domain/model/{IamLoginLog,IamOperationLog}.java` 改为 record；精简 `infra/persistence/repository/IamPersistenceConverter.java`，更新 `app/{LoginLogAppService,LogAppService}.java` 的构造与访问方法，不扩展至其他模型。
- [x] 同步 `.agents/skills/oig-java-development/references/architecture-boundaries.md`：明确 App 可直接使用框架接口、配置与执行行为分离、只读模型按需保留字段和转换方向；核对与分层规则一致。
- [x] 运行 `mvn -B -ntp -pl boot -am "-Dtest=AuthAppServiceTests,RefreshTokenAppServiceTests,IamAuthIntegrationTests,IamManagementIntegrationTests,IamPersistenceIntegrationTests,IamLayeringTests,ModuleBoundaryTests,OpenApiDocumentationTests" test`；审核差异，并在包含最终源码的独立副本运行 `mvn -B -ntp clean verify`，确认各模块实际测试数量。此次不修改 SQL，不重复初始化 MySQL。

### 简化验证结果

- 定向验证：8 个测试类，共 80 项，失败、错误、跳过均为 0。
- 独立干净副本：`mvn -B -ntp clean verify` 返回 0；core 60、iam 35、system 68、boot 122，共 285 项，失败、错误、跳过均为 0。核对 Surefire XML 计数，并确认副本与当时工作区全部非忽略源码文件字节一致。
- 生产代码相对本节修改前的工作区净减少 432 行（新增 145、删除 577）；删除 4 个包装类，新增 1 个参数 record，两类日志模型由 379 行减至 45 行。
- 独立差异审阅未发现本次回归；旧包装类无源码引用。OIG skill 校验与按仓库换行配置执行的 `git diff --check` 通过。
- 本节是行为保持的结构简化：新增行为断言先在原实现通过，再用于精简后的回归，不把它描述为修复新发现的认证或日志行为缺陷。
