# 文件导出中心设计

## 1. 背景

当前仓库已经具备 `demo-system/file` 文件存储能力，支持通过统一接口将文件写入 `local`、`qiniu`、`minio` provider，并提供删除、临时下载地址、直传凭证等能力。

现阶段缺失的是一层与具体业务解耦的“文件导出中心”能力，用于统一管理业务导出文件的记录、状态、有效期、下载、删除和过期清理。该能力不负责承载具体业务查询，也不直接依赖对象存储厂商 SDK。

本设计的目标是：

- 为跨业务复用的导出记录与生命周期管理提供统一平台能力。
- 将“导出内容生成”和“导出记录编排”解耦，避免平台层侵入具体业务逻辑。
- 复用现有 `demo-system/file` 能力完成文件落盘，不重复建设文件存储逻辑。

## 2. 目标与非目标

### 2.1 目标

- 建立统一的导出记录模型，覆盖 `PROCESSING`、`SUCCESS`、`FAILED`、`EXPIRED`、`DELETED` 全生命周期。
- 支持业务模块异步发起导出，并立即获得导出记录 ID。
- 支持按导出场景扩展业务导出处理器，首期文件格式只实现 Excel。
- 支持下载中心列表、详情、删除、获取下载地址、下载留痕、过期清理。
- 支持全局默认有效期，同时允许业务发起导出时显式传入本次有效期；最终实际有效期必须落表。
- 首期权限边界只支持“本人可见、本人可删、本人可下载”。

### 2.2 非目标

- 不在本期引入真实业务导出场景。
- 不建设统一的通用 HTTP 导出入口；具体业务仍由各自模块暴露导出接口。
- 不实现 SSE、WebSocket 或轮询通知策略；导出完成后的客户端通知机制后续按前端需要再定。
- 不实现下载中心全局管理员能力。
- 不实现代理流式下载；首期仅返回可下载地址。
- 不实现导出记录归档与历史清理；`FAILED`、`EXPIRED`、`DELETED` 记录首期长期保留。
- 不实现重试旧记录；重试应表现为再次新建一条导出记录。

## 3. 顶层模块边界

本设计遵循仓库顶层模块边界约定，不新增 `demo-export` 模块。

### 3.1 `demo-core`

放置与具体业务解耦的导出原生抽象：

- `@ExportScene`
- `ExportHandler`
- `ExportPayload`
- `FileRenderer`
- `RenderedFile`
- `ExportFileSink`

`demo-core` 只定义契约和抽象，不落导出记录表，不承载状态编排。

### 3.2 `demo-system`

继续承载统一文件存储能力。导出框架如需将渲染结果写入对象存储，必须通过 `demo-system/file` 提供的统一文件能力完成，不直接接触厂商 SDK。

### 3.3 `demo-mdm`

承载跨业务复用、带明确业务语义的平台型业务能力。本期文件导出中心及导出编排落在 `demo-mdm`，包括：

- 导出记录表及其 Entity / Mapper
- 导出记录状态流转服务
- 下载中心 AppService 与 Controller
- 导出提交流程编排
- 异步执行入口与过期清理任务

### 3.4 `demo-{biz}`

承载具体业务导出实现。每个业务模块通过实现 `ExportHandler` 来提供：

- 导出参数解释
- 业务数据查询
- 列定义与导出内容构建

业务模块不直接操作导出记录表，也不直接调用对象存储 provider。

## 4. 核心设计原则

- 导出中心只管理“导出记录与文件生命周期”，不承载具体业务查询。
- 导出执行与导出状态编排解耦，避免单个服务同时承担业务查询、文件渲染、状态更新、文件存储四类职责。
- 业务导出通过 `bizCode` 接入平台，平台层只识别标准化快照、导出结果和状态，不识别具体业务领域对象。
- 下载中心返回可下载地址，而不是代理文件流；因此下载计数的语义定义为“成功领取下载地址次数”。
- 表里的业务状态 `DELETED` 与数据库逻辑删除字段 `deleted` 不是同一概念。首期记录长期保留，数据库逻辑删除默认不使用。

## 5. 业务生命周期

### 5.1 状态定义

