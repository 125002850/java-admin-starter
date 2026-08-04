# 能力分层、审计与批量翻译架构

- 状态：已接受
- 日期：2026-08-04
- 适用范围：后端业务能力、OpenAPI 契约、前端字典展示和导出

## 背景

基于本脚手架的业务系统会持续增加主数据、交易和单据能力。如果应用层直接调用 Mapper、在列表循环中逐个查询名称，或让前后端各自猜测枚举描述，模块边界会很快失控，并产生难以发现的 N+1 查询。

本决策统一以下问题：

- 一个能力内部如何分层和分包；
- MyBatis-Plus `IService/ServiceImpl` 应属于哪一层；
- DTO、Entity、Domain Model 和跨模块契约如何隔离；
- 审计 ID、字典编码、枚举和未来主数据编码如何返回及翻译；
- Web 页面与导出场景分别由哪一端负责展示名称；
- 如何从结构上避免翻译器产生 N+1 查询。

## 1. 能力分层

标准能力包结构：

```text
com.oigit.admin.{capability}
├── controller
├── dto
│   ├── req
│   │   └── query
│   └── rsp
├── app
├── domain
│   ├── model
│   ├── repository
│   └── service
├── infra
│   ├── persistence
│   │   ├── entity
│   │   ├── mapper
│   │   ├── service
│   │   │   └── impl
│   │   └── repository
│   ├── query
│   ├── provider
│   ├── translation
│   └── config
└── enums
```

依赖方向固定为：

```text
Controller -> AppService -> Domain Model / Domain Service / Repository port
                                      ^
                                      |
                          Infra Repository adapter
                                      |
                         IService/ServiceImpl -> Mapper
```

具体规则：

- Controller 只做协议适配、校验和响应包装，只依赖 AppService 与 DTO。
- DTO 与 `controller`、`app` 同级；请求放 `dto.req`，响应放 `dto.rsp`，动态查询请求放 `dto.req.query`。
- AppService 负责编排、权限/归属校验和事务边界，不直接依赖 Entity、Mapper 或具体 Repository 实现。
- Domain 保存业务状态和规则，Repository 只定义端口，不依赖 Spring、MyBatis 或 Web。
- Mapper 调用只能存在于 `infra/persistence`。MyBatis-Plus Service 必须使用 `IService` 与 `ServiceImpl` 表达，并分别放在 `service` 与 `service.impl`。
- 没有真实领域规则时不制造空 Domain Service；AppService 可以直接依赖 Domain Repository 端口。
- 模块私有枚举放本能力的 `enums`。需要跨模块同步使用的枚举、DTO、接口或事件，按真实消费者建立独立 API 契约模块，禁止把 Entity/Mapper 暴露出去。

全局字典是上述结构的参考实现。仓库级 ArchUnit 已锁定字典层间依赖、MyBatis-Plus persistence 位置和现有能力的 DTO 方向；新增能力必须同步扩展架构测试。

## 2. 审计字段契约

数据库审计列只存稳定 ID：

```text
create_by bigint
update_by bigint
create_time datetime
update_time datetime
```

`create_by`、`update_by` 由 MyBatis-Plus `MetaObjectHandler` 从 `OperatorContext` 统一填充。开发环境缺失操作人时回退 `0L`，业务代码不得手工维护这四个字段。

需要返回审计信息的响应 DTO 继承 `AuditRspDTO`，统一暴露：

```text
createById: Long
createByName: String
updateById: Long
updateByName: String
createTime: LocalDateTime
updateTime: LocalDateTime
```

翻译注解标在 ID 源字段上。AppService 只复制 ID 和时间，不逐行查询名称，也不手工 set `createByName/updateByName`。

操作人名称由当前产品分支的身份模块批量提供：SSO 分支使用 `staff/infra/persistence` 中的展示缓存，本地 IAM 分支使用 IAM 员工数据。`core` 只定义通用翻译契约，不依赖任一身份实现。

## 3. 通用批量翻译引擎

后端翻译采用两阶段算法：

```text
扫描对象图与注解
  -> 按 (translationType, qualifier, sourceValue) 去重分组
  -> 每个 provider 一次批量调用
  -> 将结果批量回填到 targetField
```

引擎支持普通响应、`R`、`PageResult`、集合、Map、数组和嵌套 DTO；反射元数据按 Class 缓存，不按行重复解析。`TranslationProvider` 只提供批量接口，禁止提供或调用逐值查询 API。

当前翻译类型：

