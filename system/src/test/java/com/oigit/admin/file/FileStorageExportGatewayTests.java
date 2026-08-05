package com.oigit.admin.file;

import com.oigit.admin.core.export.model.ExportStoreRequest;
import com.oigit.admin.core.export.model.ExportStoredFile;
import com.oigit.admin.core.export.model.RenderedExportFile;
import com.oigit.admin.file.app.FileAppService;
import com.oigit.admin.file.domain.model.StoredFile;
import com.oigit.admin.file.infra.config.FileStorageProperties;
import com.oigit.admin.file.infra.export.FileStorageExportGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageExportGatewayTests {

    @TempDir
    Path tempDir;

    @Test
    void store_should_delegate_to_file_service_and_return_export_file_metadata() throws Exception {
        FileAppService fileAppService = mock(FileAppService.class);
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("local");
        when(fileAppService.store(
                any(InputStream.class),
                eq(4L),
                eq("export/demo"),
                eq(null),
                eq("demo.csv"),
                eq("text/csv;charset=UTF-8")
        ))
            .thenReturn(new StoredFile("export/demo/file.csv", "http://origin", "demo.csv", "text/csv;charset=UTF-8", 32L));

        FileStorageExportGateway gateway = new FileStorageExportGateway(fileAppService, properties);
        Path contentPath = tempDir.resolve("demo.csv");
        Files.writeString(contentPath, "demo");
        RenderedExportFile file = new RenderedExportFile();
        file.setFileName("demo.csv");
        file.setContentType("text/csv;charset=UTF-8");
        file.setContentPath(contentPath);
        file.setFileSize(4L);

        ExportStoreRequest request = new ExportStoreRequest();
        request.setBizPath("export/demo");

        ExportStoredFile storedFile = gateway.store(file, request);

        assertThat(storedFile.getObjectKey()).isEqualTo("export/demo/file.csv");
        assertThat(storedFile.getStorageType()).isEqualTo("local");
        assertThat(storedFile.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(storedFile.getFileSize()).isEqualTo(32L);
        verify(fileAppService).store(
                any(InputStream.class),
                eq(4L),
                eq("export/demo"),
                eq(null),
                eq("demo.csv"),
                eq("text/csv;charset=UTF-8")
        );
    }

    @Test
    void fetchTempUrl_should_delegate_to_file_service() {
        FileAppService fileAppService = mock(FileAppService.class);
        FileStorageProperties properties = new FileStorageProperties();
        when(fileAppService.fetchTempUrl("export/demo/file.csv")).thenReturn("http://download");

        FileStorageExportGateway gateway = new FileStorageExportGateway(fileAppService, properties);
        String url = gateway.fetchTempUrl("export/demo/file.csv");

        assertThat(url).isEqualTo("http://download");
        verify(fileAppService).fetchTempUrl("export/demo/file.csv");
    }
}