- `PROCESSING`：导出记录已创建，后台导出尚未完成。
- `SUCCESS`：导出文件已生成并写入文件存储，可下载、可手动删除。
- `FAILED`：导出失败，不可下载，不支持直接转成功。
- `EXPIRED`：记录已过有效期，禁止下载，等待过期清理删除底层文件。
- `DELETED`：底层文件已物理删除完成，记录保留用于审计。

### 5.2 状态流转

只允许以下状态迁移：

- `PROCESSING -> SUCCESS`
- `PROCESSING -> FAILED`
- `SUCCESS -> EXPIRED`
- `SUCCESS -> DELETED`
- `EXPIRED -> DELETED`

明确禁止：

- `FAILED -> SUCCESS`
- `DELETED -> SUCCESS`
- `EXPIRED -> SUCCESS`
- 任意状态直接回滚为 `PROCESSING`

## 6. 数据模型

主表建议命名为 `sys_export_record_global`。

### 6.1 字段设计

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `bigint` | 主键 |
| `export_biz_code` | `varchar(64)` | 导出业务场景编码 |
| `export_biz_name` | `varchar(128)` | 导出业务场景名称快照 |
| `file_name` | `varchar(256)` | 导出文件名 |
| `file_type` | `varchar(32)` | 文件类型，首期为 `EXCEL` |
| `content_type` | `varchar(128)` | 文件 MIME 类型 |
| `file_size` | `bigint` | 文件大小，单位字节 |
| `object_key` | `varchar(256)` | 文件存储对象键 |
| `storage_type` | `varchar(32)` | 文件存储 provider 类型快照 |
| `status` | `tinyint` | 业务状态 |
| `finished_time` | `datetime` | 导出结束时间，成功或失败时写入 |
| `expire_time` | `datetime` | 过期时间 |
| `deleted_time` | `datetime` | 业务删除完成时间 |
| `delete_reason` | `tinyint` | 删除原因：手动删除或过期清理 |
| `fail_code` | `varchar(64)` | 标准化失败码 |
| `fail_message` | `varchar(255)` | 面向用户的失败摘要 |
| `query_snapshot_json` | `longtext` | 导出参数 JSON 快照 |
| `query_snapshot_summary` | `varchar(512)` | 导出参数摘要 |
| `download_count` | `int` | 成功领取下载地址次数 |
| `last_download_time` | `datetime` | 最近一次领取下载地址时间 |
| `last_download_by` | `bigint` | 最近一次领取下载地址的用户 ID |
| `expire_seconds` | `int` | 本次导出实际采用的有效期秒数 |
| `create_time` | `datetime` | 创建时间 |
| `update_time` | `datetime` | 更新时间 |
| `create_by` | `bigint` | 创建人 |
| `update_by` | `bigint` | 更新人 |
| `deleted` | `bigint` | 逻辑删除字段，首期默认为 `0` |

### 6.2 字段约束

- `PROCESSING` 状态下允许 `content_type`、`file_size`、`object_key`、`storage_type` 为空。
- `SUCCESS` 状态下 `content_type`、`file_size`、`object_key`、`storage_type` 必须齐备。
- `FAILED` 状态下 `fail_code`、`fail_message`、`finished_time` 必须齐备。
- `expire_seconds` 记录本次导出的最终有效期取值，不依赖后续全局配置变更。
- `export_biz_name`、`query_snapshot_summary` 为历史快照，不随代码中的名称调整而回溯变更。

### 6.3 索引建议

- `idx_export_record_creator_status_time (create_by, status, create_time)`
- `idx_export_record_status_expire_time (status, expire_time)`
- `idx_export_record_biz_code_time (export_biz_code, create_time)`

## 7. 平台服务拆分

### 7.1 `demo-core` 抽象

#### `@ExportScene`

用于在业务 `ExportHandler` 上声明：

- `code`
- `name`

#### `ExportHandler`

业务扩展点，仅负责：

- 解释业务导出参数
- 查询业务数据
- 产出标准化 `ExportPayload`

不负责：

- 更新导出记录状态
- 调用对象存储
- 直接写 HTTP 响应

#### `ExportPayload`

