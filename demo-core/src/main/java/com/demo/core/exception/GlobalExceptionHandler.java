package com.demo.core.exception;

import com.demo.core.web.R;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        return ResponseEntity.ok(
            R.fail(ex.getErrorCode().code(), ex.getErrorCode().message())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidException(
        MethodArgumentNotValidException ex
    ) {
        String msg = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse(CommonErrorCode.FAILED.message());

        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(R.fail(CommonErrorCode.PARAM_ERROR.code(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.fail(CommonErrorCode.FAILED.code(), CommonErrorCode.FAILED.message()));
    }
}