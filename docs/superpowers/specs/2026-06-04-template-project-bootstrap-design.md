# 模板项目初始化脚本设计

## 1. 背景

当前仓库 `java-admin-starter` 需要作为后续新项目的模板仓库使用。目标是在模板仓库内提供一个初始化脚本，由使用者在模板仓库根目录执行，一次性完成以下动作：

- 复制模板仓库到指定新项目目录
- 将新目录初始化为干净的新 Git 仓库，不继承模板历史
- 根据显式传入的项目名替换 Maven 坐标、应用名、模块名、数据库名、本地存储目录
- 根据显式传入的包名参数替换 Java 包名，并迁移源码/测试目录结构
- 将启动类统一固定为 `BootApplication`

## 2. 目标

脚本执行后，新项目目录满足以下约束：

- 目标目录是一个独立的新仓库，`git log` 不包含模板仓库历史
- Maven 根工程名从 `java-admin-starter` 替换为目标项目名，例如 `track-bench`
- 四个顶层模块目录从 `admin-*` 重命名为 `{project-name}-*`
- Java 包名从 `com.example.admin` 替换为用户传入的包名，例如 `com.trackbench`
- 启动类名固定为 `BootApplication`
- Spring 应用名、OpenAPI 文档名、Compose 项目标识、开发库名、本地上传目录等模板标识同步替换

## 3. 非目标

本期不处理以下事项：

- 不自动根据项目名生成包名，包名必须由调用者显式传入
- 不尝试语义改写历史设计文档，只做模板标识替换
- 不改动业务模块内部语义命名，例如 `mdm`、`system`
- 不支持把任意仓库当作模板源，脚本仅服务当前仓库
- 不负责执行 `mvn test`，但会提供独立回归验证脚本用于验证模板替换结果

## 4. 用户接口

脚本路径：

```text
scripts/init_template_project.py
```

调用方式：

```bash
python3 scripts/init_template_project.py \
  --project-name track-bench \
  --project-path ~/work/generated-project \
  --package com.trackbench
```

参数约束：

- `--project-name`：必填，目标项目名，必须使用小写 kebab-case，例如 `track-bench`；该值写入 Maven POM 及其他项目标识
- `--project-path`：必填，目标项目目录；目录名与项目名相互独立，例如项目可以生成到 `~/work/generated-project`
- `--package`：必填，合法 Java 包名，例如 `com.trackbench`
- 首版不提供 `--force`；目标目录已存在且非空时直接失败，避免误覆盖

失败约束：

- 参数缺失时失败退出
- 包名不合法时失败退出
- 目标目录已存在且非空时失败退出
- 运行环境缺少 `python3`、`git` 或复制所需标准库能力时失败退出

## 5. 关键命名规则

### 5.1 项目名

- 源项目名：`java-admin-starter`
- 新项目名：取 `--project-name`，例如 `track-bench`

### 5.2 模块目录与 Maven 模块名

模板中的四个模块统一映射如下：

- `admin-boot` -> `{project-name}-boot`
- `admin-core` -> `{project-name}-core`
- `admin-mdm` -> `{project-name}-mdm`
- `admin-system` -> `{project-name}-system`

对应 Maven 坐标同步替换：

- 根 `artifactId`：`java-admin-starter` -> `{project-name}`
- 子模块 `artifactId`：`admin-*` -> `{project-name}-*`
- 子模块 `parent.artifactId`：`java-admin-starter` -> `{project-name}`
- 根 `pom.xml` 的 `<modules>` 列表同步替换为新模块目录名

### 5.3 包名

- 源根包：`com.example.admin`
- 目标根包：取 `--package`

Java/Kotlin 风格合法性仅按 Java 包名校验：

- 由 `.` 分段
- 每段仅允许字母、数字、下划线
- 每段首字符必须为字母或下划线
- 不允许空段

### 5.4 启动类

模板统一将启动类固定为：

- 类名：`BootApplication`
- 文件名：`BootApplication.java`

后续新项目初始化时不再按项目名动态生成启动类名。

## 6. 替换范围

### 6.1 必须替换的目录名

- 仓库根下四个模块目录
- 各模块 `src/main/java` 下根包目录
- 各模块 `src/test/java` 下根包目录

### 6.2 必须替换的内容标识

- `java-admin-starter`
- `java_admin_starter`
- `java-admin-starter-feature-sso`
- `admin-boot`
- `admin-core`
- `admin-mdm`
- `admin-system`
- `com.example.admin`
- `AdminBootApplication`

