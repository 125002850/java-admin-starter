# 租户与用户管理功能 Review Findings

## 评审范围

- 评审基线：`05f3d0f` (`docs: expose tenant and user management apis`)
- 评审方式：基于 `git archive HEAD` 导出的只读快照复现，避免工作区未提交内容污染结论

## 已修复的前序问题

以下问题在当前 `HEAD` 上已不再复现，因此不再作为当前缺陷记录：

- `OpenApiConfig` 缺少 `system-tenant` / `system-user` 分组
- `AuthService` 未拦截禁用用户登录
- `UserController` / `UserService` 与生命周期测试不同步

对应定向验证命令在当前 `HEAD` 上均通过：

```bash
mvn -q -pl demo-system -am test -Dtest=AuthFlowTests,UserManagementTests,TenantManagementTests -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests,OpenApiDocumentationTests -Dsurefire.failIfNoSpecifiedTests=false
```

## 当前仍成立的缺陷

### F1. 仓库无法满足 README 声明的全量 `mvn test` 完成标准

**严重级别：** 中

**现象：**

在当前机器环境上执行仓库级 `mvn -q test` 失败，失败点不是租户/用户业务逻辑，而是 `demo-core` 的 `TenantFilterTests` 依赖 Mockito mock maker 自附着能力；当前 JDK/OS 组合下无法完成初始化。

**复现命令：**

```bash
mvn -q test
```

**复现结果摘要：**

- `demo-core` 模块失败
- 失败用例：
  - `com.demo.core.tenant.TenantFilterTests.should_delegate_tenant_resolution_and_clear_context_after_request`
  - `com.demo.core.tenant.TenantFilterTests.should_serialize_error_response_when_invalid_tenant_message_contains_quotes`
- 关键错误：

```text
Could not initialize inline Byte Buddy mock maker.
It appears as if your JDK does not supply a working agent attachment mechanism.
Could not self-attach to current VM using external process
```

**影响：**

- 与 [README.md](/Users/youdingte/studys/java-demo/README.md:219) 中“每次修改后至少执行 `mvn test`”的完成标准冲突
- 当前功能即使定向测试全部通过，也无法在本机完成仓库级完整验证

**定位线索：**

- [TenantFilterTests.java](/Users/youdingte/studys/java-demo/demo-core/src/test/java/com/demo/core/tenant/TenantFilterTests.java:27)
- [TenantFilterTests.java](/Users/youdingte/studys/java-demo/demo-core/src/test/java/com/demo/core/tenant/TenantFilterTests.java:44)

**备注：**

这条是测试基础设施/可移植性问题，不是本次租户与用户管理业务逻辑的直接功能错误，但它确实阻断了仓库声明的验收流程。

### F2. 租户删除与用户创建之间缺少并发一致性保护，可能写入“已删除租户下的有效用户”

**严重级别：** 高

**现象：**

当前删除租户和创建用户是两个独立事务，但二者之间没有任何行级锁、外键约束或删除后二次校验：

- `TenantService.delete()` 先检查租户存在，再统计当前未删除用户数，最后把租户标记为 `deleted = 1`
- `UserService.create()` 只在插入前做一次 `tenantService.exists(tenantId)` 普通存在性查询，然后直接插入用户

如果两个事务并发交错，可能出现以下顺序：

1. 创建用户事务读取到租户“存在”
2. 删除租户事务统计用户数为 `0`，然后把租户逻辑删除
3. 创建用户事务继续提交，插入一个 `deleted = 0` 的新用户

这样会落出“租户已删除，但租户下仍有有效用户”的脏状态。

**影响：**

- 破坏“存在未删除用户时拒绝删除”的业务不变量
- 后续认证链路不会再次校验租户是否仍有效，`AuthService.authenticate()` 只按 `tenant_id + username` 查 `sys_user`，因此这类脏数据一旦出现，登录仍可能成功
- 用户管理链路同样只按 `sys_user` 查租户内用户，不会额外阻断已删除租户下的孤儿账号

**定位线索：**

- [TenantService.java](/Users/youdingte/studys/java-demo/demo-system/src/main/java/com/demo/system/service/TenantService.java:45)
- [UserService.java](/Users/youdingte/studys/java-demo/demo-system/src/main/java/com/demo/system/service/UserService.java:28)
- [AuthService.java](/Users/youdingte/studys/java-demo/demo-system/src/main/java/com/demo/system/service/AuthService.java:24)

### F3. `V5` migration 的存量查重提示与实际唯一约束语义不一致

**严重级别：** 中

**现象：**

`V5__add_tenant_user_management.sql` 开头给出的存量查重 SQL 只检查 `deleted = 0` 的重复数据，但后面真正加上的唯一约束：

- `uk_sys_tenant_global_name unique (tenant_name)`
- `uk_sys_user_tenant_username unique (tenant_id, username)`

都会约束整张表，不会忽略 `deleted = 1` 的历史行。

这意味着如果库里只有“逻辑删除行之间的重名/重用户名”重复，按脚本注释执行预检查会误判为“可迁移”，但实际 `alter table ... add constraint unique ...` 仍会直接失败。

**影响：**

- 脏数据清理提示不完整，容易让执行人误以为已经满足迁移前置条件
- 真实 MySQL 8 上可能在 Flyway 执行 `V5` 时中断，留下失败记录，阻断后续启动

**定位线索：**

- [V5__add_tenant_user_management.sql](/Users/youdingte/studys/java-demo/demo-boot/src/main/resources/db/migration/V5__add_tenant_user_management.sql:1)
- [V5__add_tenant_user_management.sql](/Users/youdingte/studys/java-demo/demo-boot/src/main/resources/db/migration/V5__add_tenant_user_management.sql:14)
- [V5__add_tenant_user_management.sql](/Users/youdingte/studys/java-demo/demo-boot/src/main/resources/db/migration/V5__add_tenant_user_management.sql:29)
