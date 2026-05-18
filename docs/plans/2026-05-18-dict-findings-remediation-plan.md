# 字典实现复查整改 Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 把字典模块复查出的 6 个问题落成可执行整改任务，消除当前 `500` 误报、并发重复键兜底缺失、租户来源耦合、时间字段规范不一致、分页 DTO 重复和 `R.ok((Void) null)` 可读性问题。

**Architecture:** 本计划按“先收紧行为边界，再做一致性收敛，最后处理架构演进”的顺序推进。P0 先修租户异常语义和并发唯一键翻译，保证接口不再把可预期错误打成 `500`；P1 再对齐时间字段规范、分页 DTO 和响应工厂；P2 最后把租户来源从裸 Header 读取收敛成可替换策略，为后续认证接入预留稳定扩展点。

**Tech Stack:** Java 17, Spring Boot 3.3, Spring MVC, MyBatis-Plus, Flyway, H2, MockMvc, JUnit 5

---

### Task 1: 标准化租户上下文异常语义

**Files:**
- Modify: `demo-core/src/main/java/com/demo/core/tenant/TenantFilter.java`
- Modify: `demo-core/src/main/java/com/demo/core/tenant/TenantContext.java`
- Modify: `demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java`
- Create: `demo-core/src/main/java/com/demo/core/tenant/InvalidTenantHeaderException.java`
- Create: `demo-core/src/main/java/com/demo/core/tenant/MissingTenantContextException.java`
- Modify: `demo-core/src/test/java/com/demo/core/tenant/TenantContextTests.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`

- [ ] **Step 1: 先把当前错误语义锁成失败测试**

在 `TenantContextTests.java` 先把 `requireTenantId()` 的契约改成抛出 `MissingTenantContextException`，不要再断言 `IllegalStateException`：

```java
@Test
void should_require_tenant_context_explicitly() {
    TenantContext.clear();

    assertThatThrownBy(TenantContext::requireTenantId)
        .isInstanceOf(MissingTenantContextException.class)
        .hasMessage("Missing tenant context for tenant-isolated query");
}
```

在 `DictModuleSmokeTests.java` 追加两个接口级回归用例，锁定字典接口的 header 语义：

```java
@Test
void listTypes_should_return_400_when_tenant_header_missing() throws Exception {
    mockMvc.perform(post("/api/mdm/dict/types/list")
                    .contentType(APPLICATION_JSON)
                    .content("{\"pageNo\":1,\"pageSize\":20}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("缺少X-Tenant-Id"));
}

@Test
void listTypes_should_return_400_when_tenant_header_is_not_numeric() throws Exception {
    mockMvc.perform(post("/api/mdm/dict/types/list")
                    .header("X-Tenant-Id", "tenant-a")
                    .contentType(APPLICATION_JSON)
                    .content("{\"pageNo\":1,\"pageSize\":20}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("X-Tenant-Id必须为数字"));
}
```

- [ ] **Step 2: 运行红灯，确认当前实现会把这些场景打成 `500`**

Run: `mvn -q -pl demo-core -am test -Dtest=TenantContextTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前 `TenantContext.requireTenantId()` 仍抛 `IllegalStateException`

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，缺少或非法 `X-Tenant-Id` 目前会落到 `500 / 操作失败`

- [ ] **Step 3: 写最小实现，把租户上下文错误翻译成 `400`**

新增两个明确异常，保持 `TenantFilter` 和 `TenantContext` 都只抛领域清晰的异常：

```java
public class InvalidTenantHeaderException extends RuntimeException {

    public InvalidTenantHeaderException(String tenantIdHeader) {
        super("X-Tenant-Id必须为数字");
    }
}
```

```java
public class MissingTenantContextException extends RuntimeException {

    public MissingTenantContextException() {
        super("Missing tenant context for tenant-isolated query");
    }
}
```

`TenantFilter.java` 不要直接 `Long.parseLong(...)`，改成显式转换和明确异常：

```java
String tenantIdHeader = request.getHeader(TENANT_ID_HEADER);
if (StringUtils.hasText(tenantIdHeader)) {
    try {
        TenantContext.setTenantId(Long.parseLong(tenantIdHeader));
    } catch (NumberFormatException exception) {
        throw new InvalidTenantHeaderException(tenantIdHeader);
    }
}
```

`TenantContext.java` 改成：

```java
public static Long requireTenantId() {
    Long tenantId = TENANT_HOLDER.get();
    if (tenantId == null) {
        throw new MissingTenantContextException();
    }
    return tenantId;
}
```

`GlobalExceptionHandler.java` 追加两个 handler，把 transport 错误稳定映射成 `400`：

```java
@ExceptionHandler(InvalidTenantHeaderException.class)
public ResponseEntity<R<Void>> handleInvalidTenantHeader(InvalidTenantHeaderException ex) {
    return ResponseEntity.badRequest().body(R.fail(CommonErrorCode.PARAM_ERROR, ex.getMessage()));
}