- `USER_NAME`：操作人 ID 由当前身份模块批量解析为用户名；SSO 场景中当前请求操作人名称优先于旧缓存。
- `DICT_ITEM_NAME`：按一个或多个字典类型批量读取 code/name。
- 未来主数据：以客户编码、商品编码等稳定业务编码作为 source，由对应主数据模块实现批量 provider。

场景由 `TranslationScene` 隔离：

- `WEB_RESPONSE`：在统一 `ResponseBodyAdvice` 中执行。
- `EXPORT`：标准导出在渲染前执行；分包导出在每个固定大小 chunk 渲染前执行，禁止先把全量数据装入内存。

缺失翻译的策略由注解声明。默认填 `null`；需要保留可排障原值时使用 source fallback。Provider 缺失视为装配错误，不能静默触发逐行降级查询。

## 4. 字典、枚举和主数据的展示边界

| 字段类别 | 数据库存储 | Web 页面 | 后端导出 |
|---|---|---|---|
| 普通字典 | 字典 item code | 前端批量字典组件翻译 | `@Translate` + 字典 provider |
| 后端枚举 | enum code | 当作字典 code，由前端翻译 | `@Translate` + 字典 provider |
| 审计用户 | user ID | 后端统一翻译为 `xxxByName` | 后端统一翻译 |
| 主数据引用 | 客户/商品等稳定编码 | 默认后端批量翻译或按接口契约返回 name | 后端批量翻译 |
| 主数据快照 | 编码 + 业务确认的快照名称 | 可直接展示快照 | 可直接导出快照 |

普通字典在 Web 响应中只返回 code，不返回 `statusName` 一类冗余字段。前端通过 `POST /api/system/dict/global/items/options` 一次请求页面所需的多个字典类型，并使用 React Query 缓存：

- 表格单元格只读取页面级 `DictionaryScope`，不得在 cell 内发请求；
- code/name 映射包含已停用项，保证历史数据仍能显示；
- 表单 options 只包含启用项，禁止新数据继续选择停用 code；
- 未找到名称时显示原 code，便于定位脏数据。

主数据引用默认不复制名称到业务表，因为名称会变化。只有法规单据、历史成交信息或已明确接受“当时名称”语义时才保存快照；使用快照时字段名必须显式表达 `snapshot`，不能与实时翻译混用。

## 5. 枚举与字典双轨契约

影响后端分支的状态仍使用 Java Enum，但所有对前端可见的枚举都同时在全局字典中建同 code 的字典项：

- Java enum code 是业务逻辑权威；
- 字典 item name 是前端和导出的展示文案权威；
- JSON 只序列化 code 字符串；
- OpenAPI schema 为 string enum，并携带 `x-dict-type`；
- 前端只按字典处理，不需要复制 Java 枚举描述。

枚举使用 `@DictionaryEnum` 声明字典类型。受该注解管理的字典类型和 item code 不允许在后台删除、改码或手工增减；名称、启停、排序和备注允许维护。每次新增或修改 enum code，必须追加 Flyway migration，并由启动测试校验 Java code 与未删除字典项完全一致。

## 6. 性能与验证红线

- 不允许在 DTO getter、序列化器、表格 cell 或导出单元格回调内查数据库/远端服务。
- 一次翻译调用中，同一个 provider 只接收一次去重后的 key 集合。
- Provider 使用 `IN`/批量接口。ID 很多时按固定上限分片，分片发生在 provider/infra 内，不回退为逐行调用。
- 前端一个页面应先声明需要的字典类型并一次批量获取，不能每列、每行各发请求。
- 必须有测试证明：多行重复 key 被去重、provider 调用次数不随行数增长、超大 ID 集合按固定大小分片、导出场景与 Web 场景隔离。

## 7. 新业务能力落地清单

新增客户、商品或订单能力时：

1. 先建立 `controller/dto/app/domain/infra/enums` 骨架，只创建实际使用的子包。
2. 在 Domain 定义模型、规则和 Repository 端口；在 Infra 用 Entity、Mapper、`IService/ServiceImpl` 和 Repository adapter 实现。
3. 响应审计字段继承 `AuditRspDTO`，不手工查询用户名称。
4. 普通字典与枚举响应只返回 code；页面登记所需字典类型并批量获取。
5. 导出 DTO 对需要展示名称的字段声明 `@Translate(scenes = EXPORT)`。
6. 主数据引用先决定实时名称还是业务快照，再选择 provider 或 snapshot 字段。
7. 同步补 Flyway、OpenAPI、生成客户端、ArchUnit、provider 调用次数和真实 MySQL 验证。