导出中间模型。首期只需支撑表格型导出数据，后续可扩展到 CSV、PDF 等其他渲染模式。

#### `FileRenderer`

负责将 `ExportPayload` 渲染为具体文件。首期实现 `ExcelFileRenderer`。

#### `RenderedFile`

渲染结果对象，包含：

- `fileName`
- `contentType`
- `size`
- 文件内容流或字节数据

#### `ExportFileSink`

统一导出文件落盘抽象。首期由 `demo-system` 基于现有文件能力提供实现。

### 7.2 `demo-system` 适配

提供 `FileStorageExportFileSink`，内部复用现有 `FileService` 完成文件上传，并回传：

- `objectKey`
- `contentType`
- `fileSize`
- `storageType`

### 7.3 `demo-mdm` 平台服务

#### `ExportCoordinatorAppService`

职责：

- 接收平台统一提交命令
- 创建 `PROCESSING` 记录
- 投递异步执行任务
- 立即返回导出记录 ID

#### `DownloadCenterAppService`

职责：

- 导出记录分页查询
- 详情查询
- 删除
- 获取下载地址
- 下载留痕更新

#### `ExportRecordService`

职责：

- 封装状态流转规则
- `markSuccess`
- `markFailed`
- `markExpired`
- `markDeleted`

#### `ExportExecutionDispatcher`

`@Async` 异步执行入口，只负责将已创建的导出记录交给后台执行。

#### `ExportExecutionWorker`

职责：

- 按 `bizCode` 查找 `ExportHandler`
- 执行导出内容构建
- 调用 `FileRenderer` 渲染文件
- 调用 `ExportFileSink` 落盘
- 回写成功或失败状态

#### `ExportSceneRegistry`

启动时扫描并注册所有 `ExportHandler`，校验：

- `bizCode` 唯一
- `bizName` 非空
- 每个 `bizCode` 均有可用 handler

## 8. 提交流程与执行链路

### 8.1 导出提交

业务模块继续自行暴露 HTTP 导出入口，例如：

- `/api/{biz}/export/{action}`

Controller 收到业务 `ReqDTO` 后，不直接操作下载中心表，而是组装平台统一命令并调用 `ExportCoordinatorAppService.submit(...)`。

### 8.2 平台统一提交命令

提交命令包含：

- `exportBizCode`
- `fileName`
- `fileType`
- `querySnapshotJson`
- `querySnapshotSummary`
- `expireSeconds`
- `operatorId`
- `operatorName`
- `payload`

其中 `payload` 为业务模块标准化后的导出参数对象，而不是原始 HTTP 请求对象。

### 8.3 执行链路

1. 业务模块调用 `ExportCoordinatorAppService.submit(...)`
2. 平台创建 `PROCESSING` 记录
3. `ExportExecutionDispatcher` 异步执行导出
4. `ExportExecutionWorker` 根据 `bizCode` 找到对应 `ExportHandler`
5. `ExportHandler` 产出 `ExportPayload`
6. `ExcelFileRenderer` 生成 `RenderedFile`
7. `FileStorageExportFileSink` 调用 `demo-system/file` 落盘
8. 平台回写 `SUCCESS` 或 `FAILED`

## 9. 下载中心接口设计

首期下载中心 HTTP 接口放在 `demo-mdm`。

### 9.1 分页查询

- `POST /api/mdm/export/record/page`

规则：

- 默认只返回当前用户自己的 `PROCESSING`、`SUCCESS`
- `FAILED`、`EXPIRED`、`DELETED` 需显式状态筛选后才返回

### 9.2 详情

- `POST /api/mdm/export/record/detail`

返回内容：

- 状态
- 文件信息
- 有效期
- 快照摘要
- 失败摘要
- 下载留痕

### 9.3 删除

- `POST /api/mdm/export/record/delete`

规则：

- 只允许删除当前用户自己的 `SUCCESS` 记录
- 同步删除底层文件
- 成功后状态转 `DELETED`
- 如果底层文件已不存在，也视为删除成功并转 `DELETED`

### 9.4 获取下载地址

- `POST /api/mdm/export/record/download-url/fetch`

规则：

