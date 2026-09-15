package com.oigit.admin.core.exception;

import com.oigit.admin.core.web.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        return ResponseEntity.ok(R.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex
    ) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + "：" + (StringUtils.hasText(error.getDefaultMessage())
                        ? error.getDefaultMessage() : CommonErrorCode.PARAM_ERROR.getMsg()))
                .orElse(CommonErrorCode.PARAM_ERROR.getMsg());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(R.fail(CommonErrorCode.PARAM_ERROR, msg));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<R<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        ErrorCode errorCode = ex.isForReturnValue() ? CommonErrorCode.FAILED : CommonErrorCode.PARAM_ERROR;
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(R.fail(errorCode, extractValidationMessage(ex)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity
                .badRequest()
                .body(R.fail(CommonErrorCode.PARAM_ERROR, extractValidationMessage(ex)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .badRequest()
                .body(R.fail(CommonErrorCode.PARAM_ERROR, "缺少必填参数：" + ex.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .badRequest()
                .body(R.fail(CommonErrorCode.PARAM_ERROR));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .badRequest()
                .body(R.fail(CommonErrorCode.PARAM_ERROR, "参数类型错误：" + ex.getName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String msg = "请求方法 " + ex.getMethod() + "，请使用 " + ex.getSupportedHttpMethods().stream().map(HttpMethod::name).collect(Collectors.joining(", ")) + " 请求";
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(CommonErrorCode.PARAM_ERROR, msg));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .badRequest()
                .body(R.fail(CommonErrorCode.PARAM_ERROR, "上传文件大小超过服务器配置的限制"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        // 客户端主动断开连接（如取消请求、页面刷新、网关提前超时），静默降级处理不记录系统 ERROR 堆栈
        if (isClientAbortException(ex)) {
            log.debug("Client aborted or reset connection prematurely: {}", ex.getMessage());
            return null;
        }
        // 静态资源未找到（如 favicon.ico），静默返回 404
        if ("org.springframework.web.servlet.resource.NoResourceFoundException".equals(ex.getClass().getName())) {
            return ResponseEntity.notFound().build();
        }
        log.error("system error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(CommonErrorCode.FAILED));
    }

    private boolean isClientAbortException(Throwable throwable) {
        java.util.Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Throwable curr = throwable;
        while (curr != null && visited.add(curr)) {
            String className = curr.getClass().getName();
            if (className.equals("org.apache.catalina.connector.ClientAbortException")
                    || className.equals("org.springframework.web.context.request.async.AsyncRequestNotUsableException")) {
                return true;
            }
            curr = curr.getCause();
        }
        return false;
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
