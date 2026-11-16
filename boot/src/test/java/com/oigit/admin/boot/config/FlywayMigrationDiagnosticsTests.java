package com.oigit.admin.boot.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlywayMigrationDiagnosticsTests {

    @TempDir
    Path migrations;

    private final FlywayMigrationDiagnostics diagnostics = new FlywayMigrationDiagnostics();
    private final Logger logger = (Logger) LoggerFactory.getLogger(FlywayMigrationDiagnostics.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachLogs() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void springBootLogsTheFirstSqlFailureBeforeStartupAborts() throws IOException {
        writeBrokenMigration();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class))
                .withUserConfiguration(FlywayMigrationDiagnostics.class)
                .withPropertyValues("spring.datasource.url=" + databaseUrl(),
                        "spring.flyway.locations=filesystem:" + migrations)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("missing_check");
                    assertThat(appender.list).singleElement().satisfies(event -> {
                        assertThat(event.getFormattedMessage()).contains("flyway_migration_failed",
                                "script=V1__broken.sql", "line=2", "sqlState=90057", "errorCode=90057",
                                "reason=Constraint").doesNotContain("alter table", "create table");
                        assertThat(event.getThrowableProxy()).isNull();
                    });
                });
    }

    @Test
    void laterValidationFailureDoesNotRepairOrRetryTheFailedMigration() throws IOException {
        writeBrokenMigration();
        Flyway flyway = Flyway.configure().dataSource(databaseUrl(), "sa", "")
                .locations("filesystem:" + migrations).load();
        assertThatThrownBy(() -> diagnostics.migrate(flyway)).isInstanceOf(FlywayException.class);
        appender.list.clear();

        assertThatThrownBy(() -> diagnostics.migrate(flyway)).isInstanceOf(FlywayValidateException.class);

        assertThat(appender.list).singleElement().satisfies(event -> assertThat(event.getFormattedMessage())
                .contains("flyway_validation_failed", "version 1", "earlier flyway_migration_failed", "before repair",
                        "does not retain the original SQL error"));
        var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        assertThat(jdbc.queryForObject("select count(*) from \"flyway_schema_history\" where \"success\"=false",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void redactsQuotedDatabaseValuesAndPreservesTheOriginalException() {
        Flyway flyway = mock(Flyway.class);
        var failure = new FlywayException("migration failed", new SQLException(
                "Duplicate entry 'private-token' for key 'uk_example'\nSQL statement:\ninsert into example values ('private-token')",
                "23000", 1062));
        when(flyway.migrate()).thenThrow(failure);

        assertThatThrownBy(() -> diagnostics.migrate(flyway)).isSameAs(failure);

        assertThat(appender.list).singleElement().satisfies(event -> assertThat(event.getFormattedMessage())
                .contains("sqlState=23000", "errorCode=1062", "Duplicate entry '<redacted>'")
                .doesNotContain("private-token", "SQL statement", "insert into"));
    }

    @Test
    void successfulMigrationStillRunsNormally() throws IOException {
        Files.writeString(migrations.resolve("V1__valid.sql"), "create table migration_ready (id int primary key);");
        Flyway flyway = Flyway.configure().dataSource(databaseUrl(), "sa", "")
                .locations("filesystem:" + migrations).load();

        diagnostics.migrate(flyway);

        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
        assertThat(appender.list).isEmpty();
    }

    private void writeBrokenMigration() throws IOException {
        Files.writeString(migrations.resolve("V1__broken.sql"), """
                create table migration_example (id int primary key);
                alter table migration_example drop constraint missing_check;
                """);
    }

    private String databaseUrl() {
        return "jdbc:h2:mem:flyway-diagnostics-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }
}
