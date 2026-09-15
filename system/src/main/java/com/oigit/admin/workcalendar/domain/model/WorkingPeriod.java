package com.oigit.admin.workcalendar.domain.model;

import java.time.LocalTime;

public record WorkingPeriod(LocalTime start, LocalTime end) {
}
