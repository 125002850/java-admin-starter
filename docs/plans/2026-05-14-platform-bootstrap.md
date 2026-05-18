# Platform Bootstrap Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 基于当时的建模规范文档（原 `docs/create.md`，现已废弃）搭建一个可启动、可测试、可迁移数据库、具备多租户基础设施的 Java 模块化单体底座，完成 `demo-boot`、`demo-core`、`demo-system`、`demo-mdm` 四个模块的最小可用实现。

**Architecture:** 工程采用 Maven 多模块单体架构，业务链路固定为 `Controller -> AppService -> Domain/Service -> Infra/Mapper`。`demo-core` 仅承载跨模块稳定复用的基础设施，包括统一响应、异常、校验、TraceId、日志、租户、MyBatis-Plus、密码编码器和测试支撑；业务能力放在 `demo-system` 和 `demo-mdm` 中。默认所有业务数据表必须包含 `tenant_id`，平台级全局表必须显式标记为非租户表，并纳入 `TenantLineHandler` 忽略清单。

**Tech Stack:** JDK 17, Spring Boot 3.x, Maven, MyBatis-Plus, Flyway, MySQL 8, Jackson, Hibernate Validator, HikariCP, Knife4j, Spring Boot Actuator, JUnit 5, Spring Boot Test

---

> 当前目录还不是 Git 仓库，因此下面的 Commit 步骤在仓库接入 Git 后执行；如果仍未初始化 Git，就跳过提交步骤，只保留变更清单。
>
> 本计划只覆盖当前可以写出精确路径的“平台底座”部分。首个真实业务模块因为尚未确定模块名和业务域，不在本计划中硬编码占位路径，待业务名确定后再单独写第二份实现计划。
>
> 实施时请同时遵守 `@Test-Driven Development`、`@Verification Before Completion`、`@Defense-in-Depth Validation`。

### Task 1: 建立根工程和模块骨架

**Files:**
- Create: `pom.xml`
- Create: `demo-boot/pom.xml`
- Create: `demo-core/pom.xml`
- Create: `demo-system/pom.xml`
- Create: `demo-mdm/pom.xml`

**Step 1: 验证当前工程还没有 Maven 根工程**

Run: `mvn -q -DskipTests validate`
Expected: FAIL，提示当前目录没有可用的 `pom.xml`

**Step 2: 创建根 `pom.xml` 和四个子模块 `pom.xml`**

根 `pom.xml` 至少包含以下内容：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.demo</groupId>
    <artifactId>java-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.3.0</spring-boot.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <flyway.version>10.15.0</flyway.version>
        <knife4j.version>4.5.0</knife4j.version>
    </properties>

    <modules>
        <module>demo-boot</module>
        <module>demo-core</module>
        <module>demo-system</module>
        <module>demo-mdm</module>
    </modules>
</project>
```

每个子模块 `pom.xml` 先只做最小继承关系，确保能被聚合构建。

**Step 3: 复核现有 `README.md`，不要覆盖**

检查现有 `README.md` 是否已经包含以下最小信息：

```md
## 项目目标
## 模块说明
## 本地依赖
## 启动方式
## 数据库初始化
```

如果已存在，就不在 Task 1 改它；只有缺失这些最小小节时才补齐。

**Step 4: 运行 Maven 验证聚合工程**

Run: `mvn -q -DskipTests validate`
Expected: PASS

**Step 5: Commit**

```bash
git add pom.xml demo-boot/pom.xml demo-core/pom.xml demo-system/pom.xml demo-mdm/pom.xml
git commit -m "chore: initialize multi-module maven project"
```

### Task 2: 创建启动模块和多环境配置

**Files:**
- Modify: `demo-boot/pom.xml`
- Create: `demo-boot/src/main/java/com/demo/boot/DemoBootApplication.java`
- Create: `demo-boot/src/main/resources/application.yml`
- Create: `demo-boot/src/main/resources/application-dev.yml`
- Create: `demo-boot/src/main/resources/application-test.yml`

**Step 1: 先写最小启动验证**

Create: `demo-boot/src/test/java/com/demo/boot/DemoBootApplicationTests.java`

```java
package com.demo.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoBootApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

