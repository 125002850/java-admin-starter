package com.demo.file;

import com.demo.core.export.model.ExportStoreRequest;
import com.demo.core.export.model.ExportStoredFile;
import com.demo.core.export.model.RenderedExportFile;
import com.demo.file.config.FileStorageProperties;
import com.demo.file.export.FileStorageExportGateway;
import com.demo.file.service.FileService;
import com.demo.file.service.StoredFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageExportGatewayTests {

    @Test
    void store_should_delegate_to_file_service_and_return_export_file_metadata() {
        FileService fileService = mock(FileService.class);
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("local");
        when(fileService.upload(any(), eq("export/demo"), eq(null), eq("demo.csv"), eq("text/csv;charset=UTF-8")))
            .thenReturn(new StoredFile("export/demo/file.csv", "http://origin", "demo.csv", "text/csv;charset=UTF-8", 32L));

        FileStorageExportGateway gateway = new FileStorageExportGateway(fileService, properties);
        RenderedExportFile file = new RenderedExportFile();
        file.setFileName("demo.csv");
        file.setContentType("text/csv;charset=UTF-8");
        file.setContent("demo".getBytes());

        ExportStoreRequest request = new ExportStoreRequest();
        request.setBizPath("export/demo");

        ExportStoredFile storedFile = gateway.store(file, request);

        assertThat(storedFile.getObjectKey()).isEqualTo("export/demo/file.csv");
        assertThat(storedFile.getStorageType()).isEqualTo("local");
        assertThat(storedFile.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(storedFile.getFileSize()).isEqualTo(32L);
    }

    @Test
    void fetchTempUrl_should_delegate_to_file_service() {
        FileService fileService = mock(FileService.class);
        FileStorageProperties properties = new FileStorageProperties();
        when(fileService.fetchTempUrl("export/demo/file.csv")).thenReturn("http://download");

        FileStorageExportGateway gateway = new FileStorageExportGateway(fileService, properties);
        String url = gateway.fetchTempUrl("export/demo/file.csv");

        assertThat(url).isEqualTo("http://download");
        verify(fileService).fetchTempUrl("export/demo/file.csv");
    }
}
