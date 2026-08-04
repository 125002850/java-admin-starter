package com.oigit.admin.export.app;

import com.oigit.admin.core.export.spi.ExportFileAccessor;
import com.oigit.admin.export.domain.model.ExportRecord;
import org.springframework.stereotype.Service;

@Service
public class ExportDownloadAppService {

    private final ExportRecordLifecycleAppService exportRecordLifecycleAppService;
    private final ExportFileAccessor exportFileAccessor;

    public ExportDownloadAppService(
            ExportRecordLifecycleAppService exportRecordLifecycleAppService,
            ExportFileAccessor exportFileAccessor
    ) {
        this.exportRecordLifecycleAppService = exportRecordLifecycleAppService;
        this.exportFileAccessor = exportFileAccessor;
    }

    public String fetchDownloadUrl(ExportRecord record, Long operatorId) {
        record.requireDownloadable();
        String downloadUrl = exportFileAccessor.fetchTempUrl(record.getObjectKey());
        exportRecordLifecycleAppService.recordDownloadLinkAcquired(record.getId(), operatorId);
        return downloadUrl;
    }
}
