package com.oigit.admin.core.export.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class RenderedExportFile implements AutoCloseable {

    private String fileName;
    private String fileType;
    private String contentType;
    private Path contentPath;
    private long fileSize;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Path getContentPath() {
        return contentPath;
    }

    public void setContentPath(Path contentPath) {
        this.contentPath = contentPath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(contentPath);
    }

    @Override
    public void close() throws IOException {
        if (contentPath != null) {
            Files.deleteIfExists(contentPath);
        }
    }
}
