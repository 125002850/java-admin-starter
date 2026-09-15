package com.oigit.admin.workcalendar.dto.rsp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oigit.admin.workcalendar.enums.PublicationChangeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record DateOverrideChangeRspDTO(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @Schema()
        PublicationChangeType changeType,
        WorkCalendarDateOverrideRspDTO before,
        WorkCalendarDateOverrideRspDTO after
) {
}
