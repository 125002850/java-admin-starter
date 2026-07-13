package com.example.admin.mdm.export.service;

import com.example.admin.core.exception.BizException;
import com.example.admin.core.export.spi.ExportFileAccessor;
import com.example.admin.mdm.export.enums.ExportCenterErrorCode;
import com.example.admin.mdm.export.enums.ExportRecordStatus;
import com.example.admin.mdm.export.infra.entity.ExportRecordEntity;
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
        exportRecordService.recordDownloadLinkAcquired(entity.getId(), operatorId);
        return exportFileAccessor.fetchTempUrl(entity.getObjectKey());
    }

    private void ensureDownloadable(ExportRecordEntity entity) {
        if (entity.getStatus() == null
                || !entity.getStatus().equals(ExportRecordStatus.SUCCESS.getIntCode())
                || !StringUtils.hasText(entity.getObjectKey())) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
        }
    }
}
