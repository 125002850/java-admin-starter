package com.oigit.admin.core.export.spi;

import com.oigit.admin.core.export.model.ExportStoreRequest;
import com.oigit.admin.core.export.model.ExportStoredFile;
import com.oigit.admin.core.export.model.RenderedExportFile;

public interface ExportFileSink {

    ExportStoredFile store(RenderedExportFile file, ExportStoreRequest request);

    void delete(String objectKey);
}
