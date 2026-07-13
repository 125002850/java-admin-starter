# 文件导出中心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不新增顶层模块的前提下，为仓库落地文件导出中心能力，支持异步导出记录、生命周期管理、下载地址获取、手动删除、过期清理，以及可扩展的导出 SPI。

**Architecture:** `admin-core` 新增导出抽象契约，`admin-system` 提供基于现有文件能力的导出文件落盘适配，`admin-mdm` 承载导出记录表、状态机、编排与下载中心接口，`admin-{biz}` 未来通过 `ExportHandler` 接入具体导出场景。首期不接真实业务导出，而是在测试侧提供假场景完成端到端验证。

**Tech Stack:** Java 17, Spring Boot 3.3, MyBatis-Plus, Flyway, Apache POI, JUnit 5, MockMvc, H2, MySQL 8

---

## 1. 文件结构与职责

### `admin-core`

- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportScene.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportFileType.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportHandler.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/TableExportPayload.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/RenderedFile.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/StoredExportFile.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/FileRenderer.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportFileSink.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportFileLifecycle.java`

### `admin-system`

- Modify: `admin-system/src/main/java/com/example/admin/file/service/FileService.java`
- Create: `admin-system/src/main/java/com/example/admin/file/export/FileStorageExportFileSink.java`
- Create: `admin-system/src/main/java/com/example/admin/file/export/FileStorageExportFileLifecycle.java`
- Test: `admin-system/src/test/java/com/example/admin/file/FileStorageExportFileSinkTests.java`

### `admin-mdm`

- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportCenterErrorCode.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportRecordStatus.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportDeleteReason.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/config/ExportCenterProperties.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/config/ExportExecutionConfig.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/infra/entity/ExportRecordEntity.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/infra/mapper/ExportRecordMapper.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportRecordService.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportSceneRegistry.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExcelFileRenderer.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionDispatcher.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionWorker.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportCleanupScheduler.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/app/command/ExportSubmitCommand.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/app/ExportCoordinatorAppService.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/app/DownloadCenterAppService.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/ExportRecordController.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordPageReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordPageRspDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDetailReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDetailRspDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDeleteReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDownloadUrlReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDownloadUrlRspDTO.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportRecordServiceTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportSceneRegistryTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExcelFileRendererTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportCoordinatorAppServiceTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/DownloadCenterAppServiceTests.java`

### `admin-boot`

- Modify: `admin-boot/src/main/java/com/example/admin/boot/AdminBootApplication.java`
- Modify: `admin-boot/src/main/resources/application.yml`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/flyway/FlywaySmokeTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- Create: `admin-boot/src/main/resources/db/migration/V7__add_export_record_table.sql`
- Create: `admin-boot/src/test/java/com/example/admin/boot/export/TestExportSceneConfig.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/export/TestExportController.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/export/FileExportCenterModuleSmokeTests.java`

## 2. 任务拆分

### Task 1: 导出记录表与状态机

**Files:**
- Create: `admin-boot/src/main/resources/db/migration/V7__add_export_record_table.sql`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/flyway/FlywaySmokeTests.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportCenterErrorCode.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportRecordStatus.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/enums/ExportDeleteReason.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/infra/entity/ExportRecordEntity.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/infra/mapper/ExportRecordMapper.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportRecordService.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportRecordServiceTests.java`

- [ ] **Step 1: 先写 Flyway 与状态流转红灯**

在 `FlywaySmokeTests` 追加对 `V7` 和 `sys_export_record_global` 的断言；在 `ExportRecordServiceTests` 写状态流转失败用例：

```java
@Test
void flywayMigrationMatchesPlatformSchemaContract() {
    assertThat(hasSuccessfulMigration("7")).isTrue();
    assertThat(tableColumns("sys_export_record_global"))
        .contains("export_biz_code", "file_name", "status", "expire_time", "query_snapshot_json");
}

