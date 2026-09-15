package com.oigit.admin.workcalendar.dto.req;

import com.oigit.admin.workcalendar.enums.WorkCalendarYearView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "工作日历年度详情请求")
public record WorkCalendarYearDetailReqDTO(
        @NotNull @Min(2000) @Max(2099)
        @Schema(description = "自然年", example = "2026")
        Integer year,
        @Schema(
                description = "查看视图；不传时优先返回草稿，否则返回当前生效版本",
                implementation = String.class,
                allowableValues = {"draft", "active"},
                example = "draft"
        )
        WorkCalendarYearView view
) {
}
