# 动态查询 DSL 开发范式

## 强制约束

所有分页查询接口必须使用动态查询 DSL。请求 DTO 统一继承 `BasePagedDynamicQueryReqDTO`，前端通过 `condition` 条件树传递筛选参数，禁止在 `ReqDTO` 上散落平铺查询字段。

对外部 API 的代理转发场景同样适用：条件树在后端 AppService 中解析转换为外部 API 参数，不改变前端动态查询交互范式。

## 执行链路

```text
前端 JSON 条件树
-> DTO 反序列化 + Bean Validation
-> SceneQueryMapper 通过 DynamicQueryAstMapper 转内部 AST
-> DynamicQueryGuard 校验
-> MybatisPlusQueryExecutor 生成 LambdaQueryWrapper
-> 执行 SQL
```

禁止在 Service 层直接拼 SQL 条件；所有动态条件必须走 AST -> QueryExecutor 链路。

对 join / 派生投影等 `LambdaQueryWrapper` 无法表达的查询，可以使用专用 SQL 渲染器，但仍必须满足：

- DTO -> AST -> DynamicQueryGuard 链路不变。
- SQL 渲染必须递归保留 `AND` / `OR` 分组括号，禁止把条件树拉平成单层 `AND`。
- `${}` 只能注入后端白名单列名、排序片段或服务端生成的 SQL 结构；所有用户值必须通过 `#{}` 参数绑定。
- 未知字段、未知排序、未知操作符必须抛 `BizException(DynamicQueryErrorCode)`。

## 前端传值规范

条件树通过 `nodeType` 鉴别器实现多态反序列化：

| `nodeType` | 含义 | 子字段 |
|---|---|---|
| `compose` | 逻辑组合 | `logic`、`children` |
| `text` | 文本字段条件 | `field`、`op`、`value` 或 `values` |
| `dateTime` | 时间字段条件 | `field`、`op`、`value` 或 `start` + `end` |
| `enum` | 枚举码值条件 | `field`、`op`、`value` 或 `values` |

- 文本操作符：`EQ`、`CONTAINS`、`STARTS_WITH`、`ENDS_WITH`、`IN`、`IS_NULL`、`IS_NOT_NULL`。
- 时间操作符：`GT`、`GTE`、`LT`、`LTE`、`BETWEEN`、`IS_NULL`、`IS_NOT_NULL`。
- 枚举操作符：`EQ`、`IN`、`IS_NULL`、`IS_NOT_NULL`。
- 排序方向：`ASC`、`DESC`。
- `op = IN` 时使用 `values` 数组，不使用 `value`。
- `op = BETWEEN` 时使用 `start` + `end`，不使用 `value`。
- `op = IS_NULL / IS_NOT_NULL` 时不传 `value`、`values`、`start`、`end`。
- `nodeType` 鉴别器值使用 `compose`，不要使用 `group`。

## 后端接入新场景

1. 定义 `CriteriaReqDTO`

- 位置：`controller/dto/query/`。
- 继承 `BaseDynamicCriteriaReqDTO<N, S>`。
- 声明内部枚举：`TextField`、`DateTimeField`、`SortField`。
- 声明 `ConditionNode` 接口，用 `@JsonTypeInfo` + `@JsonSubTypes` 标注多态子类型。
- 为每种条件声明内部静态类：`GroupCondition`、`TextCondition`、`DateTimeCondition`。
- 声明 `SortItem` 继承 `SortItemDTO<SortField>`。
- 字符串值字段一律归入 `TextField`，不要为 `VARCHAR` / `TEXT` 列单独声明 `EnumField` / `EnumCondition`。
- 仅当字段值类型不是 `String` 且需要 Jackson 类型安全反序列化时，才考虑 `EnumField`。

2. 定义 Page/List 请求 DTO

- 继承 `BasePagedDynamicQueryReqDTO<N, S>`。
- 重写 `getCondition()`，使用 `@Schema(implementation = XxxCriteriaReqDTO.ConditionNode.class)`。
- 重写 `getSort()`，使用 `@ArraySchema`。
- 禁止在 `getCondition()` 的 `@Schema` 上重复声明 `discriminatorProperty` + `oneOf`。

示例：

