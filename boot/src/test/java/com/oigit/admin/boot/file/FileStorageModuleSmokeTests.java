package com.oigit.admin.boot.file;

import com.oigit.admin.boot.AdminBootApplication;
import com.oigit.admin.boot.iam.IamTestAuth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminBootApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FileStorageModuleSmokeTests {

    private static final Path STORAGE_ROOT = createStorageRoot();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.file.storage.type", () -> "local");
        registry.add("platform.file.storage.zone-id", () -> "Asia/Shanghai");
        registry.add("platform.file.storage.local.root-dir", () -> STORAGE_ROOT.toString());
        registry.add("platform.file.storage.local.base-url", () -> "/local-files");
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(STORAGE_ROOT);
        try (var stream = Files.walk(STORAGE_ROOT)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(STORAGE_ROOT))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("failed to clean " + path, ex);
                        }
                    });
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (Files.notExists(STORAGE_ROOT)) {
            return;
        }
        try (var stream = Files.walk(STORAGE_ROOT)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("failed to delete " + path, ex);
                        }
                    });
        }
    }

    @Test
    void upload_should_store_file_under_local_root_and_return_origin_url() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello local storage".getBytes()
        );

        String content = mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .header("Authorization", authorizationHeader())
                        .param("bizPath", "avatar/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(content).get("data");
        String objectKey = data.get("objectKey").asText();
        String originUrl = data.get("originUrl").asText();

        assertThat(objectKey).startsWith("avatar/user/");
        assertThat(originUrl).isEqualTo("/local-files/" + objectKey);
        assertThat(Files.exists(STORAGE_ROOT.resolve(objectKey))).isTrue();
        assertThat(Files.readString(STORAGE_ROOT.resolve(objectKey))).isEqualTo("hello local storage");
    }

    @Test
    void fetchTempUrl_should_return_local_base_url_plus_object_key() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "fetch temp".getBytes()
        );

        String uploadResponse = mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .header("Authorization", authorizationHeader())
                        .param("bizPath", "avatar/user"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String objectKey = objectMapper.readTree(uploadResponse).get("data").get("objectKey").asText();

        mockMvc.perform(post("/api/file/storage/object/temp-url/fetch")
                        .header("Authorization", authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"" + objectKey + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.objectKey").value(objectKey))
                .andExpect(jsonPath("$.data.tempUrl").value("/local-files/" + objectKey));
    }

    @Test
    void delete_should_remove_stored_file() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "delete me".getBytes()
        );

        String uploadResponse = mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .header("Authorization", authorizationHeader())
                        .param("bizPath", "avatar/user"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String objectKey = objectMapper.readTree(uploadResponse).get("data").get("objectKey").asText();
        Path storedFile = STORAGE_ROOT.resolve(objectKey);
        assertThat(Files.exists(storedFile)).isTrue();

        mockMvc.perform(post("/api/file/storage/object/delete")
                        .header("Authorization", authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"" + objectKey + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(Files.exists(storedFile)).isFalse();
    }

    @Test
    void upload_should_reject_empty_file() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .header("Authorization", authorizationHeader())
                        .param("bizPath", "avatar/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3002001))
                .andExpect(jsonPath("$.msg").value("上传文件不能为空"));
    }

    @Test
    void upload_should_reject_illegal_biz_path() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "illegal.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "illegal biz path".getBytes()
        );

        mockMvc.perform(multipart("/api/file/storage/object/upload")
                        .file(file)
                        .header("Authorization", authorizationHeader())
                        .param("bizPath", "avatar//user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3002002))
                .andExpect(jsonPath("$.msg").value("业务路径格式非法"));
    }

    @Test
    void fetchDirectUploadCredential_should_reject_when_local_provider_does_not_support_it() throws Exception {
        mockMvc.perform(post("/api/file/storage/direct-upload/credential/fetch")
                        .header("Authorization", authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizPath\":\"avatar/user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3002008))
                .andExpect(jsonPath("$.msg").value("当前存储类型不支持直传凭证"));
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("java-admin-starter-system-file-smoke-tests");
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create temp directory", ex);
        }
    }

    private String authorizationHeader() throws Exception {
        return "Bearer " + IamTestAuth.adminAccessToken(mockMvc, objectMapper);
    }
}
