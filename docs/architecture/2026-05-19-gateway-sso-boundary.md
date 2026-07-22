# 网关-SSO 边界契约

> 本文档定义网关到 java-admin-starter 应用的正向契约。本仓库不实现 SSO 登录、JWT 校验、Token 刷新、权限判定或租户解析。

## 信任模型

- 网关与 java-admin-starter 之间为**私有链路**（内网或 mTLS），应用信任网关透传的 header。
- 应用自身**不做登录态校验**，不对外暴露"随便带头即可冒充用户"的调用方式。
- 开发/测试环境无网关时，应用允许缺失操作人头继续处理请求，审计字段回退为 `0L`。

## 网关透传 Header

| Header | 必需 | 类型 | 用途 |
|--------|------|------|------|
| `X-User-Id` | 是（生产环境） | 数值型 Long | 审计字段 `create_by` / `update_by` 的唯一来源 |
| `X-User-Name` | 否 | URL 编码字符串 | 写入操作人上下文并缓存为审计字段的展示用户名 |
| `X-User-Phone` | 否 | 字符串 | 写入操作人上下文并缓存为用户展示信息 |
| `X-Real-Name` | 否 | URL 编码字符串 | 写入操作人上下文并缓存为用户展示信息 |
| `X-User-Code` | 否 | 字符串 | 缓存为用户展示信息，不写入操作人上下文 |

除 `X-User-Id` 外，其余 Header 只用于展示、缓存或排障，不参与身份和权限判定。

以下 header **不会**被本仓库接收或消费：

- `X-Tenant-Id` — 本仓库不再实现多租户
- `X-User-Roles`、`X-User-Permissions` — 本仓库不做权限判定
- `Authorization`、`Cookie` — 鉴权在网关完成

## 应用侧实现

### OperatorContext

线程级上下文，由 `GatewayOperatorFilter` 在请求进入时填充，请求结束时清理：

```
GatewayOperatorFilter → OperatorContext.set(userId, userName, userPhone, realName)
                       → 异步更新 sys_user_cache（包含 userCode）
                       → filterChain.doFilter()
                       → OperatorContext.clear()
```

`GatewayOperatorFilter` 同时将网关透传的用户展示信息写入 `sys_user_cache`。对外响应中的
`createBy` / `updateBy` 由应用服务显式批量解析为 `user_name`：当前操作人优先使用本次请求的
`X-User-Name`，其他操作人从缓存批量读取。数据库实体和内部任务对象仍保持 `Long` ID。
导出异步任务在提交时快照 `OperatorContext` 与 MDC，工作线程执行结束后必须清理，避免线程池上下文串扰。

### CommonMetaObjectHandler

审计字段填充策略：

1. 优先从 `OperatorContext` 读取当前操作人 ID
2. 缺失时（无网关本地开发）回退 `0L`

### 错误处理

- `X-User-Id` 存在但无法解析为 Long → HTTP 400，暴露网关契约错误
- `X-User-Id` 缺失 → 请求继续，审计字段回退 `0L`（不阻断业务）

## Breaking Change 迁移表

| 旧接口 | 状态 | 迁移目标 |
|--------|------|----------|
| `POST /api/system/auth/login` | 已删除 | 由网关 SSO 承接 |
| `POST /api/system/tenant/**` | 已删除 | 不再提供 |
| `POST /api/system/user/**` | 已删除 | 不再提供 |
| `POST /api/mdm/dict/items/by-type` | 已删除 | `POST /api/mdm/dict/global/items/by-type` |
| `POST /api/mdm/dict/types/list` | 已删除 | `POST /api/mdm/dict/global/types/list` |
| `POST /api/mdm/dict/type/create` | 已删除 | `POST /api/mdm/dict/global/type/create` |
| `POST /api/mdm/dict/type/update` | 已删除 | `POST /api/mdm/dict/global/type/update` |
| `POST /api/mdm/dict/type/delete` | 已删除 | `POST /api/mdm/dict/global/type/delete` |
| `POST /api/mdm/dict/item/create` | 已删除 | `POST /api/mdm/dict/global/item/create` |
| `POST /api/mdm/dict/item/update` | 已删除 | `POST /api/mdm/dict/global/item/update` |
| `POST /api/mdm/dict/item/delete` | 已删除 | `POST /api/mdm/dict/global/item/delete` |

## 本地开发

- dev profile 不启动网关模拟 filter
- 缺失 `X-User-Id` 时，所有写操作的 `create_by` / `update_by` = `0L`
- 如需模拟操作人，在请求中手动添加 header `X-User-Id: 1`
