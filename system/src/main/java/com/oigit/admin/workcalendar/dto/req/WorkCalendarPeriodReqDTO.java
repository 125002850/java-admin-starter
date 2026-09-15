package com.oigit.admin.workcalendar.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "工作时段请求，采用左闭右开区间")
public record WorkCalendarPeriodReqDTO(
        @NotBlank
        @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d")
        @Schema(description = "开始时间，包含", example = "09:00")
        String start,
        @NotBlank
        @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d")
        @Schema(description = "结束时间，不包含", example = "12:00")
        String end
) {
}
