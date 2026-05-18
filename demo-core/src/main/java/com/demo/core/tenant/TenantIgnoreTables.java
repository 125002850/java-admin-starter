package com.demo.core.tenant;

import java.util.Locale;
import java.util.Set;

public final class TenantIgnoreTables {

    private static final Set<String> TABLES = Set.of(
        "sys_tenant_global",
        "sys_dict_type_global",
        "sys_dict_item_global"
    );

    private TenantIgnoreTables() {
    }

    public static boolean contains(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return false;
        }
        return TABLES.contains(tableName.toLowerCase(Locale.ROOT));
    }
}