@Test
void markSuccess_should_reject_non_processing_record() {
    ExportRecordEntity entity = buildRecord(ExportRecordStatus.SUCCESS);
    assertThatThrownBy(() -> exportRecordService.markSuccess(entity.getId(), successPatch()))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("导出记录状态非法");
}
```

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-mdm,admin-boot -am test -Dtest=FlywaySmokeTests,ExportRecordServiceTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，缺少 `V7` 迁移、`ExportRecordService`、状态枚举和错误码。

- [ ] **Step 3: 写最小实现**

先落表与状态机，不引入编排逻辑。迁移和核心服务至少包含以下内容：

```sql
create table sys_export_record_global (
  id bigint primary key,
  export_biz_code varchar(64) not null,
  export_biz_name varchar(128) not null,
  file_name varchar(256) not null,
  file_type varchar(32) not null,
  content_type varchar(128) null,
  file_size bigint null,
  object_key varchar(256) null,
  storage_type varchar(32) null,
  status tinyint not null,
  finished_time datetime null,
  expire_time datetime not null,
  deleted_time datetime null,
  delete_reason tinyint null,
  fail_code varchar(64) null,
  fail_message varchar(255) null,
  query_snapshot_json longtext not null,
  query_snapshot_summary varchar(512) not null,
  download_count int not null default 0,
  last_download_time datetime null,
  last_download_by bigint null,
  expire_seconds int not null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  key idx_sys_export_record_global_creator_status_time (create_by, status, create_time),
  key idx_sys_export_record_global_status_expire_time (status, expire_time),
  key idx_sys_export_record_global_biz_code_time (export_biz_code, create_time)
);
```

```java
public enum ExportRecordStatus {
    PROCESSING(1),
    SUCCESS(2),
    FAILED(3),
    EXPIRED(4),
    DELETED(5);
}

@Service
public class ExportRecordService {

