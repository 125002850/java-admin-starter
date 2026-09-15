package com.oigit.admin.workcalendar.dto.rsp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oigit.admin.workcalendar.enums.ClassificationBasis;
import com.oigit.admin.workcalendar.enums.DayTrait;
import com.oigit.admin.workcalendar.enums.EffectiveDayKind;
import com.oigit.admin.workcalendar.enums.HolidayStatus;
import com.oigit.admin.workcalendar.enums.TimeClassification;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "工作日历时间判定结果")
public record TimeClassificationRspDTO(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime inputLocalDateTime,
        @Schema(description = "固定时区", allowableValues = "Asia/Shanghai") String zoneId,
        @Schema()
        TimeClassification classification,
        Boolean working,
        @Schema()
        EffectiveDayKind effectiveDayKind,
        @ArraySchema(schema = @Schema())
        List<DayTrait> traits,
        String dateName,
        List<WorkCalendarPeriodRspDTO> effectivePeriods,
        WorkCalendarPeriodRspDTO matchedPeriod,
        Boolean authoritative,
        @Schema()
        ClassificationBasis basis,
        @Schema()
        HolidayStatus holidayStatus,
        Integer calendarYear,
        Long calendarVersionId,
        Integer calendarVersionNo
) {

    public TimeClassificationRspDTO {
        traits = List.copyOf(traits);
        effectivePeriods = List.copyOf(effectivePeriods);
    }
}
