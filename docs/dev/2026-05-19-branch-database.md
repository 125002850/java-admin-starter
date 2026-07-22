# 分支开发数据库说明

## 目标

当前分支不再依赖数据库启动脚本，而是直接由分支自身维护数据库配置文件：

- 仓库根目录 `compose.yaml`
- `boot/src/main/resources/application-dev.yml`

这样开发者切回当前分支时，数据库容器配置和 Spring Boot 的开发连接地址会随 Git 分支一起切换，不需要再额外执行分支识别脚本。

## 当前分支配置

当前 `feature/sso` 分支固定使用以下数据库配置：

| 项目 | 值 |
|------|----|
| Compose 项目标识 | `java-admin-starter-feature-sso` |
| MySQL 端口 | `3307` |
| 数据库名 | `java_admin_starter_sso` |
| 用户名 | `root` |
| 密码 | `root` |

## 开发者如何启动数据库

在当前分支直接执行：

```bash
docker compose up -d
```

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f mysql
```

停止数据库：

```bash
docker compose down
```

删除数据库并重建：

```bash
docker compose down -v
docker compose up -d
```

`down -v` 会删除当前分支对应的 MySQL 数据卷，只适合在你确认要清空本地开发数据时使用。

## 启动 Spring Boot

推荐由统一脚本检查 Docker/MySQL、安装兄弟模块并启动服务：

```bash
./scripts/start-dev.sh
```

也可以在数据库启动后手动运行：

```bash
mvn -pl boot -am install -DskipTests
cd boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

当前分支的 [`application-dev.yml`](../../boot/src/main/resources/application-dev.yml) 未设置 `JAVA_ADMIN_STARTER_DATASOURCE_*` 时连接到：

```text
jdbc:mysql://127.0.0.1:3307/java_admin_starter_sso
```

如需使用远程独立库，可通过 `JAVA_ADMIN_STARTER_DATASOURCE_URL`、`JAVA_ADMIN_STARTER_DATASOURCE_USERNAME`、`JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD` 覆盖；此时 `start-dev.sh` 会跳过本地 Docker/MySQL。

## 为什么这样更简单

- 不再需要按分支维护数据库映射脚本
- 不再需要 `eval`
- 不再需要脚本判断当前分支
- 本地数据库仍可直接使用 `docker compose up -d`，完整开发启动使用 `./scripts/start-dev.sh`

切分支时，真正跟着切换的是分支里的 YAML 内容，而不是脚本里的映射规则。

## 对其他分支的约束

如果 `main` 分支也要保持独立数据库，应在 `main` 分支内分别维护它自己的：

- `compose.yaml`
- `boot/src/main/resources/application-dev.yml`

并保证它们使用不同的：

- Compose `name`
- MySQL 映射端口
- 数据库名

否则不同分支之间仍然可能出现端口、容器名或数据卷冲突。
