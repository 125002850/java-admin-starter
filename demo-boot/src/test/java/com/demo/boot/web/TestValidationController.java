package com.demo.boot.web;

import com.demo.core.exception.BizException;
import com.demo.core.exception.ErrorCode;
import com.demo.core.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test/echo")
class TestValidationController {

    @PostMapping("/submit")
    public R<EchoRspDTO> submit(@Valid @RequestBody EchoReqDTO request) {
        return R.ok(new EchoRspDTO(request.content()));
    }

    @PostMapping("/fail")
    public R<Void> fail() {
        throw new BizException(TestErrorCode.BIZ_FAILURE);
    }

    @PostMapping("/panic")
    public R<Void> panic() {
        throw new IllegalStateException("unexpected state");
    }

    @PostMapping("/method")
    public R<EchoRspDTO> method(@RequestParam("content") @NotBlank(message = "content不能为空") String content) {
        return R.ok(new EchoRspDTO(content));
    }

    @PostMapping("/time")
    public R<TimeRspDTO> time() {
        return R.ok(new TimeRspDTO(
                LocalDate.of(2026, 5, 14),
                LocalDateTime.of(2026, 5, 14, 10, 20, 30)
        ));
    }

    @PostMapping("/time/echo")
    public R<TimeRspDTO> echoTime(@Valid @RequestBody TimeReqDTO request) {
        return R.ok(new TimeRspDTO(request.day(), request.time()));
    }

    record EchoReqDTO(@NotBlank(message = "content不能为空") String content) {
    }

    record EchoRspDTO(String content) {
    }

    record TimeReqDTO(LocalDate day, LocalDateTime time) {
    }

    record TimeRspDTO(LocalDate day, LocalDateTime time) {
    }

    private enum TestErrorCode implements ErrorCode {
        BIZ_FAILURE(9000001, "业务失败");

        private final int code;
        private final String message;

        TestErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
