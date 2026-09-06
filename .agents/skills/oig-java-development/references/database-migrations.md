# 数据库迁移规范

## 版本化迁移

- Flyway 版本化迁移文件统一放在 `boot/src/main/resources/db/migration/`。
- 文件名必须匹配 `V*__*.sql`，新增迁移使用下述命令生成的 14 位时间戳版本号。
- 新增版本必须高于仓库当前最大 migration 版本；不得重复、倒退或插入历史版本。
- 历史 `V*__*.sql` 一旦提交，禁止修改、删除、重命名文件名或内容。
- 经明确批准的模块目录整体重命名，只允许把 migration 目录原样搬迁；必须确认所有历史文件均为 `R100`，不存在内容变化或遗漏。
- 数据库结构变更必须新增版本号更高的迁移脚本。

当前初始化基线为 `V20260805103327__squash_initial_schema.sql`，历史版本链已在该文件中合并。这只是已经发生的初始化历史，不提供再次 squash、删除历史或替换基线的通用例外。后续结构和种子数据变更均追加迁移，不能只重写测试中的版本断言来掩盖迁移资源缺失。

2026-09-06 架构修复包含该基线的已验证语法修正：仅删除两个 CREATE TABLE 中的 7 个非法 AFTER 子句。迁移保护仅允许此文件从 Git blob `b39166bf1209ba802c784aa4e722b4144f95f08f` 变为 `aa2845e1365913945414328175e5d1758751770b`；路径、任一内容或变更类型不匹配均拒绝。此项不能用于后续历史修改或再次 squash，详见 `docs/superpowers/specs/2026-09-05-migration-baseline-syntax-repair.md`。

### 版本号生成

- AI 或开发者创建 migration 前，必须在仓库根目录即时执行 `TZ=Asia/Shanghai date '+%Y%m%d%H%M%S'`，并原样使用命令输出作为版本号。
- 禁止根据任务计划时间、整点或半点手工构造版本号；禁止截断真实的分钟、秒，或用 `00`、`0000` 补齐。命令自然生成的零值不受此限制。
- 创建文件前必须重新读取仓库当前最大 migration 版本并确认新版本更高；同一秒需要多个 migration 时，等待下一秒后重新执行命令，不得复用或手工递增时间戳。

## SQL 方言

- Flyway migration 的 SQL 方言必须以真实目标库 MySQL 8 为准。
- H2 可执行不代表 MySQL 8 兼容。
- 涉及列默认值、时间字段、`alter table` 的 DDL 时，优先使用 MySQL 8 语法。
- 修改默认值应写 `alter table ... modify column ... default ...`，不要写 H2 可过但 MySQL 8 会失败的 `alter column ... set default ...`。

## 失败处理

- 先读取 Flyway 错误和数据库当前状态，核对是否存在失败历史及已经执行的 DDL；再次启动可能在 `validate` 阶段被拦截。
- 已提交 SQL 仍受历史保护约束。区分脚本语法、数据库状态和 checksum 问题，不将 `repair`、清空数据库或修改历史记录当作固定修复步骤。
- 不要在失败原因和数据库状态未核对时反复启动应用并误判新错误。

## 约束与查重

- 对逻辑删除表新增唯一约束时，必须先明确约束作用范围。
- 如果唯一索引列不包含 `deleted`，则约束整张表，包含已逻辑删除行。
- 迁移前查重必须按同样语义检查，不能只筛 `deleted = 0`。

## 自动检查

- `scripts/check-migrations.sh` 默认检查暂存变更，由 `pre-commit` 调用；拒绝历史修改/删除/重命名、重复和倒退版本，以及再次 squash。
- `--all` 检查当前全树的迁移命名、版本重复和禁止的 Java migration；它不替代历史差异检查。
- `--base <commit> [--head <commit>]` 检查已提交范围，head 默认 `HEAD`；CI 用它检查 PR 相对基线的迁移变更。无法解析的 ref 必须报错，不能当作没有变更放行。
- 脚本回归由 `scripts/tests/test_check_migrations.sh` 使用临时 Git 仓库验证；CI 入口为 `.github/workflows/verify.yml`。
- 安装 Lefthook：

```bash
lefthook install
```

- 手动验证：

```bash
lefthook run pre-commit
bash scripts/check-migrations.sh --all
```

## 迁移验证

- 涉及数据库迁移时，必须验证 Flyway 脚本可执行。
- `FlywaySmokeTests` 核对当前迁移资源的版本集合、执行状态和结构/种子数据契约，不再假设已删除的历史版本链。
- 至少在一套独立 MySQL 8 数据库完成初始化验证；默认 H2 测试不能替代此项。具体命令和结果要求见 [验证与完成标准](verification.md)。
- 如需单独执行迁移：

```bash
cd boot
mvn flyway:migrate
```
