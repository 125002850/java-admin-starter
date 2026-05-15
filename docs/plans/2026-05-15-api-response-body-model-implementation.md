# API Response Body Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `demo-core` / `demo-system` / `demo-boot` 的响应体、错误码、业务异常和全局异常处理与 [docs/api-response-body-model.md](/Users/youdingte/studys/java-demo/docs/api-response-body-model.md:1) 一致，并补齐可防退化的测试与文档。

**Architecture:** 本计划按“行为约束优先”实现 spec：`ErrorCode` 统一提供 `getCode()/getMsg()`，`BizException` 只持有 `ErrorCode`，全局异常处理同时输出真实 HTTP Status 和统一 body。`R` 在本计划中明确实现为“`final class` + 私有构造器 + 静态工厂”，不采用 spec 示例里的 `record`，因为 Java `record` 无法满足“禁止外部直接 `new R(...)`”这一行为约束；若后续要回到 `record`，必须先同步放宽 spec 的该条约束再另起变更。错误码号段方面，本计划把 spec 第五节的分段表视为建议而非强制迁移项：公共码固定使用 `200 / 400 / 401 / 403 / 404 / 429 / 500`，业务模块继续沿用仓库现有的 7 位号段约定，例如认证 `2001xxx`、主数据 `3001xxx`、测试/保留 `9000xxx`，本次响应模型改造不做全仓重编号。

**Tech Stack:** Java 17, Spring Boot 3.3, Jackson, Spring Validation, MockMvc, JUnit 5

---

### Task 1: 锁定响应契约红灯测试

**Files:**
- Modify: `demo-core/src/test/java/com/demo/core/web/RTests.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/web/ValidationIntegrationTests.java`
- Modify: `demo-system/src/test/java/com/demo/system/AuthFlowTests.java`

- [ ] **Step 1: 先把 spec 行为写成失败测试**

`RTests.java` 追加失败用例，锁定响应工厂与错误码接口：

```java
@Test
void fail_should_wrap_error_code_and_null_data() {
    R<Void> result = R.fail(CommonErrorCode.FAILED);

    assertThat(result.getCode()).isEqualTo(500);
    assertThat(result.getMsg()).isEqualTo("操作失败");
    assertThat(result.getData()).isNull();
}

@Test
void fail_with_override_msg_should_keep_error_code_and_override_message() {
    R<Void> result = R.fail(CommonErrorCode.PARAM_ERROR, "content不能为空");

    assertThat(result.getCode()).isEqualTo(400);
    assertThat(result.getMsg()).isEqualTo("content不能为空");
    assertThat(result.getData()).isNull();
}
```

`ValidationIntegrationTests.java` 统一按 spec 收敛为：

```java
mockMvc.perform(post("/api/test/echo/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
    .andExpect(status().isUnprocessableEntity())
    .andExpect(jsonPath("$.code").value(400))
    .andExpect(jsonPath("$.msg").value("content不能为空"));

mockMvc.perform(post("/api/test/echo/method").param("content", ""))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.code").value(400))
    .andExpect(jsonPath("$.msg").value("content不能为空"));

mockMvc.perform(post("/api/test/echo/time/echo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "day": "2026/05/14",
                  "time": "2026-05-14T10:20:30"
                }
                """))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.code").value(400))
    .andExpect(jsonPath("$.msg").value("参数错误"));

mockMvc.perform(post("/api/test/echo/panic"))
    .andExpect(status().isInternalServerError())
    .andExpect(jsonPath("$.code").value(500))
    .andExpect(jsonPath("$.msg").value("操作失败"));
```

`AuthFlowTests.java` 继续锁定业务失败 HTTP 200 + 业务 code：

```java
mockMvc.perform(post("/api/system/auth/login")
        .contentType(APPLICATION_JSON)
        .content("{\"tenantId\":200,\"username\":\"admin\",\"password\":\"admin123\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(2001001))
    .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
```

- [ ] **Step 2: 运行红灯测试确认当前实现不满足 spec**

Run: `mvn -q -pl demo-core -am test -Dtest=RTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，提示 `R.fail(ErrorCode)` / `R.fail(ErrorCode, String)` 缺失

Run: `mvn -q -pl demo-boot -am test -Dtest=ValidationIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，当前实现会返回 422/500 与 body.code 不一致

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS 或局部 FAIL；若 PASS 说明业务失败链路已基本符合 spec，可作为保护网保留