    @Transactional
    public Long createProcessingRecord(ExportRecordEntity entity) {
        entity.setStatus(ExportRecordStatus.PROCESSING.getCode());
        exportRecordMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void markSuccess(Long recordId, SuccessPatch patch) {
        ExportRecordEntity entity = getRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.PROCESSING);
        entity.setStatus(ExportRecordStatus.SUCCESS.getCode());
        entity.setObjectKey(patch.objectKey());
        entity.setContentType(patch.contentType());
        entity.setFileSize(patch.fileSize());
        entity.setStorageType(patch.storageType());
        entity.setFinishedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markFailed(Long recordId, String failCode, String failMessage) {
        ExportRecordEntity entity = getRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.PROCESSING);
        entity.setStatus(ExportRecordStatus.FAILED.getCode());
        entity.setFailCode(failCode);
        entity.setFailMessage(failMessage);
        entity.setFinishedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markExpired(Long recordId) {
        ExportRecordEntity entity = getRequired(recordId);
        if (entity.getStatus() == ExportRecordStatus.SUCCESS.getCode()) {
            entity.setStatus(ExportRecordStatus.EXPIRED.getCode());
            exportRecordMapper.updateById(entity);
        }
    }

    @Transactional
    public void markDeleted(Long recordId, ExportDeleteReason reason) {
        ExportRecordEntity entity = getRequired(recordId);
        if (entity.getStatus() == ExportRecordStatus.SUCCESS.getCode()
                || entity.getStatus() == ExportRecordStatus.EXPIRED.getCode()) {
            entity.setStatus(ExportRecordStatus.DELETED.getCode());
            entity.setDeleteReason(reason.getCode());
            entity.setDeletedTime(LocalDateTime.now());
            exportRecordMapper.updateById(entity);
        }
    }
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `mvn -q -pl admin-mdm,admin-boot -am test -Dtest=FlywaySmokeTests,ExportRecordServiceTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add admin-boot/src/main/resources/db/migration/V7__add_export_record_table.sql \
  admin-boot/src/test/java/com/example/admin/boot/flyway/FlywaySmokeTests.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/enums \
  admin-mdm/src/main/java/com/example/admin/mdm/export/infra \
  admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportRecordService.java \
  admin-mdm/src/test/java/com/example/admin/mdm/export/ExportRecordServiceTests.java
git commit -m "feat: add export record schema and state machine"
```

### Task 2: 导出抽象与文件落盘适配

**Files:**
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportScene.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportFileType.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportHandler.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/TableExportPayload.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/RenderedFile.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/StoredExportFile.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/FileRenderer.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportFileSink.java`
- Create: `admin-core/src/main/java/com/example/admin/core/export/ExportFileLifecycle.java`
- Modify: `admin-system/src/main/java/com/example/admin/file/service/FileService.java`
- Create: `admin-system/src/main/java/com/example/admin/file/export/FileStorageExportFileSink.java`
- Create: `admin-system/src/main/java/com/example/admin/file/export/FileStorageExportFileLifecycle.java`
- Test: `admin-system/src/test/java/com/example/admin/file/FileStorageExportFileSinkTests.java`

- [ ] **Step 1: 写 sink 红灯**

新增 `FileStorageExportFileSinkTests`，验证“非 `MultipartFile` 文件也能通过现有文件能力落盘”：

```java
@Test
void store_should_delegate_to_file_service_and_return_stored_export_file() {
    RenderedFile renderedFile = new RenderedFile(
        "report.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "hello".getBytes(StandardCharsets.UTF_8)
    );

    StoredExportFile storedFile = exportFileSink.store(renderedFile, "export/center", null);

    assertThat(storedFile.getObjectKey()).startsWith("export/center/");
    assertThat(storedFile.getFileName()).isEqualTo("report.xlsx");
}
```

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-system -am test -Dtest=FileStorageExportFileSinkTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，缺少导出抽象、`ExportFileSink` 实现，且 `FileService` 只有 `MultipartFile` 上传入口。

- [ ] **Step 3: 写最小实现**

`admin-core` 只放抽象，`admin-system` 补一个非 Web 上传入口并实现 sink：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportScene {
    String code();
    String name();
}

public interface ExportHandler<T> {
    TableExportPayload build(T payload);
}

public interface FileRenderer {
    ExportFileType supports();
    RenderedFile render(String fileName, TableExportPayload payload);
}

public interface ExportFileSink {
    StoredExportFile store(RenderedFile renderedFile, String bizPath, String objectKey);
}

public interface ExportFileLifecycle {
    String fetchDownloadUrl(String objectKey);
    void delete(String objectKey);
}
```

```java
public StoredFile store(InputStream inputStream,
                        String bizPath,
                        String objectKey,
                        String contentType,
                        long size,
                        String fileName) {
    String normalizedBizPath = normalizeBizPath(bizPath);
    String resolvedObjectKey = StringUtils.hasText(objectKey)
        ? normalizeObjectKey(objectKey)
        : generateObjectKey(normalizedBizPath, fileName);
    return fileStorageProvider.upload(inputStream, resolvedObjectKey, contentType, size, fileName);
}
```

```java
@Component
public class FileStorageExportFileSink implements ExportFileSink {

    @Override
    public StoredExportFile store(RenderedFile renderedFile, String bizPath, String objectKey) {
        try (InputStream inputStream = renderedFile.openStream()) {
            StoredFile storedFile = fileService.store(
                inputStream,
                bizPath,
                objectKey,
                renderedFile.getContentType(),
                renderedFile.getSize(),
                renderedFile.getFileName()
            );
            return new StoredExportFile(
                storedFile.getObjectKey(),
                storedFile.getOriginUrl(),
                storedFile.getFileName(),
                storedFile.getContentType(),
                storedFile.getSize()
            );
        } catch (IOException ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}

@Component
public class FileStorageExportFileLifecycle implements ExportFileLifecycle {

    @Override
    public String fetchDownloadUrl(String objectKey) {
        return fileService.fetchTempUrl(objectKey);
    }

    @Override
    public void delete(String objectKey) {
        try {
            fileService.delete(objectKey);
        } catch (BizException ex) {
            if (ex.getCode() != FileErrorCode.FILE_NOT_FOUND.getCode()) {
                throw ex;
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `mvn -q -pl admin-system -am test -Dtest=FileStorageExportFileSinkTests,LocalFileStorageProviderTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add admin-core/src/main/java/com/example/admin/core/export \
  admin-system/src/main/java/com/example/admin/file/service/FileService.java \
  admin-system/src/main/java/com/example/admin/file/export/FileStorageExportFileSink.java \
  admin-system/src/main/java/com/example/admin/file/export/FileStorageExportFileLifecycle.java \
  admin-system/src/test/java/com/example/admin/file/FileStorageExportFileSinkTests.java
git commit -m "feat: add export abstractions and file sink"
```

### Task 3: 导出场景注册、Excel 渲染与异步执行链路

**Files:**
- Modify: `admin-mdm/pom.xml`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/config/ExportCenterProperties.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/config/ExportExecutionConfig.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/app/command/ExportSubmitCommand.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportSceneRegistry.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExcelFileRenderer.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionDispatcher.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionWorker.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/app/ExportCoordinatorAppService.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportSceneRegistryTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExcelFileRendererTests.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/ExportCoordinatorAppServiceTests.java`

- [ ] **Step 1: 写注册与编排红灯**

用假 handler、假 sink 写三个失败测试：重复 `bizCode`、Excel 渲染、提交导出创建 `PROCESSING` 并异步回写成功。

```java
@Test
void registry_should_reject_duplicate_export_scene_code() {
    assertThatThrownBy(() -> new ExportSceneRegistry(List.of(new AHandler(), new BHandler())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("duplicate export scene code");
}

@Test
void submit_should_create_processing_record_and_mark_success_after_async_execution() {
    Long recordId = exportCoordinatorAppService.submit(command());
    awaitRecordStatus(recordId, ExportRecordStatus.SUCCESS);
}
```

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-mdm -am test -Dtest=ExportSceneRegistryTests,ExcelFileRendererTests,ExportCoordinatorAppServiceTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，缺少 POI 依赖、注册器、渲染器、提交命令和异步执行链路。

- [ ] **Step 3: 写最小实现**

在 `admin-mdm` 中补齐执行链路，首期只支持 Excel：

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
</dependency>
```

```java
@Component
public class ExportSceneRegistry {

    private final Map<String, SceneDefinition> scenes;

    public ExportSceneRegistry(List<ExportHandler<?>> handlers) {
        this.scenes = handlers.stream().collect(Collectors.toMap(
            this::resolveCode,
            this::toDefinition
        ));
    }

    public SceneDefinition getRequired(String bizCode) {
        SceneDefinition scene = scenes.get(bizCode);
        if (scene == null) {
            throw new BizException(ExportCenterErrorCode.EXPORT_SCENE_NOT_FOUND);
        }
        return scene;
    }
}
```

```java
@Component
public class ExcelFileRenderer implements FileRenderer {

    @Override
    public ExportFileType supports() {
        return ExportFileType.EXCEL;
    }

    @Override
    public RenderedFile render(String fileName, TableExportPayload payload) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("export");
            // 写表头与数据行
            workbook.write(outputStream);
            return RenderedFile.fromBytes(fileName, EXCEL_CONTENT_TYPE, outputStream.toByteArray());
        } catch (IOException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RENDER_FAILED);
        }
    }
}
```

```java
@Service
public class ExportCoordinatorAppService {

