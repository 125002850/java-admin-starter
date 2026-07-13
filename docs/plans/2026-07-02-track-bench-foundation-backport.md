# track-bench 基础能力反写计划

**Goal:** 以当前 `feature/sso` 分支为目标，只补齐 `~/work/track-bench` 中已经沉淀出来的基础能力，删除/排除 track-bench 业务模块，形成基于 SSO 的可运行 Java 后端基座。

**Architecture:** 保持当前项目模块边界：`admin-core` 放业务无关抽象，`admin-system` 放外部系统集成，`admin-mdm` 放字典和导出中心这类平台型业务能力，`admin-boot` 只做启动装配和配置。代码从 `com.trackbench.*` 迁移为 `com.example.admin.*`，API 契约采用 track-bench 当前风格，同时继续满足 `R.ok(...)` / `R.fail(...)` 和 `BizException(ErrorCode)`。

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
- `admin-boot/src/main/resources/application-dev.yml` 改为支持环境变量覆盖：
  - `JAVA_ADMIN_STARTER_DATASOURCE_URL`
  - `JAVA_ADMIN_STARTER_DATASOURCE_USERNAME`
  - `JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD`
- 本地 Docker MySQL 仍可作为 fallback，远程新库通过环境变量启用。

---

## File Structure

### Create

- `admin-core/src/main/java/com/example/admin/core/enums/BaseEnum.java`
- `admin-core/src/main/java/com/example/admin/core/enums/EnableStatusEnum.java`
- `admin-core/src/main/java/com/example/admin/core/enums/YesNoEnum.java`
- `admin-core/src/main/java/com/example/admin/core/web/EnumVO.java`
- `admin-core/src/main/java/com/example/admin/core/web/AuditRspDTO.java`
- `admin-core/src/main/java/com/example/admin/core/dict/DictItemNameResolver.java`
- `admin-core/src/main/java/com/example/admin/core/operator/CacheUserEntity.java`
- `admin-core/src/main/java/com/example/admin/core/operator/CacheUserMapper.java`
- `admin-core/src/main/java/com/example/admin/core/operator/CacheUserService.java`
- `admin-core/src/main/java/com/example/admin/core/operator/CacheUserHolder.java`
- `admin-core/src/main/java/com/example/admin/core/operator/CacheUserProperties.java`
- `admin-core/src/main/java/com/example/admin/core/operator/CacheUserAsyncConfiguration.java`
- `admin-core/src/main/java/com/example/admin/core/jackson/AuditUserIdSerializer.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/ConditionAstNode.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/ConditionGroupAst.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/ConditionLeafAst.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/QueryAst.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/QueryLogicOperator.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/QueryOperator.java`
- `admin-core/src/main/java/com/example/admin/core/query/ast/SortSpec.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/AbstractConditionNodeDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/BaseDynamicCriteriaReqDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/BasePagedDynamicQueryReqDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/ConditionGroupDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/DateTimeConditionDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/EnumConditionDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/SortItemDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/dto/TextConditionDTO.java`
- `admin-core/src/main/java/com/example/admin/core/query/exception/DynamicQueryErrorCode.java`
- `admin-core/src/main/java/com/example/admin/core/query/executor/MybatisPlusQueryExecutor.java`
- `admin-core/src/main/java/com/example/admin/core/query/scene/DynamicQueryAstMapper.java`
- `admin-core/src/main/java/com/example/admin/core/query/scene/SceneQueryDefinition.java`
- `admin-core/src/main/java/com/example/admin/core/query/scene/SceneQueryMapper.java`
- `admin-core/src/main/java/com/example/admin/core/query/support/DynamicQueryGuard.java`
- `admin-core/src/main/java/com/example/admin/core/query/support/DynamicQuerySummaryRenderer.java`
- `admin-core/src/main/java/com/example/admin/core/query/support/QueryComplexityScorer.java`
- `admin-core/src/main/java/com/example/admin/core/query/validation/DynamicQueryLimits.java`
- `admin-core/src/main/java/com/example/admin/core/export/dto/BaseExportDynamicCriteriaReqDTO.java`
- `admin-core/src/main/java/com/example/admin/core/export/dto/ExportOptionsReqDTO.java`
- `admin-core/src/main/java/com/example/admin/core/export/dto/ExportRangeReqDTO.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportColumn.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportMeta.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportRenderRequest.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportScope.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportStoreRequest.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportStoredFile.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/ExportTaskResult.java`
- `admin-core/src/main/java/com/example/admin/core/export/model/RenderedExportFile.java`
- `admin-core/src/main/java/com/example/admin/core/export/renderer/CsvExportRenderer.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportFileAccessor.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportFileSink.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportHandler.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportRenderer.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportRendererRegistry.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportSceneRegistry.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/ExportTaskSubmitter.java`
- `admin-core/src/main/java/com/example/admin/core/export/spi/PackageableExportHandler.java`
- `admin-core/src/main/java/com/example/admin/core/export/support/AbstractCsvListExportHandler.java`
- `admin-core/src/main/java/com/example/admin/core/export/support/SpringExportRendererRegistry.java`
- `admin-core/src/main/java/com/example/admin/core/export/support/SpringExportSceneRegistry.java`
- `admin-system/src/main/java/com/example/admin/file/export/FileStorageExportGateway.java`
- `admin-system/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlBatchReqDTO.java`
- `admin-system/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlBatchRspDTO.java`
- `admin-system/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlItemRspDTO.java`
- `admin-system/src/main/java/com/example/admin/staff/controller/StaffController.java`
- `admin-system/src/main/java/com/example/admin/staff/app/StaffAppService.java`
- `admin-system/src/main/java/com/example/admin/staff/config/StaffCiConfiguration.java`
- `admin-system/src/main/java/com/example/admin/staff/controller/dto/StaffInfoRspDTO.java`
- `admin-system/src/main/java/com/example/admin/staff/controller/dto/query/StaffListAllReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/controller/dto/query/GlobalDictItemDynamicCriteriaReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/controller/dto/query/GlobalDictItemDynamicPageReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/controller/dto/query/GlobalDictTypeDynamicCriteriaReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/controller/dto/query/GlobalDictTypeDynamicListReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/query/globaldict/GlobalDictItemSceneQueryDefinition.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/query/globaldict/GlobalDictItemSceneQueryMapper.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/query/globaldict/GlobalDictTypeSceneQueryDefinition.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/query/globaldict/GlobalDictTypeSceneQueryMapper.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/service/GlobalDictItemNameResolver.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/app/ExportCenterAppService.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/ExportCenterController.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportBatchDownloadReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportBatchDownloadRspDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportDownloadRspDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDeleteReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordIdReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordRspDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportSubmitReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportSubmitRspDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/query/ExportRecordDynamicCriteriaReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/query/ExportRecordDynamicPageReqDTO.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/handler/GlobalDictTypeListExportHandler.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/query/ExportRecordSceneQueryDefinition.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/query/ExportRecordSceneQueryMapper.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportBatchDownloadService.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportDownloadService.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionService.java`
- `admin-boot/src/main/java/com/example/admin/boot/config/SpringDocOperationIdConfig.java`
- `admin-boot/src/main/java/com/example/admin/boot/config/EnumModelConverter.java`
- `admin-boot/src/main/resources/db/migration/V8__backport_track_bench_foundation.sql`