@ExceptionHandler(MissingTenantContextException.class)
public ResponseEntity<R<Void>> handleMissingTenantContext(MissingTenantContextException ex) {
    return ResponseEntity.badRequest().body(R.fail(CommonErrorCode.PARAM_ERROR, "缺少X-Tenant-Id"));
}
```

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-core -am test -Dtest=TenantContextTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，新增的缺 header / 非数字 header 用例返回 `400`

- [ ] **Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/tenant/TenantFilter.java \
  demo-core/src/main/java/com/demo/core/tenant/TenantContext.java \
  demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java \
  demo-core/src/main/java/com/demo/core/tenant/InvalidTenantHeaderException.java \
  demo-core/src/main/java/com/demo/core/tenant/MissingTenantContextException.java \
  demo-core/src/test/java/com/demo/core/tenant/TenantContextTests.java \
  demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java
git commit -m "fix: normalize tenant header error semantics"
```

### Task 2: 并发场景下统一翻译唯一索引冲突

**Files:**
- Modify: `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- Create: `demo-mdm/src/test/java/com/demo/mdm/service/DictServiceDuplicateKeyTests.java`

- [ ] **Step 1: 写失败测试，锁定 update 路径也必须翻译 `DuplicateKeyException`**

新增 `DictServiceDuplicateKeyTests.java`，用 Mockito 直测 4 条 update 路径，避免为了模拟并发硬写集成测试：

```java
@Test
void updateType_should_translate_duplicate_key_to_biz_exception() {
    TenantContext.setTenantId(100L);
    when(dictTypeMapper.selectById(1L)).thenReturn(existingTenantType());
    when(dictTypeMapper.selectCount(any())).thenReturn(0L);
    doThrow(new DuplicateKeyException("uk_mdm_dict_type_tenant_code"))
        .when(dictTypeMapper).updateById(any(DictTypeEntity.class));

    assertThatThrownBy(() -> service.updateType(1L, "gender", "性别"))
        .isInstanceOf(BizException.class)
        .hasMessage("租户字典类型编码已存在");
}
```

同一个测试类里至少覆盖下面 4 个方法：

- `updateType(...) -> DictErrorCode.DICT_TYPE_CODE_DUPLICATED`
- `updateItem(...) -> DictErrorCode.DICT_ITEM_CODE_DUPLICATED`
- `updateGlobalType(...) -> DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED`
- `updateGlobalItem(...) -> DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED`

- [ ] **Step 2: 运行红灯，确认 update 路径现在仍然会冒 `500`**

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictServiceDuplicateKeyTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前 update 方法没有 catch `DuplicateKeyException`

- [ ] **Step 3: 在 4 条 update 路径上补并发兜底**

`DictService.java` 里把下面 4 个方法都改成“先做业务前置校验，再用 `try/catch` 兜底数据库唯一键冲突”：

- `updateType(...)`
- `updateItem(...)`
- `updateGlobalType(...)`
- `updateGlobalItem(...)`

实现模式统一成这样：

```java
try {
    dictTypeMapper.updateById(entity);
} catch (DuplicateKeyException exception) {
    throw new BizException(DictErrorCode.DICT_TYPE_CODE_DUPLICATED);
}
```

同步项编码变更时的 `dictItemMapper.update(...)` / `globalDictItemMapper.update(...)` 也要放进同一个 `try/catch` 作用域里，避免“主表改成功、同步项更新因为唯一键冲突再炸 `500`”。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictServiceDuplicateKeyTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，现有字典 smoke 用例无回归

- [ ] **Step 5: Commit**

```bash
git add demo-mdm/src/main/java/com/demo/mdm/service/DictService.java \
  demo-mdm/src/test/java/com/demo/mdm/service/DictServiceDuplicateKeyTests.java
