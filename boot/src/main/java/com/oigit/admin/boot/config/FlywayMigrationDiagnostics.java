package com.oigit.admin.boot.config;

import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.flywaydb.core.internal.sqlscript.FlywaySqlScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

/** 在 Bean 初始化异常包装前输出迁移失败原因，保留 Flyway 原有的校验与失败行为。 */
@Component
public class FlywayMigrationDiagnostics implements FlywayMigrationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(FlywayMigrationDiagnostics.class);

    @Override
    public void migrate(Flyway flyway) {
        try {
            flyway.migrate();
        } catch (FlywayValidateException failure) {
            LOG.error("flyway_validation_failed reason={}; inspect the earlier flyway_migration_failed log "
                    + "and schema before repair; schema history does not retain the original SQL error",
                    safeReason(failure.getMessage().replace('\n', ' ').replace('\r', ' ')));
            throw failure;
        } catch (FlywayException failure) {
            FlywaySqlScriptException scriptFailure = null;
            SQLException sqlFailure = null;
            Throwable reason = failure;
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof FlywaySqlScriptException script) {
                    scriptFailure = script;
                }
                if (cause instanceof SQLException sql) {
                    sqlFailure = sql;
                }
                reason = cause;
            }
            LOG.error("flyway_migration_failed script={} line={} sqlState={} errorCode={} reason={}",
                    scriptFailure == null ? "-" : scriptFailure.getResource().getFilename(),
                    scriptFailure == null ? "-" : scriptFailure.getLineNumber(),
                    sqlFailure == null ? "-" : sqlFailure.getSQLState(),
                    sqlFailure == null ? "-" : sqlFailure.getErrorCode(), safeReason(reason.getMessage()));
            throw failure;
        }
    }

    private static String safeReason(String message) {
        if (message == null || message.isBlank()) {
            return "-";
        }
        // JDBC 驱动可能在后续行附带 SQL；引号中的值也可能来自业务数据。
        String firstLine = message.lines().findFirst().orElse("-")
                .replaceAll("'(?:''|[^'])*'", "'<redacted>'");
        return firstLine.substring(0, Math.min(firstLine.length(), 2048));
    }
}
