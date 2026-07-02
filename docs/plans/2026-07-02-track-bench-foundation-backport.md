# track-bench 基础能力反写计划

**Goal:** 以当前 `feature/sso` 分支为目标，只补齐 `~/work/track-bench` 中已经沉淀出来的基础能力，删除/排除 track-bench 业务模块，形成基于 SSO 的可运行 Java 后端基座。

**Architecture:** 保持当前项目模块边界：`demo-core` 放业务无关抽象，`demo-system` 放外部系统集成，`demo-mdm` 放字典和导出中心这类平台型业务能力，`demo-boot` 只做启动装配和配置。代码从 `com.trackbench.*` 迁移为 `com.demo.*`，API 契约采用 track-bench 当前风格，同时继续满足 `R.ok(...)` / `R.fail(...)` 和 `BizException(ErrorCode)`。

**Tech Stack:** JDK 17, Spring Boot 3.3.0, MyBatis-Plus 3.5.7, Flyway 10.15.0, MySQL 8, Knife4j/OpenAPI 3, local/qiniu/minio file provider, optional `oigit-appcik` SSO staff client.

---

## Scope

迁移：

- 动态查询 DSL：支撑字典分页、导出记录分页和 OpenAPI 条件树 schema。
- 字典能力升级：状态、备注、排序、动态查询、全量列表、字典项分页。
- 导出中心：导出 SPI、CSV renderer、导出记录生命周期、提交/查询/下载/批量下载/删除接口。
- 文件能力增强：字节数组上传、对象下载、批量临时 URL、导出文件落盘网关。
- SSO 基座增强：操作人 header 扩展、`sys_user_cache`、审计 ID 显示名序列化、可选员工查询 `/api/staff/list-all`。
- OpenAPI 风格：稳定 `operationId`、`BaseEnum` 枚举 schema、动态查询 schema 修正。

不迁移：

- `track-bench-postloan/**`
- `com.trackbench.postloan.*`
- 所有 `tb_track_*`、客户工作台、订单、库存、贷后跟踪、附件业务表和代码。
- track-bench 中与贷后业务相关的 migration：`V4`、`V5`、`V7`、`V12` 到 `V34`、`V38`、`V39` 的业务部分。
- 贷后业务字典：`TRACK_RECORD_STATUS`、`TRACK_ISSUE_STATUS`、`TRACK_DIMENSION`、`TRACK_RECORD_OPERATION_TYPE`、`CUSTOMER_*`。

业务模块删除判定标准：

- 以 Maven module 和 Java package 为第一判断：具体业务 module/package 不迁入当前项目。
- 以数据库表名前缀为第二判断：`tb_track_` 和客户/订单/库存人工数据表不迁入。
- 以 API 路径为第三判断：`/api/postloan/**` 不迁入。
- `track-bench-system/src/main/java/com/trackbench/staff/**` 不按业务模块处理，它是 SSO 外部员工查询集成，作为可选基础能力迁入。

---

## Database Recommendation

推荐方案：**在同一 MySQL 服务器上新建独立数据库，并只追加 Flyway migration，不修改历史 `V1` 到 `V7`。**

理由：

- 新库可以完整验证从空库执行到目标 schema，避免旧库的失败 migration 或脏数据影响判断。
- 当前仓库已有 `scripts/check-migrations.sh` 保护历史 migration，追加 `V8__backport_track_bench_foundation.sql` 符合仓库约束。
- 当前项目已有 `V6__remove_tenant_auth_tables.sql` 清掉本地用户/租户表，新库跑完历史脚本后不会残留本地认证表。

推荐库名：

- `basic_platform_sso`

建库命令：

```bash
mysql --protocol=tcp -h 192.168.186.154 -P 32425 -u oig -p \
  -e "create database if not exists basic_platform_sso character set utf8mb4 collate utf8mb4_general_ci;"
```

配置原则：

- 不把数据库密码写入仓库。
- `demo-boot/src/main/resources/application-dev.yml` 改为支持环境变量覆盖：
  - `JAVA_DEMO_DATASOURCE_URL`
  - `JAVA_DEMO_DATASOURCE_USERNAME`
  - `JAVA_DEMO_DATASOURCE_PASSWORD`
- 本地 Docker MySQL 仍可作为 fallback，远程新库通过环境变量启用。

---

## File Structure

### Create

