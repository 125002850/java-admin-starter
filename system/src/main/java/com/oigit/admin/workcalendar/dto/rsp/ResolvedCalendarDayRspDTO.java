package com.oigit.admin.workcalendar.dto.rsp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oigit.admin.workcalendar.enums.DayTrait;
import com.oigit.admin.workcalendar.enums.EffectiveDayKind;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "工作日历单日解析结果")
public record ResolvedCalendarDayRspDTO(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @Schema(description = "星期，1 为周一、7 为周日") Integer dayOfWeek,
        @Schema()
        EffectiveDayKind effectiveDayKind,
        @ArraySchema(schema = @Schema())
        List<DayTrait> traits,
        String dateName,
        List<WorkCalendarPeriodRspDTO> effectivePeriods
) {

    public ResolvedCalendarDayRspDTO {
        traits = List.copyOf(traits);
        effectivePeriods = List.copyOf(effectivePeriods);
    }
}