git commit -m "fix: translate duplicate key conflicts on dict updates"
```

### Task 3: 租户来源治理，收敛到可替换策略

**Files:**
- Create: `demo-core/src/main/java/com/demo/core/tenant/TenantResolver.java`
- Create: `demo-core/src/main/java/com/demo/core/tenant/HeaderTenantResolver.java`
- Modify: `demo-core/src/main/java/com/demo/core/tenant/TenantFilter.java`
- Modify: `demo-system/src/main/java/com/demo/system/controller/AuthController.java`
- Modify: `demo-system/src/main/java/com/demo/system/app/AuthAppService.java`
- Modify: `demo-system/src/main/java/com/demo/system/service/AuthService.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`
- Modify: `README.md`

- [ ] **Step 1: 先写兼容性失败测试，锁定“租户来源只允许通过 resolver 链进入上下文”**

`AuthFlowTests.java` 先加一个最小契约测试，确保认证链路之后不再允许业务代码直接 `TenantContext.setTenantId(...)`：

```java
@Test
void authenticate_should_resolve_tenant_through_resolver_boundary() {
    assertThat(AuthService.class.getDeclaredMethods())
        .extracting(Method::getName)
        .doesNotContain("setTenantId");
}
```

再给 `TenantFilter` 加一个纯单元测试类，例如 `TenantResolverTests.java`，先锁定“filter 只调用 resolver，不自己 parse header”：

```java
@Test
void filter_should_delegate_tenant_resolution_to_resolver() {
    when(tenantResolver.resolve(request)).thenReturn(Optional.of(100L));

    filter.doFilter(request, response, filterChain);

    verify(tenantResolver).resolve(request);
}
```

- [ ] **Step 2: 运行红灯，确认当前实现把 header trust 写死在 filter / service 里**

Run: `mvn -q -pl demo-core -am test -Dtest=TenantResolverTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前没有 `TenantResolver`

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，`AuthService.authenticate(...)` 仍直接操作 `TenantContext`

- [ ] **Step 3: 实现最小可替换策略，不在本任务引入完整 token 体系**

本任务只做“收口 trust boundary”，不一步到位引入 session/token。实现约束如下：

- `TenantFilter` 不再直接读 header，而是依赖 `TenantResolver`
- 默认提供 `HeaderTenantResolver` 作为兼容实现，行为与当前接口保持一致
- `AuthService` 不再手工 `TenantContext.setTenantId(...)`，改为显式查询时自己带 `tenantId` 条件
- `README.md` 追加一段迁移说明，明确 `X-Tenant-Id` 是兼容方案，后续认证接入后要把 resolver 切换到登录态来源

建议接口形态：

```java
public interface TenantResolver {

    Optional<Long> resolve(HttpServletRequest request);
}
```

```java
@Component
public class HeaderTenantResolver implements TenantResolver {

    @Override
    public Optional<Long> resolve(HttpServletRequest request) {
        String tenantIdHeader = request.getHeader(TenantFilter.TENANT_ID_HEADER);
        if (!StringUtils.hasText(tenantIdHeader)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(tenantIdHeader));
        } catch (NumberFormatException exception) {
            throw new InvalidTenantHeaderException(tenantIdHeader);
        }
    }
}
```

`TenantFilter.java` 改成：

```java
Optional<Long> tenantId = tenantResolver.resolve(request);
tenantId.ifPresent(TenantContext::setTenantId);
```

`AuthService.java` 改成普通 mapper 查询，不要再写：

```java
TenantContext.setTenantId(tenantId);
try {
    ...
} finally {
    TenantContext.clear();
}
```

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-core -am test -Dtest=TenantResolverTests,TenantContextTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，登录行为不变，但 tenant trust boundary 已经收口

- [ ] **Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/tenant/TenantResolver.java \
  demo-core/src/main/java/com/demo/core/tenant/HeaderTenantResolver.java \
  demo-core/src/main/java/com/demo/core/tenant/TenantFilter.java \
  demo-system/src/main/java/com/demo/system/controller/AuthController.java \
  demo-system/src/main/java/com/demo/system/app/AuthAppService.java \
  demo-system/src/main/java/com/demo/system/service/AuthService.java \
  demo-system/src/test/java/com/demo/system/AuthFlowTests.java \
  README.md