- `demo-core/src/main/java/com/demo/core/enums/BaseEnum.java`
- `demo-core/src/main/java/com/demo/core/enums/EnableStatusEnum.java`
- `demo-core/src/main/java/com/demo/core/enums/YesNoEnum.java`
- `demo-core/src/main/java/com/demo/core/web/EnumVO.java`
- `demo-core/src/main/java/com/demo/core/web/AuditRspDTO.java`
- `demo-core/src/main/java/com/demo/core/dict/DictItemNameResolver.java`
- `demo-core/src/main/java/com/demo/core/operator/CacheUserEntity.java`
- `demo-core/src/main/java/com/demo/core/operator/CacheUserMapper.java`
- `demo-core/src/main/java/com/demo/core/operator/CacheUserService.java`
- `demo-core/src/main/java/com/demo/core/operator/CacheUserHolder.java`
- `demo-core/src/main/java/com/demo/core/operator/CacheUserProperties.java`
- `demo-core/src/main/java/com/demo/core/operator/CacheUserAsyncConfiguration.java`
- `demo-core/src/main/java/com/demo/core/jackson/AuditUserIdSerializer.java`
- `demo-core/src/main/java/com/demo/core/query/ast/ConditionAstNode.java`
- `demo-core/src/main/java/com/demo/core/query/ast/ConditionGroupAst.java`
- `demo-core/src/main/java/com/demo/core/query/ast/ConditionLeafAst.java`
- `demo-core/src/main/java/com/demo/core/query/ast/QueryAst.java`
- `demo-core/src/main/java/com/demo/core/query/ast/QueryLogicOperator.java`
- `demo-core/src/main/java/com/demo/core/query/ast/QueryOperator.java`
- `demo-core/src/main/java/com/demo/core/query/ast/SortSpec.java`
- `demo-core/src/main/java/com/demo/core/query/dto/AbstractConditionNodeDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/BaseDynamicCriteriaReqDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/BasePagedDynamicQueryReqDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/ConditionGroupDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/DateTimeConditionDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/EnumConditionDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/SortItemDTO.java`
- `demo-core/src/main/java/com/demo/core/query/dto/TextConditionDTO.java`
- `demo-core/src/main/java/com/demo/core/query/exception/DynamicQueryErrorCode.java`
- `demo-core/src/main/java/com/demo/core/query/executor/MybatisPlusQueryExecutor.java`
- `demo-core/src/main/java/com/demo/core/query/scene/DynamicQueryAstMapper.java`
- `demo-core/src/main/java/com/demo/core/query/scene/SceneQueryDefinition.java`
- `demo-core/src/main/java/com/demo/core/query/scene/SceneQueryMapper.java`
- `demo-core/src/main/java/com/demo/core/query/support/DynamicQueryGuard.java`
- `demo-core/src/main/java/com/demo/core/query/support/DynamicQuerySummaryRenderer.java`
- `demo-core/src/main/java/com/demo/core/query/support/QueryComplexityScorer.java`
- `demo-core/src/main/java/com/demo/core/query/validation/DynamicQueryLimits.java`
- `demo-core/src/main/java/com/demo/core/export/dto/BaseExportDynamicCriteriaReqDTO.java`
- `demo-core/src/main/java/com/demo/core/export/dto/ExportOptionsReqDTO.java`
- `demo-core/src/main/java/com/demo/core/export/dto/ExportRangeReqDTO.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportColumn.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportMeta.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportRenderRequest.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportScope.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportStoreRequest.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportStoredFile.java`
- `demo-core/src/main/java/com/demo/core/export/model/ExportTaskResult.java`
- `demo-core/src/main/java/com/demo/core/export/model/RenderedExportFile.java`
- `demo-core/src/main/java/com/demo/core/export/renderer/CsvExportRenderer.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportFileAccessor.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportFileSink.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportHandler.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportRenderer.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportRendererRegistry.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportSceneRegistry.java`
- `demo-core/src/main/java/com/demo/core/export/spi/ExportTaskSubmitter.java`
- `demo-core/src/main/java/com/demo/core/export/spi/PackageableExportHandler.java`
- `demo-core/src/main/java/com/demo/core/export/support/AbstractCsvListExportHandler.java`
- `demo-core/src/main/java/com/demo/core/export/support/SpringExportRendererRegistry.java`
- `demo-core/src/main/java/com/demo/core/export/support/SpringExportSceneRegistry.java`
- `demo-system/src/main/java/com/demo/file/export/FileStorageExportGateway.java`
- `demo-system/src/main/java/com/demo/file/controller/dto/FetchTempUrlBatchReqDTO.java`
- `demo-system/src/main/java/com/demo/file/controller/dto/FetchTempUrlBatchRspDTO.java`
- `demo-system/src/main/java/com/demo/file/controller/dto/FetchTempUrlItemRspDTO.java`
- `demo-system/src/main/java/com/demo/staff/controller/StaffController.java`
- `demo-system/src/main/java/com/demo/staff/app/StaffAppService.java`
- `demo-system/src/main/java/com/demo/staff/config/StaffCiConfiguration.java`
- `demo-system/src/main/java/com/demo/staff/controller/dto/StaffInfoRspDTO.java`
- `demo-system/src/main/java/com/demo/staff/controller/dto/query/StaffListAllReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/controller/dto/query/GlobalDictItemDynamicCriteriaReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/controller/dto/query/GlobalDictItemDynamicPageReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/controller/dto/query/GlobalDictTypeDynamicCriteriaReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/controller/dto/query/GlobalDictTypeDynamicListReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/query/globaldict/GlobalDictItemSceneQueryDefinition.java`
- `demo-mdm/src/main/java/com/demo/mdm/query/globaldict/GlobalDictItemSceneQueryMapper.java`
- `demo-mdm/src/main/java/com/demo/mdm/query/globaldict/GlobalDictTypeSceneQueryDefinition.java`
- `demo-mdm/src/main/java/com/demo/mdm/query/globaldict/GlobalDictTypeSceneQueryMapper.java`
- `demo-mdm/src/main/java/com/demo/mdm/service/GlobalDictItemNameResolver.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/app/ExportCenterAppService.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/ExportCenterController.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportBatchDownloadReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportBatchDownloadRspDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportDownloadRspDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportRecordDeleteReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportRecordIdReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportRecordRspDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportSubmitReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/ExportSubmitRspDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/query/ExportRecordDynamicCriteriaReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/query/ExportRecordDynamicPageReqDTO.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/handler/GlobalDictTypeListExportHandler.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/query/ExportRecordSceneQueryDefinition.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/query/ExportRecordSceneQueryMapper.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/service/ExportBatchDownloadService.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/service/ExportDownloadService.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/service/ExportExecutionService.java`
- `demo-boot/src/main/java/com/demo/boot/config/SpringDocOperationIdConfig.java`
- `demo-boot/src/main/java/com/demo/boot/config/EnumModelConverter.java`
- `demo-boot/src/main/resources/db/migration/V8__backport_track_bench_foundation.sql`

