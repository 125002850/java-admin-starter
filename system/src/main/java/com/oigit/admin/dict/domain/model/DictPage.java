package com.oigit.admin.dict.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * 字典领域查询分页结果，隔离 MyBatis-Plus Page。
 */
public record DictPage<T>(List<T> records, long total) {

    public DictPage {
        records = List.copyOf(Objects.requireNonNull(records, "records must not be null"));
    }
}
