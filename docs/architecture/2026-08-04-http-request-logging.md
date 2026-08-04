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
    -> GatewayOperatorFilter
      -> Controller
```

- `TraceIdFilter` 先建立 MDC，日志过滤器始终能读取 traceId。
- 日志过滤器包裹网关操作人过滤器，因此网关返回的 400 也能产生完成日志。
- userId 记录网关透传的 `X-User-Id`；业务身份与审计仍以 `OperatorContext` 为准。
- 请求结束后由原有过滤器清理 MDC 和操作人 ThreadLocal。

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
