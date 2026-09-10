package com.oigit.admin.core.query.scene;

import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SceneQuerySorts {

    public static final String CREATE_TIME_FIELD = "createTime";
    public static final String ID_FIELD = "id";

    private SceneQuerySorts() {
    }

    public static List<SortSpec> defaultCreationSorts(Set<String> sortableFields) {
        if (!sortableFields.contains(CREATE_TIME_FIELD) || !sortableFields.contains(ID_FIELD)) {
            return List.of();
        }
        return List.of(
                new SortSpec(CREATE_TIME_FIELD, SortItemDTO.SortDirection.DESC),
                new SortSpec(ID_FIELD, SortItemDTO.SortDirection.DESC)
        );
    }

    public static List<SortSpec> appendIdDescIfAbsent(List<SortSpec> sorts, Set<String> sortableFields) {
        if (sorts == null || sorts.isEmpty() || !sortableFields.contains(ID_FIELD)) {
            return sorts;
        }
        if (sorts.stream().anyMatch(sort -> ID_FIELD.equals(sort.getFieldKey()))) {
            return sorts;
        }
        List<SortSpec> stableSorts = new ArrayList<>(sorts);
        stableSorts.add(new SortSpec(ID_FIELD, SortItemDTO.SortDirection.DESC));
        return List.copyOf(stableSorts);
    }
}
