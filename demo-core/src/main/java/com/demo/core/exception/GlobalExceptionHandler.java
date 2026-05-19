package com.demo.core.exception;

import com.demo.core.web.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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
            .map(FieldError::getDefaultMessage)
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
            .badRequest()
            .body(R.fail(CommonErrorCode.PARAM_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        log.error("system error", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.fail(CommonErrorCode.FAILED));
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
