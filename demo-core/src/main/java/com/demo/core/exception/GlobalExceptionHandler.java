package com.demo.core.exception;

import com.demo.core.web.R;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    public R<Void> handleValidation(Exception ex) {
        return R.fail(400, "参数校验失败");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return R.fail(400, "参数校验失败");
    }

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException ex) {
        return R.fail(ex.getCode(), ex.getMessage());
    }
}
