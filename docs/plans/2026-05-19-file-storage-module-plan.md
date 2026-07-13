# 文件存储模块（本地优先 + 七牛适配） Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.
>
> **注：** 仓库后续已将模块目录与 Maven `artifactId` 从 `admin-file` 调整为 `admin-system`，当前实现代码位于 `admin-system/src/main/java/com/example/admin/file`；下文保留当时的命名，便于追溯实施过程。

**Goal:** 为当前仓库新增一个不泄漏厂商语义、支持 `local` 与 `qiniu` 切换的文件存储模块，首期交付服务端上传、删除、临时访问地址和直传凭证四类能力。

**Architecture:** 新建独立业务模块 `admin-file`，对外只暴露统一的文件存储 API 与 DTO；Controller 严格走 `AppService -> Service -> Provider` 链路，不直接接触七牛 SDK。`local` 作为默认与测试基线实现，`qiniu` 作为可选 provider；厂商相关配置、异常和 URL 组装全部收敛在 `infra/provider/qiniu`，避免 `qiniu-java-sdk`、七牛 token、七牛异常或固定区域配置渗透到业务层。provider 基础能力与“直传凭证”扩展能力拆分为独立接口，避免后续 `minio` / `s3` 接入时 capability 判断继续膨胀；对象键日期生成显式使用可配置时区，而不是依赖部署机默认时区。

**Tech Stack:** Java 17, Spring Boot 3.3, Spring MVC Multipart, OpenAPI 3, JUnit 5, MockMvc, qiniu-java-sdk

---

## 1. 现状判断

- 当前仓库只有 `admin-boot`、`admin-core`、`admin-mdm` 三个模块，没有文件存储模块，也没有统一的上传抽象。
- 仓库规范要求 `admin-core` 保持“薄核心”，只放全局共享基础设施；对外接口必须统一走 `R.ok(...) / R.fail(...)`，业务异常必须统一使用 `BizException(ErrorCode)`。
- `README.md` 已明确“文件存储先做本地实现，有需要再补 MinIO，通过配置切换”，因此首期设计必须具备 provider 切换能力，不能把业务直接绑定到单一云厂商。
- 参考的 `qiniu-starter` 仓库虽然思路可借鉴，但存在以下边界问题，不适合直接引入：
  - 自动注册了固定路径的 `QiniuController`，会把 `/api/framework/qiniu/**` 暴露到当前应用。
  - 依赖外部框架的 `R`、`ServiceException`、工具类和 Swagger 2 注解，不符合当前仓库的响应与异常规范。
  - `QiniuTemplate` 里把区域写死为 `Region.huanan()`，不具备可配置性。
  - provider 抽象缺失，控制器和模板直接暴露了七牛语义，后续切换 `local`、`minio`、`s3` 的成本会变高。

## 2. 方案比较

### 方案 A：直接引入现成 `qiniu-starter`

- 优点：初始接入速度最快。
- 缺点：
  - 会引入不属于本仓库的控制器、响应模型、异常模型和注解体系。
  - API 路径、返回体、异常语义都不受当前仓库控制。
  - 后续切换本地、MinIO 或其他对象存储时，业务层改动面过大。

### 方案 B：在现有模块里直接使用 `qiniu-java-sdk`

- 优点：少一个中间层，依赖最直接。
- 缺点：
  - 七牛配置、对象键规则、URL 规则和异常翻译容易散落到 Controller / AppService / Service。
  - 当前仓库已经要求“先本地实现再按需切换”，直接在业务代码里使用 SDK 会把这个扩展点提前堵死。

### 方案 C：新建独立 `admin-file` 模块，统一接口 + provider 适配

- 优点：
  - 完全符合当前仓库的分层与 API 规范。
  - 首期可以同时支持 `local` 和 `qiniu`，并把 `qiniu-java-sdk` 约束在 provider 层。
  - 不需要新增数据库表，能用最小成本交付能力，同时保留后续切 MinIO 的演进空间。
- 缺点：首期设计与测试工作量高于直接引库。

**推荐方案：** 采用方案 C。实现自己的薄模块，参考 `qiniu-starter` 的“模板/配置”思路，但不直接复用它的控制器、请求响应对象和异常模型。