```java
@Override
@Valid
@Schema(
    description = "查询条件树",
    implementation = XxxCriteriaReqDTO.ConditionNode.class
)
public XxxCriteriaReqDTO.ConditionNode getCondition() {
    return super.getCondition();
}

@Override
@Valid
@ArraySchema(
    arraySchema = @Schema(description = "排序项"),
    schema = @Schema(implementation = XxxCriteriaReqDTO.SortItem.class)
)
public List<XxxCriteriaReqDTO.SortItem> getSort() {
    return super.getSort();
}
```

3. 更新 `OpenApiConfig` 动态查询场景注册表

新增动态查询场景后，只在 `DYNAMIC_QUERY_SCENE_SCHEMAS` 中新增一项：

```java
new DynamicQuerySceneSchema(
    "XxxDynamicPageReqDTO",
    "XxxConditionNode",
    Map.of(
        "XxxGroupCondition", "compose",
        "XxxTextCondition", "text",
        "XxxDateTimeCondition", "dateTime"
    )
)
```

原因：SpringDoc 无法从内部接口上的 `@JsonSubTypes` 自动推导 `discriminator.mapping`，也无法为 `implementation = ConditionNode.class` 的 getter 生成稳定 `$ref`。

4. 实现 `SceneQueryDefinition`

- 位置：对应模块的 `query/` 包。
- 实现 `SceneQueryDefinition<Entity>`。
- 返回全局唯一 `sceneCode()`，建议格式 `{模块}.{领域}.{实体}.{动作}`。
- 提供 `textFields()`、`dateTimeFields()`、`enumFields()`、`sortFields()` 四个 Map。
- Map key 使用字段枚举值，value 使用 MyBatis-Plus `SFunction` lambda。
- 必须覆写或明确限制 `allowedOperators(fieldKey)`，不能对所有字段全部开放。
- 按需覆写 `defaultSorts()`。

5. 实现 `SceneQueryMapper`

- 实现 `SceneQueryMapper<CriteriaReqDTO>`。
- `toQueryAst(reqDTO)` 使用 `DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort())` 将 DTO 条件树转换为内部 AST。
- `IN` 操作符的 `typedValue` 为 `List`。
- `BETWEEN` 操作符的 `typedValue` 为 `List.of(start, end)`。
- `IS_NULL / IS_NOT_NULL` 操作符的 `typedValue` 为 `null`。
- 如需服务端注入额外条件，在 mapper 中合并约束，例如 `mergeOwnerConstraint()`。

6. Controller 接入

- 接收 `*DynamicListReqDTO` 或 `*DynamicPageReqDTO`。
- 只调用 AppService，不直接访问 Service/Mapper。

## 字段枚举约定

- `TextField`、`DateTimeField`、`SortField` 的枚举名使用小写 camelCase。
- 枚举名必须与目标实体 getter 对应的 JSON 字段名严格一致，例如 `dictTypeCode`、`createTime`、`id`。
- 禁止使用 `UPPER_SNAKE_CASE`。
- `SceneQueryDefinition` map key、`allowedOperators` switch case、`defaultSorts` 的 `SortSpec` 必须同步保持一致。

## 防护限制

| 限制项 | 值 | 错误码 |
|---|---|---|
| DSL 版本 | `1` | 3000001 |
| 树最大深度 | `4` | 3000011 |
| 最大节点数 | `20` | 3000012 |
| IN 集合最大大小 | `200` | 3000009 |
| 分页最大 pageSize | `2000` | 3000010 |
| 排序项最大数量 | `3` | 无 |
| 复杂度评分上限 | 场景定义，默认 `20` | 3000013 |

复杂度评分：

| 因素 | 加分 |
|---|---|
| 每个叶节点 | +1 |
| `CONTAINS` 或 `ENDS_WITH` | +2 |
| `BETWEEN` | +1 |
| `IN` N 个值 | +1 + floor(N / 20) |
| `OR` 组合 | +2 |
| `AND` 组合 | +0 |

## 错误码速查

| 错误码 | 含义 |
|---|---|
| 3000001 | 协议版本不支持 |
| 3000002 | 节点类型不支持 |
| 3000003 | 节点类型未知 |
| 3000004 | 排序字段非法 |
| 3000005 | 分组子节点不能为空 |
| 3000006 | 区间参数非法 |
| 3000007 | 文本条件 value/values 不匹配操作符 |
| 3000008 | 枚举条件 value/values 不匹配操作符 |
| 3000009 | IN 集合过大 |
| 3000010 | 分页大小超限 |
| 3000011 | 条件树深度超限 |
| 3000012 | 条件节点数量超限 |
| 3000013 | 查询复杂度超限 |
| 3000014 | 操作符不支持 |
