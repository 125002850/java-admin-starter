package com.demo.boot.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiJsonShouldExposeBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.info.title").value("java-demo API 文档"))
            .andExpect(content().string(containsString("/api/system/auth/login")))
            .andExpect(content().string(containsString("/api/mdm/dict/items/by-type")));
    }

    @Test
    void knife4jPageShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/doc.html"))
            .andExpect(status().isOk());
    }

    @Test
    void groupedOpenApiJsonShouldSplitEndpointsByModule() throws Exception {
        mockMvc.perform(get("/v3/api-docs/system-auth"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tags[0].name").value("系统认证"))
            .andExpect(jsonPath("$.paths['/api/system/auth/login'].post.summary").value("租户用户登录"))
            .andExpect(content().string(containsString("/api/system/auth/login")))
            .andExpect(content().string(not(containsString("/api/mdm/dict/items/by-type"))));

        mockMvc.perform(get("/v3/api-docs/mdm-dict"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tags[0].name").value("主数据字典"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/items/by-type'].post.summary").value("按字典类型查询字典项"))
            .andExpect(content().string(containsString("/api/mdm/dict/items/by-type")))
            .andExpect(content().string(not(containsString("/api/system/auth/login"))));
    }
}