## 3. 范围与非目标

### 本期范围

- 新增独立模块 `admin-file`。
- 提供统一 API：
  - `POST /api/file/storage/object/upload`
  - `POST /api/file/storage/object/delete`
  - `POST /api/file/storage/object/temp-url/fetch`
  - `POST /api/file/storage/direct-upload/credential/fetch`
- 支持两种 provider：
  - `local`：默认实现，供本地开发、测试和无云依赖环境使用。
  - `qiniu`：按配置启用，用于生产或联调环境。
- 统一对象键生成规则、配置模型、错误码和异常翻译。
- 补齐 OpenAPI、ArchUnit、契约测试、README 配置说明。
- 为七牛 provider 补一条可手动启用的真实网络集成测试，作为上线前 gate，而不是日常单测的一部分。

### 明确不做

- 不新增文件元数据表、Flyway migration、Mapper、Entity。
- 不做分片上传、断点续传、图片压缩、水印、鉴黄、异步转码。
- 不在业务层暴露七牛 token、七牛 bucket、七牛异常等厂商概念；如果需要直传凭证，只通过中性 DTO 暴露。
- 不做鉴权、ACL、租户隔离、权限判定；如果未来需要“谁能访问哪个文件”，应在具体业务模块单独建模。
- 不把 `MultipartFile` 暴露进 provider 接口，避免 web 类型下沉到 infra。

## 4. 目标状态

### 4.1 新模块与职责

- 根 `pom.xml` 新增 `admin-file` 模块，并统一锁定七牛 SDK 版本属性。
- `admin-boot` 依赖 `admin-file`，新增 `file-storage` OpenAPI 分组。
- `admin-file` 只依赖 `admin-core` 与 Spring Web/Validation/OpenAPI，不依赖 `admin-mdm`、`admin-boot`。
- `admin-file` 结构如下：

```text
admin-file/
├── src/main/java/com/example/admin/file/
│   ├── controller/
│   │   ├── FileStorageController.java
│   │   └── dto/
│   ├── app/
│   │   └── FileAppService.java
│   ├── service/
│   │   ├── FileService.java
│   │   ├── StoredFile.java
│   │   └── DirectUploadCredential.java
│   ├── infra/
│   │   └── provider/
│   │       ├── FileStorageProvider.java
│   │       ├── local/
│   │       └── qiniu/
│   ├── config/
│   │   ├── FileStorageProperties.java
│   │   ├── StorageType.java
│   │   └── LocalFileStorageWebConfig.java
│   └── enums/
│       └── FileErrorCode.java
└── src/test/java/com/example/admin/file/
```

### 4.2 对外 API 约定

- `POST /api/file/storage/object/upload`
  - 请求：`multipart/form-data`
  - 字段：`file`、`bizPath`、`objectKey`（可选）
  - 返回：`R<StoredFileRspDTO>`
- `POST /api/file/storage/object/delete`
  - 请求：`DeleteFileReqDTO`
  - 返回：`R<Void>`
- `POST /api/file/storage/object/temp-url/fetch`
  - 请求：`FetchTempUrlReqDTO`
  - 返回：`R<FetchTempUrlRspDTO>`
- `POST /api/file/storage/direct-upload/credential/fetch`
  - 请求：`FetchDirectUploadCredentialReqDTO`
  - 返回：`R<FetchDirectUploadCredentialRspDTO>`

### 4.3 对象键与 URL 规则

- 统一对象键规则：`{bizPath}/{yyyy}/{MM}/{dd}/{uuid}{ext}`
- `bizPath` 只允许 `[A-Za-z0-9/_-]`，由服务层规范化，禁止 `..`、反斜杠和双斜杠，防止本地 provider 路径穿越。
- `local` provider：
  - 物理目录：`demo.file.storage.local.root-dir`
  - 访问前缀：`demo.file.storage.local.base-url`，例如 `/local-files`
  - `temp-url` 直接返回 `base-url/objectKey`
- 对象键日期生成时区：`demo.file.storage.zone-id`，默认 `Asia/Shanghai`
- `qiniu` provider：
  - 基础 URL：`demo.file.storage.qiniu.base-url`
  - 上传域名：`demo.file.storage.qiniu.upload-host`
  - 私有 bucket 时，`temp-url` 由 provider 按对象键签名生成