**Step 2: 运行测试，确认当前一定失败**

Run: `mvn -q -pl demo-boot -am test -Dtest=DemoBootApplicationTests`
Expected: FAIL，提示启动类或 Spring Boot 依赖缺失

**Step 3: 写最小可启动实现**

`demo-boot/pom.xml` 至少引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

启动类：

```java
package com.demo.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.demo")
public class DemoBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBootApplication.class, args);
    }
}
```

`application.yml` 至少包含：

```yaml
spring:
  application:
    name: java-demo
  profiles:
    active: dev

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

`application-dev.yml` 和 `application-test.yml` 先只放占位数据源与日志级别。

**Step 4: 重新运行测试**

Run: `mvn -q -pl demo-boot -am test -Dtest=DemoBootApplicationTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-boot/pom.xml demo-boot/src/main demo-boot/src/test
git commit -m "feat: add boot module and profiles"
```

### Task 3: 搭建 `demo-core` 包结构和统一响应模型

**Files:**
- Modify: `demo-core/pom.xml`
- Create: `demo-core/src/main/java/com/demo/core/web/R.java`
- Create: `demo-core/src/main/java/com/demo/core/web/PageReqDTO.java`
- Create: `demo-core/src/main/java/com/demo/core/web/PageResult.java`
- Create: `demo-core/src/test/java/com/demo/core/web/RTests.java`

**Step 1: 写统一响应对象的失败测试**

```java
package com.demo.core.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RTests {

    @Test
    void ok_should_wrap_data_with_code_200() {
        R<String> result = R.ok("value");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("ok");
        assertThat(result.getData()).isEqualTo("value");
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-core test -Dtest=RTests`
Expected: FAIL，提示 `R` 类不存在

**Step 3: 写最小实现**

`R.java` 至少包含：

```java
package com.demo.core.web;

public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
}
```

分页对象先只保留必要字段：

```java
public class PageReqDTO {
    private long pageNo = 1;
    private long pageSize = 20;
}
```

```java
public class PageResult<T> {
    private long total;
    private List<T> list;
}
```

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-core test -Dtest=RTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-core/pom.xml demo-core/src/main demo-core/src/test
git commit -m "feat: add core web response models"
```

### Task 4: 实现异常、参数校验和 Jackson 统一配置

**Files:**
- Modify: `demo-core/pom.xml`
- Create: `demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java`
- Create: `demo-core/src/main/java/com/demo/core/exception/BizException.java`
- Create: `demo-core/src/main/java/com/demo/core/validation/ValidationConfig.java`
- Create: `demo-core/src/main/java/com/demo/core/jackson/JacksonConfig.java`
- Create: `demo-boot/src/test/java/com/demo/boot/web/ValidationIntegrationTests.java`
- Create: `demo-boot/src/test/java/com/demo/boot/web/TestValidationController.java`

**Step 1: 写参数校验失败的集成测试**

`ValidationIntegrationTests.java`：

```java
package com.demo.boot.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ValidationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalid_request_should_return_wrapped_error() throws Exception {
        mockMvc.perform(post("/api/test/echo/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").exists())
            .andExpect(jsonPath("$.msg").exists());
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-boot -am test -Dtest=ValidationIntegrationTests`
Expected: FAIL，提示 Controller、异常处理器或校验配置缺失

**Step 3: 写最小实现**

创建测试用 Controller 和 DTO，DTO 上使用 `@NotBlank`。全局异常处理器至少处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handleValidation(MethodArgumentNotValidException ex) {
        return R.fail(400, "参数校验失败");
    }

    @ExceptionHandler(BizException.class)
    public R<String> handleBiz(BizException ex) {
        return R.fail(ex.getCode(), ex.getMessage());
    }
}
```

Jackson 配置至少显式注册 `LocalDateTime` 与 `LocalDate` 格式。

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-boot -am test -Dtest=ValidationIntegrationTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/exception demo-core/src/main/java/com/demo/core/validation demo-core/src/main/java/com/demo/core/jackson demo-boot/src/test/java/com/demo/boot/web
git commit -m "feat: add validation and global exception handling"
```

