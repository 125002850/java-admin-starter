package com.oigit.admin.file.export;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.export.model.ExportStoreRequest;
import com.oigit.admin.core.export.model.ExportStoredFile;
import com.oigit.admin.core.export.model.RenderedExportFile;
import com.oigit.admin.core.export.spi.ExportFileAccessor;
import com.oigit.admin.core.export.spi.ExportFileSink;
import com.oigit.admin.file.config.FileStorageProperties;
import com.oigit.admin.file.enums.FileErrorCode;
import com.oigit.admin.file.service.FileService;
import com.oigit.admin.file.service.StoredFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

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
        StoredFile storedFile;
        try (InputStream inputStream = file.openInputStream()) {
            storedFile = fileService.upload(
                    inputStream,
                    file.getFileSize(),
                    request.getBizPath(),
                    request.getObjectKey(),
                    file.getFileName(),
                    file.getContentType()
            );
        } catch (IOException ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
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
    public InputStream openStream(String objectKey) {
        return fileService.openDownloadStream(objectKey);
    }

    @Override
    public void delete(String objectKey) {
        fileService.delete(objectKey);
    }
}
