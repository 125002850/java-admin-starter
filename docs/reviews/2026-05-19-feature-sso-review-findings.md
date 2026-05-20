# feature/sso Review Findings

## 评审范围

- 阶段一：`d856281`（Task 2）与 `6803403`（Task 3）
- 阶段二：`7d300c2`（Task 4）、`e5fd276`（Task 5）、`0936e61`（Task 6）
- 评审方式：分别使用临时 detached worktree 复现对应提交，避免当前工作区后续提交污染结论

## 阶段一：Task 1-3

### 复现命令

```bash
git worktree add /private/tmp/java-demo-review-6803403 --detach 6803403
cd /private/tmp/java-demo-review-6803403
mvn -pl demo-boot -am test-compile -DskipTests
```

### 当前仍成立的缺陷

#### F1. `Task 1-3` 基线无法通过 `demo-boot` 测试源码编译

**严重级别：** 高

**现象：**

`6803403` 已删除 `DictTypeEntity`、`DictItemEntity` 和整个 `demo-system` 模块，但 `demo-boot` 里的 `AuditFieldMetadataTests` 仍静态引用这些已删除类，导致干净构建下 `demo-boot:testCompile` 直接失败。

**失败位置：**

- `6803403:demo-boot/src/test/java/com/demo/boot/mybatis/AuditFieldMetadataTests.java:5-9`
- `6803403:demo-boot/src/test/java/com/demo/boot/mybatis/AuditFieldMetadataTests.java:21-27`

**复现结果摘要：**

```text
[ERROR] 找不到符号
  符号:   类 DictItemEntity
  位置: 程序包 com.demo.mdm.infra.entity
[ERROR] 找不到符号
  符号:   类 DictTypeEntity
  位置: 程序包 com.demo.mdm.infra.entity
[ERROR] 程序包com.demo.system.infra.entity不存在
```

**影响：**

- `Task 1-3` 交付物本身不满足“干净 checkout 后可编译”
- 后续 reviewer 如果只在脏工作区上做增量编译，会被历史 `target/` 假绿误导

#### F2. 文档已宣告新的网关审计契约，但 `Task 1-3` 时点的代码尚未兑现

**严重级别：** 中

**现象：**

`Task 1-3` 的 README 和边界文档已经写成“`create_by/update_by` 优先从 `OperatorContext -> X-User-Id` 获取，且本仓库不再消费 `X-Tenant-Id`”，但 `6803403` 时点的运行时代码仍保留旧租户过滤和旧审计填充：

- `6803403:README.md:134-136`
- `6803403:docs/architecture/2026-05-19-gateway-sso-boundary.md:18-46`
- `6803403:demo-core/src/main/java/com/demo/core/mybatis/CommonMetaObjectHandler.java:13-33`
- `6803403:demo-core/src/main/java/com/demo/core/tenant/HeaderTenantResolver.java:15-24`
- `6803403:demo-core/src/main/java/com/demo/core/tenant/TenantFilter.java:35-53`

**具体偏差：**

- `CommonMetaObjectHandler` 仍固定写入 `0L`
- `HeaderTenantResolver` 仍解析 `X-Tenant-Id`
- `TenantFilter` 遇到非法 `X-Tenant-Id` 仍会返回 `400`

**影响：**

- 文档把 `Task 4` 的结果提前宣告成了 `Task 1-3` 的既成事实
- 阶段性交付边界不清，容易误导后续实现和验收

#### F3. OpenAPI 契约测试删除了“旧接口必须不存在”的负向断言

**严重级别：** 中

**现象：**

`6803403` 对 `OpenApiDocumentationTests` 的收缩只保留了“全局字典接口存在”的正向断言，但去掉了对以下内容的负向校验：

- `/v3/api-docs/system-auth`
- `/v3/api-docs/system-tenant`
- `/v3/api-docs/system-user`
- 旧租户字典路径 `/api/mdm/dict/**`（非 `/global/**`）

**定位位置：**

- `6803403:demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java:24-59`

**影响：**

- 一旦后续有人误把旧控制器或旧分组加回仓库，当前测试未必会报错
- 对“接口已删除”的回归保护不完整

## 阶段二：Task 4-6

### 复现命令

```bash
git worktree add /private/tmp/java-demo-review-head --detach 0936e61
cd /private/tmp/java-demo-review-head

# 局部 gate
mvn -pl demo-boot -am test -Dtest=ModuleBoundaryTests,OpenApiDocumentationTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false

# 全量 gate
mvn -q test
```

### 验证结论

- 局部 gate 通过：`ModuleBoundaryTests`、`OpenApiDocumentationTests`、`ErrorCodeContractTests`
- 全量 `mvn -q test` 失败：失败点为 `demo-core` 的 `CommonMetaObjectHandlerTests`

### 当前仍成立的缺陷

#### F4. `Task 4-6` 仍未满足计划定义的全量 `mvn -q test` 完成标准

**严重级别：** 高

**现象：**

`CommonMetaObjectHandlerTests` 基于 `MockitoExtension + @Mock MetaObject`，在当前 JDK/OS 组合上再次触发 inline Byte Buddy mock maker 自附着失败，导致全量测试中断。

**定位位置：**

- `demo-core/src/test/java/com/demo/core/mybatis/CommonMetaObjectHandlerTests.java:20-28`
- `demo-core/pom.xml:70-77`

**复现结果摘要：**

```text
Could not initialize inline Byte Buddy mock maker.
It appears as if your JDK does not supply a working agent attachment mechanism.
Could not self-attach to current VM using external process
```

**影响：**

- `Task 6` 计划要求的最终门禁是 `mvn -q test`，当前交付仍未达到
- 这和前一轮 `TenantFilterTests` 的问题是同一类可移植性回归，只是故障点换成了新加的 `CommonMetaObjectHandlerTests`

#### F5. `ModuleBoundaryTests` 没有真正覆盖计划里要求的“控制器/路径边界”契约

**严重级别：** 中

**现象：**

当前 `ModuleBoundaryTests` 只检查模块间依赖和已删除包依赖：

- 禁止依赖 `com.demo.system..`
- 禁止依赖 `com.demo.core.tenant..`
- 禁止 `core -> mdm/boot`、`mdm -> boot`

但它没有落实计划里要求的这两条关键门禁：

- 禁止存在 `/api/system/**` 控制器
- 禁止存在 `/api/mdm/dict/**` 非 `/global/**` 控制器路径

**定位位置：**

- `demo-boot/src/test/java/com/demo/boot/archunit/ModuleBoundaryTests.java:24-76`

**影响：**

- 如果后续有人重新加回 `@RequestMapping("/api/system/...")` 控制器，但不依赖已删除包，当前 ArchUnit 规则仍可能通过
- “字节码级 contract gate” 只落了一半，未覆盖对外接口边界

#### F6. `OpenApiDocumentationTests` 只有正向存在断言，没有对已删除分组/旧路径做负向回归保护

**严重级别：** 中

**现象：**

当前 OpenAPI 测试只校验全局字典路径存在，但没有断言以下内容必须不存在：

- `/v3/api-docs/system-auth`
- `/v3/api-docs/system-tenant`
- `/v3/api-docs/system-user`
- 旧租户字典路径 `/api/mdm/dict/items/by-type`
- 旧租户字典路径 `/api/mdm/dict/type/*`、`/api/mdm/dict/item/*`

**定位位置：**

- `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java:24-59`

**影响：**

- 当前局部 gate 能证明“新接口在”，但不能证明“旧接口不在”
- 与 F5 叠加后，接口回归可能漏检

