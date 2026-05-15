package com.demo.system;

import com.demo.core.exception.GlobalExceptionHandler;
import com.demo.core.tenant.TenantContext;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthFlowTests.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:demo-system-auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        }
)
@AutoConfigureMockMvc
class AuthFlowTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists sys_user");
        jdbcTemplate.execute("create table sys_user ("
                + "id bigint primary key,"
                + "tenant_id bigint not null,"
                + "username varchar(64) not null,"
                + "password varchar(255) not null,"
                + "create_time timestamp not null,"
                + "update_time timestamp not null,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0"
                + ")");

        insertUser(1L, 100L, "admin", "admin123");
        insertUser(2L, 200L, "admin", "other-pass");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("drop table if exists sys_user");
    }

    @Test
    void password_should_not_be_stored_as_plain_text() {
        TenantContext.setTenantId(100L);
        try {
            SysUserEntity user = sysUserMapper.selectById(1L);
            assertThat(user.getPassword()).isNotEqualTo("admin123");
            assertThat(passwordEncoder.matches("admin123", user.getPassword())).isTrue();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void login_should_honor_tenant_scope() throws Exception {
                mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":200,\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001001))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    void login_should_return_user_when_credentials_match() throws Exception {
        mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.tenantId").value(100))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_should_fail_explicitly_when_duplicate_username_exists_in_same_tenant() throws Exception {
        insertUser(3L, 100L, "admin", "another-pass");

                mockMvc.perform(post("/api/system/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001002))
                .andExpect(jsonPath("$.msg").value("用户名重复，请联系管理员处理"));
    }

    private void insertUser(Long id, Long tenantId, String username, String rawPassword) {
        SysUserEntity entity = new SysUserEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(0L);
        sysUserMapper.insert(entity);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            AuthController.class,
            AuthAppService.class,
            AuthService.class,
            PasswordConfig.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}
