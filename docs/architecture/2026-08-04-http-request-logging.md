# HTTP 请求日志规范

## 目标

所有进入 Java 应用的业务 HTTP 请求由 `HttpRequestLoggingFilter` 统一记录完成日志。Controller、AppService 和业务模块不得重复打印请求体、响应体或自行拼装访问日志。

统一格式：

```text
2026-08-04 16:30:30.136 [http-nio-8080-exec-1] [trace-id] INFO  com.oigit.admin.core.logging.HttpRequestLoggingFilter - REQ DONE
+---------------- request ----------------
| method  : POST
| uri     : /api/system/dict/global/types/list
| traceId : trace-id
| userId  : 10001
| ip      : 10.0.0.8
| query   : -
| body    : {"pageNo":1,"pageSize":20}
+---------------- response ---------------
| status  : 200
| cost    : 25ms
| body    : {"code":200,"msg":"ok","data":{}}
+----------------------------------------
```

本项目没有应用内租户契约，不接收 `X-Tenant-Id`，因此日志不输出虚假的 `tenantId`。未来如果引入可信租户上下文，应从上下文读取，不能直接信任浏览器请求头。

## 过滤器顺序

```text
TraceIdFilter
  -> HttpRequestLoggingFilter
    -> GatewayOperatorFilter（按配置启用）
      -> Spring Security / JwtAuthenticationFilter
        -> Controller
```

- `TraceIdFilter` 先建立 MDC，日志过滤器始终能读取 traceId。
- 日志过滤器包裹网关过滤器和本地 IAM 认证链，因此网关返回的 400、认证失败的 401 和权限拒绝的 403 都能产生完成日志。
- `userId` 只读取 `OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID`。JWT 校验成功后由认证过滤器写入该请求属性，日志不读取原始 `X-User-Id`，也不从操作人 ThreadLocal 回退取值。
- JWT 过滤器清理 ThreadLocal 后，请求属性仍保留到本次请求结束，外层日志过滤器在 `finally` 中仍能记录认证身份。未认证或认证失败时 `userId` 显示 `-`；已认证但被权限规则拒绝的请求仍可记录真实身份。
- 请求结束后由原有过滤器清理 MDC 和操作人 ThreadLocal。

## 身份与客户端 IP 的可信来源

`GatewayOperatorFilter` 只负责头格式校验和操作人上下文适配，不验证用户身份，也不写入认证请求属性。启用它不会使 `X-User-Id` 成为可信认证身份；仅携带网关头、没有通过 JWT 认证的请求，其 HTTP 日志 `userId` 仍显示 `-`。同时携带 JWT 和网关头时，日志记录 JWT 认证身份，网关头不能覆盖该值。

网关头适配功能的部署前提是应用仅允许可信入口访问：入口必须先完成认证，清除客户端自行携带的身份头，再按认证结果设置这些头。当前过滤器的数字格式校验不能代替入口认证或网络隔离。将来接入其他认证方式时，也只能由完成身份验证的认证组件写入认证请求属性，禁止 Controller 或仅解析头的过滤器代写。

HTTP 日志通过 `core/operator/ClientIpResolver` 端口解析客户端 IP，使用 Spring `ObjectProvider` 可选注入：

- 没有解析器实现时，只记录 `request.getRemoteAddr()`，忽略所有客户端转发头。
- 本地 IAM 提供实现，复用 `platform.iam.client-ip.trusted-proxy-cidrs` 配置；默认列表为空，只记录直连对端地址。
- 只有直连对端属于配置中的可信代理网段时，才按 `X-Forwarded-For`、`Forwarded`、`X-Real-IP` 的优先级解析转发来源。
- 代理链从右向左剥离可信代理，在第一个非可信地址停止，避免把请求方伪造的最左地址记为客户端 IP。

部署在实际代理之后时，可通过 `IAM_CLIENT_IP_TRUSTED_PROXY_CIDRS` 配置允许直连应用的代理 CIDR，例如 `10.20.30.0/24`。只配置真实代理网段，不配置 `0.0.0.0/0` 或 `::/0`。代理本身还应正确追加或重建转发链；HTTP 日志不得另行解析转发头来绕过此端口。

## 数据安全与内存边界

- 默认请求体最多缓存 8KB，响应体最多旁路截取 16KB；超过限制只打印省略标记。
- 响应包装器边写客户端边截取有限字节，不阻塞响应，也不缓存完整文件。
- 只解析 JSON 和表单正文；multipart、二进制和未知 Content-Type 不打印正文。
- password、token、ticket、Authorization、Cookie、secret、手机号、身份证和银行卡等字段递归脱敏。
- query 参数使用相同字段名规则脱敏，并限制单行长度。
- Spring Web 日志固定为 INFO，防止其 DEBUG 日志绕过统一脱敏并输出反序列化对象。
- 健康检查、OpenAPI、Swagger 静态资源和本地文件访问默认不记录完成日志。

## 配置

配置前缀为 `platform.http-logging`：

```yaml
platform:
  http-logging:
    enabled: true
    request-body-enabled: true
    response-body-enabled: true
    max-request-body-bytes: 8192
    max-response-body-bytes: 16384
    excluded-paths:
      - /actuator/**
      - /v3/api-docs/**
      - /doc.html
      - /swagger-ui/**
      - /webjars/**
      - /local-files/**
```

生产环境需要调整正文策略时覆盖配置即可，不允许通过删除过滤器或在业务代码中建立另一套访问日志实现。

## 与业务审计日志的边界

HTTP 请求日志回答“某次请求何时完成、耗时和结果是什么”，不等同于业务操作审计。后续单据领域的创建、审核、作废、状态变更和关键字段前后值，需要另行通过业务事件或操作审计能力记录。