### Modify

- `pom.xml`
- `demo-core/pom.xml`
- `demo-system/pom.xml`
- `demo-boot/pom.xml`
- `demo-core/src/main/java/com/demo/core/operator/OperatorContext.java`
- `demo-core/src/main/java/com/demo/core/operator/GatewayOperatorFilter.java`
- `demo-core/src/main/java/com/demo/core/jackson/JacksonConfig.java`
- `demo-core/src/main/java/com/demo/core/mybatis/MybatisPlusConfig.java`
- `demo-core/src/main/java/com/demo/core/mybatis/BaseEntity.java`
- `demo-system/src/main/java/com/demo/file/service/FileService.java`
- `demo-system/src/main/java/com/demo/file/app/FileAppService.java`
- `demo-system/src/main/java/com/demo/file/controller/FileStorageController.java`
- `demo-system/src/main/java/com/demo/file/enums/FileErrorCode.java`
- `demo-system/src/main/java/com/demo/file/infra/provider/FileStorageProvider.java`
- `demo-system/src/main/java/com/demo/file/infra/provider/local/LocalFileStorageProvider.java`
- `demo-system/src/main/java/com/demo/file/infra/provider/qiniu/QiniuFileStorageProvider.java`
- `demo-system/src/main/java/com/demo/file/infra/provider/minio/MinioFileStorageProvider.java`
- `demo-mdm/src/main/java/com/demo/mdm/controller/GlobalDictController.java`
- `demo-mdm/src/main/java/com/demo/mdm/app/DictAppService.java`
- `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- `demo-mdm/src/main/java/com/demo/mdm/infra/entity/GlobalDictTypeEntity.java`
- `demo-mdm/src/main/java/com/demo/mdm/infra/entity/GlobalDictItemEntity.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/service/ExportRecordService.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/infra/entity/ExportRecordEntity.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/enums/ExportRecordStatus.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/enums/ExportDeleteReason.java`
- `demo-mdm/src/main/java/com/demo/mdm/export/enums/ExportCenterErrorCode.java`
- `demo-boot/src/main/java/com/demo/boot/config/OpenApiConfig.java`
- `demo-boot/src/main/resources/application.yml`
- `demo-boot/src/main/resources/application-dev.yml`
- `demo-boot/src/main/resources/application-test.yml`
- `README.md`

### Test

- `demo-core/src/test/java/com/demo/core/query/dto/DynamicQueryDtoValidationTests.java`
- `demo-core/src/test/java/com/demo/core/query/executor/MybatisPlusQueryExecutorTests.java`
- `demo-core/src/test/java/com/demo/core/query/support/DynamicQueryGuardTests.java`
- `demo-core/src/test/java/com/demo/core/query/support/DynamicQuerySummaryRendererTests.java`
- `demo-core/src/test/java/com/demo/core/export/CsvExportRendererTests.java`
- `demo-core/src/test/java/com/demo/core/export/support/AbstractCsvListExportHandlerTests.java`
- `demo-core/src/test/java/com/demo/core/operator/CacheUserServiceTests.java`
- `demo-core/src/test/java/com/demo/core/operator/GatewayOperatorFilterTests.java`
- `demo-system/src/test/java/com/demo/file/FileStorageExportGatewayTests.java`
- `demo-system/src/test/java/com/demo/staff/app/StaffAppServiceTests.java`
- `demo-boot/src/test/java/com/demo/boot/file/FileStorageModuleSmokeTests.java`
- `demo-boot/src/test/java/com/demo/boot/web/DynamicQueryContractIntegrationTests.java`
- `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`
- `demo-boot/src/test/java/com/demo/boot/archunit/ModuleBoundaryTests.java`
- `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`
- `demo-mdm/src/test/java/com/demo/mdm/service/DictServiceDuplicateKeyTests.java`
- `demo-mdm/src/test/java/com/demo/mdm/service/GlobalDictItemNameResolverTests.java`
- `demo-mdm/src/test/java/com/demo/mdm/export/ExportCenterSmokeTests.java`
- `demo-mdm/src/test/java/com/demo/mdm/export/ExportRecordServiceTests.java`

### Reference

- `/Users/youdingte/studys/java-demo/README.md`
- `/Users/youdingte/work/track-bench/README.md`
- `/Users/youdingte/work/track-bench/.agents/skills/oig-java-development/SKILL.md`
- `/Users/youdingte/work/track-bench/.agents/skills/oig-java-development/references/architecture-boundaries.md`
- `/Users/youdingte/work/track-bench/.agents/skills/oig-java-development/references/api-and-modeling-contracts.md`
- `/Users/youdingte/work/track-bench/.agents/skills/oig-java-development/references/dynamic-query-dsl.md`
- `/Users/youdingte/work/track-bench/.agents/skills/oig-java-development/references/database-migrations.md`
- `/Users/youdingte/work/track-bench/.agents/skills/oig-java-development/references/verification.md`

---

### Task 1: Schema And Dependency Baseline

**Type:** `migration`

**Files**

- Create: `demo-boot/src/main/resources/db/migration/V8__backport_track_bench_foundation.sql`
- Modify: `pom.xml`
- Modify: `demo-core/pom.xml`
- Modify: `demo-system/pom.xml`
- Modify: `demo-boot/pom.xml`
- Modify: `demo-boot/src/main/resources/application.yml`
- Modify: `demo-boot/src/main/resources/application-dev.yml`
- Modify: `demo-boot/src/main/resources/application-test.yml`
- Modify: `README.md`
- Test: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`