- [ ] **Step 3: Commit**

```bash
git add demo-core/src/test/java/com/demo/core/web/RTests.java \
  demo-boot/src/test/java/com/demo/boot/web/ValidationIntegrationTests.java \
  demo-system/src/test/java/com/demo/system/AuthFlowTests.java
git commit -m "test: lock api response body contract"
```

### Task 2: 重构核心错误码与响应抽象

**Files:**
- Modify: `demo-core/src/main/java/com/demo/core/exception/ErrorCode.java`
- Modify: `demo-core/src/main/java/com/demo/core/exception/CommonErrorCode.java`
- Modify: `demo-core/src/main/java/com/demo/core/exception/BizException.java`
- Modify: `demo-core/src/main/java/com/demo/core/web/R.java`
- Modify: `demo-system/src/main/java/com/demo/system/enums/AuthErrorCode.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/web/TestValidationController.java`

- [ ] **Step 1: 统一 `ErrorCode` 接口命名**

把接口改成与 spec 一致：

```java
public interface ErrorCode {

    int getCode();

    String getMsg();
}
```

- [ ] **Step 2: 改造 `CommonErrorCode`、统一业务枚举接口并确认号段策略**

`CommonErrorCode.java` 对齐 spec：

```java
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(200, "ok"),
    FAILED(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁");

    private final int code;
    private final String msg;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
```

`AuthErrorCode.java` 和 `TestValidationController.TestErrorCode` 同步改成 `getCode()/getMsg()`，同时明确本次改造不重编已有业务码，只保留现有 7 位模块号段：

```java
public enum AuthErrorCode implements ErrorCode {

    USERNAME_OR_PASSWORD_INVALID(2001001, "用户名或密码错误"),
    USERNAME_DUPLICATED(2001002, "用户名重复，请联系管理员处理");

    private final int code;
    private final String msg;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}

private enum TestErrorCode implements ErrorCode {

    BIZ_FAILURE(9000001, "业务失败");

    private final int code;
    private final String msg;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
```

补充约束说明：

- `200 / 400 / 401 / 403 / 404 / 429 / 500` 只允许 `CommonErrorCode` 占用。
- `AuthErrorCode` 继续使用 `2001xxx`，不为了贴合 spec 示例表改成 5 位码。
- `TestErrorCode` 作为测试专用业务码保留在 `9000xxx` 保留段，契约测试只校验“唯一且不占用公共码”，不在本次计划里强推全仓号段迁移。

- [ ] **Step 3: 改造 `BizException`**

保留 spec 要求的两个构造器：

```java
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 4: 改造 `R<T>`，只暴露工厂方法**

按“禁止外部直接 `new R(...)`”约束实现不可变类：

```java
public final class R<T> {

    private final int code;
    private final String msg;
    private final T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String msg) {
        return new R<>(errorCode.getCode(), msg, null);
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }
}
```

- [ ] **Step 5: 运行核心测试**

Run: `mvn -q -pl demo-core -am test -Dtest=RTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/exception/ErrorCode.java \
  demo-core/src/main/java/com/demo/core/exception/CommonErrorCode.java \
  demo-core/src/main/java/com/demo/core/exception/BizException.java \
  demo-core/src/main/java/com/demo/core/web/R.java \
  demo-system/src/main/java/com/demo/system/enums/AuthErrorCode.java \
  demo-boot/src/test/java/com/demo/boot/web/TestValidationController.java
git commit -m "refactor: align response and error code abstractions"
```

### Task 3: 补齐全局异常映射与现有调用点

**Files:**
- Modify: `demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java`
- Modify: `demo-system/src/main/java/com/demo/system/service/AuthService.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/web/TestValidationController.java`
- Modify: `demo-boot/src/test/java/com/demo/boot/web/ValidationIntegrationTests.java`

- [ ] **Step 1: 新增 Logger 字段并补齐异常映射**

`GlobalExceptionHandler.java` 先补 `private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);`，再覆盖以下场景。`HandlerMethodValidationException` 和 `ConstraintViolationException` 分开处理，避免把两个不同 API 混成一个未定义 helper：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        return ResponseEntity.ok(R.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(CommonErrorCode.PARAM_ERROR.getMsg());
        return ResponseEntity.unprocessableEntity().body(R.fail(CommonErrorCode.PARAM_ERROR, msg));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<R<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        ErrorCode errorCode = ex.isForReturnValue() ? CommonErrorCode.FAILED : CommonErrorCode.PARAM_ERROR;
        return ResponseEntity.status(ex.getStatusCode()).body(R.fail(errorCode, extractValidationMessage(ex)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(R.fail(CommonErrorCode.PARAM_ERROR, extractValidationMessage(ex)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(R.fail(CommonErrorCode.PARAM_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        log.error("system error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail(CommonErrorCode.FAILED));
    }

    private String extractValidationMessage(HandlerMethodValidationException ex) {
        return ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(CommonErrorCode.PARAM_ERROR.getMsg());
    }

    private String extractValidationMessage(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(CommonErrorCode.PARAM_ERROR.getMsg());
    }
}
```