    @Transactional
    public <T> Long submit(ExportSubmitCommand<T> command) {
        ExportSceneRegistry.SceneDefinition scene = exportSceneRegistry.getRequired(command.getExportBizCode());
        Long recordId = exportRecordService.createProcessingRecord(buildProcessingRecord(scene, command));
        exportExecutionDispatcher.dispatch(recordId, scene, command.getPayload(), command.getOperatorId(), command.getOperatorName());
        return recordId;
    }
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `mvn -q -pl admin-mdm -am test -Dtest=ExportSceneRegistryTests,ExcelFileRendererTests,ExportCoordinatorAppServiceTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add admin-mdm/pom.xml \
  admin-mdm/src/main/java/com/example/admin/mdm/export/config \
  admin-mdm/src/main/java/com/example/admin/mdm/export/app/command/ExportSubmitCommand.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/app/ExportCoordinatorAppService.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportSceneRegistry.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExcelFileRenderer.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionDispatcher.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportExecutionWorker.java \
  admin-mdm/src/test/java/com/example/admin/mdm/export/ExportSceneRegistryTests.java \
  admin-mdm/src/test/java/com/example/admin/mdm/export/ExcelFileRendererTests.java \
  admin-mdm/src/test/java/com/example/admin/mdm/export/ExportCoordinatorAppServiceTests.java
git commit -m "feat: add export execution pipeline"
```

### Task 4: 下载中心接口、权限和过期清理

**Files:**
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/ExportRecordController.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordPageReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordPageRspDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDetailReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDetailRspDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDeleteReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDownloadUrlReqDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/controller/dto/ExportRecordDownloadUrlRspDTO.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/app/DownloadCenterAppService.java`
- Create: `admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportCleanupScheduler.java`
- Test: `admin-mdm/src/test/java/com/example/admin/mdm/export/DownloadCenterAppServiceTests.java`

- [ ] **Step 1: 写列表/下载/删除/过期红灯**

围绕三条规则写测试：默认列表只返回自己的 `PROCESSING/SUCCESS`；下载已过期记录时先转 `EXPIRED` 再拒绝；删除时同步删底层文件并转 `DELETED`。

```java
@Test
void pageMine_should_only_return_processing_and_success_by_default() {
    seedRecord(101L, ExportRecordStatus.PROCESSING);
    seedRecord(101L, ExportRecordStatus.SUCCESS);
    seedRecord(101L, ExportRecordStatus.FAILED);
    seedRecord(102L, ExportRecordStatus.SUCCESS);

    PageResult<ExportRecordPageRspDTO> result = downloadCenterAppService.pageMine(defaultPageReq());

    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getList()).extracting(ExportRecordPageRspDTO::getStatus)
        .containsExactly("PROCESSING", "SUCCESS");
}

