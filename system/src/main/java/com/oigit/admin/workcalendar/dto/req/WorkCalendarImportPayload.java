package com.oigit.admin.workcalendar.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WorkCalendarImportPayload(
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        @NotNull @Min(2000) @Max(2099) Integer year,
        @Size(max = 256) String source,
        @NotNull @Size(max = 366) List<@Valid WorkCalendarDateOverrideReqDTO> dateOverrides
) {

    public WorkCalendarImportPayload {
        dateOverrides = dateOverrides == null ? null : List.copyOf(dateOverrides);
    }
}
