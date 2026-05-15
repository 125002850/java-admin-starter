package com.demo.boot.flyway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(hasSuccessfulV1Migration()).isTrue();

        assertThat(tableColumns("sys_tenant_global"))
            .containsAll(AUDIT_COLUMNS)
            .doesNotContain("tenant_id");

        assertThat(tableColumns("sys_user"))
            .containsAll(AUDIT_COLUMNS)
            .contains("tenant_id");
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)) {
            return tables.next();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect table metadata", exception);
        }
    }

    private boolean hasSuccessfulV1Migration() {
        String sql = """
            select count(*)
            from flyway_schema_history
            where version = ?
              and success = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "1");
            statement.setBoolean(2, true);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to inspect flyway history", exception);
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
