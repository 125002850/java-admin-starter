# 验证与完成标准

## 必跑验证

根目录 `README.md` 是完成标准的权威来源。每次修改后，在仓库根目录至少执行：

```bash
mvn clean test
mvn test
mvn compile
```

三条命令都成功后才能汇报完成。针对单模块或单测试类的命令可用于开发阶段快速反馈，但不能替代最终三条命令。

## 按风险追加验证

| 场景 | 追加验证 |
|---|---|
| `.agents/**`、README、docs、注释 | 做文本、路径、命令、模块名和 skill 结构自检 |
| 单模块代码 | 在三条全仓命令前，按需运行对应模块编译、测试或指定测试类 |
| 公共契约、启动装配、OpenAPI、模块依赖 | 补受影响模块及 `admin-boot` 集成测试 |
| Flyway、表结构、索引、约束 | 运行迁移拦截、Flyway 测试，并至少验证一套真实 MySQL 8 环境 |
| 跨模块收缩、删除或重命名 | 在 clean checkout 或 detached worktree 中复验 |

单独运行 `admin-boot` 测试且依赖兄弟模块最新产物时，先安装 reactor 产物：

```bash
mvn clean install -DskipTests
mvn test -pl admin-boot -Dtest=指定测试类
```

## 升级条件

出现以下任一情况时，除必跑验证外，必须追加跨模块或契约验证：

- 改了 `admin-core` 中被多个模块依赖的公共契约、SPI、上下文、基础配置。
- 改了 `OpenApiConfig`、`SpringDocOperationIdConfig`、`EnumModelConverter` 等启动装配或 OpenAPI 生成逻辑。
- 改了 Controller DTO、动态查询 schema、公共错误码、公共枚举，且可能影响其他模块或前端契约。
- 改了 Flyway migration、数据库表结构、索引、约束。
- 改了 `pom.xml`、模块依赖关系、插件配置、父子模块构建行为。
- 自己无法明确判断影响面时，按最高风险处理并补全跨模块、契约与启动层验证。

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
mvn clean test
mvn test
mvn compile
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