@Test
void fetchDownloadUrl_should_mark_expired_before_throwing_when_expire_time_passed() {
    Long recordId = seedExpiredSuccessRecord(101L);

    assertThatThrownBy(() -> downloadCenterAppService.fetchDownloadUrl(new ExportRecordDownloadUrlReqDTO(recordId)))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("导出文件已过期");

    assertThat(loadRecord(recordId).getStatus()).isEqualTo(ExportRecordStatus.EXPIRED.getCode());
}

@Test
void delete_should_mark_deleted_when_sink_file_missing() {
    Long recordId = seedSuccessRecord(101L);
    willDoNothing().given(exportFileLifecycle).delete(anyString());

    downloadCenterAppService.deleteMine(new ExportRecordDeleteReqDTO(recordId));

    assertThat(loadRecord(recordId).getStatus()).isEqualTo(ExportRecordStatus.DELETED.getCode());
}
```

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-mdm -am test -Dtest=DownloadCenterAppServiceTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，缺少下载中心 AppService、Controller DTO 和过期清理任务。

- [ ] **Step 3: 写最小实现**

下载中心接口和服务遵循“本人权限 + 地址返回，不代理下载”：

```java
@Validated
@RestController
@Tag(name = "文件导出中心", description = "导出记录查询、下载地址获取、删除与生命周期管理接口")
@RequestMapping("/api/mdm/export/record")
public class ExportRecordController {

    @PostMapping("/page")
    public R<PageResult<ExportRecordPageRspDTO>> page(@Valid @RequestBody ExportRecordPageReqDTO reqDTO) {
        return R.ok(downloadCenterAppService.pageMine(reqDTO));
    }

    @PostMapping("/detail")
    public R<ExportRecordDetailRspDTO> detail(@Valid @RequestBody ExportRecordDetailReqDTO reqDTO) {
        return R.ok(downloadCenterAppService.detailMine(reqDTO));
    }

    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody ExportRecordDeleteReqDTO reqDTO) {
        downloadCenterAppService.deleteMine(reqDTO);
        return R.ok();
    }

    @PostMapping("/download-url/fetch")
    public R<ExportRecordDownloadUrlRspDTO> fetchDownloadUrl(@Valid @RequestBody ExportRecordDownloadUrlReqDTO reqDTO) {
        return R.ok(downloadCenterAppService.fetchDownloadUrl(reqDTO));
    }
}
```

```java
@Service
public class DownloadCenterAppService {

