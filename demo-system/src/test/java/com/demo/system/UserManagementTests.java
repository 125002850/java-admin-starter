package com.demo.system;

import com.demo.core.exception.GlobalExceptionHandler;
import com.demo.core.tenant.HeaderTenantResolver;
import com.demo.core.mybatis.CommonMetaObjectHandler;
import com.demo.core.mybatis.MybatisPlusConfig;
import com.demo.core.tenant.TenantContext;
import com.demo.core.tenant.TenantFilter;
import com.demo.system.app.AuthAppService;
import com.demo.system.controller.AuthController;
import com.demo.system.infra.entity.SysUserEntity;
import com.demo.system.infra.mapper.SysUserMapper;
import com.demo.system.security.PasswordConfig;
import com.demo.system.service.AuthService;
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
        classes = UserManagementTests.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:demo-system-user;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        }
)
@AutoConfigureMockMvc
class UserManagementTests {

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

        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (100, 'tenant-100', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_tenant_global (id, tenant_name, create_time, update_time, deleted) "
                        + "values (200, 'tenant-200', current_timestamp, current_timestamp, 0)"
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        jdbcTemplate.execute("drop table if exists sys_user");
        jdbcTemplate.execute("drop table if exists sys_tenant_global");
    }

    // ── User create (register) ──

    @Test
    void create_should_return_user_id_and_persist() throws Exception {
        mockMvc.perform(post("/api/system/user/create")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass123456\",\"displayName\":\"New User\",\"mobile\":\"13800138000\",\"email\":\"new@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").isNumber());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_user where tenant_id = ? and username = ? and deleted = 0",
                Integer.class,
                100L,
                "newuser"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void create_should_fail_when_username_duplicated_in_same_tenant() throws Exception {
        SysUserEntity existing = new SysUserEntity();
        existing.setId(1L);
        existing.setTenantId(100L);
        existing.setUsername("duplicate");
        existing.setPassword(passwordEncoder.encode("pass123456"));
        existing.setDeleted(0L);
        sysUserMapper.insert(existing);

        mockMvc.perform(post("/api/system/user/create")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"duplicate\",\"password\":\"pass123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003002))
                .andExpect(jsonPath("$.msg").value("用户名已存在"));
    }

    @Test
    void create_should_allow_same_username_across_tenants() throws Exception {
        SysUserEntity existing = new SysUserEntity();
        existing.setId(1L);
        existing.setTenantId(100L);
        existing.setUsername("sameuser");
        existing.setPassword(passwordEncoder.encode("pass123456"));
        existing.setDeleted(0L);
        sysUserMapper.insert(existing);

        mockMvc.perform(post("/api/system/user/create")
                        .header("X-Tenant-Id", "200")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"sameuser\",\"password\":\"pass123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").isNumber());
    }

    @Test
    void create_should_return_400_when_tenant_header_missing() throws Exception {
        mockMvc.perform(post("/api/system/user/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("缺少X-Tenant-Id"));
    }

    @Test
    void create_should_store_encrypted_password() throws Exception {
        mockMvc.perform(post("/api/system/user/create")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"cryptuser\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String password = jdbcTemplate.queryForObject(
                "select password from sys_user where tenant_id = ? and username = ? and deleted = 0",
                String.class,
                100L,
                "cryptuser"
        );
        assertThat(password).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", password)).isTrue();
    }

    @Test
    void create_should_fail_when_tenant_not_found() throws Exception {
        mockMvc.perform(post("/api/system/user/create")
                        .header("X-Tenant-Id", "999")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"orphan\",\"password\":\"pass123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002001))
                .andExpect(jsonPath("$.msg").value("租户不存在"));
    }

    // ── User status update (enable/disable) ──

    @Test
    void status_update_should_toggle_between_enabled_and_disabled() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("statususer");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        mockMvc.perform(post("/api/system/user/status/update")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbcTemplate.queryForObject(
                "select status from sys_user where id = ? and tenant_id = ?",
                Integer.class,
                1L,
                100L
        );
        assertThat(status).isEqualTo(0);

        mockMvc.perform(post("/api/system/user/status/update")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        status = jdbcTemplate.queryForObject(
                "select status from sys_user where id = ? and tenant_id = ?",
                Integer.class,
                1L,
                100L
        );
        assertThat(status).isEqualTo(1);
    }

    @Test
    void status_update_should_not_affect_other_tenant_user() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("user-a");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        mockMvc.perform(post("/api/system/user/status/update")
                        .header("X-Tenant-Id", "200")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003001))
                .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    @Test
    void disabled_user_should_not_be_able_to_login() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("disableduser");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        jdbcTemplate.update("update sys_user set status = 0 where id = ?", 1L);

        mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"username\":\"disableduser\",\"password\":\"pass123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001003))
                .andExpect(jsonPath("$.msg").value("用户已被禁用"));
    }

    // ── Password update ──

    @Test
    void password_update_should_allow_new_password_to_login() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("pwuser");
        user.setPassword(passwordEncoder.encode("oldpass123"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        mockMvc.perform(post("/api/system/user/password/update")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"password\":\"newpass456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"username\":\"pwuser\",\"password\":\"newpass456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"username\":\"pwuser\",\"password\":\"oldpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001001))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    // ── Profile update ──

    @Test
    void profile_update_should_persist_display_name_mobile_email() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("profileuser");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        mockMvc.perform(post("/api/system/user/profile/update")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1,\"displayName\":\"Profile User\",\"mobile\":\"13900139000\",\"email\":\"profile@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_user where id = ? and tenant_id = ? and display_name = ? and mobile = ? and email = ? and deleted = 0",
                Integer.class,
                1L,
                100L,
                "Profile User",
                "13900139000",
                "profile@test.com"
        );
        assertThat(count).isEqualTo(1);
    }

    // ── User delete ──

    @Test
    void delete_should_soft_delete_user() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("deleteuser");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        mockMvc.perform(post("/api/system/user/delete")
                        .header("X-Tenant-Id", "100")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_user where id = ? and deleted = 1",
                Integer.class,
                1L
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleted_user_should_not_be_able_to_login() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("deletedlogin");
        user.setPassword(passwordEncoder.encode("pass123456"));
        user.setDeleted(0L);
        sysUserMapper.insert(user);

        jdbcTemplate.update("update sys_user set deleted = 1 where id = ?", 1L);

        mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"username\":\"deletedlogin\",\"password\":\"pass123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001001))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            AuthController.class,
            AuthAppService.class,
            AuthService.class,
            PasswordConfig.class,
            CommonMetaObjectHandler.class,
            MybatisPlusConfig.class,
            GlobalExceptionHandler.class,
            HeaderTenantResolver.class,
            TenantFilter.class
    })
    static class TestApplication {
    }
}
