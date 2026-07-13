package com.example.admin.core.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GatewayOperatorFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CacheUserService cacheUserService;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        cacheUserService = Mockito.mock(CacheUserService.class);
        taskExecutor = Runnable::run;
    }

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
    }

    @Test
    void should_set_operator_context_from_headers_and_clear_after_request() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> {
            chainCalled.set(true);
            assertThat(OperatorContext.getOperatorId()).isEqualTo(100L);
            assertThat(OperatorContext.getOperatorName()).isEqualTo("test user");
            assertThat(OperatorContext.getOperatorPhone()).isEqualTo("13800138000");
            assertThat(OperatorContext.getOperatorRealName()).isEqualTo("张三");
        };
        GatewayOperatorFilter filter = newFilter(() -> 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "100");
        request.addHeader("X-User-Name", "test%20user");
        request.addHeader("X-User-Phone", " 13800138000 ");
        request.addHeader("X-Real-Name", "%E5%BC%A0%E4%B8%89");
        request.addHeader("X-User-Code", " U100 ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isTrue();
        verify(cacheUserService).upsert(100L, "test user", "13800138000", "张三", "U100");
        assertThat(OperatorContext.getOperatorId()).isNull();
        assertThat(OperatorContext.getOperatorName()).isNull();
        assertThat(OperatorContext.getOperatorRealName()).isNull();
    }

    @Test
    void should_allow_request_when_headers_are_missing() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);
        GatewayOperatorFilter filter = newFilter(() -> 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(OperatorContext.getOperatorId()).isNull();
        verifyNoInteractions(cacheUserService);
    }

    @Test
    void should_return_400_when_user_id_is_not_numeric() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);
        GatewayOperatorFilter filter = newFilter(() -> 1_000L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isFalse();
        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(jsonNode.get("code").asInt()).isEqualTo(400);
        assertThat(jsonNode.get("msg").asText()).isEqualTo("X-User-Id 值非法: not-a-number");
        verifyNoInteractions(cacheUserService);
    }

    @Test
    void should_throttle_duplicate_cache_update_within_window() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);
        GatewayOperatorFilter filter = newFilter(() -> 1_000L);
        MockHttpServletRequest firstRequest = buildUserRequest("100", "test%20user", null, null, "U100");
        MockHttpServletRequest secondRequest = buildUserRequest("100", "test%20user", null, null, "U100");

        filter.doFilter(firstRequest, new MockHttpServletResponse(), filterChain);
        filter.doFilter(secondRequest, new MockHttpServletResponse(), filterChain);

        assertThat(chainCalled.get()).isTrue();
        verify(cacheUserService, times(1)).upsert(100L, "test user", null, null, "U100");
    }

    @Test
    void should_allow_cache_update_when_payload_changes_within_window() throws Exception {
        FilterChain filterChain = (req, res) -> {
        };
        GatewayOperatorFilter filter = newFilter(() -> 1_000L);
        MockHttpServletRequest firstRequest = buildUserRequest("100", "test%20user", null, null, "U100");
        MockHttpServletRequest secondRequest = buildUserRequest("100", "test%20user", null, "%E5%BC%A0%E4%B8%89", "U100");

        filter.doFilter(firstRequest, new MockHttpServletResponse(), filterChain);
        filter.doFilter(secondRequest, new MockHttpServletResponse(), filterChain);

        verify(cacheUserService).upsert(100L, "test user", null, null, "U100");
        verify(cacheUserService).upsert(100L, "test user", null, "张三", "U100");
    }

    @Test
    void should_skip_cache_update_when_only_user_id_is_present() throws Exception {
        FilterChain filterChain = (req, res) -> {
        };
        GatewayOperatorFilter filter = newFilter(() -> 1_000L);
        MockHttpServletRequest request = buildUserRequest("100", null, null, null, null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verifyNoInteractions(cacheUserService);
    }

    private GatewayOperatorFilter newFilter(java.util.function.LongSupplier currentTimeMillisSupplier) {
        return new GatewayOperatorFilter(objectMapper, cacheUserService, taskExecutor, 600_000L, currentTimeMillisSupplier);
    }

    private MockHttpServletRequest buildUserRequest(String userId,
                                                    String userName,
                                                    String userPhone,
                                                    String realName,
                                                    String userCode) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userId != null) {
            request.addHeader("X-User-Id", userId);
        }
        if (userName != null) {
            request.addHeader("X-User-Name", userName);
        }
        if (userPhone != null) {
            request.addHeader("X-User-Phone", userPhone);
        }
        if (realName != null) {
            request.addHeader("X-Real-Name", realName);
        }
        if (userCode != null) {
            request.addHeader("X-User-Code", userCode);
        }
        return request;
    }
}
