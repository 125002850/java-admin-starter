package com.oigit.admin.iam.domain.model;

import java.util.List;

public record PageSlice<T>(List<T> records, long total) {
    public PageSlice {
        records = List.copyOf(records);
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }
}
