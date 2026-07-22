# 数据库迁移规范

## 版本化迁移

- Flyway 版本化迁移文件统一放在 `boot/src/main/resources/db/migration/`。
- 文件名必须匹配 `V*__*.sql`，新增迁移统一使用时间戳版本号，例如 `V20260709173500__add_xxx.sql`。
- 新增迁移不得复用已有版本号。
- 历史 `V*__*.sql` 一旦提交，禁止修改、删除、重命名文件名或内容。
- 经明确批准的模块目录整体重命名，只允许把 migration 目录原样搬迁；必须确认所有历史文件均为 `R100`，不存在内容变化或遗漏。
- 数据库结构变更必须新增版本号更高的迁移脚本。
- 当前仓库已存在 `V1` 到 `V8` 多个历史迁移脚本；后续新增迁移统一切换为时间戳版本号，避免团队协作时发生顺序号冲突。

## SQL 方言

- Flyway migration 的 SQL 方言必须以真实目标库 MySQL 8 为准。
- H2 可执行不代表 MySQL 8 兼容。
- 涉及列默认值、时间字段、`alter table` 的 DDL 时，优先使用 MySQL 8 语法。
- 修改默认值应写 `alter table ... modify column ... default ...`，不要写 H2 可过但 MySQL 8 会失败的 `alter column ... set default ...`。

## 失败处理

- migration 在本地或测试库执行失败后，Flyway 会在 `flyway_schema_history` 留下 `success = false` 记录。
- 后续启动会被 `validate` 阶段拦截。
- 修复 SQL 后，需要先 `repair` 或清理失败记录，再重新执行迁移。
- 不要在已有失败记录未处理时反复启动应用并误判新错误。

## 约束与查重

- 对逻辑删除表新增唯一约束时，必须先明确约束作用范围。
- 如果唯一索引列不包含 `deleted`，则约束整张表，包含已逻辑删除行。
- 迁移前查重必须按同样语义检查，不能只筛 `deleted = 0`。

## 本地拦截

- 仓库 `pre-commit` 通过 `scripts/check-migrations.sh` 拦截历史版本化 migration 的修改。
- 安装 Lefthook：

```bash
lefthook install
```

- 手动验证：

```bash
lefthook run pre-commit
```

## 迁移验证

- 涉及数据库迁移时，必须验证 Flyway 脚本可执行。
- 至少保证一套真实 MySQL 8 环境验证通过，不能只跑 H2 测试后提交。
- 如需单独执行迁移：

```bash
cd boot
mvn flyway:migrate
```
