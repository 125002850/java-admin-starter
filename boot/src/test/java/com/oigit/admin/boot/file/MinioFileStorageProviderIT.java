package com.oigit.admin.boot.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.oigit.admin.boot.AdminBootApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminBootApplication.class)
@ActiveProfiles("minio-it")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_MINIO_IT", matches = "true")
class MinioFileStorageProviderIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void upload_real_file_should_return_origin_url() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "minio-it.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "minio integration".getBytes()
        );

        String content = mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .param("bizPath", "integration/minio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(content).get("data");
        assertThat(data.get("objectKey").asText()).startsWith("integration/minio/");
        assertThat(data.get("originUrl").asText()).contains(data.get("objectKey").asText());
    }

    @Test
    void fetch_temp_url_for_private_bucket_should_return_signed_url() throws Exception {
        mockMvc.perform(post("/api/file/storage/object/temp-url/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"integration/minio/manual-check.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tempUrl")
                        .value(org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.containsString("X-Amz-Signature"),
                                org.hamcrest.Matchers.containsString("X-Amz-Algorithm"))));
    }

    @Test
    void delete_real_file_should_remove_object() throws Exception {
        mockMvc.perform(post("/api/file/storage/object/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"integration/minio/manual-check.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
