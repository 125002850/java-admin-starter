package com.oigit.admin.export.domain.model;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportRecordStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportRecordTests {

    @Test
    void requireOwnedBy_should_reject_foreign_operator() {
        ExportRecord record = new ExportRecord();
        record.setCreateBy(1001L);

        assertThatThrownBy(() -> record.requireOwnedBy(1002L))
                .isInstanceOf(BizException.class)
                .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_FORBIDDEN.getMsg());
    }

    @Test
    void requireBatchDownloadable_should_reject_zip_record() {
        ExportRecord record = new ExportRecord();
        record.setStatus(ExportRecordStatus.SUCCESS);
        record.setObjectKey("export/demo.zip");
        record.setFileName("demo.zip");
        record.setFileType("zip");
        record.setFileSize(1024L);

        assertThatThrownBy(record::requireBatchDownloadable)
                .isInstanceOf(BizException.class)
                .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE.getMsg());
    }
}
