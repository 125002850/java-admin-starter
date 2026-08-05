package com.oigit.admin.export.domain.model;

import java.util.List;
import java.util.Objects;

/** 分页查询结果，隔离 MyBatis-Plus Page。 */
public record ExportRecordPage(List<ExportRecord> records, long total) {

    public ExportRecordPage {
        records = List.copyOf(Objects.requireNonNull(records, "records must not be null"));
    }
}
