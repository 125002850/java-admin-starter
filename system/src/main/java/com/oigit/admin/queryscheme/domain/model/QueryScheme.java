package com.oigit.admin.queryscheme.domain.model;

import java.time.LocalDateTime;

public record QueryScheme(Long id, String sceneCode, String schemeName, String contentJson,
                          Integer schemaVersion, Boolean defaultScheme, Integer version,
                          LocalDateTime createTime, LocalDateTime updateTime,
                          Long createBy, Long updateBy) {
}
