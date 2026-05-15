# API 响应模型改造计划

## 目标

统一响应结构为公司现有风格：

```json
{
  "code": 200,
  "msg": "ok",
  "data": {}
}
```

设计原则：

* `code=200` 表示默认成功
* `code=500` 表示默认失败
* 允许业务领域特殊 code
* 所有 code/msg 必须来自实现 `ErrorCode` 的 enum
* 禁止手写魔法数字、魔法字符串
* HTTP Status 仍保留真实协议语义

---

## 一、定义 ErrorCode 接口

新增统一接口：

```java
public interface ErrorCode {

    int getCode();

    String getMsg();
}
```

---

## 二、定义 CommonErrorCode

新增通用错误码 enum：

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

    CommonErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

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

注意：

* 不要同时定义多个 `500`
* `FAILED(500, "操作失败")` 作为默认失败即可

---

## 三、定义统一响应体 R

```java
public record R<T>(
    int code,
    String msg,
    T data
) {

    public static <T> R<T> ok(T data) {
        return new R<>(
            CommonErrorCode.SUCCESS.getCode(),
            CommonErrorCode.SUCCESS.getMsg(),
            data
        );
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(
            errorCode.getCode(),
            errorCode.getMsg(),
            null
        );
    }

    public static <T> R<T> fail(ErrorCode errorCode, String msg) {
        return new R<>(
            errorCode.getCode(),
            msg,
            null
        );
    }
}
```

约束：

* 外部禁止直接 `new R<>(200, "ok", data)`
* 业务代码统一使用 `R.ok(...)` / `R.fail(...)`

---

## 四、定义 BizException

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

约束：

* 不提供 `BizException(int code, String msg)`
* 不提供 `BizException(String msg)`
* 避免魔法数字和随意文案

---

## 五、业务领域错误码

按模块拆分 enum。

示例：

```java
public enum OrderErrorCode implements ErrorCode {

    OUT_OF_STOCK(20001, "库存不足"),

    ORDER_CLOSED(20002, "订单已关闭"),

    ORDER_STATUS_INVALID(20003, "订单状态不允许当前操作");

    private final int code;
    private final String msg;

    OrderErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

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

建议 code 分段：

| 模块 | code 范围     |
| -- | ----------- |
| 通用 | 0-999       |
| 用户 | 10000-19999 |
| 订单 | 20000-29999 |
| 支付 | 30000-39999 |
| 系统 | 90000-99999 |

---

## 六、全局异常处理

新增或改造 `GlobalExceptionHandler`。

### 业务异常

```java
@ExceptionHandler(BizException.class)
public ResponseEntity<R<Void>> handleBizException(BizException ex) {
    return ResponseEntity.ok(
        R.fail(ex.getErrorCode(), ex.getMessage())
    );
}
```

### 参数校验异常

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<R<Void>> handleValidException(
    MethodArgumentNotValidException ex
) {
    String msg = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .findFirst()
        .map(FieldError::getDefaultMessage)
        .orElse(CommonErrorCode.PARAM_ERROR.getMsg());

    return ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(R.fail(CommonErrorCode.PARAM_ERROR, msg));
}
```

### 未登录 / 无权限

按项目实际安全框架接入：

* 未登录：HTTP 401 + `CommonErrorCode.UNAUTHORIZED`
* 无权限：HTTP 403 + `CommonErrorCode.FORBIDDEN`

### 系统异常

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<R<Void>> handleException(Exception ex) {
    log.error("system error", ex);

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(R.fail(CommonErrorCode.FAILED));
}
```

---

## 七、Controller 改造规范

成功返回：

```java
return R.ok(data);
```

默认业务失败：

```java
throw new BizException(CommonErrorCode.FAILED);
```

特殊业务失败：

```java
throw new BizException(OrderErrorCode.OUT_OF_STOCK);
```

带动态提示：

```java
throw new BizException(
    OrderErrorCode.ORDER_STATUS_INVALID,
    "当前订单状态不允许取消"
);
```

---

## 八、HTTP Status 与 body.code 约定

| 场景     | HTTP Status | body.code |
| ------ | ----------: | --------: |
| 成功     |         200 |       200 |
| 普通业务失败 |         200 |       500 |
| 特殊业务失败 |         200 | 业务领域 code |
| 参数错误   |   400 / 422 |       400 |
| 未登录    |         401 |       401 |
| 无权限    |         403 |       403 |
| 资源不存在  |         404 |       404 |
| 限流     |         429 |       429 |
| 系统异常   |         500 |       500 |

---

## 九、校验与防退化

新增单元测试：

1. 扫描所有实现 `ErrorCode` 的 enum
2. 校验 `code` 不重复
3. 校验 `msg` 非空
4. 校验业务 code 不占用通用 HTTP code
5. 禁止业务代码中直接出现 `new R<>(...)`
6. 禁止 `throw new RuntimeException("业务失败")` 这类业务异常写法

---

## 十、执行顺序

1. 新增 `ErrorCode`
2. 新增 `CommonErrorCode`
3. 新增或改造 `R<T>`
4. 新增或改造 `BizException`
5. 新增或改造 `GlobalExceptionHandler`
6. 迁移现有 Controller 返回值
7. 迁移业务异常抛出方式
8. 增加 ErrorCode 唯一性测试
9. 更新 README / AGENTS.md 中的 API 响应规范
10. 运行测试与接口联调
