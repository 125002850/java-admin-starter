package com.oigit.admin.workcalendar.domain.gateway;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import java.time.LocalTime;
import java.util.List;
public interface WorkCalendarCodec {
    String encodePeriods(List<WorkingPeriod> periods);
    List<WorkingPeriod> decodePeriods(String json);
    WorkingPeriod parsePeriod(String start, String end);
    String formatTime(LocalTime time);
    String snapshotHash(
            int year,
            List<WorkingPeriod> standardPeriods,
            List<CalendarDateOverride> overrides
    );
}
