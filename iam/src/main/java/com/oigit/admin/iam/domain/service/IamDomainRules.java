package com.oigit.admin.iam.domain.service;

import com.oigit.admin.iam.domain.model.IamDept;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class IamDomainRules {
    private IamDomainRules() {}

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static Set<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0L)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static Set<Long> descendants(Collection<Long> rootIds, Collection<IamDept> depts) {
        Map<Long, List<Long>> children =
                depts.stream()
                        .filter(dept -> dept.getParentId() != null)
                        .collect(
                                Collectors.groupingBy(
                                        IamDept::getParentId,
                                        LinkedHashMap::new,
                                        Collectors.mapping(IamDept::getId, Collectors.toList())));
        Set<Long> ids = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long id = queue.removeFirst();
            if (ids.add(id)) {
                queue.addAll(children.getOrDefault(id, List.of()));
            }
        }
        return ids;
    }
}