- 只允许访问当前用户自己的 `SUCCESS` 记录
- 通过现有 `demo-system/file` 获取临时下载地址
- 成功返回下载地址后，更新：
  - `download_count`
  - `last_download_time`
  - `last_download_by`

语义定义：

- `download_count` 表示“成功领取下载地址次数”，不表示底层对象存储完成文件传输的次数

## 10. 有效期与清理策略

### 10.1 有效期来源

- 平台配置全局默认有效期
- 业务发起导出时允许显式传入本次有效期
- 若业务未传，则使用全局默认值
- 若业务传值，则只校验 `> 0`
- 本次最终采用的值写入 `expire_seconds`
- `expire_time = create_time + expire_seconds`

### 10.2 下载前过期判定

下载前必须再次校验：

- 当前状态是否为 `SUCCESS`
- 当前时间是否已超过 `expire_time`

如果已过期，即使定时任务尚未执行，也必须先拒绝下载，并将记录转为 `EXPIRED`。

### 10.3 过期清理任务

定时任务扫描条件：

- `status in (SUCCESS, EXPIRED)`
- `expire_time <= now`

处理流程：

1. 若当前状态为 `SUCCESS`，先将记录转为 `EXPIRED`
2. 删除底层文件
3. 删除成功后转 `DELETED`
4. 删除失败则保持 `EXPIRED`，等待下次任务继续处理

## 11. 异步与操作人上下文

当前仓库的 `OperatorContext` 基于 `ThreadLocal`，不会自动跨 `@Async` 线程传播。

本设计不依赖上下文透传，而是采用显式传递：

- 提交导出时读取当前请求线程的 `operatorId`、`operatorName`
- 将其作为统一提交命令字段传入
- 创建记录时写入 `create_by`
- 后台状态回写时显式使用该操作人信息，不依赖异步线程中的 `OperatorContext`

该设计兼容后续从 `@Async` 演进到定时任务、消息队列或独立 worker。

## 12. 错误处理

### 12.1 提交阶段校验

- `exportBizCode` 必须已注册
- `fileType` 必须存在对应 `FileRenderer`
- `expireSeconds` 如传值，必须大于 `0`
- `fileName` 必须非空，且应进行基础规范化
- `querySnapshotJson`、`querySnapshotSummary` 必须在创建记录前确定

### 12.2 失败记录

- `failCode` 存平台标准化错误码
- `failMessage` 存面向用户的简短失败摘要
- 完整异常栈只写日志，不落表

### 12.3 删除与下载幂等

- 删除时若底层文件不存在，仍可按成功删除处理，直接转 `DELETED`
- 已过期记录禁止下载
- 已删除记录禁止下载与重复删除

## 13. 测试策略

### 13.1 `demo-core`

- `ExportSceneRegistry` 对重复 `bizCode` 的校验
- `FileRenderer` 选择与不支持格式异常测试
- 导出抽象契约测试

### 13.2 `demo-system`

- `FileStorageExportFileSink` 与现有 `FileService` 的适配测试

### 13.3 `demo-mdm`

- 导出记录状态流转单测
- 本人权限边界单测
- 下载前过期校验单测
- 删除幂等单测
- `@Async` 成功与失败回写单测
- 过期清理任务单测

### 13.4 `demo-boot`

- Flyway migration 冒烟测试
- OpenAPI 文档测试
- 端到端集成测试：
  - 使用测试专用假 `ExportHandler` 跑通 `PROCESSING -> SUCCESS`
  - 使用测试专用假 `ExportHandler` 跑通 `PROCESSING -> FAILED`

## 14. 首期交付范围

- 平台能力可用，但不接真实业务导出场景
- 导出文件格式具备扩展点，首期只实现 Excel
- 下载中心只返回下载地址，不做代理流下载
- 只支持本人可见、本人可删、本人可下载
- 非活跃记录首期长期保留

## 15. 明确不采用的方案

- 不新增 `demo-export` 顶层模块
- 不将文件存储能力上移到 `demo-core`
- 不在平台层建设通用动态导出 HTTP 接口
- 不通过修改旧记录状态实现“重试”
- 不将下载计数定义为对象存储真实传输完成次数
