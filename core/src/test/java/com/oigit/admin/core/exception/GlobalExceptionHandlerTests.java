package com.oigit.admin.core.exception;

import com.oigit.admin.core.web.R;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldIncludeNestedFieldPathWithoutRejectedValue() throws Exception {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        String field = "routingRules[1].recipientSelectors[0].mentionSelectors[0].type";
        binding.addError(new FieldError("request", field, "private-input", false,
                null, null, "不能为null"));
        var parameter = new MethodParameter(getClass().getDeclaredMethod("accept", Object.class), 0);

        var response = handler.handleMethodArgumentNotValid(new MethodArgumentNotValidException(parameter, binding));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getMsg()).isEqualTo(field + "：不能为null").doesNotContain("private-input");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void shouldFallBackWhenFieldMessageIsMissing() throws Exception {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "name", null));
        var parameter = new MethodParameter(getClass().getDeclaredMethod("accept", Object.class), 0);
        var response = handler.handleMethodArgumentNotValid(new MethodArgumentNotValidException(parameter, binding));
        assertThat(response.getBody().getMsg()).isEqualTo("name：" + CommonErrorCode.PARAM_ERROR.getMsg());
    }

    @Test
    void ignoresTypedClientAbortAndReportsConfiguredUploadLimit() {
        var abort = new org.springframework.web.context.request.async.AsyncRequestNotUsableException("closed");
        assertThat(handler.handleException(new RuntimeException(abort))).isNull();
        var upload = handler.handleMaxUploadSizeExceeded(new org.springframework.web.multipart.MaxUploadSizeExceededException(1024));
        assertThat(upload.getStatusCode().value()).isEqualTo(400);
        assertThat(upload.getBody().getMsg()).contains("服务器配置").doesNotContain("50MB");
    }

    private void accept(Object request) { }

    @Test
    void shouldNotHideAnExceptionBasedOnItsMessage() {
        IOException cause = new IOException("Connection reset by peer");
        Exception ex = new RuntimeException("AsyncRequestNotUsableException", cause);

        ResponseEntity<R<Void>> response = handler.handleException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void shouldNotHideAnUnclassifiedIoFailure() {
        IOException ex = new IOException("Broken pipe");

        ResponseEntity<R<Void>> response = handler.handleException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void shouldHandleNormalSystemException() {
        Exception ex = new RuntimeException("Some database error");

        ResponseEntity<R<Void>> response = handler.handleException(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.FAILED.getCode());
    }
}
