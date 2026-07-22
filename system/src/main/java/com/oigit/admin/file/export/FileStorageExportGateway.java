package com.oigit.admin.file.export;

import com.oigit.admin.core.export.model.ExportStoreRequest;
import com.oigit.admin.core.export.model.ExportStoredFile;
import com.oigit.admin.core.export.model.RenderedExportFile;
import com.oigit.admin.core.export.spi.ExportFileAccessor;
import com.oigit.admin.core.export.spi.ExportFileSink;
import com.oigit.admin.file.config.FileStorageProperties;
import com.oigit.admin.file.service.FileService;
import com.oigit.admin.file.service.StoredFile;
import org.springframework.stereotype.Component;

@Component
public class FileStorageExportGateway implements ExportFileSink, ExportFileAccessor {

    private final FileService fileService;
    private final FileStorageProperties fileStorageProperties;

    public FileStorageExportGateway(FileService fileService, FileStorageProperties fileStorageProperties) {
        this.fileService = fileService;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public ExportStoredFile store(RenderedExportFile file, ExportStoreRequest request) {
        StoredFile storedFile = fileService.upload(
                file.getContent(),
                request.getBizPath(),
                request.getObjectKey(),
                file.getFileName(),
                file.getContentType()
        );
        ExportStoredFile exportStoredFile = new ExportStoredFile();
        exportStoredFile.setObjectKey(storedFile.getObjectKey());
        exportStoredFile.setStorageType(fileStorageProperties.getType());
        exportStoredFile.setContentType(storedFile.getContentType());
        exportStoredFile.setFileSize(storedFile.getSize());
        return exportStoredFile;
    }

    @Override
    public String fetchTempUrl(String objectKey) {
        return fileService.fetchTempUrl(objectKey);
    }

    @Override
    public byte[] fetchContent(String objectKey) {
        return fileService.download(objectKey);
    }
}
