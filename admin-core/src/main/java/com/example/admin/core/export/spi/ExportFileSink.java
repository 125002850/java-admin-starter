package com.example.admin.core.export.spi;

import com.example.admin.core.export.model.ExportStoreRequest;
import com.example.admin.core.export.model.ExportStoredFile;
import com.example.admin.core.export.model.RenderedExportFile;

public interface ExportFileSink {

    ExportStoredFile store(RenderedExportFile file, ExportStoreRequest request);
}