这一步需要显式补齐 import：`org.slf4j.Logger`、`org.slf4j.LoggerFactory`、`org.springframework.context.MessageSourceResolvable`、`org.springframework.util.StringUtils`、`jakarta.validation.ConstraintViolation`。

- [ ] **Step 2: 保持业务调用点只抛 `BizException(ErrorCode)`**

`AuthService.java` 只保留：

```java
if (users.size() > 1) {
    throw new BizException(AuthErrorCode.USERNAME_DUPLICATED);
}
if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
    throw new BizException(AuthErrorCode.USERNAME_OR_PASSWORD_INVALID);
}
```

`TestValidationController.java` 保持业务失败入口：

```java
@PostMapping("/fail")
public R<Void> fail() {
    throw new BizException(TestErrorCode.BIZ_FAILURE);
}
```

- [ ] **Step 3: 运行异常链路测试**

Run: `mvn -q -pl demo-boot -am test -Dtest=ValidationIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

Run: `mvn -q -pl demo-system -am test -Dtest=AuthFlowTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add demo-core/src/main/java/com/demo/core/exception/GlobalExceptionHandler.java \
  demo-system/src/main/java/com/demo/system/service/AuthService.java \
  demo-boot/src/test/java/com/demo/boot/web/TestValidationController.java \
  demo-boot/src/test/java/com/demo/boot/web/ValidationIntegrationTests.java
git commit -m "feat: map api response errors by spec"
```

### Task 4: 增加错误码与防退化测试

**Files:**
- Create: `demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java`
- Modify: `demo-boot/pom.xml`

- [ ] **Step 1: 在聚合启动模块中增加契约测试**

`ErrorCodeContractTests.java` 用 `demo-boot` 作为扫描入口，覆盖 `demo-core`、`demo-system`、`demo-mdm`。`ErrorCode` enum 扫描使用 Spring 的 `ClassPathScanningCandidateComponentProvider`；源码反模式检查通过 `repoRoot` helper 定位模块源码目录，不能再使用依赖 working directory 的 `Files.walk(Path.of("."))`：

```java
class ErrorCodeContractTests {

    @Test
    void all_error_code_enums_should_have_unique_codes_and_non_blank_msgs() {
        Set<Class<?>> errorCodeEnums = scanErrorCodeEnums("com.demo");
        Map<Integer, String> owners = new HashMap<>();

        for (Class<?> errorCodeEnum : errorCodeEnums) {
            for (Object constant : errorCodeEnum.getEnumConstants()) {
                ErrorCode value = (ErrorCode) constant;
                assertThat(value.getMsg()).isNotBlank();
                String previous = owners.putIfAbsent(value.getCode(), errorCodeEnum.getName() + "#" + constant);
                assertThat(previous).as("duplicate error code: " + value.getCode()).isNull();
            }
        }
    }

    @Test
    void business_error_codes_should_not_reuse_common_http_codes() {
        Set<Integer> reserved = Set.of(200, 400, 401, 403, 404, 429, 500);
        Set<Class<?>> errorCodeEnums = scanErrorCodeEnums("com.demo");
        for (Class<?> errorCodeEnum : errorCodeEnums) {
            if (errorCodeEnum == CommonErrorCode.class) {
                continue;
            }
            for (Object constant : errorCodeEnum.getEnumConstants()) {
                ErrorCode value = (ErrorCode) constant;
                assertThat(reserved).doesNotContain(value.getCode());
            }
        }
    }

    @Test
    void main_sources_should_not_new_r_or_throw_runtime_exception_for_business_errors() throws Exception {
        List<Path> sources = scanMainSourceFiles();

        for (Path source : sources) {
            String content = Files.readString(source);
            assertThat(content).doesNotContain("new R<>(").doesNotContain("new R(");
            assertThat(content).doesNotContain("throw new RuntimeException(");
        }
    }