git commit -m "refactor: centralize tenant resolution boundary"
```

### Task 4: 对齐时间字段维护策略与 README 规范

**Files:**
- Modify: `README.md`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/CommonMetaObjectHandler.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- Modify: `demo-boot/src/main/resources/db/migration/V1__init_platform_tables.sql`
- Modify: `demo-boot/src/main/resources/db/migration/V2__add_global_dict_tables.sql`
- Modify: `demo-boot/src/main/resources/db/migration/V3__add_tenant_dict_tables.sql`
- Modify: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`

- [ ] **Step 1: 先加失败测试，锁定“时间字段由数据库维护”**

在 `FlywaySmokeTests.java` 追加一个 schema 契约测试，至少断言 `sys_user`、`sys_dict_type_global`、`mdm_dict_type` 的 `create_time` / `update_time` 存在数据库默认值：

```java
@Test
void flywayMigrationShouldAutoMaintainAuditTimestamps() {
    assertThat(columnDefaultValue("sys_user", "create_time")).containsIgnoringCase("CURRENT_TIMESTAMP");
    assertThat(columnDefaultValue("sys_user", "update_time")).containsIgnoringCase("CURRENT_TIMESTAMP");
    assertThat(columnDefaultValue("sys_dict_type_global", "create_time")).containsIgnoringCase("CURRENT_TIMESTAMP");
    assertThat(columnDefaultValue("mdm_dict_type", "update_time")).containsIgnoringCase("CURRENT_TIMESTAMP");
}
```

在 `DictModuleSmokeTests.java` 追加一个用例，锁定创建字典类型时即使应用层不主动 set 时间，也能入库成功。

- [ ] **Step 2: 运行红灯，确认当前 schema 和 service 仍依赖手工时间戳**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，migration 里的时间列没有默认值

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，去掉 service 手工时间戳后会插入失败

- [ ] **Step 3: 让 schema 成为时间字段唯一来源，并删掉 service 手工维护**

本任务选择“实现对齐 README”，不回退 README 规范。具体动作：

- `V1/V2/V3` 里的 `create_time` 改成 `datetime not null default current_timestamp`
- `update_time` 改成 `datetime not null default current_timestamp on update current_timestamp`
- `CommonMetaObjectHandler` 继续只负责 `create_by` / `update_by`，不要把时间字段偷偷塞回应用层
- `DictService.java` 删除所有 `setCreateTime(...)` / `setUpdateTime(...)` 和 wrapper 里的 `.set(...UpdateTime...)`
- `AuthFlowTests.java` 和 `DictModuleSmokeTests.java` 的建表 SQL 同步加默认值，insert 语句删除显式时间列，避免测试继续遮蔽真实 schema 行为

`DictService.java` 里的 create/update 目标形态应该类似：

```java
DictTypeEntity entity = new DictTypeEntity();
entity.setTenantId(tenantId);
entity.setDictTypeCode(dictTypeCode);
entity.setDictTypeName(dictTypeName);
entity.setDeleted(0L);
dictTypeMapper.insert(entity);
```

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add README.md \
  demo-core/src/main/java/com/demo/core/mybatis/CommonMetaObjectHandler.java \
  demo-mdm/src/main/java/com/demo/mdm/service/DictService.java \
  demo-boot/src/main/resources/db/migration/V1__init_platform_tables.sql \
  demo-boot/src/main/resources/db/migration/V2__add_global_dict_tables.sql \
  demo-boot/src/main/resources/db/migration/V3__add_tenant_dict_tables.sql \
  demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java \
  demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java \
  demo-system/src/test/java/com/demo/system/AuthFlowTests.java
git commit -m "refactor: align audit timestamp ownership with schema contract"
```

### Task 5: 复用 `PageReqDTO`，收敛分页请求模型

**Files:**
- Modify: `demo-core/src/main/java/com/demo/core/web/PageReqDTO.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeListReqDTO.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/controller/dto/GlobalDictTypeListReqDTO.java`
- Modify: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`

- [ ] **Step 1: 先写失败测试，确保 DTO 继承后行为和文档都不退化**

`DictModuleSmokeTests.java` 保留现有分页测试，再追加一个带 `keyword` 的分页请求：

```java
@Test
void listTypes_should_support_page_request_base_fields_and_keyword() throws Exception {
    mockMvc.perform(post("/api/mdm/dict/types/list")
                    .header("X-Tenant-Id", "100")
                    .contentType(APPLICATION_JSON)
                    .content("{\"pageNo\":1,\"pageSize\":20,\"keyword\":\"status\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(1));
}
```

