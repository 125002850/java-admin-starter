package com.example.admin.boot.flyway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class FlywaySmokeTests {

    @DynamicPropertySource
    static void isolateDefaultH2Database(DynamicPropertyRegistry registry) {
        String datasourceUrl = System.getenv("JAVA_ADMIN_STARTER_DATASOURCE_URL");
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            registry.add(
                    "spring.datasource.url",
                    () -> "jdbc:h2:mem:flyway-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
            );
        }
    }

    private static final Set<String> AUDIT_COLUMNS = Set.of(
        "create_time",
        "update_time",
        "create_by",
        "update_by",
        "deleted"
    );

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayMigrationMatchesPlatformSchemaContract() {
        assertThat(tableExists("flyway_schema_history")).isTrue();
        assertThat(hasSuccessfulMigration("1")).isTrue();
        assertThat(hasSuccessfulMigration("2")).isTrue();
        assertThat(hasSuccessfulMigration("3")).isTrue();
        assertThat(hasSuccessfulMigration("4")).isTrue();
        assertThat(hasSuccessfulMigration("5")).isTrue();
        assertThat(hasSuccessfulMigration("6")).isTrue();
        assertThat(hasSuccessfulMigration("7")).isTrue();
        assertThat(hasSuccessfulMigration("8")).isTrue();
        assertThat(hasSuccessfulMigration("9")).isTrue();
        assertThat(hasSuccessfulMigration("10")).isTrue();

        assertThat(tableExists("sys_tenant_global")).isFalse();
        assertThat(tableExists("sys_user")).isFalse();
        assertThat(tableExists("mdm_dict_type")).isFalse();
        assertThat(tableExists("mdm_dict_item")).isFalse();

        assertThat(tableColumns("sys_dict_type_global"))
            .containsAll(AUDIT_COLUMNS)
            .contains("remark", "status", "version")
            .doesNotContain("tenant_id");

        assertThat(tableColumns("sys_dict_item_global"))
            .containsAll(AUDIT_COLUMNS)
            .contains("remark", "status", "sort_order", "version")
            .doesNotContain("tenant_id");
        assertThat(tableIndexes("sys_dict_item_global"))
            .contains("idx_sys_dict_item_global_type_sort");

        assertThat(tableExists("sys_export_record_global")).isTrue();
        assertThat(tableColumns("sys_export_record_global"))
            .containsAll(AUDIT_COLUMNS)
            .contains(
                "export_biz_code",
                "export_biz_name",
                "file_name",
                "file_type",
                "status",
                "expire_time",
                "query_snapshot_json",
                "query_snapshot_summary",
                "download_count",
                "expire_seconds",
                "version"
            )
            .doesNotContain("tenant_id");
        assertThat(tableIndexes("sys_export_record_global"))
            .contains(
                "idx_sys_export_record_global_creator_status_time",
                "idx_sys_export_record_global_status_expire_time",
                "idx_sys_export_record_global_biz_code_time"
            );

        assertThat(tableExists("sys_user_cache")).isFalse();

        assertThat(tableExists("sys_staff")).isTrue();
        assertThat(tableColumns("sys_staff"))
            .containsAll(AUDIT_COLUMNS)
            .contains(
                "username",
                "password_hash",
                "staff_code",
                "staff_name",
                "dept_id",
                "status",
                "must_change_password",
                "password_updated_time",
                "version"
            );
        assertThat(tableIndexes("sys_staff"))
            .anyMatch(index -> index.startsWith("uk_sys_staff_username"))
            .anyMatch(index -> index.startsWith("uk_sys_staff_code"));

        assertThat(tableExists("sys_dept")).isTrue();
        assertThat(tableColumns("sys_dept"))
            .containsAll(AUDIT_COLUMNS)
            .contains("parent_id", "dept_code", "dept_name", "full_path", "status", "version");

        assertThat(tableExists("sys_role")).isTrue();
        assertThat(tableColumns("sys_role"))
            .containsAll(AUDIT_COLUMNS)
            .contains("role_code", "role_name", "data_scope_type", "system_builtin", "version");
        assertThat(tableIndexes("sys_role"))
            .anyMatch(index -> index.startsWith("uk_sys_role_code"))
            .anyMatch(index -> index.startsWith("uk_sys_role_name"));

        assertThat(tableExists("sys_menu")).isTrue();
        assertThat(tableColumns("sys_menu"))
            .containsAll(AUDIT_COLUMNS)
            .contains("menu_code", "menu_name", "menu_type", "permission_code", "hidden", "cached", "version");
        assertThat(tableIndexes("sys_menu"))
            .anyMatch(index -> index.startsWith("uk_sys_menu_code"))
            .anyMatch(index -> index.startsWith("uk_sys_menu_permission"));

        assertThat(tableExists("sys_staff_role")).isTrue();
        assertThat(tableExists("sys_role_menu")).isTrue();
        assertThat(tableExists("sys_role_data_scope_dept")).isTrue();
        assertThat(tableExists("sys_refresh_token")).isTrue();
        assertThat(tableExists("sys_login_log")).isTrue();
        assertThat(tableExists("sys_operation_log")).isTrue();

        assertThat(queryCount("select count(*) from sys_staff where username = 'admin' and deleted = 0")).isEqualTo(1);
        assertThat(queryCount("select count(*) from sys_role where role_code = 'SUPER_ADMIN' and data_scope_type = 'ALL' and deleted = 0")).isEqualTo(1);
        assertThat(queryCount("select count(*) from sys_staff_role where staff_id = 1 and role_id = 1 and deleted = 0")).isEqualTo(1);
        assertThat(queryCount("select count(*) from sys_role_menu where role_id = 1 and deleted = 0")).isGreaterThan(0);
    }

    @Test
    void flywayMigrationShouldApplyUniqueConstraintsForGlobalDictTables() {
        insertGlobalDictType(101L, "gender_schema", "性别");
        assertThatThrownBy(() -> insertGlobalDictType(102L, "gender_schema", "性别重复"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("failed to insert global dict type");

        insertGlobalDictItem(201L, "gender_schema", "MALE", "男");
        assertThatThrownBy(() -> insertGlobalDictItem(202L, "gender_schema", "MALE", "男性"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("failed to insert global dict item");
    }

    @Test
    void flywayMigrationShouldAutoFillAuditTimestampsOnInsert() {
        insertGlobalDictType(701L, "audit_gender", "审计性别");

        assertThat(queryTimestampCount("select count(*) from sys_dict_type_global where id = ? and create_time is not null and update_time is not null", 701L))
            .isEqualTo(1);
    }

    @Test
    void flywayMigrationShouldSeedIamOperationLogActionDictionary() {
        assertThat(queryCount("""
                select count(*)
                from sys_dict_type_global
                where dict_type_code = 'IAM_OPERATION_LOG_ACTION'
                  and dict_type_name = '操作日志动作'
                  and status = 'enable'
                  and deleted = 0
                """))
            .isEqualTo(1);

        assertThat(queryGlobalDictItems("IAM_OPERATION_LOG_ACTION"))
            .containsExactly(
                Map.entry("CREATE", "新增"),
                Map.entry("UPDATE", "编辑"),
                Map.entry("DELETE", "删除"),
                Map.entry("STATUS_UPDATE", "状态变更"),
                Map.entry("ASSIGN", "分配"),
                Map.entry("RESET_PASSWORD", "重置密码"),
                Map.entry("CHANGE_PASSWORD", "修改密码"),
                Map.entry("LOGIN", "登录"),
                Map.entry("LOGOUT", "退出")
            );
    }

    @Test
    void flywayMigrationShouldSeedIamLoginLogDictionaries() {
        assertThat(queryGlobalDictItems("IAM_LOGIN_EVENT_TYPE"))
            .containsExactly(
                Map.entry("LOGIN", "登录"),
                Map.entry("REFRESH", "刷新令牌"),
                Map.entry("LOGOUT", "退出登录")
            );
        assertThat(queryGlobalDictItems("IAM_LOGIN_RESULT"))
            .containsExactly(
                Map.entry("SUCCESS", "成功"),
                Map.entry("FAIL", "失败")
            );
        assertThat(queryGlobalDictItems("IAM_LOGIN_FAILURE_REASON"))
            .containsExactly(
                Map.entry("BAD_CREDENTIALS", "用户名或密码错误"),
                Map.entry("STAFF_DISABLED", "员工已禁用"),
                Map.entry("REFRESH_TOKEN_INVALID", "刷新令牌无效"),
                Map.entry("REFRESH_TOKEN_EXPIRED", "刷新令牌已过期")
            );
    }

    @Test
    void flywayMigrationShouldAllowReassigningLogicallyDeletedStaffRole() {
        executeUpdate("update sys_staff_role set deleted = 100 where staff_id = 1 and role_id = 1");

        assertThatCode(() -> executeUpdate("""
                insert into sys_staff_role (staff_id, role_id, create_by, update_by, deleted)
                values (1, 1, 0, 0, 0)
                """))
            .doesNotThrowAnyException();
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect table metadata", exception);
        }
    }

    private boolean hasSuccessfulMigration(String version) {
        String sql = """
            select count(*)
            from flyway_schema_history
            where version = ?
              and success = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, version);
            statement.setBoolean(2, true);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect flyway history", exception);
        }
    }

    private void insertGlobalDictType(Long id, String dictTypeCode, String dictTypeName) {
        String sql = """
            insert into sys_dict_type_global
                (id, dict_type_code, dict_type_name, create_by, update_by, deleted)
            values
                (?, ?, ?, 0, 0, 0)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, dictTypeCode);
            statement.setString(3, dictTypeName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to insert global dict type", exception);
        }
    }

    private void insertGlobalDictItem(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
        String sql = """
            insert into sys_dict_item_global
                (id, dict_type_code, dict_item_code, dict_item_name, create_by, update_by, deleted)
            values
                (?, ?, ?, ?, 0, 0, 0)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, dictTypeCode);
            statement.setString(3, dictItemCode);
            statement.setString(4, dictItemName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to insert global dict item", exception);
        }
    }

    private int queryTimestampCount(String sql, Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to query audit timestamp count", exception);
        }
    }

    private int queryCount(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to query count", exception);
        }
    }

    private Map<String, String> queryGlobalDictItems(String dictTypeCode) {
        String sql = """
            select dict_item_code, dict_item_name
            from sys_dict_item_global
            where dict_type_code = ?
              and status = 'enable'
              and deleted = 0
            order by sort_order, id
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dictTypeCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, String> items = new LinkedHashMap<>();
                while (resultSet.next()) {
                    items.put(resultSet.getString("dict_item_code"), resultSet.getString("dict_item_name"));
                }
                return items;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to query global dict items", exception);
        }
    }

    private void executeUpdate(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to execute update", exception);
        }
    }

    private Set<String> tableColumns(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, null)) {
            Set<String> columnNames = new LinkedHashSet<>();
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
            return columnNames;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect column metadata", exception);
        }
    }

    private Set<String> tableIndexes(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            Set<String> indexNames = new LinkedHashSet<>();
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null) {
                    indexNames.add(indexName.toLowerCase(Locale.ROOT));
                }
            }
            return indexNames;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect index metadata", exception);
        }
    }
}