**Shared Runtime Contracts**

- Flyway versioned migration history
- MyBatis-Plus ID generation
- MyBatis-Plus enum persistence
- Development database configuration

**Invariants**

- Do not modify, delete, or rename existing `V1` to `V7` migration files.
- Do not reintroduce local login, token, tenant, user, role, menu, or permission tables.
- Do not add `track-bench-postloan` to the Maven reactor.
- Do not commit database passwords.

**Constraints**

- `V8__backport_track_bench_foundation.sql` must be valid MySQL 8 SQL.
- `sys_dict_type_global`, `sys_dict_item_global`, and `sys_export_record_global` must end with `id bigint primary key auto_increment` semantics after migration.
- Existing explicit ID inserts in tests must still work on auto-increment tables.
- `mybatis-plus.type-enums-package` must scan `com.demo`.

**Acceptance Criteria**

- [ ] `profile: task-1-migration-script` passes.
- [ ] `profile: task-1-mysql-flyway` passes against `basic_platform_sso`.
- [ ] `profile: task-1-dependency-resolve` passes.
- [ ] `sys_user_cache` exists.
- [ ] dict tables contain `remark`, `status`, `version`; dict item table contains `sort_order`.
- [ ] export table contains `version` and keeps `download_count` as “下载链接获取次数”.

**Verification Profile**

- `profile: task-1-migration-script`
  - `sh scripts/check-migrations.sh`
- `profile: task-1-dependency-resolve`
  - `mvn -pl demo-system -am dependency:tree -Dincludes=com.oigit.appcik:oigit-appcik`
- `profile: task-1-mysql-flyway`
  - `mysql --protocol=tcp -h 192.168.186.154 -P 32425 -u oig -p -e "drop database if exists basic_platform_sso; create database basic_platform_sso character set utf8mb4 collate utf8mb4_general_ci;"`
  - `test -n "$JAVA_DEMO_DATASOURCE_PASSWORD"`
  - `JAVA_DEMO_DATASOURCE_URL='jdbc:mysql://192.168.186.154:32425/basic_platform_sso?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true' JAVA_DEMO_DATASOURCE_USERNAME=oig mvn -pl demo-boot test -Dtest=FlywaySmokeTests`
- `Expected Signals:` migration script check exits `0`; Maven resolves `com.oigit.appcik:oigit-appcik`; `FlywaySmokeTests` reports `BUILD SUCCESS`; `flyway_schema_history` has successful version `8`.

**Verification Strategy**

- `dry-run + rollback`

**Browser Gate Role**

- `none`

**Manual Verification Exception**

- `Waiver Reason:` real remote MySQL credentials are intentionally not committed.
- `Automated Smoke Check:` `profile: task-1-mysql-flyway`
- `Manual Verification Steps:` export `JAVA_DEMO_DATASOURCE_PASSWORD` from local secure config, run the exact profile command, then inspect `show tables from basic_platform_sso`.
- `Expected Results:` `sys_dict_type_global`, `sys_dict_item_global`, `sys_export_record_global`, `sys_user_cache`, and `flyway_schema_history` exist.
- `Follow-up Automation:` not needed; credential injection is environment-specific.

- [ ] Step 1: Add Maven versions and dependencies from track-bench that are actually needed: `h2.version`, `oigit-appcik.version`, `com.oigit.appcik:oigit-appcik`, runtime `org.apache.httpcomponents:httpclient`.
- [ ] Step 2: Add `V8__backport_track_bench_foundation.sql` with schema diffs only: auto-increment IDs, dict `remark/status/version/sort_order`, export `version`, `sys_user_cache`, common dict seeds for `ENABLE_STATUS`, `YES_NO`, `EXPORT_RECORD_STATUS`, `EXPORT_DELETE_REASON`.
- [ ] Step 3: Add MyBatis-Plus YAML settings for logical delete, `id-type: auto`, `type-enums-package: com.demo`, camel-case mapping, and no-logging SQL implementation.
- [ ] Step 4: Change dev datasource to env-var driven config with remote new DB support and local fallback.
- [ ] Step 5: Update README database section to document `basic_platform_sso`, env vars, and that local login/user/role are intentionally absent on this SSO branch.
- [ ] Step 6: Run `profile: task-1-migration-script`, `profile: task-1-dependency-resolve`, and `profile: task-1-mysql-flyway`.
- [ ] Step 7: Commit `chore: prepare foundation schema and dependencies`.