### Modify

- `pom.xml`
- `admin-core/pom.xml`
- `admin-system/pom.xml`
- `admin-boot/pom.xml`
- `admin-core/src/main/java/com/example/admin/core/operator/OperatorContext.java`
- `admin-core/src/main/java/com/example/admin/core/operator/GatewayOperatorFilter.java`
- `admin-core/src/main/java/com/example/admin/core/jackson/JacksonConfig.java`
- `admin-core/src/main/java/com/example/admin/core/mybatis/MybatisPlusConfig.java`
- `admin-core/src/main/java/com/example/admin/core/mybatis/BaseEntity.java`
- `admin-system/src/main/java/com/example/admin/file/service/FileService.java`
- `admin-system/src/main/java/com/example/admin/file/app/FileAppService.java`
- `admin-system/src/main/java/com/example/admin/file/controller/FileStorageController.java`
- `admin-system/src/main/java/com/example/admin/file/enums/FileErrorCode.java`
- `admin-system/src/main/java/com/example/admin/file/infra/provider/FileStorageProvider.java`
- `admin-system/src/main/java/com/example/admin/file/infra/provider/local/LocalFileStorageProvider.java`
- `admin-system/src/main/java/com/example/admin/file/infra/provider/qiniu/QiniuFileStorageProvider.java`
- `admin-system/src/main/java/com/example/admin/file/infra/provider/minio/MinioFileStorageProvider.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/controller/GlobalDictController.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/app/DictAppService.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/service/DictService.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/infra/entity/GlobalDictTypeEntity.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/infra/entity/GlobalDictItemEntity.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportRecordService.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/infra/entity/ExportRecordEntity.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportRecordStatus.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportDeleteReason.java`
- `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportCenterErrorCode.java`
- `admin-boot/src/main/java/com/example/admin/boot/config/OpenApiConfig.java`
- `admin-boot/src/main/resources/application.yml`
- `admin-boot/src/main/resources/application-dev.yml`
- `admin-boot/src/main/resources/application-test.yml`
- `README.md`

