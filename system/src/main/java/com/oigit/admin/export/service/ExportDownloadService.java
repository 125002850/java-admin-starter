package com.oigit.admin.export.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.export.spi.ExportFileAccessor;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportRecordStatus;
import com.oigit.admin.export.infra.entity.ExportRecordEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportDownloadService {

    private final ExportRecordService exportRecordService;
    private final ExportFileAccessor exportFileAccessor;

    public ExportDownloadService(ExportRecordService exportRecordService, ExportFileAccessor exportFileAccessor) {
        this.exportRecordService = exportRecordService;
        this.exportFileAccessor = exportFileAccessor;
    }

    public String fetchDownloadUrl(ExportRecordEntity entity, Long operatorId) {
        ensureDownloadable(entity);
        String downloadUrl = exportFileAccessor.fetchTempUrl(entity.getObjectKey());
        exportRecordService.recordDownloadLinkAcquired(entity.getId(), operatorId);
        return downloadUrl;
    }

    private void ensureDownloadable(ExportRecordEntity entity) {
        if (entity.getStatus() == null
                || !entity.getStatus().equals(ExportRecordStatus.SUCCESS.getIntCode())
                || !StringUtils.hasText(entity.getObjectKey())) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
        }
    }
}
