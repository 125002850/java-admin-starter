package com.oigit.admin.core.export.dto;

public interface ExportOptionsReqDTO {

    ExportRangeReqDTO getRange();

    void setRange(ExportRangeReqDTO range);

    Boolean getPackageMode();

    void setPackageMode(Boolean packageMode);

    Integer getChunkSize();

    void setChunkSize(Integer chunkSize);
}
