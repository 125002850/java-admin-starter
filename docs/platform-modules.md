# 通用平台模块

业务项目回灌的能力位于 `system` 的 `operationaudit`、`schedule`、`workcalendar` 包，沿用 Controller → App → Domain Repository → Infra 的调用边界。Flyway 增量迁移创建所需表、字典和 IAM 菜单，不包含业务任务或业务数据。

## 操作审计

在 Controller 上声明 `@OperationAuditModule(code = "...")` 与 `@Tag(name = "...")`，在变更接口上声明 `@OperationAudit(action = ...)` 与 `@Operation(summary = "...")` 即可接入。当前覆盖字典变更、调度任务变更及日历保存、导入、发布。

审计保存认证后的操作人快照、请求路径、耗时、状态及脱敏参数。复用 HTTP 日志脱敏规则，正文最多采集 8 KiB，不保存上传文件正文；审计使用独立事务记录业务失败，不影响主请求结果。现有 IAM 专用操作日志继续负责 IAM 功能。

查询权限为 `system:audit:query`，前端菜单键为 `operation-audit`。

## 调度中心

支持任务增删改、启停、手动触发、执行/操作记录和 Cron 预览。Cron 使用 Spring 六段表达式，例如 `0 0/5 * * * ?`；保存时校验，数据库事务提交后再更新调度器。

```yaml
platform:
  schedule:
    time-zone: Asia/Shanghai
    allowed-bean-routes: bean://maintenanceJob.run
    allowed-http-origins: https://jobs.example.com
```

- Bean 任务实现为 Spring Bean 的无参公开方法，调用路由必须精确列入 `allowed-bean-routes`。
- HTTP 任务发送空 JSON POST；目标 origin 必须列入 `allowed-http-origins`。不跟随重定向，连接/读取/总调用超时为 5/30/35 秒。
- 两份白名单默认均为空。模板不注册实际业务任务。
- 当前调度器适用于单实例部署，没有分布式锁、集群一致性或补跑能力；多副本环境需先接入统一调度平台。源项目未实现的锁租约参数未暴露为模板功能。
- 管理权限为 `system:schedule:manage`，菜单键为 `schedule-center`。

## 工作日历

支持年度工作时段、特殊日期、调休、JSON 导入、草稿试算、发布差异预检及生效快照查询。时间语义固定为 `Asia/Shanghai`，年份范围为 2000–2099。JSON 导入最大 1 MiB，页面提供导入模板。

发布必须同时匹配草稿版本、乐观锁版本和预检内容 hash，防止覆盖他人的修改。已发布快照只读；修改另存草稿并重新发布。模板不预置任何年度法定节假日，需要维护者导入并发布。

管理权限为 `system:calendar:manage`，菜单键为 `work-calendar`。

## 接入与裁剪

前端页面位于对应 feature 目录，路由集中于 `dashboard/system-management`；新增用户需授予对应菜单权限。后端接口的枚举返回 code，由字典批量接口提供显示名称。

无需某模块时，可先撤销菜单授权。创建新业务项目前若要删除模块，应同时移除对应前端 feature/route、后端能力包、MapperScan 和 OpenAPI 场景注册，再重新生成客户端；已执行的历史迁移仍应保留。
