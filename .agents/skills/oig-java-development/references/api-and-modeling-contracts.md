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
- 分页接口默认提供 `list` 和 `list-all` 两个端点；`list-all` 请求 DTO 不得继承 `PageReqDTO`。
- 全局异常统一转换为标准 `R<T>` 响应。
- 日期格式使用 `yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd`。

## 错误码规范

- 成功响应固定为 `code = 200`、`msg = ok`。
- 默认失败响应使用 `CommonErrorCode.FAILED(500, "操作失败")`。
- 参数错误、未登录、无权限、资源不存在、限流使用公共 HTTP 语义码：`400 / 401 / 403 / 404 / 429`。
- 模块私有业务码使用独立号段，例如 `track-bench-system` 使用 `3001xxx`。
- 业务异常统一使用 `BizException(ErrorCode)`。
- `BizException` 只接受 `ErrorCode`，禁止业务代码散落裸错误码、裸失败文案。

## 命名规范

项目名为 `track-bench`，业务表和业务对象统一使用 `track` 前缀，不使用 `follow` 等近义词。

| 层面 | 规范 | 示例 |
|---|---|---|
| 表名 | `tb_track_` 前缀 | `tb_track_record`、`tb_track_issue` |
| 列名 | 有明确业务语义的列使用 `track_` 前缀 | `track_no`、`track_dimension`、`track_type` |
| Java 类名 | `Track` 前缀 | `TrackRecord`、`TrackIssue` |
| 枚举 | `Track` 前缀 | `TrackRecordStatus`、`TrackIssueStatus` |
| REST 路径 | `/track-` 前缀 | `/api/postloan/track-record` |
| 错误码 | `TRACK_` 前缀 | `TRACK_RECORD_NOT_FOUND` |
| 索引/约束 | 跟随表名 | `idx_tb_track_record_customer_status` |

例外：数仓同步表字段名保持数仓侧命名不变，例如 `follow_up_code`。

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
import com.trackbench.core.enums.BaseEnum;

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
- 逻辑删除字段统一为 `deleted`。
- 本仓库不要求业务表包含 `tenant_id`，不校验 `X-Tenant-Id`。

## 对象模型

第一阶段只保留三类核心对象：

| 对象 | 用途 |
|---|---|
| `Entity` | 数据库持久化对象 |
| `ReqDTO` / `RspDTO` | Web 层请求/响应对象 |
| `XxxQuery` | 复杂查询场景，按需引入 |

禁止提前引入 `VO`、`BO`、`DO`、`Param`、`Form`、`Command` 等多套近义对象。
