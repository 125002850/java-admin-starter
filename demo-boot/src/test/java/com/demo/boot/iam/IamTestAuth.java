package com.demo.boot.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public final class IamTestAuth {

    private static final String DEFAULT_PASSWORD = "Admin@123456";
    private static final String TEST_PASSWORD = "Admin@654321";

    private IamTestAuth() {
    }

    public static String adminAccessToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        JsonNode login = login(mockMvc, objectMapper, DEFAULT_PASSWORD);
        if (login.path("code").asInt() == 200) {
            JsonNode data = login.path("data");
            String accessToken = data.path("accessToken").asText();
            if (!data.path("mustChangePassword").asBoolean()) {
                return accessToken;
            }
            JsonNode changed = changePassword(mockMvc, objectMapper, accessToken, DEFAULT_PASSWORD, TEST_PASSWORD);
            return changed.path("data").path("accessToken").asText();
        }

        JsonNode changedLogin = login(mockMvc, objectMapper, TEST_PASSWORD);
        if (changedLogin.path("code").asInt() == 200) {
            return changedLogin.path("data").path("accessToken").asText();
        }
        throw new IllegalStateException("failed to login default IAM admin for test requests: " + changedLogin);
    }

    private static JsonNode login(MockMvc mockMvc, ObjectMapper objectMapper, String password) throws Exception {
        String content = mockMvc.perform(post("/api/iam/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + password + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private static JsonNode changePassword(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String accessToken,
            String oldPassword,
            String newPassword
    ) throws Exception {
        String content = mockMvc.perform(post("/api/iam/auth/password/change")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }
}
