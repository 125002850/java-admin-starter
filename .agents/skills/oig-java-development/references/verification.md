# 验证与完成标准

## 按影响面选择验证

使用能覆盖当前风险的最小充分验证。文件扩展名不决定验证等级，已通过的充分检查无需机械重复。

| 场景 | 适用范围 | 最低验证要求 |
|---|---|---|
| 文档改动 | `README`、`docs/**`、`.agents/**`、注释，以及不改变运行行为的格式整理 | 核对文本、路径、命令和规则一致性；不强制跑 Maven |
| 单模块实现 | 不影响启动装配、Flyway、OpenAPI 或跨模块契约 | 当前模块编译和受影响测试；使用 `-am` 纳入所需兄弟模块 |
| 运行配置 | `application*.yml`、`logback-spring.xml` 等改变认证、日志、数据源、Flyway、线程池或条件装配的配置 | 验证配置绑定、启动装配和受影响行为；涉及启动层时运行对应 `boot` 集成测试 |
| 跨模块契约 | 共享接口、DTO、权限、配置装配、模块依赖或持久化边界 | 在当前 reactor 构建受影响模块并运行单元/契约/集成测试 |
| 中大改动或影响面不清 | 跨模块重构、删除能力、准备提交较大变更 | 运行 `mvn clean test`；删除或收缩改造额外在干净 checkout 复验 |

修改认证或 HTTP 日志配置属于运行配置，不能以“YAML 是纯文本”为由只做文本检查。数据库迁移另需真实 MySQL 8 验证。

## 确保测试使用当前源码

优先从仓库根目录使用 reactor，一次纳入目标模块及依赖，例如：

```bash
mvn -pl boot -am "-Dtest=ModuleBoundaryTests,IamLayeringTests,RequestMappingRuleTests,HttpRequestLoggingFilterTests,OpenApiDocumentationTests" test
```

`-am` 使本次测试使用同次构建的兄弟模块，不要求先机械执行全仓 `clean install`。选择只运行 `boot`、未带 `-am`，或在 `boot` 单独启动时，必须确认兄弟模块的本地 jar 对应当前源码；需要更新时使用：

```bash
mvn -pl boot -am install -DskipTests
```

根据实际改动选择测试类，不把示例列表当成所有任务的固定步骤。单模块也可使用 `mvn -pl core -am test`，全量覆盖使用 `mvn clean test`。

本仓库允许没有匹配指定测试的模块继续构建。使用 `-Dtest` 后必须核对目标模块的 Surefire 报告、测试类和实际执行数量；每个指定目标都应有执行记录，`BUILD SUCCESS` 或零测试不能证明目标已验证。`core` 与 `boot` 各有 `HttpRequestLoggingFilterTests`，需要两层覆盖时分别确认报告。

## 规则与可运行验证

| 规则 | 验证入口 |
|---|---|
| 模块/分层边界、DTO 分包、Entity/Mapper/MyBatis 依赖方向 | `boot: ModuleBoundaryTests`；`iam: IamLayeringTests` |
| 禁止 HTTP 路径，规则正反例确实生效 | `boot: RequestMappingRuleTests,ModuleBoundaryTests` |
| 日志认证身份、可信代理 IP、脱敏、限长和排除 | `core/boot: HttpRequestLoggingFilterTests`；`iam: ClientRequestInfoTests` |
| IAM 认证、权限、批量名称装配及现有 JSON/schema/operationId 兼容 | `boot: IamAuthIntegrationTests,IamManagementIntegrationTests,OpenApiDocumentationTests` |
| IAM 领域与持久化转换保留乐观锁、连续更新和旧版本冲突 | `boot: IamPersistenceIntegrationTests` |
| 当前 Flyway 版本集合、迁移成功、表和种子数据契约 | `boot: FlywaySmokeTests`，另在独立 MySQL 8 数据库执行 |
| 历史迁移不可变、版本唯一递增、禁止再次 squash | `scripts/check-migrations.sh`；`bash scripts/tests/test_check_migrations.sh` |
| 持续执行构建与迁移约束 | `.github/workflows/verify.yml`，检查 PR 迁移差异并运行 Maven 与脚本回归 |

规则测试不能替代对应的行为或数据库验证；按本次改动选择相关入口。

## 数据库迁移验证

- 涉及迁移资源、基线或结构契约时，确认当前 SQL 能从空库初始化，并执行 `FlywaySmokeTests`。
- 至少在一套独立 MySQL 8 数据库验证，默认 H2 测试不能替代真实方言和初始化检查。
- 使用测试配置已有的 `JAVA_ADMIN_STARTER_DATASOURCE_URL`、`JAVA_ADMIN_STARTER_DATASOURCE_USERNAME`、`JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD` 连接该独立库，然后运行：

```bash
mvn -pl boot -am "-Dtest=FlywaySmokeTests" test
```

数据库失败先核对日志和状态，处理方式见 [数据库迁移规范](database-migrations.md)。不要对共享数据库执行初始化测试。

## 失败与结果记录

- 失败时先读日志，区分编译、断言、数据库迁移和外部环境问题，再修复。
- 记录实际命令、目标模块、测试数量、失败/跳过数量和数据库类型；指定测试未运行时不能报告通过。
- 干净 checkout 复验应包含本次最终改动，并使用同一组相关检查，避免脏工作区或增量产物掩盖问题。
- 外部服务、数据库或凭据不可用时，说明具体阻塞和已经完成的本地检查，不把未运行部分描述为通过。