---

### Task 2: Core Runtime Foundation

**Type:** `wiring`

**Files**

- Create: all `demo-core/src/main/java/com/demo/core/enums/**`
- Create: all `demo-core/src/main/java/com/demo/core/query/**`
- Create: all `demo-core/src/main/java/com/demo/core/export/**`
- Create: `demo-core/src/main/java/com/demo/core/web/EnumVO.java`
- Create: `demo-core/src/main/java/com/demo/core/web/AuditRspDTO.java`
- Create: `demo-core/src/main/java/com/demo/core/dict/DictItemNameResolver.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/CacheUserEntity.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/CacheUserMapper.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/CacheUserService.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/CacheUserHolder.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/CacheUserProperties.java`
- Create: `demo-core/src/main/java/com/demo/core/operator/CacheUserAsyncConfiguration.java`
- Create: `demo-core/src/main/java/com/demo/core/jackson/AuditUserIdSerializer.java`
- Modify: `demo-core/src/main/java/com/demo/core/operator/OperatorContext.java`
- Modify: `demo-core/src/main/java/com/demo/core/operator/GatewayOperatorFilter.java`
- Modify: `demo-core/src/main/java/com/demo/core/jackson/JacksonConfig.java`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/MybatisPlusConfig.java`
- Modify: `demo-core/src/main/java/com/demo/core/mybatis/BaseEntity.java`
- Test: all `demo-core/src/test/java/com/demo/core/query/**`
- Test: all `demo-core/src/test/java/com/demo/core/export/**`
- Test: `demo-core/src/test/java/com/demo/core/operator/CacheUserServiceTests.java`
- Test: `demo-core/src/test/java/com/demo/core/operator/GatewayOperatorFilterTests.java`

**Shared Runtime Contracts**

- Request-scoped operator context
- Audit field fill and JSON serialization
- Dynamic query AST and operator allowlist
- Export SPI registry
- MyBatis-Plus optimistic lock and logical delete

**Invariants**

- Controller and service code must not parse SSO headers directly; use `OperatorContext`.
- `X-User-Id` remains optional in dev/test and falls back through existing audit behavior.
- Invalid numeric `X-User-Id` still returns `R.fail(CommonErrorCode.PARAM_ERROR, ...)`.
- Dynamic query errors must throw `BizException(DynamicQueryErrorCode)`.

**Constraints**

- Package names must be `com.demo.*`, not `com.trackbench.*`.
- `GatewayOperatorFilter` must consume `X-User-Id`, `X-User-Name`, `X-User-Phone`, `X-Real-Name`, and `X-User-Code`.
- Cache user writes must be async and throttled by `platform.operator.cache-user.update-window-seconds`.
- `BaseEntity` must add `@TableLogic` and `@Version` consistently with `V8`.

**Acceptance Criteria**

- [ ] `profile: task-2-core-unit` passes.
- [ ] `GatewayOperatorFilterTests` proves header normalization, URL-decoding, cache-user submission, invalid user ID response, and context cleanup.
- [ ] `CacheUserServiceTests` proves upsert and real-name lookup behavior.
- [ ] query tests prove condition tree validation, MyBatis wrapper generation, nested group preservation, operator allowlist, and summary rendering.
- [ ] export renderer tests prove CSV BOM, header order, escaping, and registry duplicate rejection.

**Verification Profile**

- `profile: task-2-core-unit`
  - `mvn -pl demo-core test -Dtest=GatewayOperatorFilterTests,CacheUserServiceTests,DynamicQueryDtoValidationTests,DynamicQueryGuardTests,DynamicQuerySummaryRendererTests,MybatisPlusQueryExecutorTests,CsvExportRendererTests,AbstractCsvListExportHandlerTests`
- `Expected Signals:` `Tests run` count is nonzero and Maven exits with `BUILD SUCCESS`.

**Verification Strategy**

- `integration smoke`

**Browser Gate Role**

- `none`

- [ ] Step 1: Port `core/enums`, `core/query`, and `core/export` from track-bench with package/import rename.
- [ ] Step 2: Enhance `OperatorContext` and `GatewayOperatorFilter` for SSO header payload and `sys_user_cache` async upsert.
- [ ] Step 3: Add audit user serialization via `AuditUserIdSerializer` and `JacksonConfig` serializer modifier.
- [ ] Step 4: Align MyBatis config with optimistic locking and entity logical-delete annotations.
- [ ] Step 5: Port and adapt core tests.
- [ ] Step 6: Run `profile: task-2-core-unit`.
- [ ] Step 7: Commit `feat: add core query export and sso operator foundation`.

---

### Task 3: System Integrations For Export And SSO

**Type:** `wiring`

**Files**

- Create: `demo-system/src/main/java/com/demo/file/export/FileStorageExportGateway.java`
- Create: `demo-system/src/main/java/com/demo/file/controller/dto/FetchTempUrlBatchReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/file/controller/dto/FetchTempUrlBatchRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/file/controller/dto/FetchTempUrlItemRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/staff/controller/StaffController.java`
- Create: `demo-system/src/main/java/com/demo/staff/app/StaffAppService.java`
- Create: `demo-system/src/main/java/com/demo/staff/config/StaffCiConfiguration.java`
- Create: `demo-system/src/main/java/com/demo/staff/controller/dto/StaffInfoRspDTO.java`
- Create: `demo-system/src/main/java/com/demo/staff/controller/dto/query/StaffListAllReqDTO.java`
- Modify: `demo-system/src/main/java/com/demo/file/service/FileService.java`
- Modify: `demo-system/src/main/java/com/demo/file/app/FileAppService.java`
- Modify: `demo-system/src/main/java/com/demo/file/controller/FileStorageController.java`
- Modify: `demo-system/src/main/java/com/demo/file/enums/FileErrorCode.java`
- Modify: `demo-system/src/main/java/com/demo/file/infra/provider/FileStorageProvider.java`
- Modify: `demo-system/src/main/java/com/demo/file/infra/provider/local/LocalFileStorageProvider.java`
- Modify: `demo-system/src/main/java/com/demo/file/infra/provider/qiniu/QiniuFileStorageProvider.java`
- Modify: `demo-system/src/main/java/com/demo/file/infra/provider/minio/MinioFileStorageProvider.java`
- Test: `demo-system/src/test/java/com/demo/file/FileStorageExportGatewayTests.java`
- Test: `demo-system/src/test/java/com/demo/staff/app/StaffAppServiceTests.java`
- Test: `demo-boot/src/test/java/com/demo/boot/file/FileStorageModuleSmokeTests.java`

**Shared Runtime Contracts**

- File object key normalization
- File provider upload/download/temp URL contract
- SSO staff external client configuration

**Invariants**

- Vendor SDK calls stay inside `demo-system`.
- Business/export code must use `ExportFileSink` and `ExportFileAccessor`, not direct provider SDKs.
- `/api/file/storage/object/upload`, `/object/delete`, `/object/temp-url/fetch`, `/direct-upload/credential/fetch` keep existing behavior.
- `/api/staff/list-all` returns data from SSO client and does not persist local users.

**Constraints**

- Batch temp URL request max size is `200`.
- Batch temp URL response preserves first-seen order and deduplicates duplicate object keys.
- `FileService.upload(byte[])` must reuse the same bizPath/objectKey validation as multipart upload.
- `FileService.download(objectKey)` must wrap provider errors as `BizException(FileErrorCode.FILE_DOWNLOAD_FAILED)` unless already a `BizException`.

**Acceptance Criteria**

- [ ] `profile: task-3-system-unit` passes.
- [ ] `profile: task-3-file-smoke` passes.
- [ ] file export gateway can store rendered bytes and fetch temp URL/content.
- [ ] staff service can page through SSO results, deduplicate by staff code, and filter keyword/sex locally.

**Verification Profile**

- `profile: task-3-system-unit`
  - `mvn -pl demo-system test -Dtest=FileStorageExportGatewayTests,StaffAppServiceTests,LocalFileStorageProviderTests,QiniuFileStorageProviderTests,MinioFileStorageProviderTests`
- `profile: task-3-file-smoke`
  - `mvn -pl demo-boot -am install -DskipTests`
  - `mvn -pl demo-boot test -Dtest=FileStorageModuleSmokeTests`
- `Expected Signals:` local file upload/delete/temp URL tests pass; batch temp URL test returns two items for three requested keys with one duplicate.

**Verification Strategy**

- `integration smoke`

**Browser Gate Role**

- `none`

**Manual Verification Exception**

- `Waiver Reason:` real SSO staff query depends on external SSO service availability and credentials.
- `Automated Smoke Check:` `profile: task-3-system-unit`
- `Manual Verification Steps:` run app with valid `oigit.appcik.sso.server-addr`, call `POST /api/staff/list-all` with `{"keyword":"张"}` through a gateway-authenticated request.
- `Expected Results:` response is `R.ok([...])`, each item has `staffCode`, `ssoAccountId`, `userName`, `account`, `mobile`.
- `Follow-up Automation:` not needed for this branch; external SSO contract is third-party/environment-specific.

- [ ] Step 1: Add provider `download` support and byte-array upload support to file service and providers.
- [ ] Step 2: Add batch temp URL DTOs, AppService method, Controller endpoint, and `operationId`.
- [ ] Step 3: Add `FileStorageExportGateway` implementing `ExportFileSink` and `ExportFileAccessor`.
- [ ] Step 4: Add staff SSO integration under `com.demo.staff`.
- [ ] Step 5: Port system/file/staff tests.
- [ ] Step 6: Run `profile: task-3-system-unit` and `profile: task-3-file-smoke`.
- [ ] Step 7: Commit `feat: add export file gateway and sso staff lookup`.

---

### Task 4: MDM Dict And Export Center

**Type:** `behavior`

**Files**

- Create: all `demo-mdm/src/main/java/com/demo/mdm/controller/dto/query/**`
- Create: all `demo-mdm/src/main/java/com/demo/mdm/query/globaldict/**`
- Create: `demo-mdm/src/main/java/com/demo/mdm/service/GlobalDictItemNameResolver.java`
- Create: all missing `demo-mdm/src/main/java/com/demo/mdm/export/app/**`
- Create: all missing `demo-mdm/src/main/java/com/demo/mdm/export/controller/**`
- Create: all missing `demo-mdm/src/main/java/com/demo/mdm/export/controller/dto/**`
- Create: all missing `demo-mdm/src/main/java/com/demo/mdm/export/handler/**`
- Create: all missing `demo-mdm/src/main/java/com/demo/mdm/export/query/**`
- Create: all missing `demo-mdm/src/main/java/com/demo/mdm/export/service/**`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/controller/GlobalDictController.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/app/DictAppService.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/GlobalDictTypeEntity.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/GlobalDictItemEntity.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/export/service/ExportRecordService.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/export/infra/entity/ExportRecordEntity.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/export/enums/ExportRecordStatus.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/export/enums/ExportDeleteReason.java`
- Modify: `demo-mdm/src/main/java/com/demo/mdm/export/enums/ExportCenterErrorCode.java`
- Test: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`
- Test: `demo-mdm/src/test/java/com/demo/mdm/service/DictServiceDuplicateKeyTests.java`
- Test: `demo-mdm/src/test/java/com/demo/mdm/service/GlobalDictItemNameResolverTests.java`
- Test: `demo-mdm/src/test/java/com/demo/mdm/export/ExportCenterSmokeTests.java`
- Test: `demo-mdm/src/test/java/com/demo/mdm/export/ExportRecordServiceTests.java`

**Shared Runtime Contracts**

- Global dict API contract
- Export center record ownership
- Export scene registry
- Dynamic query DSL request/AST mapping
- File storage export sink/accessor

**Invariants**

- Controllers call only AppService.
- DTOs and entities remain separate.
- All public endpoints use `POST`.
- All controller methods return `R.ok(...)`.
- All business failures use `BizException(ErrorCode)`.
- Export records are scoped to current `OperatorContext.getOperatorId()`; missing operator falls back to `0L`.

**Constraints**

- `GlobalDictController` must match track-bench API behavior:
  - `/api/mdm/dict/global/types/list` accepts `GlobalDictTypeDynamicListReqDTO` and returns `PageResult<GlobalDictTypeRspDTO>`.
  - `/api/mdm/dict/global/types/list-all` accepts `GlobalDictTypeListReqDTO` and returns `List<GlobalDictTypeRspDTO>`.
  - `/api/mdm/dict/global/items/by-type` accepts `GlobalDictItemDynamicPageReqDTO` and returns `PageResult<DictItemRspDTO>`.
- `ExportCenterController` must expose:
  - `/api/mdm/export/submit`
  - `/api/mdm/export/my/page`
  - `/api/mdm/export/detail`
  - `/api/mdm/export/download`
  - `/api/mdm/export/download/batch`
  - `/api/mdm/export/delete`
- `ExportRecordStatus` and `ExportDeleteReason` must implement `BaseEnum`.
- Manual deletion must use soft-delete `deleted = 1`, not status `DELETED` as a visible list state.

**Acceptance Criteria**

- [ ] `profile: task-4-mdm-unit` passes.
- [ ] `profile: task-4-export-smoke` passes.
- [ ] Global dict list supports dynamic condition tree and sorting.
- [ ] Global dict item list returns `PageResult`.
- [ ] Export submit creates a success record, stores CSV through file gateway, returns a temporary download URL, and increments download link count.
- [ ] Export detail/download/delete/batch-download enforce owner scoping.
- [ ] Batch download packages existing successful records into a ZIP without creating another export record.

**Verification Profile**

- `profile: task-4-mdm-unit`
  - `mvn -pl demo-mdm test -Dtest=DictServiceDuplicateKeyTests,GlobalDictItemNameResolverTests,ExportRecordServiceTests`
- `profile: task-4-export-smoke`
  - `mvn -pl demo-mdm test -Dtest=ExportCenterSmokeTests,DictModuleSmokeTests`
- `Expected Signals:` export smoke proves submit, download, batch download, delete, forbidden access, and query snapshot summary; dict smoke proves create/list/update/delete and dynamic page behavior.

**Verification Strategy**

- `TDD`

**Browser Gate Role**

- `none`

- [ ] Step 1: Upgrade dict entities/DTO mappings for `remark`, `status`, `sortOrder`, audit fields, and `EnableStatusEnum`.
- [ ] Step 2: Add dict dynamic query DTOs, scene definitions, mappers, and AppService wiring.
- [ ] Step 3: Add export center controller, DTOs, app service, execution service, download service, batch download service, query scene, and global dict export handler.
- [ ] Step 4: Replace existing minimal export record service semantics with track-bench soft-delete, owner-scoped page, and download-link accounting.
- [ ] Step 5: Port MDM tests and remove expectations tied to old list-only item API.
- [ ] Step 6: Run `profile: task-4-mdm-unit` and `profile: task-4-export-smoke`.
- [ ] Step 7: Commit `feat: backport dict dynamic query and export center`.

---

### Task 5: Boot OpenAPI Contract And Final Verification

**Type:** `config`

**Files**

- Create: `demo-boot/src/main/java/com/demo/boot/config/SpringDocOperationIdConfig.java`
- Create: `demo-boot/src/main/java/com/demo/boot/config/EnumModelConverter.java`
- Modify: `demo-boot/src/main/java/com/demo/boot/config/OpenApiConfig.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/web/DynamicQueryContractIntegrationTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/archunit/ModuleBoundaryTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`
- Modify: `README.md`

**Shared Runtime Contracts**

- OpenAPI schema generation
- Stable operationId generation
- Dynamic query discriminator mappings
- Module dependency boundaries
- Error code uniqueness

**Invariants**

- `demo-boot` stays free of business logic.
- OpenAPI docs must include only current base modules: core DTOs, file DTOs, staff DTOs, mdm dict DTOs, mdm export DTOs.
- OpenAPI dynamic query scene list must include only:
  - `GlobalDictTypeDynamicListReqDTO`
  - `GlobalDictItemDynamicPageReqDTO`
  - `ExportRecordDynamicPageReqDTO`
- No `postloan` schema, operation, or package remains in generated docs.

**Constraints**

- Explicit `operationId` on track-bench-consumed endpoints must stay stable.
- Generated fallback operationId must be path-derived for methods without explicit IDs.
- `BaseEnum` fields must be represented as `EnumVO`.
- Dynamic query `condition` fields must reference the scene-specific `ConditionNode` schema.

**Acceptance Criteria**

- [ ] `profile: task-5-openapi-contract` passes.
- [ ] `profile: task-5-full-local` passes.
- [ ] `profile: task-5-mysql-smoke` passes when MySQL env vars are present.
- [ ] OpenAPI output contains operation IDs for dict, file, export, and staff endpoints.
- [ ] OpenAPI output does not contain `postloan`.
- [ ] README accurately describes the SSO branch, database choice, exported endpoints, and intentionally excluded business modules.

**Verification Profile**

- `profile: task-5-openapi-contract`
  - `mvn -pl demo-boot -am install -DskipTests`
  - `mvn -pl demo-boot test -Dtest=OpenApiDocumentationTests,DynamicQueryContractIntegrationTests,ModuleBoundaryTests,ErrorCodeContractTests`
- `profile: task-5-full-local`
  - `mvn clean test`
  - `mvn compile`
- `profile: task-5-mysql-smoke`
  - `test -n "$JAVA_DEMO_DATASOURCE_PASSWORD"`
  - `JAVA_DEMO_DATASOURCE_URL='jdbc:mysql://192.168.186.154:32425/basic_platform_sso?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' JAVA_DEMO_DATASOURCE_USERNAME=oig mvn -pl demo-boot spring-boot:run -Dspring-boot.run.profiles=dev`
  - In another shell: `curl -s http://127.0.0.1:8080/actuator/health`
- `Expected Signals:` OpenAPI contract tests pass; full Maven build exits `BUILD SUCCESS`; health endpoint returns JSON containing `"status":"UP"`.

**Verification Strategy**

- `build + smoke`

**Browser Gate Role**

- `none`

**Manual Verification Exception**

- `Waiver Reason:` running `spring-boot:run` against the shared MySQL server requires local credential injection and a free port.
- `Automated Smoke Check:` `profile: task-5-full-local`
- `Manual Verification Steps:` export datasource credentials, run `profile: task-5-mysql-smoke`, then call `/actuator/health`.
- `Expected Results:` app starts without Flyway validation error and health is `UP`.
- `Follow-up Automation:` not needed; this is environment-specific startup verification.

- [ ] Step 1: Port OpenAPI customizers and trim package/scene lists to current base modules.
- [ ] Step 2: Add/adjust operation IDs on controllers consumed by the migrated frontend.
- [ ] Step 3: Update tests for dynamic query schema, enum schema, operationId stability, module boundaries, and error code uniqueness.
- [ ] Step 4: Update README with final module capabilities and excluded business scope.
- [ ] Step 5: Run `profile: task-5-openapi-contract`, `profile: task-5-full-local`, and `profile: task-5-mysql-smoke`.
- [ ] Step 6: Commit `test: verify foundation backport contracts`.

---

## Execution Order

1. Task 1: Schema And Dependency Baseline
2. Task 2: Core Runtime Foundation
3. Task 3: System Integrations For Export And SSO
4. Task 4: MDM Dict And Export Center
5. Task 5: Boot OpenAPI Contract And Final Verification

Recommended execution mode: `executing-plans`.

---

## Final Acceptance

- `track-bench-postloan` and all postloan-specific code/data are not present in current project.
- Current project starts as an SSO-based backend base with no local login/user/role implementation.
- Dict endpoints match track-bench current backend contract.
- Export center endpoints match track-bench current backend contract.
- File storage supports export center lifecycle.
- SSO headers are consumed consistently through `OperatorContext`.
- `sys_user_cache` records SSO-transmitted user display data without becoming a local user system.
- New MySQL database `basic_platform_sso` can be migrated from empty state.
- `mvn clean test` and `mvn compile` pass before reporting completion.

---

## Quality Checklist Result

- Scope is one shippable slice: SSO-based backend foundation backport.
- Exact create/modify/test paths are listed.
- Verification commands and expected signals are listed for every task.
- No historical Flyway migration is modified.
- Business module exclusion rules are explicit.
- Residual risk is limited to remote MySQL/SSO credentials and is covered by bounded manual verification exceptions.