### 4.4 配置模型

```yaml
demo:
  file:
    storage:
      type: local
      zone-id: Asia/Shanghai
      local:
        root-dir: ${java.io.tmpdir}/java-admin-starter/uploads
        base-url: /local-files
      qiniu:
        access-key:
        secret-key:
        bucket-name:
        base-url:
        upload-host: https://upload.qiniup.com
        private-bucket: false
        region: huanan
        direct-upload-expire-seconds: 1800
        temp-url-expire-seconds: 600
```

## 5. 影响文件与职责

### 根模块与启动模块

- Modify: `pom.xml`
- Modify: `admin-boot/pom.xml`
- Modify: `admin-boot/src/main/java/com/example/admin/boot/config/OpenApiConfig.java`
- Modify: `admin-boot/src/main/resources/application.yml`
- Modify: `admin-boot/src/main/resources/application-dev.yml`
- Modify: `admin-boot/src/main/resources/application-test.yml`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/contract/ErrorCodeContractTests.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/file/FileStorageModuleSmokeTests.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/file/QiniuFileStorageProviderIT.java`

### 新模块 `admin-file`

- Create: `admin-file/pom.xml`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/FileStorageController.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/UploadFileReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/DeleteFileReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchDirectUploadCredentialReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/StoredFileRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchDirectUploadCredentialRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/app/FileAppService.java`
- Create: `admin-file/src/main/java/com/example/admin/file/service/FileService.java`
- Create: `admin-file/src/main/java/com/example/admin/file/service/StoredFile.java`
- Create: `admin-file/src/main/java/com/example/admin/file/service/DirectUploadCredential.java`
- Create: `admin-file/src/main/java/com/example/admin/file/enums/FileErrorCode.java`
- Create: `admin-file/src/main/java/com/example/admin/file/config/StorageType.java`
- Create: `admin-file/src/main/java/com/example/admin/file/config/FileStorageProperties.java`
- Create: `admin-file/src/main/java/com/example/admin/file/config/LocalFileStorageWebConfig.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/FileStorageProvider.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/local/LocalFileStorageProvider.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/qiniu/QiniuFileStorageProvider.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/qiniu/QiniuOperations.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/qiniu/DefaultQiniuOperations.java`
- Create: `admin-file/src/test/java/com/example/admin/file/LocalFileStorageProviderTests.java`
- Create: `admin-file/src/test/java/com/example/admin/file/QiniuFileStorageProviderTests.java`

### 文档

- Modify: `README.md`
- Create: `docs/plans/2026-05-19-file-storage-module-plan.md`

## 6. 分阶段任务

### Task 1: 锁定模块边界与 API 契约