    @Transactional(readOnly = true)
    public PageResult<ExportRecordPageRspDTO> pageMine(ExportRecordPageReqDTO reqDTO) {
        Long operatorId = requireOperatorId();
        List<Integer> statuses = reqDTO.resolveStatusesOrDefaults(
            List.of(ExportRecordStatus.PROCESSING.getCode(), ExportRecordStatus.SUCCESS.getCode())
        );
        Page<ExportRecordEntity> page = exportRecordMapper.selectMineByStatuses(
            new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()),
            operatorId,
            statuses
        );
        return toPageResult(page);
    }

    @Transactional(readOnly = true)
    public ExportRecordDetailRspDTO detailMine(ExportRecordDetailReqDTO reqDTO) {
        return toDetailRsp(getOwnedRecord(reqDTO.getRecordId()));
    }

    @Transactional
    public ExportRecordDownloadUrlRspDTO fetchDownloadUrl(ExportRecordDownloadUrlReqDTO reqDTO) {
        ExportRecordEntity record = getOwnedRecord(reqDTO.getRecordId());
        if (record.getExpireTime().isBefore(LocalDateTime.now())) {
            exportRecordService.markExpired(record.getId());
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_EXPIRED);
        }
        String downloadUrl = exportFileLifecycle.fetchDownloadUrl(record.getObjectKey());
        // 更新 download_count / last_download_time / last_download_by
        return new ExportRecordDownloadUrlRspDTO(record.getId(), record.getFileName(), downloadUrl, record.getExpireTime());
    }

    @Transactional
    public void deleteMine(ExportRecordDeleteReqDTO reqDTO) {
        ExportRecordEntity record = getOwnedRecord(reqDTO.getRecordId());
        exportFileLifecycle.delete(record.getObjectKey());
        exportRecordService.markDeleted(record.getId(), ExportDeleteReason.MANUAL);
    }
}

@Component
public class ExportCleanupScheduler {

