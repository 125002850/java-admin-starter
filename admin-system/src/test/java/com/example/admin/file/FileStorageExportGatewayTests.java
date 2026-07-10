package com.example.admin.file;

import com.example.admin.core.export.model.ExportStoreRequest;
import com.example.admin.core.export.model.ExportStoredFile;
import com.example.admin.core.export.model.RenderedExportFile;
import com.example.admin.file.config.FileStorageProperties;
import com.example.admin.file.export.FileStorageExportGateway;
import com.example.admin.file.service.FileService;
import com.example.admin.file.service.StoredFile;
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
        when(fileService.upload(any(), eq("export/sample"), eq(null), eq("sample.csv"), eq("text/csv;charset=UTF-8")))
            .thenReturn(new StoredFile("export/sample/file.csv", "http://origin", "sample.csv", "text/csv;charset=UTF-8", 32L));

        FileStorageExportGateway gateway = new FileStorageExportGateway(fileService, properties);
        RenderedExportFile file = new RenderedExportFile();
        file.setFileName("sample.csv");
        file.setContentType("text/csv;charset=UTF-8");
        file.setContent("sample".getBytes());

        ExportStoreRequest request = new ExportStoreRequest();
        request.setBizPath("export/sample");

        ExportStoredFile storedFile = gateway.store(file, request);

        assertThat(storedFile.getObjectKey()).isEqualTo("export/sample/file.csv");
        assertThat(storedFile.getStorageType()).isEqualTo("local");
        assertThat(storedFile.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(storedFile.getFileSize()).isEqualTo(32L);
    }

    @Test
    void fetchTempUrl_should_delegate_to_file_service() {
        FileService fileService = mock(FileService.class);
        FileStorageProperties properties = new FileStorageProperties();
        when(fileService.fetchTempUrl("export/sample/file.csv")).thenReturn("http://download");

        FileStorageExportGateway gateway = new FileStorageExportGateway(fileService, properties);
        String url = gateway.fetchTempUrl("export/sample/file.csv");

        assertThat(url).isEqualTo("http://download");
        verify(fileService).fetchTempUrl("export/sample/file.csv");
    }
}
