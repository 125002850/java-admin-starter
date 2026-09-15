package com.oigit.admin.workcalendar.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作时段响应，采用左闭右开区间")
public record WorkCalendarPeriodRspDTO(
        @Schema(description = "开始时间，包含", example = "09:00") String start,
        @Schema(description = "结束时间，不包含", example = "12:00") String end
) {
}
