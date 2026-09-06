# 已提交迁移基线语法修复记录

## 失败证据

`mvn -pl boot -am -Dtest=ModuleBoundaryTests,FlywaySmokeTests,HttpRequestLoggingFilterTests test` 在 Flyway 初始化失败。`V20260805103327__squash_initial_schema.sql` 的两个 CREATE TABLE 中保留了原 ALTER TABLE 的 7 个 AFTER 子句。首条建表语句即失败，追加更高版本 migration 无法先于它执行。

## 应用的补丁

仅去掉以下 7 个列定义末尾的 AFTER 子句，列顺序已由 CREATE TABLE 的声明顺序确定。文件名称、版本、表/列定义、种子数据不变。补丁不操作现有数据库。

```diff
--- boot/src/main/resources/db/migration/V20260805103327__squash_initial_schema.sql
+++ boot/src/main/resources/db/migration/V20260805103327__squash_initial_schema.sql
@@ sys_dict_type_global
-  remark varchar(512) null comment '备注' after dict_type_name,
-  status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用' after remark,
-  version int not null default 0 comment '乐观锁版本号' after status,
+  remark varchar(512) null comment '备注',
+  status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用',
+  version int not null default 0 comment '乐观锁版本号',
@@ sys_dict_item_global
-  remark varchar(512) null comment '备注' after dict_item_name,
-  status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用' after remark,
-  sort_order int not null default 0 comment '排序号' after status,
-  version int not null default 0 comment '乐观锁版本号' after sort_order,
+  remark varchar(512) null comment '备注',
+  status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用',
+  sort_order int not null default 0 comment '排序号',
+  version int not null default 0 comment '乐观锁版本号',
```

## 约束

仓库迁移规范禁止修改已提交的版本化 SQL。本次先把补丁作为外部测试夹具验证，再按用户确认修复并继续执行的指示纳入当前修复。

迁移保护脚本只接受该路径的修改，以及完整 Git blob 从 `b39166bf1209ba802c784aa4e722b4144f95f08f` 到 `aa2845e1365913945414328175e5d1758751770b` 的精确变更。删除、重命名、混入额外改动或修复后再次修改都不会命中例外。后续迁移继续追加，不提供通用 squash 豁免。

若环境曾登记相同版本但内容不同的成功记录，必须先单独核对历史与 checksum；本次不自动 repair、clean 或修改已有数据库历史。

## 补丁验证证据（2026-09-06）

在独立干净副本中，把上述 7 处修正后的 SQL 放入外部测试夹具目录，并通过 `-Dspring.flyway.locations=filesystem:<候选目录>` 指定；源目录中的历史 SQL 未修改。

- `mvn -B -ntp clean verify` 配合该夹具完成全量构建：core 60、iam 29、system 68、boot 119，共 276 项测试，失败、错误、跳过均为 0。
- 一次性 MySQL 8.0 数据库从空库成功应用该基线；`mvn -pl boot -am -Dtest=FlywaySmokeTests test` 配合相同夹具完成 7 项测试，失败、错误、跳过均为 0，Flyway validate 通过。
- 上述结果是应用前的候选补丁验证；应用后的实际源码验证记录见实现计划。