    @Scheduled(fixedDelayString = "${platform.export.cleanup-fixed-delay-ms:60000}")
    public void cleanupExpiredRecords() {
        List<ExportRecordEntity> records = exportRecordMapper.selectExpiredForCleanup(LocalDateTime.now());
        for (ExportRecordEntity record : records) {
            exportRecordService.markExpired(record.getId());
            try {
                exportFileLifecycle.delete(record.getObjectKey());
                exportRecordService.markDeleted(record.getId(), ExportDeleteReason.EXPIRE_CLEANUP);
            } catch (BizException ex) {
                log.warn("cleanup export record failed, recordId={}", record.getId(), ex);
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `mvn -q -pl admin-mdm -am test -Dtest=DownloadCenterAppServiceTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add admin-mdm/src/main/java/com/example/admin/mdm/export/controller \
  admin-mdm/src/main/java/com/example/admin/mdm/export/app/DownloadCenterAppService.java \
  admin-mdm/src/main/java/com/example/admin/mdm/export/service/ExportCleanupScheduler.java \
  admin-mdm/src/test/java/com/example/admin/mdm/export/DownloadCenterAppServiceTests.java
git commit -m "feat: add download center api"
```

### Task 5: Boot 接线、契约测试与端到端集成

**Files:**
- Modify: `admin-boot/src/main/java/com/example/admin/boot/AdminBootApplication.java`
- Modify: `admin-boot/src/main/resources/application.yml`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/export/TestExportSceneConfig.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/export/TestExportController.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/export/FileExportCenterModuleSmokeTests.java`

- [ ] **Step 1: 先写端到端红灯**

补 Boot 集成测试，使用测试专用假 handler 暴露一个测试导出入口，跑通提交流程、列表查询、下载地址、删除：

```java
@Test
void submit_export_should_create_record_then_expose_download_url_and_delete_flow() throws Exception {
    String response = mockMvc.perform(post("/api/test/export/create")
            .header("X-User-Id", "101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"keyword\":\"alpha\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andReturn().getResponse().getContentAsString();

    Long recordId = objectMapper.readTree(response).get("data").get("recordId").asLong();
    awaitStatus(recordId, "SUCCESS");

    mockMvc.perform(post("/api/mdm/export/record/download-url/fetch")
            .header("X-User-Id", "101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"recordId\":" + recordId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.downloadUrl").exists());
}
```

同时补 OpenAPI 与 ArchUnit 断言：

```java
.andExpect(content().string(containsString("/api/mdm/export/record/page")))
.andExpect(jsonPath("$.paths['/api/mdm/export/record/download-url/fetch'].post.tags[0]").value("文件导出中心"));
```

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-boot -am test -Dtest=OpenApiDocumentationTests,ModuleBoundaryTests,FileExportCenterModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，缺少 Boot 侧导出记录接线、测试场景 bean、OpenAPI 路径和调度/异步开关。

- [ ] **Step 3: 写最小实现**

补 Boot 接线和测试专用场景：

```java
@SpringBootApplication(scanBasePackages = "com.example.admin")
@MapperScan({
    "com.example.admin.mdm.infra.mapper",
    "com.example.admin.mdm.export.infra.mapper"
})
public class AdminBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminBootApplication.class, args);
    }
}
```

```yaml
platform:
  export:
    default-expire-seconds: ${EXPORT_CENTER_DEFAULT_EXPIRE_SECONDS:604800}
    cleanup-fixed-delay-ms: ${EXPORT_CENTER_CLEANUP_FIXED_DELAY_MS:60000}
    file-biz-path: ${EXPORT_CENTER_FILE_BIZ_PATH:export/center}
```

```java
@TestConfiguration
class TestExportSceneConfig {

    @Bean
    @ExportScene(code = "boot_test_export", name = "Boot测试导出")
    ExportHandler<TestExportPayload> bootTestExportHandler() {
        return payload -> new TableExportPayload(
            List.of("keyword"),
            List.of(List.of(payload.getKeyword()))
        );
    }
}
```

```java
@RestController
@RequestMapping("/api/test/export")
class TestExportController {

    @PostMapping("/create")
    public R<Map<String, Long>> create(@RequestBody TestExportPayload payload) {
        Long recordId = exportCoordinatorAppService.submit(
            ExportSubmitCommand.of("boot_test_export", "boot-test.xlsx", ExportFileType.EXCEL,
                "{\"keyword\":\"" + payload.getKeyword() + "\"}", "keyword=" + payload.getKeyword(), null,
                OperatorContext.getOperatorId(), OperatorContext.getOperatorName(), payload)
        );
        return R.ok(Map.of("recordId", recordId));
    }
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `mvn -q -pl admin-boot -am test -Dtest=OpenApiDocumentationTests,ModuleBoundaryTests,FileExportCenterModuleSmokeTests,FlywaySmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add admin-boot/src/main/java/com/example/admin/boot/AdminBootApplication.java \
  admin-boot/src/main/resources/application.yml \
  admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java \
  admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java \
  admin-boot/src/test/java/com/example/admin/boot/export
git commit -m "feat: wire export center into boot"
```

### Task 6: 全量验证与收尾

**Files:**
- Verify only: existing changed files from Task 1-5

- [ ] **Step 1: 跑模块级回归**

Run: `mvn -q -pl admin-core,admin-system,admin-mdm,admin-boot -am test`

Expected: PASS。

- [ ] **Step 2: 跑仓库标准验证**

Run: `mvn clean test && mvn test && mvn compile`

Expected: 三条命令全部 PASS。

- [ ] **Step 3: 跑真实 MySQL 8 迁移验证**

先启动仓库自带 MySQL，再覆盖测试数据源跑 Flyway 冒烟：

Run: `docker compose up -d`

Expected: MySQL 容器启动成功。

Run: `mvn -q -pl admin-boot -am test -Dtest=FlywaySmokeTests,FileExportCenterModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3307/java_admin_starter_export_it?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true -Dspring.datasource.username=root -Dspring.datasource.password=root`

Expected: PASS，`V7` 迁移在真实 MySQL 8 上可执行。

- [ ] **Step 4: 检查 migration 约束脚本**

Run: `./scripts/check-migrations.sh`

Expected: 无输出并返回 `0`。

- [ ] **Step 5: Commit**

```bash
git add admin-core admin-system admin-mdm admin-boot
git commit -m "feat: deliver file export center"
```

## 3. 计划自检

### Spec coverage

- 顶层模块边界：Task 2、Task 3、Task 4、Task 5 覆盖 `core/system/mdm` 分工。
- 导出记录表与生命周期：Task 1 覆盖。
- 导出 SPI、Excel 实现、文件落盘：Task 2、Task 3 覆盖。
- 下载中心列表、详情、删除、下载地址、留痕：Task 4 覆盖。
- OpenAPI、模块边界、真实 MySQL 迁移验证：Task 5、Task 6 覆盖。

### Placeholder scan

- 本计划未使用 `TBD`、`TODO`、`后续补充` 等占位表述。
- 每个任务均包含具体文件、验证命令和提交点。

### Type consistency

- 统一使用 `ExportFileType.EXCEL`、`ExportRecordStatus`、`ExportDeleteReason`。
- 下载中心统一使用 `recordId` 作为外部主键。
- 下载计数统一定义为“成功领取下载地址次数”。
