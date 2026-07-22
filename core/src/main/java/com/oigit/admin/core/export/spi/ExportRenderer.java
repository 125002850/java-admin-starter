package com.oigit.admin.core.export.spi;

import com.oigit.admin.core.export.model.ExportRenderRequest;
import com.oigit.admin.core.export.model.RenderedExportFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface ExportRenderer {

    String fileType();

    String contentType();

    void render(ExportRenderRequest request, OutputStream outputStream) throws IOException;

    default RenderedExportFile render(ExportRenderRequest request) throws IOException {
        Path contentPath = Files.createTempFile("oig-export-", "." + fileType());
        boolean completed = false;
        try {
            try (OutputStream outputStream = Files.newOutputStream(contentPath)) {
                render(request, outputStream);
            }
            RenderedExportFile file = new RenderedExportFile();
            file.setFileName(request.getFileName());
            file.setFileType(fileType());
            file.setContentType(contentType());
            file.setContentPath(contentPath);
            file.setFileSize(Files.size(contentPath));
            completed = true;
            return file;
        } finally {
            if (!completed) {
                Files.deleteIfExists(contentPath);
            }
        }
    }
}