### Test

- `admin-core/src/test/java/com/example/admin/core/query/dto/DynamicQueryDtoValidationTests.java`
- `admin-core/src/test/java/com/example/admin/core/query/executor/MybatisPlusQueryExecutorTests.java`
- `admin-core/src/test/java/com/example/admin/core/query/support/DynamicQueryGuardTests.java`
- `admin-core/src/test/java/com/example/admin/core/query/support/DynamicQuerySummaryRendererTests.java`
- `admin-core/src/test/java/com/example/admin/core/export/CsvExportRendererTests.java`
- `admin-core/src/test/java/com/example/admin/core/export/support/AbstractCsvListExportHandlerTests.java`
- `admin-core/src/test/java/com/example/admin/core/operator/CacheUserServiceTests.java`
- `admin-core/src/test/java/com/example/admin/core/operator/GatewayOperatorFilterTests.java`
- `admin-system/src/test/java/com/example/admin/file/FileStorageExportGatewayTests.java`
- `admin-system/src/test/java/com/example/admin/staff/app/StaffAppServiceTests.java`
- `admin-boot/src/test/java/com/example/admin/boot/file/FileStorageModuleSmokeTests.java`
- `admin-boot/src/test/java/com/example/admin/boot/web/DynamicQueryContractIntegrationTests.java`
- `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- `admin-boot/src/test/java/com/example/admin/boot/flyway/FlywaySmokeTests.java`
- `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- `admin-mdm/src/test/java/com/example/admin/mdm/DictModuleSmokeTests.java`
- `admin-mdm/src/test/java/com/example/admin/mdm/service/DictServiceDuplicateKeyTests.java`
- `admin-mdm/src/test/java/com/example/admin/mdm/service/GlobalDictItemNameResolverTests.java`
- `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportCenterSmokeTests.java`
- `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportRecordServiceTests.java`

### Reference

- `/Users/youdingte/studys/java-admin-starter/README.md`
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

- Create: `admin-boot/src/main/resources/db/migration/V8__backport_track_bench_foundation.sql`
- Modify: `pom.xml`
- Modify: `admin-core/pom.xml`
- Modify: `admin-system/pom.xml`
- Modify: `admin-boot/pom.xml`
- Modify: `admin-boot/src/main/resources/application.yml`
- Modify: `admin-boot/src/main/resources/application-dev.yml`
- Modify: `admin-boot/src/main/resources/application-test.yml`
- Modify: `README.md`
- Test: `admin-boot/src/test/java/com/example/admin/boot/flyway/FlywaySmokeTests.java`

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
- `mybatis-plus.type-enums-package` must scan `com.example.admin`.

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
  - `mvn -pl admin-system -am dependency:tree -Dincludes=com.oigit.appcik:oigit-appcik`
- `profile: task-1-mysql-flyway`
  - `mysql --protocol=tcp -h 192.168.186.154 -P 32425 -u oig -p -e "drop database if exists basic_platform_sso; create database basic_platform_sso character set utf8mb4 collate utf8mb4_general_ci;"`
  - `test -n "$JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD"`
  - `JAVA_ADMIN_STARTER_DATASOURCE_URL='jdbc:mysql://192.168.186.154:32425/basic_platform_sso?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true' JAVA_ADMIN_STARTER_DATASOURCE_USERNAME=oig mvn -pl admin-boot test -Dtest=FlywaySmokeTests`
- `Expected Signals:` migration script check exits `0`; Maven resolves `com.oigit.appcik:oigit-appcik`; `FlywaySmokeTests` reports `BUILD SUCCESS`; `flyway_schema_history` has successful version `8`.

**Verification Strategy**

- `dry-run + rollback`

**Browser Gate Role**

- `none`

**Manual Verification Exception**

- `Waiver Reason:` real remote MySQL credentials are intentionally not committed.
- `Automated Smoke Check:` `profile: task-1-mysql-flyway`
- `Manual Verification Steps:` export `JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD` from local secure config, run the exact profile command, then inspect `show tables from basic_platform_sso`.
- `Expected Results:` `sys_dict_type_global`, `sys_dict_item_global`, `sys_export_record_global`, `sys_user_cache`, and `flyway_schema_history` exist.
- `Follow-up Automation:` not needed; credential injection is environment-specific.

