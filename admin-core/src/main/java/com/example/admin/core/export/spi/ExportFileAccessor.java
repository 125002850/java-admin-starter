package com.example.admin.core.export.spi;

public interface ExportFileAccessor {

    String fetchTempUrl(String objectKey);

    byte[] fetchContent(String objectKey);
}
