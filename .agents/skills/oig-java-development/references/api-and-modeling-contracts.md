# API、错误码与对象模型规范

## API 规范

- 统一响应结构：`{"code":200,"msg":"ok","data":...}`，响应对象命名为 `R`。
- Controller 返回必须使用 `R.ok(...)` / `R.fail(...)`。
- 所有接口统一使用 `POST`。
- URL 格式：`/api/{模块名}/{资源名}/{动作}`。
- 请求对象命名为 `XxxReqDTO`，响应对象命名为 `XxxRspDTO`。
- Web DTO 禁止复用数据库 Entity。
- 写操作（mutation）接口统一使用 `List<Long> ids` 参数接受批量/单条操作，不单独建 `/xxx-batch` 端点。
- Controller 类上使用 `@Tag`，方法上使用 `@Operation`。
- `@Operation` 必须显式指定全局唯一、驼峰命名的 `operationId`，供前端 OpenAPI codegen 生成 API 函数名。
- 已对外发布或已被前端消费的接口，`operationId` 视为 API 契约的一部分，必须保持稳定；不得因为 Controller 方法重命名、路径兜底策略、springdoc/Knife4j 配置变化或代码整理而改名。
- 给历史接口补 `operationId` 时，必须先核对当前已发布 OpenAPI spec / 前端生成代码中的既有名称，并显式沿用既有 `operationId`；禁止把已有路径式名称无意切换为方法名式名称。
- 确需变更已发布 `operationId` 时，必须按破坏性 API 变更处理：在计划或 PR 中说明原因、影响范围和前端迁移方案，并同步更新前端 import、测试与 OpenAPI snapshot。
- 新增或修改公开接口时，必须在 OpenAPI 契约测试中覆盖关键路径的 `operationId`，防止生成代码导出名漂移。
- `ReqDTO` / `RspDTO` 必须补齐 `@Schema` 注解，并为关键字段提供含义说明和示例值。
- 分页使用 `PageReqDTO` 和 `PageResult<T>`。
- 是否提供 `list-all` 取决于业务场景；仅当确实存在无分页全量选择诉求时才提供 `list-all`，且其请求 DTO 不得继承 `PageReqDTO`。
- 全局异常统一转换为标准 `R<T>` 响应。
- 日期格式使用 `yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd`。

## 错误码规范

- 成功响应固定为 `code = 200`、`msg = ok`。
- 默认失败响应使用 `CommonErrorCode.FAILED(500, "操作失败")`。
- 参数错误、未登录、无权限、资源不存在、限流使用公共 HTTP 语义码：`400 / 401 / 403 / 404 / 429`。
- 模块私有业务码使用独立号段，例如当前 `admin-mdm` 使用 `3001xxx`。
- 业务异常统一使用 `BizException(ErrorCode)`。
- `BizException` 只接受 `ErrorCode`，禁止业务代码散落裸错误码、裸失败文案。

## 命名规范

当前仓库是 `java-admin-starter` 基础项目，不对所有业务对象强制统一前缀。命名应遵循“按实际领域语义命名”，避免把历史业务前缀误扩散到底座模块。

| 层面 | 规范 | 示例 |
|---|---|---|
| 平台/通用业务表 | 使用领域语义命名，不强制历史业务前缀 | `sys_dict_type_global`、`sys_export_record_global` |
| 列名 | 直接表达当前业务语义 | `dict_type_code`、`export_biz_code` |
| Java 类名 | 使用完整业务语义，不强制 `Track` 前缀 | `GlobalDictTypeEntity`、`ExportRecordEntity` |
| 枚举 | 使用完整业务语义 | `EnableStatusEnum`、`ExportRecordStatus` |
| REST 路径 | 按模块语义组织 | `/api/mdm/dict/global`、`/api/mdm/export` |
| 错误码 | 按模块或领域语义命名 | `GLOBAL_DICT_TYPE_NOT_FOUND` |
| 索引/约束 | 跟随真实表名与语义 | `uk_sys_dict_type_global_code` |

例外：对接外部系统、数仓或上游表时，可保持上游约定命名不变，但不要把上游前缀扩散为当前仓库的默认规范。

## 枚举规范

业务枚举必须：

- 实现 `BaseEnum`，提供 `getCode()` 和 `getDesc()`。
- 标注 `@JsonFormat(shape = JsonFormat.Shape.OBJECT)`，响应序列化为 `{"code":"...","desc":"..."}`。
- 标注 `@JsonCreator(mode = JsonCreator.Mode.DELEGATING)`，请求接收 code 字符串反序列化。
- 使用 `@EnumValue` 标注持久化字段，MyBatis-Plus 写入 `code` 值。
- `code` 统一使用 `String`；int 型 code 的枚举额外提供 `getIntCode()` 供内部比较。
- 枚举名使用完整业务含义，例如 `EnableStatusEnum`，不要缩写成 `StatusEnum`。

标准模板：

```java
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.admin.core.enums.BaseEnum;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnableStatusEnum implements BaseEnum {

    ENABLE("enable", "启用"),
    DISABLE("disable", "禁用");

    @EnumValue
    private final String code;
    private final String desc;

    EnableStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnableStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EnableStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
```

`EnumModelConverter` 会将实现了 `BaseEnum` 的枚举字段映射为 `EnumVO` schema，DTO 字段只需写：

```java
@Schema(description = "状态")
private EnableStatusEnum status;
```

枚举与字典选择：

| 场景 | 使用 |
|---|---|
| 影响后端逻辑分支 | `Enum` |
| 仅用于前端展示或筛选 | 数据库字典表 |

## 数据规范

- 主键统一使用数据库自增 ID，DDL 写 `bigint primary key auto_increment`，配合 MyBatis-Plus 全局 `id-type: auto`。
- 所有业务表必须包含 `create_time`、`update_time`、`create_by`、`update_by`、`deleted`。
- `create_time` / `update_time` 禁止在业务 service 中手工赋值；建表时提供 `default current_timestamp`，并由 `MetaObjectHandler` 兜底填充。
- `create_by` / `update_by` 通过 `MetaObjectHandler` 自动填充，优先从 `OperatorContext` 读取 `X-User-Id`，缺失时回退 `0L`。
- 逻辑删除字段统一为 `deleted`；未删除值使用 `0`，删除值使用数据库时间戳表达式，避免软删后唯一索引冲突。
- 本仓库不要求业务表包含 `tenant_id`，不校验 `X-Tenant-Id`。

## 对象模型

第一阶段只保留三类核心对象：

| 对象 | 用途 |
|---|---|
| `Entity` | 数据库持久化对象 |
| `ReqDTO` / `RspDTO` | Web 层请求/响应对象 |
| `XxxQuery` | 复杂查询场景，按需引入 |

禁止提前引入 `VO`、`BO`、`DO`、`Param`、`Form`、`Command` 等多套近义对象。