    private static Set<Class<?>> scanErrorCodeEnums(String basePackage) {
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));

        return provider.findCandidateComponents(basePackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(Objects::nonNull)
                .map(ErrorCodeContractTests::loadClass)
                .filter(Class::isEnum)
                .filter(ErrorCode.class::isAssignableFrom)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("failed to load " + className, ex);
        }
    }

    private static List<Path> scanMainSourceFiles() throws IOException {
        Path repoRoot = resolveRepoRoot();
        List<Path> sourceRoots = List.of(
                repoRoot.resolve("demo-core/src/main/java"),
                repoRoot.resolve("demo-system/src/main/java"),
                repoRoot.resolve("demo-mdm/src/main/java"),
                repoRoot.resolve("demo-boot/src/main/java"));
        List<Path> sources = new ArrayList<>();

        for (Path sourceRoot : sourceRoots) {
            if (Files.notExists(sourceRoot)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                sources.addAll(stream
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList());
            }
        }
        return sources;
    }

    private static Path resolveRepoRoot() throws IOException {
        String mavenRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(mavenRoot)) {
            return Path.of(mavenRoot).toAbsolutePath().normalize();
        }

        try {
            Path location = Path.of(ErrorCodeContractTests.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toAbsolutePath().normalize();
            for (Path current = location; current != null; current = current.getParent()) {
                if (Files.exists(current.resolve("README.md"))
                        && Files.exists(current.resolve("demo-core/pom.xml"))
                        && Files.exists(current.resolve("demo-boot/pom.xml"))) {
                    return current;
                }
            }
        } catch (URISyntaxException ex) {
            throw new IOException("failed to resolve repository root", ex);
        }

        throw new IOException("failed to locate repository root");
    }
}
```

这一步需要补的 import 至少包括：`java.io.IOException`、`java.net.URISyntaxException`、`java.nio.file.Files`、`java.nio.file.Path`、`java.util.*`、`java.util.stream.Collectors`、`java.util.stream.Stream`、`org.springframework.beans.factory.config.BeanDefinition`、`org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider`、`org.springframework.core.type.filter.AssignableTypeFilter`、`org.springframework.util.StringUtils`。

- [ ] **Step 2: 若 `demo-boot` 的测试类路径未自动带上业务模块，补齐依赖**

`demo-boot/pom.xml` 保持或补齐：

```xml
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>demo-system</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>demo-mdm</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 3: 运行契约测试**

Run: `mvn -q -pl demo-boot -am test -Dtest=ErrorCodeContractTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add demo-boot/src/test/java/com/demo/boot/contract/ErrorCodeContractTests.java demo-boot/pom.xml
git commit -m "test: add api response body contract guards"
```

### Task 5: 同步文档并做整体验证

**Files:**
- Modify: `README.md`
- Modify: `docs/create.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 统一 README / docs/create / AGENTS 口径**

`README.md` 的“错误码规范”改成与 spec 一致：

```md
### 错误码规范

- 成功响应固定为 `code = 200`、`msg = ok`。
- 默认失败响应使用 `CommonErrorCode.FAILED(500, "操作失败")`。
- 参数错误、未登录、无权限、资源不存在、限流使用公共 HTTP 语义码：`400 / 401 / 403 / 404 / 429`。
- 模块私有业务码使用独立号段，例如 `demo-system` 当前使用 `2001xxx`。
- `BizException` 只接受 `ErrorCode`，禁止业务代码散落裸错误码、裸失败文案。
```

`docs/create.md` 对应章节移除 `1000xxx` 示例，改成：

```md
- 公共错误码使用稳定通用码，例如 `200 / 400 / 401 / 403 / 404 / 429 / 500`。
- 业务模块错误码放在模块内 `enums`，例如认证 `2001xxx`、主数据 `3001xxx`。
```

`AGENTS.md` 增加一条仓库约束：

```md
- API 响应体统一使用 `R.ok(...)` / `R.fail(...)`，业务异常统一使用 `BizException(ErrorCode)`。
```

- [ ] **Step 2: 运行全量验证**

Run: `mvn -q -DskipTests validate`
Expected: PASS

Run: `mvn test`
Expected: PASS

Run: `mvn compile`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add README.md docs/create.md AGENTS.md
git commit -m "docs: sync api response body rules"
```
