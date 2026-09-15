package com.oigit.admin.workcalendar.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工作日历发布预检结果")
public record PrepareWorkCalendarPublicationRspDTO(
        Long draftVersionId,
        Integer draftLockVersion,
        String contentHash,
        Boolean publishable,
        List<String> validationIssues,
        PublicationDiffRspDTO diffSummary
) {

    public PrepareWorkCalendarPublicationRspDTO {
        validationIssues = List.copyOf(validationIssues);
    }
}