- [ ] Step 1: Add Maven versions and dependencies from track-bench that are actually needed: `h2.version`, `oigit-appcik.version`, `com.oigit.appcik:oigit-appcik`, runtime `org.apache.httpcomponents:httpclient`.
- [ ] Step 2: Add `V8__backport_track_bench_foundation.sql` with schema diffs only: auto-increment IDs, dict `remark/status/version/sort_order`, export `version`, `sys_user_cache`, common dict seeds for `ENABLE_STATUS`, `YES_NO`, `EXPORT_RECORD_STATUS`, `EXPORT_DELETE_REASON`.
- [ ] Step 3: Add MyBatis-Plus YAML settings for logical delete, `id-type: auto`, `type-enums-package: com.example.admin`, camel-case mapping, and no-logging SQL implementation.
- [ ] Step 4: Change dev datasource to env-var driven config with remote new DB support and local fallback.
- [ ] Step 5: Update README database section to document `basic_platform_sso`, env vars, and that local login/user/role are intentionally absent on this SSO branch.
- [ ] Step 6: Run `profile: task-1-migration-script`, `profile: task-1-dependency-resolve`, and `profile: task-1-mysql-flyway`.
- [ ] Step 7: Commit `chore: prepare foundation schema and dependencies`.

---

### Task 2: Core Runtime Foundation

**Type:** `wiring`

**Files**

- Create: all `admin-core/src/main/java/com/example/admin/core/enums/**`
- Create: all `admin-core/src/main/java/com/example/admin/core/query/**`
- Create: all `admin-core/src/main/java/com/example/admin/core/export/**`
- Create: `admin-core/src/main/java/com/example/admin/core/web/EnumVO.java`
- Create: `admin-core/src/main/java/com/example/admin/core/web/AuditRspDTO.java`
- Create: `admin-core/src/main/java/com/example/admin/core/dict/DictItemNameResolver.java`
- Create: `admin-core/src/main/java/com/example/admin/core/operator/CacheUserEntity.java`
- Create: `admin-core/src/main/java/com/example/admin/core/operator/CacheUserMapper.java`
- Create: `admin-core/src/main/java/com/example/admin/core/operator/CacheUserService.java`
- Create: `admin-core/src/main/java/com/example/admin/core/operator/CacheUserHolder.java`
- Create: `admin-core/src/main/java/com/example/admin/core/operator/CacheUserProperties.java`
- Create: `admin-core/src/main/java/com/example/admin/core/operator/CacheUserAsyncConfiguration.java`
- Create: `admin-core/src/main/java/com/example/admin/core/jackson/AuditUserIdSerializer.java`
- Modify: `admin-core/src/main/java/com/example/admin/core/operator/OperatorContext.java`
- Modify: `admin-core/src/main/java/com/example/admin/core/operator/GatewayOperatorFilter.java`
- Modify: `admin-core/src/main/java/com/example/admin/core/jackson/JacksonConfig.java`
- Modify: `admin-core/src/main/java/com/example/admin/core/mybatis/MybatisPlusConfig.java`
- Modify: `admin-core/src/main/java/com/example/admin/core/mybatis/BaseEntity.java`
- Test: all `admin-core/src/test/java/com/example/admin/core/query/**`
- Test: all `admin-core/src/test/java/com/example/admin/core/export/**`
- Test: `admin-core/src/test/java/com/example/admin/core/operator/CacheUserServiceTests.java`
- Test: `admin-core/src/test/java/com/example/admin/core/operator/GatewayOperatorFilterTests.java`

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

- Package names must be `com.example.admin.*`, not `com.trackbench.*`.
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
  - `mvn -pl admin-core test -Dtest=GatewayOperatorFilterTests,CacheUserServiceTests,DynamicQueryDtoValidationTests,DynamicQueryGuardTests,DynamicQuerySummaryRendererTests,MybatisPlusQueryExecutorTests,CsvExportRendererTests,AbstractCsvListExportHandlerTests`
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

- Create: `admin-system/src/main/java/com/example/admin/file/export/FileStorageExportGateway.java`
- Create: `admin-system/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlBatchReqDTO.java`
- Create: `admin-system/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlBatchRspDTO.java`
- Create: `admin-system/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlItemRspDTO.java`
- Create: `admin-system/src/main/java/com/example/admin/staff/controller/StaffController.java`
- Create: `admin-system/src/main/java/com/example/admin/staff/app/StaffAppService.java`
- Create: `admin-system/src/main/java/com/example/admin/staff/config/StaffCiConfiguration.java`
- Create: `admin-system/src/main/java/com/example/admin/staff/controller/dto/StaffInfoRspDTO.java`
- Create: `admin-system/src/main/java/com/example/admin/staff/controller/dto/query/StaffListAllReqDTO.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/service/FileService.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/app/FileAppService.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/controller/FileStorageController.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/enums/FileErrorCode.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/infra/provider/FileStorageProvider.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/infra/provider/local/LocalFileStorageProvider.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/infra/provider/qiniu/QiniuFileStorageProvider.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/infra/provider/minio/MinioFileStorageProvider.java`
- Test: `admin-system/src/test/java/com/example/admin/file/FileStorageExportGatewayTests.java`
- Test: `admin-system/src/test/java/com/example/admin/staff/app/StaffAppServiceTests.java`
- Test: `admin-boot/src/test/java/com/example/admin/boot/file/FileStorageModuleSmokeTests.java`