`OpenApiDocumentationTests.java` 追加 request schema 断言，确保 `pageNo/pageSize` 仍在生成的 OpenAPI 文档里：

```java
.andExpect(content().string(containsString("\"pageNo\"")))
.andExpect(content().string(containsString("\"pageSize\"")))
.andExpect(content().string(containsString("\"keyword\"")));
```

- [ ] **Step 2: 运行红灯，确认 DTO 继承改造前后需要测试保护**

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS 或局部 FAIL；如果先只加 OpenAPI 断言，通常会 PASS，但这一步的目的是先建立保护网

Run: `mvn -q -pl demo-boot -am test -Dtest=OpenApiDocumentationTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS 或局部 FAIL；保护网先落下即可

- [ ] **Step 3: 用继承消掉重复字段**

`PageReqDTO.java` 保持当前 `pageNo/pageSize` 定义不动。

`DictTypeListReqDTO.java` 改成：

```java
@Schema(description = "租户字典列表查询请求")
public class DictTypeListReqDTO extends PageReqDTO {

    @Schema(description = "关键字，按字典类型编码或名称模糊查询", example = "status")
    private String keyword;
}
```

`GlobalDictTypeListReqDTO.java` 同样只保留 `keyword`，继承 `PageReqDTO`。

如果 springdoc 丢失继承字段，就只允许补 schema 注解，不允许把 `pageNo/pageSize` 再复制回子类。

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-boot -am test -Dtest=OpenApiDocumentationTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，OpenAPI 仍能看到分页字段

- [ ] **Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/web/PageReqDTO.java \
  demo-mdm/src/main/java/com/demo/mdm/controller/dto/DictTypeListReqDTO.java \
  demo-mdm/src/main/java/com/demo/mdm/controller/dto/GlobalDictTypeListReqDTO.java \
  demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java \
  demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java
git commit -m "refactor: reuse base page request dto"
```

### Task 6: 为 `R` 增加无参 `ok()`，替换 `R.ok((Void) null)`

**Files:**
- Modify: `demo-core/src/main/java/com/demo/core/web/R.java`
- Modify: `demo-core/src/test/java/com/demo/core/web/RTests.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/controller/TenantDictController.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/controller/GlobalDictController.java`

- [ ] **Step 1: 先写失败测试，锁定无参成功响应工厂**

在 `RTests.java` 增加：

```java
@Test
void ok_without_data_should_wrap_success_and_null_payload() {
    R<Void> result = R.ok();

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getMsg()).isEqualTo("ok");
    assertThat(result.getData()).isNull();
}
```

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl demo-core -am test -Dtest=RTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前没有 `R.ok()`

- [ ] **Step 3: 实现无参工厂并清理 controller**

`R.java` 增加：

```java
public static R<Void> ok() {
    return new R<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMsg(), null);
}
```

把两个 controller 里的所有：

```java
return R.ok((Void) null);
```

统一替换成：

```java
return R.ok();
```

替换完成后，跑一遍：

Run: `rg -n "R\\.ok\\(\\(Void\\) null\\)" demo-mdm demo-system demo-core`

Expected: no matches

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl demo-core -am test -Dtest=RTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/web/R.java \
  demo-core/src/test/java/com/demo/core/web/RTests.java \
  demo-mdm/src/main/java/com/demo/mdm/controller/TenantDictController.java \
  demo-mdm/src/main/java/com/demo/mdm/controller/GlobalDictController.java
git commit -m "refactor: add no-arg success response factory"
```

### Final Verification

**Files:**
- Verify only, no new code

- [ ] **Step 1: 跑本次整改的最小回归集**

Run: `mvn -q -pl demo-core -am test -Dtest=TenantContextTests,RTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests,DictServiceDuplicateKeyTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests,OpenApiDocumentationTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

- [ ] **Step 2: 检查工作区只包含本计划预期改动**

Run: `git status --short`

Expected: 只看到本次整改涉及的 `demo-core` / `demo-system` / `demo-mdm` / `demo-boot` / `README.md`

- [ ] **Step 3: 汇总风险**

实现完成后，如果 Task 3 仍只做到 `HeaderTenantResolver` 兼容模式，要在交付说明里明确写出：

- 当前只是把 trust boundary 收口，不等于已经完成登录态租户绑定
- 真正禁用裸 `X-Tenant-Id` 需要等认证令牌方案落地后再切换 resolver
- 任何把 `update_time` 继续手工 set 回应用层的提交都应视为回归
