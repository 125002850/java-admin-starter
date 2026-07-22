package com.oigit.admin.dict;

import com.oigit.admin.core.exception.GlobalExceptionHandler;
import com.oigit.admin.core.mybatis.CommonMetaObjectHandler;
import com.oigit.admin.core.mybatis.MybatisPlusConfig;
import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.core.query.support.DynamicQueryGuard;
import com.oigit.admin.core.query.support.QueryComplexityScorer;
import com.oigit.admin.dict.app.DictAppService;
import com.oigit.admin.dict.controller.GlobalDictController;
import com.oigit.admin.dict.query.globaldict.GlobalDictTypeSceneQueryDefinition;
import com.oigit.admin.dict.query.globaldict.GlobalDictTypeSceneQueryMapper;
import com.oigit.admin.dict.query.globaldict.GlobalDictItemSceneQueryMapper;
import com.oigit.admin.dict.query.globaldict.GlobalDictItemSceneQueryDefinition;
import com.oigit.admin.dict.service.DictService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = DictModuleSmokeTests.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:java-admin-starter-mdm-dict;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "mybatis-plus.global-config.db-config.logic-delete-value=unix_timestamp()",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
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
        jdbcTemplate.execute("drop table if exists sys_dict_item_global");
        jdbcTemplate.execute("drop table if exists sys_dict_type_global");
        jdbcTemplate.execute("create table sys_dict_type_global ("
                + "id bigint primary key,"
                + "dict_type_code varchar(64) not null,"
                + "dict_type_name varchar(128) not null,"
                + "remark varchar(512) null,"
                + "status varchar(32) not null default 'enable',"
                + "version int not null default 0,"
                + "create_time timestamp not null default current_timestamp,"
                + "update_time timestamp not null default current_timestamp,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0,"
                + "constraint uk_sys_dict_type_global_code unique (dict_type_code)"
                + ")");
        jdbcTemplate.execute("create table sys_dict_item_global ("
                + "id bigint primary key,"
                + "dict_type_code varchar(64) not null,"
                + "dict_item_code varchar(64) not null,"
                + "dict_item_name varchar(128) not null,"
                + "sort_order int not null default 0,"
                + "remark varchar(512) null,"
                + "status varchar(32) not null default 'enable',"
                + "version int not null default 0,"
                + "create_time timestamp not null default current_timestamp,"
                + "update_time timestamp not null default current_timestamp,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0,"
                + "constraint uk_sys_dict_item_global_type_code unique (dict_type_code, dict_item_code)"
                + ")");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("drop table if exists sys_dict_item_global");
        jdbcTemplate.execute("drop table if exists sys_dict_type_global");
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createGlobalType_should_persist_platform_dict_type() throws Exception {
        mockMvc.perform(post("/api/system/dict/global/type/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dictTypeCode\":\"gender\",\"dictTypeName\":\"性别\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_type_global where dict_type_code = ? and dict_type_name = ? and deleted = 0",
                Integer.class,
                "gender",
                "性别"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createGlobalType_should_fail_when_dict_type_code_duplicated() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (51, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/type/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dictTypeCode\":\"gender\",\"dictTypeName\":\"性别重复\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001002))
                .andExpect(jsonPath("$.msg").value("全局字典类型编码已存在"));
    }

    @Test
    void listGlobalTypes_should_return_platform_dict_types_with_dynamic_query_pagination() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (71, 'gender', '性别', timestamp '2026-06-01 08:00:00', timestamp '2026-06-01 08:00:00', 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (72, 'user_status', '用户状态', timestamp '2026-06-02 08:00:00', timestamp '2026-06-02 08:00:00', 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/types/list")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 2,
                                  "pageSize": 1,
                                  "condition": {
                                    "nodeType": "text",
                                    "field": "dictTypeName",
                                    "op": "CONTAINS",
                                    "value": "状态"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(0));
    }

    @Test
    void listGlobalTypes_should_return_audit_usernames() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global "
                        + "(id, dict_type_code, dict_type_name, create_time, update_time, create_by, update_by, deleted) "
                        + "values (76, 'audit_type', '审计字典', current_timestamp, current_timestamp, 1, 1, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/types/list")
                        .contentType(APPLICATION_JSON)
                        .content("{\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].createBy").value("admin"))
                .andExpect(jsonPath("$.data.list[0].updateBy").value("admin"));
    }

    @Test
    void listGlobalTypes_should_filter_sort_and_hide_deleted_records() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (73, 'user_status', '用户状态', timestamp '2026-06-01 08:00:00', timestamp '2026-06-01 08:00:00', 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (74, 'user_status_old', '旧用户状态', timestamp '2026-05-01 08:00:00', timestamp '2026-05-01 08:00:00', 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (75, 'order_status', '订单状态', timestamp '2026-06-03 08:00:00', timestamp '2026-06-03 08:00:00', unix_timestamp())"
        );

        mockMvc.perform(post("/api/system/dict/global/types/list")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "compose",
                                    "logic": "AND",
                                    "children": [
                                      {
                                        "nodeType": "text",
                                        "field": "dictTypeCode",
                                         "op": "CONTAINS",
                                        "value": "status"
                                      },
                                      {
                                        "nodeType": "dateTime",
"field": "createTime",
                                        "op": "GTE",
                                        "value": "2026-06-01 00:00:00"
                                      }
                                    ]
                                  },
                                  "sort": [
                                    {
                                      "field": "createTime",
                                      "direction": "DESC"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].dictTypeCode").value("user_status"));
    }

    @Test
    void listGlobalTypes_should_reject_too_deep_condition_tree() throws Exception {
        mockMvc.perform(post("/api/system/dict/global/types/list")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "compose",
                                    "logic": "AND",
                                    "children": [
                                      {
                                        "nodeType": "compose",
                                        "logic": "AND",
                                        "children": [
                                          {
                                            "nodeType": "compose",
                                            "logic": "AND",
                                            "children": [
                                              {
                                                "nodeType": "compose",
                                                "logic": "AND",
                                                "children": [
                                                  {
                                                    "nodeType": "compose",
                                                    "logic": "AND",
                                                    "children": [
                                                      {
                                                        "nodeType": "text",
                                                        "field": "dictTypeCode",
                                                         "op": "EQ",
                                                        "value": "user_status"
                                                      }
                                                    ]
                                                  }
                                                ]
                                              }
                                            ]
                                          }
                                        ]
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000011));
    }

    @Test
    void listGlobalTypes_should_return_bad_request_when_node_type_unknown() throws Exception {
        mockMvc.perform(post("/api/system/dict/global/types/list")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "mystery",
                                    "field": "dictTypeCode",
                                    "op": "EQ",
                                    "value": "user_status"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数错误"));
    }

    @Test
    void updateGlobalType_should_update_type_and_sync_item_type_code() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (81, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (82, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (83, 'gender', 'FEMALE', '女', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/type/update")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":81,\"dictTypeCode\":\"sex\",\"dictTypeName\":\"性别枚举\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"));

        Integer typeCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_type_global where id = ? and dict_type_code = ? and dict_type_name = ? and deleted = 0",
                Integer.class,
                81L,
                "sex",
                "性别枚举"
        );
        Integer itemCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_item_global where dict_type_code = ? and deleted = 0",
                Integer.class,
                "sex"
        );
        assertThat(typeCount).isEqualTo(1);
        assertThat(itemCount).isEqualTo(2);
    }

    @Test
    void deleteGlobalType_should_fail_when_type_still_has_items() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (84, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (85, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/type/delete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":84}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001010))
                .andExpect(jsonPath("$.msg").value("全局字典类型下存在字典项，不能删除"));
    }

    @Test
    void deleteGlobalType_should_soft_delete_empty_platform_dict_type() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (86, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/type/delete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":86}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_type_global where id = ? and deleted > 1",
                Integer.class,
                86L
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createGlobalItem_should_fail_when_dict_type_missing() throws Exception {
        mockMvc.perform(post("/api/system/dict/global/item/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dictTypeCode\":\"gender\",\"dictItemCode\":\"MALE\",\"dictItemName\":\"男\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001001))
                .andExpect(jsonPath("$.msg").value("全局字典类型不存在"));
    }

    @Test
    void createGlobalItem_should_persist_platform_dict_item() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (21, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/item/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dictTypeCode\":\"gender\",\"dictItemCode\":\"MALE\",\"dictItemName\":\"男\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_item_global where dict_type_code = ? and dict_item_code = ? and dict_item_name = ? and deleted = 0",
                Integer.class,
                "gender",
                "MALE",
                "男"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createGlobalItem_should_fail_when_dict_item_code_duplicated() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (61, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (62, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/item/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dictTypeCode\":\"gender\",\"dictItemCode\":\"MALE\",\"dictItemName\":\"男性\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001003))
                .andExpect(jsonPath("$.msg").value("全局字典项编码已存在"));
    }

    @Test
    void updateGlobalItem_should_update_item_and_support_switching_type() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (87, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (88, 'user_status', '用户状态', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (89, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/item/update")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":89,\"dictTypeCode\":\"user_status\",\"dictItemCode\":\"ENABLED\",\"dictItemName\":\"启用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_item_global where id = ? and dict_type_code = ? and dict_item_code = ? and dict_item_name = ? and deleted = 0",
                Integer.class,
                89L,
                "user_status",
                "ENABLED",
                "启用"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteGlobalItem_should_soft_delete_platform_dict_item() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (90, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (91, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/item/delete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"ids\":[91]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_dict_item_global where id = ? and deleted > 1",
                Integer.class,
                91L
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteGlobalItem_should_refresh_audit_fields() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (92, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (93, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "update sys_dict_item_global set update_time = ?, update_by = null where id = ?",
                Timestamp.valueOf(LocalDateTime.of(2020, 1, 1, 0, 0)),
                93L
        );

        mockMvc.perform(post("/api/system/dict/global/item/delete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"ids\":[93]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Timestamp updateTime = jdbcTemplate.queryForObject(
                "select update_time from sys_dict_item_global where id = ?",
                Timestamp.class,
                93L
        );
        Long updateBy = jdbcTemplate.queryForObject(
                "select update_by from sys_dict_item_global where id = ?",
                Long.class,
                93L
        );

        assertThat(updateTime).isNotNull();
        assertThat(updateTime.toLocalDateTime()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
        assertThat(updateBy).isEqualTo(0L);
    }

    @Test
    void listGlobalItemsByType_should_return_platform_items_for_type() throws Exception {
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (11, 'gender', '性别', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (12, 'gender', 'MALE', '男', current_timestamp, current_timestamp, 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_item_global (id, dict_type_code, dict_item_code, dict_item_name, create_time, update_time, deleted) "
                        + "values (13, 'gender', 'FEMALE', '女', current_timestamp, current_timestamp, 0)"
        );

        mockMvc.perform(post("/api/system/dict/global/items/by-type")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "text",
                                    "field": "dictTypeCode",
                                    "op": "EQ",
                                    "value": "gender"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("ok"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].dictItemCode").value("MALE"))
                .andExpect(jsonPath("$.data.list[1].dictItemCode").value("FEMALE"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            GlobalDictController.class,
            DictAppService.class,
            DictService.class,
            QueryComplexityScorer.class,
            DynamicQueryGuard.class,
            MybatisPlusQueryExecutor.class,
            GlobalDictTypeSceneQueryDefinition.class,
            GlobalDictTypeSceneQueryMapper.class,
            GlobalDictItemSceneQueryMapper.class,
            GlobalDictItemSceneQueryDefinition.class,
            CommonMetaObjectHandler.class,
            MybatisPlusConfig.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {

        @Bean
        OperatorUsernameResolver operatorUsernameResolver() {
            return operatorIds -> Map.of(1L, "admin");
        }
    }
}