### Task 5: 实现 TraceId、日志配置和健康检查

**Files:**
- Create: `demo-core/src/main/java/com/demo/core/trace/TraceIdFilter.java`
- Create: `demo-boot/src/main/resources/logback-spring.xml`
- Modify: `demo-boot/src/main/resources/application.yml`
- Create: `demo-boot/src/test/java/com/demo/boot/trace/TraceIdFilterTests.java`

**Step 1: 写 TraceId 透传测试**

说明：业务 API 统一使用 `POST` 的约束只适用于业务接口；`/actuator/health` 属于 Spring Boot 平台健康检查端点，使用 `GET` 不视为违反业务 API 规范。

```java
package com.demo.boot.trace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class TraceIdFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_echo_trace_id_header() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Trace-Id", "trace-123"))
            .andExpect(header().string("X-Trace-Id", "trace-123"));
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-boot -am test -Dtest=TraceIdFilterTests`
Expected: FAIL，响应头中没有 `X-Trace-Id`

**Step 3: 写最小实现**

`TraceIdFilter.java` 至少做三件事：

```java
String traceId = Optional.ofNullable(request.getHeader("X-Trace-Id"))
    .filter(StringUtils::hasText)
    .orElse(UUID.randomUUID().toString());
MDC.put("traceId", traceId);
response.setHeader("X-Trace-Id", traceId);
```

`logback-spring.xml` 至少包含：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} traceId=%X{traceId} - %msg%n</pattern>
```

`application.yml` 保留 Actuator 暴露：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-boot -am test -Dtest=TraceIdFilterTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/trace demo-boot/src/main/resources/logback-spring.xml demo-boot/src/main/resources/application.yml demo-boot/src/test/java/com/demo/boot/trace
git commit -m "feat: add trace id filter and logging config"
```

### Task 6: 实现租户基础设施和 MyBatis-Plus 基础配置

**Files:**
- Modify: `demo-core/pom.xml`
- Create: `demo-core/src/main/java/com/demo/core/tenant/TenantContext.java`
- Create: `demo-core/src/main/java/com/demo/core/tenant/TenantIgnoreTables.java`
- Create: `demo-core/src/main/java/com/demo/core/mybatis/MybatisPlusConfig.java`
- Create: `demo-core/src/main/java/com/demo/core/mybatis/CommonMetaObjectHandler.java`
- Create: `demo-core/src/test/java/com/demo/core/tenant/TenantIgnoreTablesTests.java`
- Create: `demo-core/src/test/java/com/demo/core/tenant/TenantContextTests.java`

**Step 1: 写非租户表忽略清单的失败测试**

```java
package com.demo.core.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIgnoreTablesTests {

    @Test
    void should_mark_platform_tables_as_ignored() {
        assertThat(TenantIgnoreTables.contains("sys_tenant")).isTrue();
        assertThat(TenantIgnoreTables.contains("sys_user")).isFalse();
    }
}
```

再补一个 `TenantContext` 最小行为测试：

```java
package com.demo.core.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTests {

    @Test
    void should_get_same_tenant_after_set_and_return_null_after_clear() {
        TenantContext.setTenantId(100L);
        assertThat(TenantContext.getTenantId()).isEqualTo(100L);
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-core test -Dtest=TenantIgnoreTablesTests`
Expected: FAIL，提示 `TenantIgnoreTables` 不存在

**Step 3: 写最小实现**

`TenantIgnoreTables.java` 至少显式维护一个不可变集合：

```java
private static final Set<String> TABLES = Set.of(
    "sys_tenant_global",
    "sys_dict_type_global"
);
```

`MybatisPlusConfig.java` 至少完成：

```java
MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
    @Override
    public Expression getTenantId() { ... }

    @Override
    public boolean ignoreTable(String tableName) {
        return TenantIgnoreTables.contains(tableName);
    }
}));
```

并显式配置逻辑删除取值，避免 `deleted bigint` 与默认逻辑删除约定不一致：

