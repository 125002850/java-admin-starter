package com.oigit.admin.boot.flyway;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleSnapshotMigrationTests {
    @Test
    void upgradesExistingScheduleDataWithoutRecreatingTheDatabase() {
        String configured = System.getenv("SCHEDULE_MIGRATION_TEST_URL");
        String url = configured == null || configured.isBlank()
                ? "jdbc:h2:mem:schedule-upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false" : configured;
        String user = configured == null ? "sa" : System.getenv("JAVA_ADMIN_STARTER_DATASOURCE_USERNAME");
        String password = configured == null ? "" : System.getenv("JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD");
        var previous = Flyway.configure().dataSource(url, user, password).target("20261106114741").load();
        previous.migrate();
        var jdbc = new JdbcTemplate(previous.getConfiguration().getDataSource());
        jdbc.update("INSERT INTO sys_schedule_job (job_code, job_name, status) VALUES ('upgrade-probe', 'migration probe', 'disable')");
        Flyway.configure().dataSource(url, user, password).load().migrate();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sys_schedule_job WHERE job_code='upgrade-probe'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sys_schedule_job_request_snapshot", Integer.class)).isZero();
    }
}
