package com.oigit.admin.workcalendar.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oigit.admin.workcalendar.enums.WorkCalendarDateOverrideType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "工作日历特殊日期请求")
public record WorkCalendarDateOverrideReqDTO(
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "特殊日期", example = "2026-10-04")
        LocalDate date,
        @NotNull
        @Schema(
                description = "特殊日期覆盖类型稳定编码",
                type = "string",
                allowableValues = {"public_holiday", "adjusted_workday", "other_non_working_day"}
        )
        WorkCalendarDateOverrideType type,
        @Size(max = 128)
        @Schema(description = "节日或安排名称", example = "国庆节、中秋节")
        String name,
        @Size(max = 8)
        @Schema(description = "调休工作日自定义时段；缺失表示继承标准时段")
        List<@Valid WorkCalendarPeriodReqDTO> customPeriods,
        @Size(max = 256)
        @Schema(description = "来源或备注")
        String sourceNote
) {

    public WorkCalendarDateOverrideReqDTO {
        customPeriods = customPeriods == null ? null : List.copyOf(customPeriods);
    }
}
