package com.demo.boot.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
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
            .andExpect(content().string(containsString("/api/mdm/dict/items/by-type")))
            .andExpect(content().string(containsString("/api/mdm/dict/types/list")))
            .andExpect(content().string(containsString("/api/mdm/dict/type/create")))
            .andExpect(content().string(containsString("/api/mdm/dict/item/create")))
            .andExpect(content().string(containsString("/api/mdm/dict/global/types/list")))
            .andExpect(content().string(containsString("/api/mdm/dict/global/type/update")))
            .andExpect(content().string(containsString("/api/mdm/dict/global/item/update")));
    }

    @Test
    void knife4jPageShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/doc.html"))
            .andExpect(status().isOk());
    }

    @Test
    void groupedOpenApiJsonShouldSplitEndpointsByModule() throws Exception {
        mockMvc.perform(get("/v3/api-docs/mdm-dict"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/mdm/dict/items/by-type'].post.summary").value("按字典类型查询字典项"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/types/list'].post.summary").value("查询租户字典列表"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/type/create'].post.summary").value("新增租户字典类型"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/types/list'].post.tags[0]").value("租户字典"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/global/types/list'].post.summary").value("查询全局字典列表"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/global/type/create'].post.tags[0]").value("全局字典"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/global/type/update'].post.summary").value("修改全局字典类型"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/global/item/update'].post.summary").value("修改全局字典项"))
            .andExpect(jsonPath("$.components.schemas.DictTypeListReqDTO.properties.pageNo.description").value("页码，从1开始"))
            .andExpect(jsonPath("$.components.schemas.DictTypeListReqDTO.properties.pageSize.description").value("每页条数"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeListReqDTO.properties.pageNo.description").value("页码，从1开始"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeListReqDTO.properties.pageSize.description").value("每页条数"))
            .andExpect(content().string(containsString("\"pageNo\"")))
            .andExpect(content().string(containsString("\"pageSize\"")))
            .andExpect(content().string(containsString("\"keyword\"")))
            .andExpect(content().string(containsString("/api/mdm/dict/items/by-type")));
    }
}