**Files:**
- Modify: `pom.xml`
- Modify: `admin-boot/pom.xml`
- Modify: `admin-boot/src/main/java/com/example/admin/boot/config/OpenApiConfig.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/contract/ErrorCodeContractTests.java`
- Create: `admin-file/pom.xml`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/FileStorageController.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/UploadFileReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/DeleteFileReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchDirectUploadCredentialReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/StoredFileRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchDirectUploadCredentialRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/app/FileAppService.java`

- [ ] **Step 1: 先写契约红灯**

在 `OpenApiDocumentationTests.java` 增加对新接口与分组的断言：

- `/v3/api-docs` 必须包含：
  - `/api/file/storage/object/upload`
  - `/api/file/storage/object/delete`
  - `/api/file/storage/object/temp-url/fetch`
  - `/api/file/storage/direct-upload/credential/fetch`
- `/v3/api-docs/file-storage` 必须可访问，且 tag 为“文件存储”。

在 `ModuleBoundaryTests.java` 增加边界约束：

- `admin-file` 只能依赖 `admin-core`，不能依赖 `admin-boot`、`admin-mdm`
- `admin-core`、`admin-mdm` 不能依赖 `com.example.admin.file..`
- 不允许任何 Controller 暴露 `/api/framework/qiniu/**`

在 `ErrorCodeContractTests.java` 把源码扫描根目录补充 `admin-file/src/main/java`，确保新模块也受“错误码唯一”和“禁止直接 new R / throw RuntimeException”约束。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-boot -am test -Dtest=OpenApiDocumentationTests,ModuleBoundaryTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前仓库还不存在 `admin-file` 模块、OpenAPI 分组和新增路径。

- [ ] **Step 3: 搭模块骨架并完成 Boot 装配**

改动要求：

- 根 `pom.xml`
  - 新增 `<module>admin-file</module>`
  - 新增明确版本属性，例如 `qiniu.sdk.version`
- `admin-boot/pom.xml`
  - 新增对 `admin-file` 的依赖
- `OpenApiConfig.java`
  - 新增 `GroupedOpenApi fileStorageApi()`，分组名固定为 `file-storage`
  - `packagesToScan("com.example.admin.file.controller")`
  - `pathsToMatch("/api/file/storage/**")`
- `admin-file/pom.xml`
  - 依赖 `admin-core`
  - 依赖 `spring-boot-starter-web`
  - 依赖 `spring-boot-starter-validation`
  - 依赖 `swagger-annotations-jakarta`
  - 依赖 `qiniu-java-sdk`
  - 依赖 `spring-boot-starter-test`
- `FileStorageController` 与 DTO 先以“可编译、可出 OpenAPI”为目标提供最小骨架
  - 允许在 Task 1 先返回占位结构或由 `FileAppService` 抛 `UnsupportedOperationException`
  - 只要求路径、OpenAPI tag、请求对象、响应对象先稳定暴露出来
  - 真正的本地/七牛实现放到 Task 2 / Task 3

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl admin-boot -am test -Dtest=OpenApiDocumentationTests,ModuleBoundaryTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，新增模块已被 Maven 与 Boot 识别，OpenAPI 分组与架构边界测试通过。

- [ ] **Step 5: Commit**

Commit message: `feat: scaffold file storage module`

### Task 2: 落地本地 provider 与统一上传/删除/临时地址链路

**Files:**
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/FileStorageController.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/dto/UploadFileReqDTO.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/dto/DeleteFileReqDTO.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlReqDTO.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/dto/StoredFileRspDTO.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchTempUrlRspDTO.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/app/FileAppService.java`
- Create: `admin-file/src/main/java/com/example/admin/file/service/FileService.java`
- Create: `admin-file/src/main/java/com/example/admin/file/service/StoredFile.java`
- Create: `admin-file/src/main/java/com/example/admin/file/enums/FileErrorCode.java`
- Create: `admin-file/src/main/java/com/example/admin/file/config/StorageType.java`
- Create: `admin-file/src/main/java/com/example/admin/file/config/FileStorageProperties.java`
- Create: `admin-file/src/main/java/com/example/admin/file/config/LocalFileStorageWebConfig.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/FileStorageProvider.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/DirectUploadCapable.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/local/LocalFileStorageProvider.java`
- Create: `admin-file/src/test/java/com/example/admin/file/LocalFileStorageProviderTests.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/file/FileStorageModuleSmokeTests.java`
- Modify: `admin-boot/src/main/resources/application.yml`
- Modify: `admin-boot/src/main/resources/application-dev.yml`
- Modify: `admin-boot/src/main/resources/application-test.yml`

- [ ] **Step 1: 先写本地模式失败测试**

`admin-boot/src/test/java/com/example/admin/boot/file/FileStorageModuleSmokeTests.java` 至少覆盖以下场景：

- `object/upload` 在 `local` 模式下可成功保存文件，并返回 `objectKey` 与 `originUrl`
- `object/temp-url/fetch` 在 `local` 模式下返回 `base-url/objectKey`
- `object/delete` 删除成功后，物理文件不存在
- `object/upload` 对空文件、非法 `bizPath` 返回业务错误

这些烟雾测试必须放在 `admin-boot`，因为它是当前仓库唯一装配完整 Spring 上下文的模块；不要把 `MockMvc + @SpringBootTest` 放在 `admin-file/src/test` 里赌配置类自动发现。

建议用 `MockMvc` + 临时目录做烟雾测试，测试属性固定为：

```properties
demo.file.storage.type=local
demo.file.storage.local.root-dir=${java.io.tmpdir}/java-admin-starter-file-tests
demo.file.storage.local.base-url=/local-files
```

`LocalFileStorageProviderTests.java` 只测 provider 细节：

- `resolve(rootDir, objectKey)` 必须防路径穿越
- 上传后目录自动创建
- 删除不存在文件时抛 `BizException(FileErrorCode.FILE_NOT_FOUND)`

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-boot -am test -Dtest=FileStorageModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，端到端上传链路尚未实现。

Run: `mvn -q -pl admin-file -am test -Dtest=LocalFileStorageProviderTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，provider 与配置尚未实现。

- [ ] **Step 3: 写最小实现**

实现要求：

- `FileStorageController`
  - `@RequestMapping("/api/file/storage")`
  - 类上 `@Tag(name = "文件存储", description = "文件上传、删除、临时访问地址与直传凭证相关接口")`
  - 所有方法返回 `R.ok(...)` 或 `R.ok()`
- `UploadFileReqDTO`
  - 只承载 `bizPath`、`objectKey`
  - `bizPath` 用 `@Pattern` 收紧格式
  - 上传接口优先使用 `@RequestPart("file") MultipartFile file` + `@Valid @RequestPart("request") UploadFileReqDTO reqDTO`
  - 在实现阶段必须用 `OpenApiDocumentationTests` 或新增断言验证 Swagger UI/OpenAPI 中 `file` 被渲染为真正的二进制文件字段，而不是普通 string
- `FileService`
  - 负责规范化 `bizPath`
  - 负责在 `objectKey` 为空时生成对象键
  - 不允许把 `MultipartFile` 下沉进 provider 接口；provider 接口应接收 `InputStream`、`contentType`、`size`、`objectKey`
- `FileStorageProvider`
  - 统一基础方法：`upload`、`delete`、`buildOriginUrl`、`buildTempUrl`
- `DirectUploadCapable`
  - 独立扩展接口：`fetchDirectUploadCredential`
  - `FileService` 通过 `instanceof DirectUploadCapable` 判断当前 provider 是否支持直传凭证
- `LocalFileStorageProvider`
  - 只在 `demo.file.storage.type=local` 时注册
  - 路径解析必须使用 `Path.normalize()`，并校验最终路径仍位于 `rootDir` 下
  - `temp-url` 直接返回 `buildOriginUrl(objectKey)`
- `LocalFileStorageWebConfig`
  - 将 `local.base-url/**` 映射到 `file:<rootDir>/`
  - 仅在 `local` 模式启用
- `FileErrorCode`
  - 新模块单独号段，建议使用 `3002xxx`
  - 至少包括：空文件、对象键非法、文件不存在、上传失败、删除失败、临时地址生成失败、直传凭证不支持、配置非法

对象键生成逻辑建议直接写在 `FileService`：

```java
String generatedObjectKey =
    normalizedBizPath
        + "/"
        + LocalDate.now(ZoneId.of(fileStorageProperties.getZoneId()))
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        + "/"
        + UUID.randomUUID()
        + extension;
```

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl admin-boot -am test -Dtest=FileStorageModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，本地模式下端到端上传、临时地址、删除与错误返回测试通过。

Run: `mvn -q -pl admin-file -am test -Dtest=LocalFileStorageProviderTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，本地 provider 的路径安全测试全部通过。

- [ ] **Step 5: Commit**

Commit message: `feat: add local file storage provider`

### Task 3: 落地七牛 provider 与直传凭证抽象

**Files:**
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchDirectUploadCredentialReqDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/controller/dto/FetchDirectUploadCredentialRspDTO.java`
- Create: `admin-file/src/main/java/com/example/admin/file/service/DirectUploadCredential.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/qiniu/QiniuFileStorageProvider.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/qiniu/QiniuOperations.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/qiniu/DefaultQiniuOperations.java`
- Create: `admin-file/src/main/java/com/example/admin/file/infra/provider/DirectUploadCapable.java`
- Create: `admin-file/src/test/java/com/example/admin/file/QiniuFileStorageProviderTests.java`
- Create: `admin-boot/src/test/java/com/example/admin/boot/file/QiniuFileStorageProviderIT.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/service/FileService.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/config/FileStorageProperties.java`
- Modify: `admin-file/src/main/java/com/example/admin/file/controller/FileStorageController.java`

- [ ] **Step 1: 先写七牛模式失败测试**

`QiniuFileStorageProviderTests.java` 不连真实网络，只测 provider 与 SDK 适配层之间的行为。建议 `QiniuFileStorageProvider` 依赖 `QiniuOperations` 接口，再在测试里用 Mockito stub 这个接口。

至少覆盖：

- `direct-upload/credential/fetch`
  - 返回 `provider=qiniu`
  - 返回 `credential`、`uploadHost`、`objectKey`、`originUrl`
- `object/upload`
  - 上传成功时返回 `StoredFile`
  - SDK 抛错时翻译成 `BizException(FileErrorCode.FILE_UPLOAD_FAILED)`
- `object/temp-url/fetch`
  - `privateBucket=false` 时直接返回 `originUrl`
  - `privateBucket=true` 时调用签名方法生成带过期时间的 URL
- `object/delete`
  - SDK 抛错时翻译成 `BizException(FileErrorCode.FILE_DELETE_FAILED)`

另外在 `FileStorageModuleSmokeTests.java` 增加一条本地模式断言：

- `direct-upload/credential/fetch` 在 `local` 模式下返回 `3002xxx` 业务错误，而不是 `500`

再补一条手动真实网络集成测试 `admin-boot/src/test/java/com/example/admin/boot/file/QiniuFileStorageProviderIT.java`：

- `@SpringBootTest`
- `@ActiveProfiles("qiniu-it")`
- `@EnabledIfEnvironmentVariable(named = "RUN_QINIU_IT", matches = "true")`
- 使用真实七牛配置完成：
  - 上传真实小文件并校验 `originUrl`
  - 私有 bucket 场景获取临时地址
  - 删除真实文件并确认再次访问失败或对象不存在

这条测试是“上线前 gate”，不是日常 `mvn test` 的默认内容；测试类命名为 `*IT`，并通过 Surefire/Failsafe 或显式 `-Dtest` 手动触发。

- [ ] **Step 2: 运行红灯**

Run: `mvn -q -pl admin-file -am test -Dtest=QiniuFileStorageProviderTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，当前还没有七牛 provider、直传凭证 DTO 与错误翻译。

Run: `mvn -q -pl admin-boot -am test -Dtest=FileStorageModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，本地模式下直传凭证错误语义尚未收敛。

- [ ] **Step 3: 写最小实现**

实现要求：

- `FileService.fetchDirectUploadCredential(...)`
  - 只调用统一基础 provider
  - 如果当前 provider 未实现 `DirectUploadCapable`，抛 `BizException(FileErrorCode.DIRECT_UPLOAD_UNSUPPORTED)`
- `QiniuOperations`
  - 只暴露必要动作：`upload`、`delete`、`createUploadToken`、`createPrivateDownloadUrl`
  - 把 SDK 细节留在 `DefaultQiniuOperations`
- `QiniuFileStorageProvider`
  - 只在 `demo.file.storage.type=qiniu` 时注册
  - 实现 `DirectUploadCapable`
  - 区域必须来自配置，禁止写死 `Region.huanan()`
  - `originUrl` 必须由 `qiniu.base-url + "/" + objectKey` 组装
  - 统一把厂商异常翻译成 `BizException(FileErrorCode.*)`

区域映射建议写成显式 `switch`，避免魔法字符串到处散落：

```java
private Region resolveRegion(String region) {
    return switch (region) {
        case "huadong" -> Region.huadong();
        case "huabei" -> Region.huabei();
        case "huanan" -> Region.huanan();
        case "beimei" -> Region.beimei();
        case "xinjiapo" -> Region.xinjiapo();
        default -> throw new BizException(FileErrorCode.FILE_STORAGE_CONFIG_INVALID, "未知七牛区域: " + region);
    };
}
```

- [ ] **Step 4: 跑绿灯**

Run: `mvn -q -pl admin-file -am test -Dtest=QiniuFileStorageProviderTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，七牛 provider 的单元测试通过。

Run: `mvn -q -pl admin-boot -am test -Dtest=FileStorageModuleSmokeTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，本地模式下直传凭证不支持的错误语义正确。

Run: `RUN_QINIU_IT=true mvn -q -pl admin-boot -am test -Dtest=QiniuFileStorageProviderIT -Dsurefire.failIfNoSpecifiedTests=false -Dspring.profiles.active=qiniu-it`

Expected: PASS，真实七牛环境下上传、临时地址、删除链路通过。

- [ ] **Step 5: Commit**

Commit message: `feat: add qiniu file storage provider`

### Task 4: 收口文档、配置与全量验收

**Files:**
- Modify: `README.md`
- Modify: `admin-boot/src/main/resources/application.yml`
- Modify: `admin-boot/src/main/resources/application-dev.yml`
- Modify: `admin-boot/src/main/resources/application-test.yml`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/openapi/OpenApiDocumentationTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/archunit/ModuleBoundaryTests.java`
- Modify: `admin-boot/src/test/java/com/example/admin/boot/contract/ErrorCodeContractTests.java`

- [ ] **Step 1: 补 README 与默认配置**

README 必须补齐：

- 新模块 `admin-file` 的职责说明
- `local` / `qiniu` provider 切换方式
- 本期不落库、不新增 migration 的原因
- 四个新增接口的用途
- `local` 模式下文件访问前缀说明

配置要求：

- `application.yml`
  - 增加 `demo.file.storage.type=local`
  - 增加 `demo.file.storage.zone-id=Asia/Shanghai`
  - 增加默认 `local.root-dir`、`local.base-url`
- `application-dev.yml`
  - 可以沿用默认值，或显式覆盖到固定开发目录
- `application-test.yml`
  - 指向隔离测试目录，避免污染真实开发目录
  - 不要误配 `qiniu` 真实密钥
- `application-qiniu-it.yml` 或等效测试配置
  - 仅服务于手动七牛集成测试
  - 从环境变量读取 `access-key`、`secret-key`、`bucket-name`、`base-url`、`region`

- [ ] **Step 2: 跑 OpenAPI 与契约回归**

Run: `mvn -q -pl admin-boot -am test -Dtest=OpenApiDocumentationTests,ModuleBoundaryTests,ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，新增接口已进入 OpenAPI，架构边界与错误码契约仍成立。

- [ ] **Step 3: 跑全量测试**

Run: `mvn -q test`

Expected: PASS，全仓没有因为 `admin-file` 引入新的架构违规、配置缺失或测试污染。

- [ ] **Step 4: Commit**

Commit message: `docs: document file storage module`

## 7. 评审重点

- provider 接口是否保持中性，是否没有把 `MultipartFile`、七牛 SDK 类型或七牛异常泄漏到 `service` / `app` / `controller`
- `local` provider 是否正确处理路径穿越、目录创建、删除不存在文件和内容类型
- `qiniu` provider 是否完全通过配置驱动，是否避免了写死 `Region.huanan()`
- `QiniuFileStorageProviderIT` 是否真的覆盖了真实网络、真实签名和真实删除回路
- `direct-upload/credential/fetch` 是否是“中性凭证接口”，而不是把字段命名成 `qiniuTokenRsp`
- 上传接口在 Swagger UI 中是否把 `file` 渲染为文件上传控件，而不是普通 string
- 对象键日期是否显式使用 `demo.file.storage.zone-id`，而不是依赖系统默认时区
- `ErrorCodeContractTests` 是否已经把 `admin-file` 纳入扫描
- `README` 与 OpenAPI 是否准确描述了“本期不落库”的设计决定

## 8. 最终验收清单

- `admin-file` 模块独立存在，且不依赖 `admin-boot` / `admin-mdm`
- 本地模式下，上传、删除、临时地址获取可用
- 七牛模式下，上传、删除、临时地址、直传凭证由 provider 提供，且不需要真实网络即可通过单测
- 七牛模式下，存在可手动启用的真实网络集成测试，并作为上线前验收项执行通过
- 所有对外响应都走 `R.ok(...) / R.fail(...)`
- 所有业务异常都走 `BizException(ErrorCode)`
- 仓库中不存在 `/api/framework/qiniu/**` 之类厂商路径
- 本期没有新增 Flyway migration、Entity、Mapper、数据库表
- `mvn -q test` 通过
