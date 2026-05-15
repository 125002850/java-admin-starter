package com.demo.boot.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ValidationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidRequestShouldReturnWrappedError() throws Exception {
        mockMvc.perform(post("/api/test/echo/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数校验失败"));
    }

    @Test
    void bizExceptionShouldReturnWrappedError() throws Exception {
        mockMvc.perform(post("/api/test/echo/fail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("业务失败"));
    }

    @Test
    void invalidMethodParameterShouldReturnWrappedError() throws Exception {
        mockMvc.perform(post("/api/test/echo/method")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数校验失败"));
    }

    @Test
    void jacksonShouldSerializeJavaTimeWithUnifiedFormat() throws Exception {
        mockMvc.perform(post("/api/test/echo/time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"))
                .andExpect(jsonPath("$.data.day").value("2026-05-14"))
                .andExpect(jsonPath("$.data.time").value("2026-05-14 10:20:30"));
    }

    @Test
    void jacksonShouldDeserializeJavaTimeWithUnifiedFormat() throws Exception {
        mockMvc.perform(post("/api/test/echo/time/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "day": "2026-05-14",
                                  "time": "2026-05-14 10:20:30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"))
                .andExpect(jsonPath("$.data.day").value("2026-05-14"))
                .andExpect(jsonPath("$.data.time").value("2026-05-14 10:20:30"));
    }

    @Test
    void invalidJavaTimeFormatShouldReturnWrappedError() throws Exception {
        mockMvc.perform(post("/api/test/echo/time/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "day": "2026/05/14",
                                  "time": "2026-05-14T10:20:30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数校验失败"));
    }
}