```java
@Bean
public GlobalConfig globalConfig() {
    GlobalConfig globalConfig = new GlobalConfig();
    GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
    dbConfig.setLogicDeleteField("deleted");
    dbConfig.setLogicDeleteValue("1");
    dbConfig.setLogicNotDeleteValue("0");
    globalConfig.setDbConfig(dbConfig);
    return globalConfig;
}
```

`CommonMetaObjectHandler.java` 至少填充 `createBy`、`updateBy`。

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-core test -Dtest=TenantIgnoreTablesTests`
Expected: PASS

Run: `mvn -q -pl demo-core test -Dtest=TenantContextTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/tenant demo-core/src/main/java/com/demo/core/mybatis demo-core/src/test/java/com/demo/core/tenant
git commit -m "feat: add tenant and mybatis-plus base config"
```

### Task 7: 建立测试基础设施和数据库迁移

**Files:**
- Modify: `demo-boot/pom.xml`
- Create: `demo-boot/src/test/resources/application-test.yml`
- Create: `demo-boot/src/main/resources/db/migration/V1__init_platform_tables.sql`
- Create: `demo-boot/src/test/java/com/demo/boot/flyway/FlywaySmokeTests.java`

**Step 1: 写数据库迁移冒烟测试**

```java
package com.demo.boot.flyway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FlywaySmokeTests {

    @Test
    void contextLoadsAfterFlywayMigration() {
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests`
Expected: FAIL，提示数据源、Flyway 或迁移脚本缺失

**Step 3: 写最小实现**

`demo-boot/pom.xml` 增加：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

`application-test.yml` 必须显式使用 H2 内存库，避免测试环境 fallback 到 `dev` 的 MySQL 配置：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
    driver-class-name: org.h2.Driver
    username: sa
    password:
  flyway:
    enabled: true
```

迁移脚本至少创建平台底座表，并显式区分：

```sql
create table sys_tenant_global (
  id bigint primary key,
  tenant_name varchar(128) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除'
);

create table sys_user (
  id bigint primary key,
  tenant_id bigint not null,
  username varchar(64) not null,
  password varchar(255) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除'
);
```

同时在 `application-dev.yml` 中放入 Hikari 关键参数占位：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000
```

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-boot -am test -Dtest=FlywaySmokeTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-boot/pom.xml demo-boot/src/main/resources/db/migration demo-boot/src/test/resources/application-test.yml demo-boot/src/test/java/com/demo/boot/flyway
git commit -m "feat: add flyway and database bootstrap"
```

### Task 8: 实现 `demo-system` 的登录闭环

**Files:**
- Modify: `demo-system/pom.xml`
- Create: `demo-system/src/main/java/com/demo/system/controller/AuthController.java`
- Create: `demo-system/src/main/java/com/demo/system/app/AuthAppService.java`
- Create: `demo-system/src/main/java/com/demo/system/service/AuthService.java`
- Create: `demo-system/src/main/java/com/demo/system/infra/entity/SysUserEntity.java`
- Create: `demo-system/src/main/java/com/demo/system/infra/mapper/SysUserMapper.java`
- Create: `demo-system/src/main/java/com/demo/system/security/PasswordConfig.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/LoginReqDTO.java`
- Create: `demo-system/src/main/java/com/demo/system/controller/dto/LoginRspDTO.java`
- Create: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`

**Step 1: 写登录失败测试**

```java
package com.demo.system;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowTests {

    @Test
    void password_should_not_be_stored_as_plain_text() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("admin123");
        assertThat(encoded).isNotEqualTo("admin123");
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests`
Expected: FAIL，提示 `PasswordEncoder` 配置和 system 模块实现缺失

**Step 3: 写最小实现**

严格按链路落地：

```text
AuthController -> AuthAppService -> AuthService -> SysUserMapper
```

`PasswordConfig.java`：

```java
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

`LoginReqDTO` 至少包含：

```java
@NotNull
private Long tenantId;

@NotBlank
private String username;

@NotBlank
private String password;
```

`AuthService` 查询用户时必须同时使用 `tenantId + username` 作为过滤条件，`AuthController` 返回 `R<LoginRspDTO>`，不直接操作 Mapper。

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-system/pom.xml demo-system/src/main demo-system/src/test
git commit -m "feat: add minimal system auth flow"
```

### Task 9: 实现 `demo-mdm` 的最小主数据能力

**Files:**
- Modify: `demo-mdm/pom.xml`
- Create: `demo-mdm/src/main/java/com/demo/mdm/controller/DictController.java`
- Create: `demo-mdm/src/main/java/com/demo/mdm/app/DictAppService.java`
- Create: `demo-mdm/src/main/java/com/demo/mdm/service/DictService.java`
- Create: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/DictTypeEntity.java`
- Create: `demo-mdm/src/main/java/com/demo/mdm/infra/entity/DictItemEntity.java`
- Create: `demo-mdm/src/main/java/com/demo/mdm/infra/mapper/DictTypeMapper.java`
- Create: `demo-mdm/src/main/java/com/demo/mdm/infra/mapper/DictItemMapper.java`
- Create: `demo-mdm/src/test/java/com/demo/mdm/DictModuleSmokeTests.java`

**Step 1: 写最小主数据模块冒烟测试**

```java
package com.demo.mdm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DictModuleSmokeTests {

    @Test
    void contextLoads() {
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests`
Expected: FAIL，提示 mdm 模块实现缺失

**Step 3: 写最小实现**

先只实现字典，不预建供应商、仓库、客户。模块链路严格保持：

```text
DictController -> DictAppService -> DictService -> DictTypeMapper / DictItemMapper
```

实体至少保留：

```java
private Long id;
private Long tenantId;
private String dictTypeCode;
private String dictItemCode;
private String dictItemName;
private LocalDateTime createTime;
private LocalDateTime updateTime;
private Long createBy;
private Long updateBy;
private Long deleted;
```

**Step 4: 运行测试确认通过**

Run: `mvn -q -pl demo-mdm -am test -Dtest=DictModuleSmokeTests`
Expected: PASS

**Step 5: Commit**

```bash
git add demo-mdm/pom.xml demo-mdm/src/main demo-mdm/src/test
git commit -m "feat: add minimal mdm dictionary module"
```

### Task 10: 做整体验证并冻结底座版本

**Files:**
- Modify: `README.md`
- Historical reference: `docs/create.md`（已废弃并删除）
- Create: `docs/plans/2026-05-14-first-business-module-TODO.md`

**Step 1: 写最终验证清单文件**

`docs/plans/2026-05-14-first-business-module-TODO.md` 只写占位清单，不写实现细节：

```md
# First Business Module TODO

- 确认业务模块名
- 确认首个业务实体
- 确认接口列表
- 另起实现计划
```

**Step 2: 跑全量验证**

Run: `mvn -q -DskipTests validate`
Expected: PASS

Run: `mvn test`
Expected: PASS

**Step 3: 手工检查底座约束**

核对以下几点：

- `Controller -> AppService -> Domain/Service -> Infra/Mapper` 是否被遵守
- 默认业务表是否包含 `tenant_id`
- 平台级全局表是否加入 `TenantLineHandler` 忽略清单
- `demo-core` 是否仍然保持薄核心
- 是否没有提前引入文件存储 SPI、翻译引擎实现、完整 `OperationLog` 落库链路

**Step 4: 更新文档**

在 `README.md` 增加：

```md
## 当前完成状态
- boot/core/system/mdm 底座已完成
- 首个业务模块待单独规划
```

原计划要求在 `docs/create.md` 增加一句执行状态说明。该文档现已废弃并删除，当前项目状态以 `README.md` 为准。

**Step 5: Commit**

```bash
git add README.md docs/plans/2026-05-14-first-business-module-TODO.md
git commit -m "docs: record bootstrap completion and next planning gate"
```

Plan complete and saved to `docs/plans/2026-05-14-platform-bootstrap.md`. Two execution options:

1. Subagent-Driven (this session) - I dispatch fresh subagent per task, review between tasks, fast iteration
2. Parallel Session (separate) - Open new session with executing-plans, batch execution with checkpoints

Which approach?
