package com.oigit.admin.workcalendar.infra.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class JacksonWorkCalendarCodec implements com.oigit.admin.workcalendar.domain.gateway.WorkCalendarCodec {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:[01]\\d|2[0-3]):[0-5]\\d");

    private final ObjectMapper objectMapper;

    public JacksonWorkCalendarCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encodePeriods(List<WorkingPeriod> periods) {
        List<Map<String, String>> payload = periods.stream()
                .map(period -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("start", formatTime(period.start()));
                    item.put("end", formatTime(period.end()));
                    return item;
                })
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("工作时段序列化失败", exception);
        }
    }

    public List<WorkingPeriod> decodePeriods(String json) {
        if (json == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                throw new IllegalStateException("工作时段快照不是数组");
            }
            List<WorkingPeriod> periods = new ArrayList<>(root.size());
            for (JsonNode item : root) {
                periods.add(parsePeriod(item.path("start").asText(null), item.path("end").asText(null)));
            }
            return List.copyOf(periods);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("工作时段快照解析失败", exception);
        }
    }

    public WorkingPeriod parsePeriod(String start, String end) {
        if (start == null
                || end == null
                || !TIME_PATTERN.matcher(start).matches()
                || !TIME_PATTERN.matcher(end).matches()) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_INVALID);
        }
        try {
            return new WorkingPeriod(LocalTime.parse(start, TIME_FORMATTER), LocalTime.parse(end, TIME_FORMATTER));
        } catch (DateTimeParseException exception) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_INVALID);
        }
    }

    public String formatTime(LocalTime time) {
        return TIME_FORMATTER.format(time);
    }

    public String snapshotHash(
            int year,
            List<WorkingPeriod> standardPeriods,
            List<CalendarDateOverride> overrides
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("year", year);
        snapshot.put("standardPeriods", periodPayload(standardPeriods));
        snapshot.put("dateOverrides", overrides.stream().map(this::overridePayload).toList());
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(snapshot);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("工作日历快照摘要生成失败", exception);
        }
    }

    private List<Map<String, String>> periodPayload(List<WorkingPeriod> periods) {
        return periods.stream().map(period -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("start", formatTime(period.start()));
            item.put("end", formatTime(period.end()));
            return item;
        }).toList();
    }

    private Map<String, Object> overridePayload(CalendarDateOverride override) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("date", override.date().toString());
        item.put("type", override.type().getCode());
        item.put("name", override.name());
        item.put("customPeriods", override.customPeriods() == null ? null : periodPayload(override.customPeriods()));
        item.put("sourceNote", override.sourceNote());
        return item;
    }
}