### 6.3 重点文件

至少保证以下文件被正确替换：

- 根 `pom.xml`
- 各模块 `pom.xml`
- `compose.yaml`
- `README.md`
- `admin-boot/src/main/resources/application.yml`
- `admin-boot/src/main/resources/application-dev.yml`
- `admin-boot/src/main/resources/application-test.yml`
- 启动类与其测试引用
- ArchUnit、契约测试、Flyway/模块测试中写死的模块路径、包名、类名

说明：

- 文档目录中的模板标识也执行统一文本替换，避免新项目仍残留 `java-admin-starter`
- `.git`、`target`、二进制文件不参与替换

## 7. 文件复制与目录重命名流程

脚本按以下顺序执行，避免路径失效：

1. 校验参数与目标目录
2. 从模板仓库复制到目标目录，排除：
   - `.git`
   - `target`
   - `.DS_Store`
   - `.claude` 中除 `skills` 软链外的本地状态
   - `.playwright-mcp`
   - `.vscode`
3. 在目标目录内先做顶层模块目录重命名
4. 迁移源码与测试包目录：
   - `.../src/main/java/com/example/admin` -> `.../src/main/java/<package-path>`
   - `.../src/test/java/com/example/admin` -> `.../src/test/java/<package-path>`
5. 将 `AdminBootApplication.java` 重命名为 `BootApplication.java`
6. 执行文本内容替换，并将生成项目 README 与 skill 中的当前分支文案规范化为 `main`
7. 删除任何复制残留的 Git 元数据（理论上复制阶段已排除）
8. 在目标目录执行 `git init --initial-branch=main`

## 8. 配置映射规则

以目标项目名 `track-bench`、目标包名 `com.trackbench` 为例：

- `spring.application.name: java-admin-starter` -> `track-bench`
- OpenAPI 标题 `java-admin-starter API 文档` -> `track-bench API 文档`
- Compose 项目标识 `java-admin-starter-feature-sso` -> `track-bench`
- 开发数据库名 `java_admin_starter_sso` -> `track_bench`
- 本地上传目录 `${user.home}/.java-admin-starter/uploads` -> `${user.home}/.track-bench/uploads`
- 测试上传目录 `${java.io.tmpdir}/java-admin-starter/test-uploads` -> `${java.io.tmpdir}/track-bench/test-uploads`

数据库命名规则：

- 将项目名中的 `-` 替换为 `_`
- 不保留模板中的 `_sso` 后缀

## 9. 实现方式

实现语言使用 Python 3 标准库，原因如下：

- 大量路径迁移、目录重命名与文本替换在 Python 中更可控
- 可跨平台稳定处理 UTF-8 文本与目录树
- 错误处理与参数校验比 shell 更清晰

实现结构建议：

- `argparse`：参数解析
- `pathlib`：路径处理
- `shutil.copytree`：目录复制
- `re`：包名校验与受控文本替换
- `subprocess.run`：执行 `git init`

## 10. 回归验证

实现前先补一个脚本级回归测试，至少验证以下行为：

- 成功从模板仓库生成一个新目录
- 新目录不存在 `.git`
- 重新初始化后的仓库存在 `.git`
- 根 `pom.xml` 的 `artifactId` 变为目标项目名
- 四个模块目录已按新项目名改名
- 启动类文件名为 `BootApplication.java`
- 关键文件不再包含 `com.example.admin`
- 关键文件不再包含 `AdminBootApplication`
- `application.yml` / `application-dev.yml` 中项目标识与路径被替换

测试方式：

- 使用 Python 测试脚本在临时目录中执行模板初始化
- 只检查文件系统和关键文本，不执行 Maven 构建

## 11. 风险与约束

### 11.1 文本替换误伤

风险：

- 全仓库替换 `admin-*` 容易误伤业务文本

约束：

- 对目录名与包名做结构化迁移
- 对内容替换采用明确映射表，不做模糊正则批量替换

### 11.2 目录重命名顺序错误

风险：

- 先改包名文本再改目录，可能导致路径找不到

约束：

- 固定“先目录、后文本”的执行顺序

### 11.3 脏目标目录覆盖

风险：

- 用户误把已有项目目录作为目标目录

约束：

- 目标目录存在且非空时直接失败

## 12. 后续实现清单

实现阶段拆为三步：

1. 新增脚本回归测试，先让测试失败
2. 实现 `scripts/init_template_project.py`
3. 跑回归测试，必要时补充一轮手工 smoke check
