package com.demo.mdm;

import com.demo.core.exception.GlobalExceptionHandler;
import com.demo.core.mybatis.MybatisPlusConfig;
import com.demo.mdm.app.DictAppService;
import com.demo.mdm.controller.DictController;
import com.demo.mdm.service.DictService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = DictModuleSmokeTests.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:demo-mdm-dict;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        }
)
@AutoConfigureMockMvc
class DictModuleSmokeTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists mdm_dict_item");
        jdbcTemplate.execute("drop table if exists mdm_dict_type");
        jdbcTemplate.execute("create table mdm_dict_type ("
                + "id bigint primary key,"
                + "tenant_id bigint not null,"
                + "dict_type_code varchar(64) not null,"
                + "dict_type_name varchar(128) not null,"
                + "create_time timestamp not null,"
                + "update_time timestamp not null,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0"
                + ")");
        jdbcTemplate.execute("create table mdm_dict_item ("
                + "id bigint primary key,"
                + "tenant_id bigint not null,"
                + "dict_type_code varchar(64) not null,"
                + "dict_item_code varchar(64) not null,"
                + "dict_item_name varchar(128) not null,"
                + "create_time timestamp not null,"
                + "update_time timestamp not null,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0"
                + ")");

        jdbcTemplate.update(
                "insert into mdm_dict_type (id, tenant_id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (1, 100, 'user_status', '用户状态', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into mdm_dict_item (id, tenant_id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (1, 100, 'user_status', 'ENABLED', '启用', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into mdm_dict_item (id, tenant_id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (2, 100, 'user_status', 'DISABLED', '停用', current_timestamp, current_timestamp, 0)"
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("drop table if exists mdm_dict_item");
        jdbcTemplate.execute("drop table if exists mdm_dict_type");
    }

    @Test
    void contextLoads() {
    }

    @Test
    void listItemsByType_should_return_items_for_tenant_and_type() throws Exception {
        mockMvc.perform(post("/api/mdm/dict/items/by-type")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tenantId\":100,\"dictTypeCode\":\"user_status\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dictItemCode").value("ENABLED"))
                .andExpect(jsonPath("$.data[1].dictItemCode").value("DISABLED"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            DictController.class,
            DictAppService.class,
            DictService.class,
            MybatisPlusConfig.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}
