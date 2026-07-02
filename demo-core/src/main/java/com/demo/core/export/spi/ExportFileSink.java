package com.demo.core.export.spi;

import com.demo.core.export.model.ExportStoreRequest;
import com.demo.core.export.model.ExportStoredFile;
import com.demo.core.export.model.RenderedExportFile;

public interface ExportFileSink {

    ExportStoredFile store(RenderedExportFile file, ExportStoreRequest request);
}
