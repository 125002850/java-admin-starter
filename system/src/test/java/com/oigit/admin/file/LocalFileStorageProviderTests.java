package com.oigit.admin.file;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.file.config.FileStorageProperties;
import com.oigit.admin.file.infra.provider.local.LocalFileStorageProvider;
import com.oigit.admin.file.service.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageProviderTests {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() throws IOException {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(tempDir))
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
    void upload_should_create_parent_directories_and_persist_content() throws Exception {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(properties());

        StoredFile storedFile = provider.upload(
                new ByteArrayInputStream("provider-data".getBytes()),
                "nested/path/file.txt",
                "text/plain",
                13L,
                "file.txt"
        );

        assertThat(storedFile.getObjectKey()).isEqualTo("nested/path/file.txt");
        assertThat(storedFile.getOriginUrl()).isEqualTo("/local-files/nested/path/file.txt");
        assertThat(Files.readString(tempDir.resolve("nested/path/file.txt"))).isEqualTo("provider-data");
        try (InputStream inputStream = provider.openStream("nested/path/file.txt")) {
            assertThat(inputStream.readAllBytes()).isEqualTo("provider-data".getBytes());
        }
    }

    @Test
    void upload_should_reject_path_traversal_object_key() {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(properties());

        assertThatThrownBy(() -> provider.upload(
                new ByteArrayInputStream("provider-data".getBytes()),
                "../escape.txt",
                "text/plain",
                13L,
                "escape.txt"
        )).isInstanceOf(BizException.class)
          .hasMessage("对象键格式非法");
    }

    @Test
    void delete_should_throw_when_file_does_not_exist() {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(properties());

        assertThatThrownBy(() -> provider.delete("nested/path/missing.txt"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件不存在");
    }

    private FileStorageProperties properties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("local");
        properties.setZoneId("Asia/Shanghai");

        FileStorageProperties.Local local = new FileStorageProperties.Local();
        local.setRootDir(tempDir.toString());
        local.setBaseUrl("/local-files");
        properties.setLocal(local);
        return properties;
    }
}
