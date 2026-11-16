package com.oigit.admin.staff.infra.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.appcik.CIConfig;
import com.oigit.appcik.core.StaffContextProvider;
import com.oigit.appcik.core.http.request.RawRequest;
import com.oigit.appcik.core.http.response.RawResponse;
import com.oigit.appcik.core.service.sso.SsoService;
import com.oigit.appcik.core.service.sso.model.SsoRsp;
import com.oigit.appcik.core.service.sso.model.UpdatePasswordReq;
import com.oigit.common.exceptions.ServiceException;
import com.oigit.common.util.JsonUtil;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.http.OutboundHttpClientOptions;
import com.oigit.admin.core.http.OutboundHttpExecutor;
import com.oigit.admin.core.http.OutboundHttpRequests;
import com.oigit.admin.core.http.OutboundHttpResponse;
import com.oigit.admin.staff.enums.StaffErrorCode;
import com.oigit.admin.staff.domain.gateway.StaffPasswordGateway;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CikStaffPasswordProvider implements StaffPasswordGateway {

    private static final Logger log = LoggerFactory.getLogger(CikStaffPasswordProvider.class);
    private static final OutboundHttpClientOptions HTTP_OPTIONS = OutboundHttpClientOptions.strict(
            Duration.ofSeconds(3), Duration.ofSeconds(10), Duration.ofSeconds(15));
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private final CIConfig config;
    private final StaffContextProvider staffContextProvider;
    private final OutboundHttpExecutor http;
    private final ObjectMapper objectMapper;

    public CikStaffPasswordProvider(CIConfig config, StaffContextProvider staffContextProvider,
            OutboundHttpExecutor http, ObjectMapper objectMapper) {
        this.config = config;
        this.staffContextProvider = staffContextProvider;
        this.http = http;
        this.objectMapper = objectMapper;
    }

    private RawResponse execute(RawRequest rawRequest, long startedAt) {
        validateEndpoint(rawRequest.getReqUrl());
        var request = OutboundHttpRequests.jsonPost(rawRequest.getReqUrl(),
                JsonUtil.toJson(rawRequest.getBody()).getBytes(StandardCharsets.UTF_8)).newBuilder();
        rawRequest.getHeaders().forEach((name, values) -> values.forEach(value -> request.addHeader(name, value)));
        OutboundHttpResponse response;
        try {
            response = http.execute(HTTP_OPTIONS, request.build(), MAX_RESPONSE_BYTES);
        } catch (InterruptedIOException exception) {
            // OkHttp 的总调用超时是 InterruptedIOException；线程中断不应误报为超时。
            boolean timeout = exception instanceof SocketTimeoutException || !Thread.currentThread().isInterrupted();
            throw failure(
                    timeout ? StaffErrorCode.STAFF_PASSWORD_CALL_TIMEOUT : StaffErrorCode.STAFF_PASSWORD_CALL_FAILED,
                    timeout ? "timeout" : "interrupted", null, startedAt, exception);
        } catch (IOException exception) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_CALL_FAILED, "transport_error", null, startedAt, exception);
        }
        int status = response.statusCode();
        if (response.bodyTooLarge()) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_RESPONSE_TOO_LARGE, "response_too_large", status, startedAt,
                    null);
        }
        if (response.unsupportedContentEncoding()) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_RESPONSE_ENCODING_UNSUPPORTED, "unsupported_content_encoding",
                    status, startedAt, null);
        }
        // 只将 400/422 的有效 SSO 业务拒绝交给 SDK；网关错误、鉴权失败和重定向属于 HTTP 故障。
        if (status != 200 && status != 400 && status != 422) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_HTTP_ERROR, "unexpected_http_status", status, startedAt, null);
        }
        SsoRsp parsed;
        try {
            // SDK 的宽松 JSON 解析器会通过 ROOT logger 记录解析失败的完整输入。
            // 先用不记录原文的解析器校验，避免异常响应回显凭据到日志。
            parsed = objectMapper.readValue(response.body(), SsoRsp.class);
        } catch (IOException exception) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID, "invalid_json", status, startedAt, exception);
        }
        if (parsed == null || (status == 200 && parsed.getSuccess() == null)) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID, "missing_success", status, startedAt, null);
        }
        if (status != 200) {
            // 保留 SDK 对无 success、但有 modules/message/code 的非 200 拒绝响应的兼容处理。
            if (Boolean.TRUE.equals(parsed.getSuccess())
                    || (parsed.getSuccess() == null && !StringUtils.hasText(parsed.getMessage())
                            && parsed.getCode() == null)) {
                throw failure(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID, "invalid_rejection", status, startedAt,
                        null);
            }
            throw new ServiceException("响应内容：" + new String(response.body(), StandardCharsets.UTF_8));
        }
        RawResponse result = new RawResponse();
        result.setStatusCode(status);
        result.setBody(response.body());
        result.setHeaders(response.headers());
        return result;
    }

    public void change(String account, String oldPassword, String newPassword, String repeatPassword) {
        long startedAt = System.nanoTime();
        validateEndpoint(config.getSso() == null ? null : config.getSso().getServerAddr());
        UpdatePasswordReq request = new UpdatePasswordReq();
        request.setAccount(account);
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        request.setRepeatPassword(repeatPassword);
        SsoRsp response;
        try {
            // SDK 默认 HTTP transport 未设置超时。仅为改密使用现有出站 HTTP 能力，
            // 保留 SDK 的 URL、请求与错误解析契约，不改变共享 CIClient 的其他调用。
            SsoService service = new SsoService(config.getSso(), staffContextProvider) {
                @Override
                public RawResponse doRequest(RawRequest rawRequest) {
                    return execute(rawRequest, startedAt);
                }
            };
            response = service.account().updatePassword(request);
        } catch (BizException exception) {
            // 已分类的配置、传输、响应故障不能再被 SDK 异常兜底覆盖。
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_CALL_FAILED, "sdk_error", null, startedAt, exception);
        }
        if (response == null || response.getSuccess() == null) {
            throw failure(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID, "missing_sdk_result", null, startedAt, null);
        }
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            // 仅识别明确的旧密码错误，其余供应商消息不直接向用户回显。
            String message = response.getMessage();
            if ("旧密码错误".equals(message) || "原密码错误".equals(message)
                    || "旧密码不正确".equals(message) || "原密码不正确".equals(message)) {
                throw new BizException(StaffErrorCode.STAFF_PASSWORD_CURRENT_INVALID);
            }
            throw new BizException(StaffErrorCode.STAFF_PASSWORD_REJECTED);
        }
    }

    private static void validateEndpoint(String value) {
        if (!StringUtils.hasText(value)) throw new BizException(StaffErrorCode.STAFF_PASSWORD_CONFIG_MISSING);
        try {
            var uri = java.net.URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new BizException(StaffErrorCode.STAFF_PASSWORD_CONFIG_INVALID);
        }
    }

    private BizException failure(StaffErrorCode code, String reason, Integer status, long startedAt,
            Exception exception) {
        // traceId 由 MDC/日志格式提供；异常消息、cause、请求与响应都可能包含密码，禁止记录。
        log.warn("SSO password change failed: code={}, reason={}, upstreamStatus={}, costMs={}, exceptionType={}",
                code.getCode(), reason, status, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
                exception == null ? "none" : exception.getClass().getSimpleName());
        return new BizException(code);
    }
}
