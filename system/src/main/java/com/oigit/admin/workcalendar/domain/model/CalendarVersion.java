package com.oigit.admin.workcalendar.domain.model;
public record CalendarVersion(Long id, Integer calendarYear, Integer versionNo, com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus status, Integer version, String contentHash, Long publishedBy, java.time.LocalDateTime publishedAt, Long updatedBy, java.time.LocalDateTime updatedAt) {
public CalendarVersion(Long id, Integer calendarYear, Integer versionNo, com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus status, Integer version, String contentHash, Long publishedBy, java.time.LocalDateTime publishedAt) { this(id, calendarYear, versionNo, status, version, contentHash, publishedBy, publishedAt, null, null); }
public Long getId() { return id; }
public Integer getCalendarYear() { return calendarYear; }
public Integer getVersionNo() { return versionNo; }
public com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus getStatus() { return status; }
public Integer getVersion() { return version; }
public String getContentHash() { return contentHash; }
public Long getPublishedBy() { return publishedBy; }
public java.time.LocalDateTime getPublishedAt() { return publishedAt; }
}
