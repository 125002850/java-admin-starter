package com.demo.boot.flyway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywaySmokeTests {

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

        assertThat(tableColumns("sys_tenant_global"))
            .containsAll(AUDIT_COLUMNS)
            .doesNotContain("tenant_id");

        assertThat(tableColumns("sys_dict_type_global"))
            .containsAll(AUDIT_COLUMNS)
            .doesNotContain("tenant_id");

        assertThat(tableColumns("sys_dict_item_global"))
            .containsAll(AUDIT_COLUMNS)
            .doesNotContain("tenant_id");

        assertThat(tableColumns("mdm_dict_type"))
            .containsAll(AUDIT_COLUMNS)
            .contains("tenant_id");

        assertThat(tableColumns("mdm_dict_item"))
            .containsAll(AUDIT_COLUMNS)
            .contains("tenant_id");

        assertThat(tableColumns("sys_user"))
            .containsAll(AUDIT_COLUMNS)
            .contains("tenant_id");
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
    void flywayMigrationShouldApplyUniqueConstraintsForTenantDictTables() {
        insertTenantDictType(301L, 100L, "user_status_schema", "用户状态");
        assertThatThrownBy(() -> insertTenantDictType(302L, 100L, "user_status_schema", "用户状态重复"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("failed to insert tenant dict type");

        insertTenantDictItem(401L, 100L, "user_status_schema", "ENABLED", "启用");
        assertThatThrownBy(() -> insertTenantDictItem(402L, 100L, "user_status_schema", "ENABLED", "重复启用"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("failed to insert tenant dict item");
    }

    @Test
    void flywayMigrationShouldAutoFillAuditTimestampsOnInsert() {
        insertTenantGlobal(501L, "平台租户");
        insertSysUser(601L, 100L, "platform_admin", "{noop}pass");
        insertGlobalDictType(701L, "audit_gender", "审计性别");
        insertTenantDictType(801L, 100L, "audit_status", "审计状态");

        assertThat(queryTimestampCount("select count(*) from sys_tenant_global where id = ? and create_time is not null and update_time is not null", 501L))
            .isEqualTo(1);
        assertThat(queryTimestampCount("select count(*) from sys_user where id = ? and create_time is not null and update_time is not null", 601L))
            .isEqualTo(1);
        assertThat(queryTimestampCount("select count(*) from sys_dict_type_global where id = ? and create_time is not null and update_time is not null", 701L))
            .isEqualTo(1);
        assertThat(queryTimestampCount("select count(*) from mdm_dict_type where id = ? and create_time is not null and update_time is not null", 801L))
            .isEqualTo(1);
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)) {
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

    private void insertTenantDictType(Long id, Long tenantId, String dictTypeCode, String dictTypeName) {
        String sql = """
            insert into mdm_dict_type
                (id, tenant_id, dict_type_code, dict_type_name, create_by, update_by, deleted)
            values
                (?, ?, ?, ?, 0, 0, 0)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, tenantId);
            statement.setString(3, dictTypeCode);
            statement.setString(4, dictTypeName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to insert tenant dict type", exception);
        }
    }

    private void insertTenantDictItem(Long id, Long tenantId, String dictTypeCode, String dictItemCode, String dictItemName) {
        String sql = """
            insert into mdm_dict_item
                (id, tenant_id, dict_type_code, dict_item_code, dict_item_name, create_by, update_by, deleted)
            values
                (?, ?, ?, ?, ?, 0, 0, 0)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, tenantId);
            statement.setString(3, dictTypeCode);
            statement.setString(4, dictItemCode);
            statement.setString(5, dictItemName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to insert tenant dict item", exception);
        }
    }

    private void insertTenantGlobal(Long id, String tenantName) {
        String sql = """
            insert into sys_tenant_global
                (id, tenant_name, create_by, update_by, deleted)
            values
                (?, ?, 0, 0, 0)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, tenantName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to insert tenant global", exception);
        }
    }

    private void insertSysUser(Long id, Long tenantId, String username, String password) {
        String sql = """
            insert into sys_user
                (id, tenant_id, username, password, create_by, update_by, deleted)
            values
                (?, ?, ?, ?, 0, 0, 0)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, tenantId);
            statement.setString(3, username);
            statement.setString(4, password);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to insert sys user", exception);
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

    private Set<String> tableColumns(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {
            Set<String> columnNames = new LinkedHashSet<>();
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"));
            }
            return columnNames;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect column metadata", exception);
        }
    }
}
