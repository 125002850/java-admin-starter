# DSL 字段定义与否定筛选回灌

来源：track-bench 的 QueryFieldSpec 和已开放的否定操作符实现；模板基线分别为 main@4fef84b 与 feature/sso@3e439b0。

## 实现

- SceneQueryDefinition 统一提供 fields()，每个 QueryFieldSpec 声明字段类型、列、名称、允许操作符、排序和值标签。移除分散字段 Map 与 QueryFieldType，业务场景及测试同步迁移。
- 保留模板 TextConditionDTO<F> 等枚举字段 DTO，JSON、公开 schema 名称和 operationId 不变。未知字段仍由反序列化和执行白名单拒绝。
- 文本增加 NE / NOT_IN / NOT_CONTAINS，枚举增加 NE / NOT_IN，时间增加 EQ / NE。DTO、AST、场景元数据、个人方案校验与 SQL 执行保持一致。
- ownerId 等内部字段继续限制为 EQ，纯排序字段不能过滤；原有排序集合和业务默认顺序保持不变。
- NOT_IN 与 IN 共用集合上限与复杂度计费；NOT_CONTAINS 与 CONTAINS 共用成本。SQL 继续参数绑定，否定条件遵循 SQL NULL 语义。
- 各分支查询上限保持原值：main 默认复杂度 100、IN 1000；feature/sso 为 20、200。
- 修正 main 已有 multipart 审计断言，使其与 11 月 16 日已合入的省略标记一致。

## 验证

全量 Maven 测试覆盖原有 IAM／SSO、架构和平台行为；新增 QueryFieldSpecTests 验证类型、参数绑定及受限字段，DynamicQueryContractIntegrationTests 验证实际筛选、逻辑删除和集合上限，QuerySceneMetadataTests 核对真实 OpenAPI 的操作符与字段枚举。

删除旧接口后在独立干净工作区运行全量测试，并执行 git diff --check、迁移守卫及现有脚本回归。无数据库结构变更，不改历史迁移；验证使用隔离环境，未更新共享数据库。

## 本次结果

当前分支原工作区及独立干净工作区全量测试均通过；干净工作区 344 项，0 失败、0 错误、0 跳过。compile、OpenAPI 与实际查询回归、脚本回归、迁移门禁和 diff 格式检查通过。历史 SQL 无变更，无需执行新迁移；验证期间未部署或更新共享数据库。