**Shared Runtime Contracts**

- File object key normalization
- File provider upload/download/temp URL contract
- SSO staff external client configuration

**Invariants**

- Vendor SDK calls stay inside `admin-system`.
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
  - `mvn -pl admin-system test -Dtest=FileStorageExportGatewayTests,StaffAppServiceTests,LocalFileStorageProviderTests,QiniuFileStorageProviderTests,MinioFileStorageProviderTests`
- `profile: task-3-file-smoke`
  - `mvn -pl admin-boot -am install -DskipTests`
  - `mvn -pl admin-boot test -Dtest=FileStorageModuleSmokeTests`
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
- [ ] Step 4: Add staff SSO integration under `com.example.admin.staff`.
- [ ] Step 5: Port system/file/staff tests.
- [ ] Step 6: Run `profile: task-3-system-unit` and `profile: task-3-file-smoke`.
- [ ] Step 7: Commit `feat: add export file gateway and sso staff lookup`.

---

### Task 4: MDM Dict And Export Center

**Type:** `behavior`

**Files**

- Create: all `admin-mdm/src/main/java/com/example/admin/mdm/controller/dto/query/**`
- Create: all `admin-mdm/src/main/java/com/example/admin/mdm/query/globaldict/**`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/service/GlobalDictItemNameResolver.java`
- Create: all missing `admin-mdm/src/main/java/com/example/admin/mdm/export/app/**`
- Create: all missing `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/**`
- Create: all missing `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/**`
- Create: all missing `admin-mdm/src/main/java/com/example/admin/mdm/export/handler/**`
- Create: all missing `admin-mdm/src/main/java/com/example/admin/mdm/export/query/**`
- Create: all missing `admin-mdm/src/main/java/com/example/admin/mdm/export/service/**`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/controller/GlobalDictController.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/app/DictAppService.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/service/DictService.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/infra/entity/GlobalDictTypeEntity.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/infra/entity/GlobalDictItemEntity.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportRecordService.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/export/infra/entity/ExportRecordEntity.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportRecordStatus.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportDeleteReason.java`
- Modify: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportCenterErrorCode.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/DictModuleSmokeTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/service/DictServiceDuplicateKeyTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/service/GlobalDictItemNameResolverTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportCenterSmokeTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportRecordServiceTests.java`

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
  - `mvn -pl admin-mdm test -Dtest=DictServiceDuplicateKeyTests,GlobalDictItemNameResolverTests,ExportRecordServiceTests`
- `profile: task-4-export-smoke`
  - `mvn -pl admin-mdm test -Dtest=ExportCenterSmokeTests,DictModuleSmokeTests`
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

- Create: `admin-boot/src/main/java/com/example/admin/boot/config/SpringDocOperationIdConfig.java`
- Create: `admin-boot/src/main/java/com/example/admin/boot/config/EnumModelConverter.java`
- Modify: `admin-boot/src/main/java/com/example/admin/boot/config/OpenApiConfig.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/web/DynamicQueryContractIntegrationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/contract/ErrorCodeContractTests.java`
- Modify: `README.md`

**Shared Runtime Contracts**

- OpenAPI schema generation
- Stable operationId generation
- Dynamic query discriminator mappings
- Module dependency boundaries
- Error code uniqueness

**Invariants**

- `admin-boot` stays free of business logic.
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
  - `mvn -pl admin-boot -am install -DskipTests`
  - `mvn -pl admin-boot test -Dtest=OpenApiDocumentationTests,DynamicQueryContractIntegrationTests,ModuleBoundaryTests,ErrorCodeContractTests`
- `profile: task-5-full-local`
  - `mvn clean test`
  - `mvn compile`
- `profile: task-5-mysql-smoke`
  - `test -n "$JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD"`
  - `JAVA_ADMIN_STARTER_DATASOURCE_URL='jdbc:mysql://192.168.186.154:32425/basic_platform_sso?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' JAVA_ADMIN_STARTER_DATASOURCE_USERNAME=oig mvn -pl admin-boot spring-boot:run -Dspring-boot.run.profiles=dev`
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
