package com.oigit.admin.core.export.spi;

import java.io.InputStream;

public interface ExportFileAccessor {

    String fetchTempUrl(String objectKey);

    InputStream openStream(String objectKey);
}
