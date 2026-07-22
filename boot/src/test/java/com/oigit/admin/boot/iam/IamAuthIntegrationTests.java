package com.oigit.admin.boot.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IamAuthIntegrationTests {

    private static final String DEFAULT_ADMIN_PASSWORD_HASH =
            "$2a$10$WbuU43YMwePH06bezaQJBO3NG8OvpJBpN/kq13BLpS.25GE6gfwf2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDefaultAdmin() {
        jdbcTemplate.update("delete from sys_refresh_token");
        jdbcTemplate.update("""
                update sys_staff
                   set password_hash = ?,
                       must_change_password = 1,
                       status = 'ENABLED',
                       deleted = 0
                 where username = 'admin'
                """, DEFAULT_ADMIN_PASSWORD_HASH);
    }

    @Test
    void localIamAuthFlowShouldLoginRequirePasswordChangeAndRefreshTokens() throws Exception {
        JsonNode login = postJson("/api/iam/auth/login", """
            {"username":"admin","password":"Admin@123456"}
            """, null, 200);
        assertThat(login.path("code").asInt()).isEqualTo(200);
        JsonNode loginData = login.path("data");
        assertThat(loginData.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(loginData.path("mustChangePassword").asBoolean()).isTrue();
        assertThat(loginData.path("staff").path("username").asText()).isEqualTo("admin");
        assertThat(objectMapper.writeValueAsString(loginData.path("permissions"))).contains("iam:staff:create");

        String accessToken = loginData.path("accessToken").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        JsonNode me = postJson("/api/iam/auth/me", "{}", accessToken, 200);
        assertThat(me.path("data").path("menus").get(0).path("menuKey").asText())
                .isEqualTo(me.path("data").path("menus").get(0).path("menuCode").asText());
        assertThat(me.path("data").path("dataScopeSummary").path("effectiveType").asText()).isEqualTo("ALL");

        JsonNode blocked = postJson("/api/iam/staff/page", "{\"pageNo\":1,\"pageSize\":10}", accessToken, 403);
        assertThat(blocked.path("code").asInt()).isEqualTo(2001007);

        JsonNode changed = postJson("/api/iam/auth/password/change", """
            {"oldPassword":"Admin@123456","newPassword":"Admin@654321"}
            """, accessToken, 200);
        String changedAccessToken = changed.path("data").path("accessToken").asText();
        assertThat(changed.path("data").path("mustChangePassword").asBoolean()).isFalse();

        JsonNode page = postJson("/api/iam/staff/page", "{\"pageNo\":1,\"pageSize\":10}", changedAccessToken, 200);
        assertThat(page.path("code").asInt()).isEqualTo(200);
        assertThat(page.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode refresh = postJson("/api/iam/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}", null, 401);
        assertThat(refresh.path("code").asInt()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject("""
                select failure_reason
                from sys_login_log
                where event_type = 'REFRESH'
                  and result = 'FAIL'
                order by id desc
                limit 1
                """, String.class)).isEqualTo("REFRESH_TOKEN_INVALID");

        String changedRefreshToken = changed.path("data").path("refreshToken").asText();
        JsonNode rotated = postJson("/api/iam/auth/refresh", "{\"refreshToken\":\"" + changedRefreshToken + "\"}", null, 200);
        assertThat(rotated.path("code").asInt()).isEqualTo(200);
        String rotatedRefreshToken = rotated.path("data").path("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(changedRefreshToken);

        postJson("/api/iam/auth/refresh", "{\"refreshToken\":\"" + changedRefreshToken + "\"}", null, 401);
    }

    @Test
    void protectedApiShouldReturn401WithoutToken() throws Exception {
        JsonNode response = postJson("/api/iam/auth/me", "{}", null, 401);
        assertThat(response.path("code").asInt()).isEqualTo(401);
    }

    @Test
    void expiredRefreshTokenShouldRecordStableFailureReasonCode() throws Exception {
        JsonNode login = postJson("/api/iam/auth/login", """
            {"username":"admin","password":"Admin@123456"}
            """, null, 200);
        String refreshToken = login.path("data").path("refreshToken").asText();
        jdbcTemplate.update("update sys_refresh_token set expire_time = '2000-01-01 00:00:00'");

        postJson("/api/iam/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}", null, 401);

        assertThat(jdbcTemplate.queryForObject("""
                select failure_reason
                from sys_login_log
                where event_type = 'REFRESH'
                  and result = 'FAIL'
                order by id desc
                limit 1
                """, String.class)).isEqualTo("REFRESH_TOKEN_EXPIRED");
    }

    private JsonNode postJson(String path, String body, String accessToken, int expectedStatus) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        String content = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }
}
