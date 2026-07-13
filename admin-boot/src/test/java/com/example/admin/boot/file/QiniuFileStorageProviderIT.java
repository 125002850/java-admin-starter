package com.example.admin.boot.file;

import com.example.admin.boot.AdminBootApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminBootApplication.class)
@ActiveProfiles("qiniu-it")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_QINIU_IT", matches = "true")
class QiniuFileStorageProviderIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void upload_real_file_should_return_origin_url() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "qiniu-it.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "qiniu integration".getBytes()
        );

        String content = mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .param("bizPath", "integration/qiniu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(content).get("data");
        assertThat(data.get("objectKey").asText()).startsWith("integration/qiniu/");
        assertThat(data.get("originUrl").asText()).contains(data.get("objectKey").asText());
    }

    @Test
    void fetch_temp_url_for_private_bucket_should_return_signed_url() throws Exception {
        // 该对象键需要由测试者预先上传，或替换成当前 bucket 中已知存在的对象键。
        mockMvc.perform(post("/api/file/storage/object/temp-url/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"integration/qiniu/manual-check.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tempUrl").value(org.hamcrest.Matchers.containsString("e=")));
    }

    @Test
    void delete_real_file_should_remove_object() throws Exception {
        // 该对象键需要由测试者预先上传，或替换成当前 bucket 中已知存在的对象键。
        mockMvc.perform(post("/api/file/storage/object/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"integration/qiniu/manual-check.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
