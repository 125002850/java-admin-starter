# 验证与完成标准

## 验证目标

- 验证应按改动范围选择“最小充分验证”。
- 不要机械执行重复三连跑，也不要把所有改动都提升到全仓全量测试。
- 目标是用尽量少的命令覆盖当前改动的真实风险面。

## 决策表

| 场景 | 适用范围 | 最低验证要求 |
|---|---|---|
| 文档改动 | `README`、`docs/**`、`.agents/**`、注释、纯文本配置 | 文本级自检，不强制跑 Maven |
| 单模块改动 | 只改一个模块，且不影响启动装配、Flyway、OpenAPI、跨模块契约 | 跑对应模块编译 + 对应模块测试或指定测试类 |
| 跨模块改动 | 改了共享契约、公共模块，或影响多个模块联动 | 先 `mvn clean install -DskipTests`，再跑受影响模块测试；如影响启动层，再跑 `admin-boot` 集成测试 |
| 提交前 | 准备提交中大改动，或自己不能确定影响面 | 跑 `mvn test`；涉及迁移再补 Flyway / 真实 MySQL 验证 |

## 对应命令

### 文档改动

- 做文本级自检，确认内容、路径、命令、模块名与当前仓库一致。
- 不强制跑 Maven。

### 单模块改动

示例：

```bash
mvn compile -pl admin-core
mvn test -pl admin-core -Dtest=指定测试类
```

### 跨模块改动

修改非启动模块后，例如 `admin-core`、`admin-iam`、`admin-system`，在单独测试 `admin-boot` 前必须先执行：

```bash
mvn clean install -DskipTests
mvn test -pl admin-boot -Dtest=要跑的测试类
```

原因：`admin-boot` 的测试依赖其他兄弟模块安装到本地 Maven 仓库中的 jar。不先 install 会导致测试使用旧 jar，出现幽灵编译错误，或测试通过但实际运行不一致。

### 提交前

示例：

```bash
mvn test
```

## 升级条件

出现以下任一情况时，不再按“单模块改动”处理，至少升级到“跨模块改动”：

- 改了 `admin-core` 中被多个模块依赖的公共契约、SPI、上下文、基础配置。
- 改了 `OpenApiConfig`、`SpringDocOperationIdConfig`、`EnumModelConverter` 等启动装配或 OpenAPI 生成逻辑。
- 改了 Controller DTO、动态查询 schema、公共错误码、公共枚举，且可能影响其他模块或前端契约。
- 改了 Flyway migration、数据库表结构、索引、约束。
- 改了 `pom.xml`、模块依赖关系、插件配置、父子模块构建行为。
- 自己无法明确判断影响面时，直接升级到“提交前”档。

## 常见命令模板

```bash
# 只验证单模块编译
mvn compile -pl admin-core

# 只验证单模块测试
mvn test -pl admin-system -Dtest=指定测试类

# 非启动模块改动后验证启动层集成测试
mvn clean install -DskipTests
mvn test -pl admin-boot -Dtest=指定测试类

# 需要提交跨模块较大改动时
mvn test
```

## 数据库变更验证

- 改动涉及数据库迁移时，必须验证 Flyway 脚本可执行。
- 至少保证一套真实 MySQL 8 环境通过。
- 不要只依赖 H2 测试。

## 收缩或删除性改造

跨模块收缩或删除性改造，必须额外在 clean checkout 或 detached worktree 中复验，避免被脏工作区或增量编译结果误导。

## 失败处理

- 命令失败时先读完整错误日志。
- 先判断失败类型：编译错误、测试断言失败、环境依赖失败、数据库迁移失败、网络/凭据失败。
- 只有定位根因后再修复。
- 如果是外部环境或凭据阻塞，汇报中必须明确阻塞条件和已完成的本地验证。

## 汇报要求

- 说明实际运行过的命令。
- 说明失败命令的根因和当前状态。
- 不要把未运行的验证描述为通过。
