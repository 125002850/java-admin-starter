# 验证与完成标准

## 默认验证

每次修改后至少执行：

```bash
mvn clean test
mvn test
mvn compile
```

## 跨模块测试

修改非启动模块后，例如 `track-bench-postloan`、`track-bench-system`、`track-bench-core`，在测试 `track-bench-boot` 前必须先执行：

```bash
mvn clean install -DskipTests
```

原因：`track-bench-boot` 的测试依赖其他模块安装到本地 Maven 仓库中的 jar。不先 install 会导致测试使用旧 jar，出现幽灵编译错误，或测试通过但实际运行不一致。

正确流程：

```bash
mvn clean install -DskipTests
mvn test -pl track-bench-boot -Dtest=要跑的测试类
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
