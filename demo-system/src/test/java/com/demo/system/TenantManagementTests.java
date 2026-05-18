package com.demo.system;

import com.demo.core.exception.GlobalExceptionHandler;
import com.demo.core.mybatis.CommonMetaObjectHandler;
import com.demo.core.mybatis.MybatisPlusConfig;
import com.demo.system.infra.entity.SysUserEntity;
import com.demo.system.infra.mapper.SysUserMapper;
import com.demo.system.security.PasswordConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TenantManagementTests.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:demo-system-tenant;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        }
)
@AutoConfigureMockMvc
class TenantManagementTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists sys_user");
        jdbcTemplate.execute("drop table if exists sys_tenant_global");
        jdbcTemplate.execute("create table sys_tenant_global ("
                + "id bigint primary key,"
                + "tenant_name varchar(128) not null,"
                + "create_time timestamp not null default current_timestamp,"
                + "update_time timestamp not null default current_timestamp,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0,"
                + "constraint uk_sys_tenant_global_name unique (tenant_name)"
                + ")");
        jdbcTemplate.execute("create table sys_user ("
                + "id bigint primary key,"
                + "tenant_id bigint not null,"
                + "username varchar(64) not null,"
                + "password varchar(255) not null,"
                + "status tinyint not null default 1,"
                + "display_name varchar(64) null,"
                + "mobile varchar(32) null,"
                + "email varchar(128) null,"
                + "create_time timestamp not null default current_timestamp,"
                + "update_time timestamp not null default current_timestamp,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0,"
                + "constraint uk_sys_user_tenant_username unique (tenant_id, username)"
                + ")");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("drop table if exists sys_user");
        jdbcTemplate.execute("drop table if exists sys_tenant_global");
    }

    @Test
    void create_should_return_tenant_id_and_persist_to_db() throws Exception {
        mockMvc.perform(post("/api/system/tenant/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantName\":\"test-tenant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tenantId").isNumber());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_tenant_global where tenant_name = ? and deleted = 0",
                Integer.class,
                "test-tenant"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void create_should_fail_when_tenant_name_duplicated() throws Exception {
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (1, 'duplicate-tenant', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/tenant/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantName\":\"duplicate-tenant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002002))
                .andExpect(jsonPath("$.msg").value("租户名称已存在"));
    }

    @Test
    void update_should_modify_tenant_name() throws Exception {
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (1, 'old-name', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/tenant/update")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"tenantName\":\"new-name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_tenant_global where id = ? and tenant_name = ? and deleted = 0",
                Integer.class,
                1L,
                "new-name"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void update_should_fail_when_tenant_name_duplicated() throws Exception {
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (1, 'tenant-a', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (2, 'tenant-b', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/tenant/update")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"tenantName\":\"tenant-b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002002))
                .andExpect(jsonPath("$.msg").value("租户名称已存在"));
    }

    @Test
    void delete_should_fail_when_tenant_has_undeleted_users() throws Exception {
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (1, 'tenant-with-users', current_timestamp, current_timestamp, 0)"
        );
        SysUserEntity user = new SysUserEntity();
        user.setId(100L);
        user.setTenantId(1L);
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        mockMvc.perform(post("/api/system/tenant/delete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002003))
                .andExpect(jsonPath("$.msg").value("租户下存在未删除用户，不能删除"));
    }

    @Test
    void delete_should_soft_delete_tenant() throws Exception {
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (1, 'empty-tenant', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/tenant/delete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_tenant_global where id = ? and deleted = 1",
                Integer.class,
                1L
        );
        assertThat(count).isEqualTo(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            CommonMetaObjectHandler.class,
            MybatisPlusConfig.class,
            PasswordConfig.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}
