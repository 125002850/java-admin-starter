package com.demo.boot.web;

import com.demo.core.operator.OperatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DynamicQueryContractIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        OperatorContext.clear();
        cleanTables();
    }

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
        cleanTables();
    }

    @Test
    void unknownNodeTypeShouldReturnWrappedParamError() throws Exception {
        mockMvc.perform(post("/api/mdm/dict/global/types/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "unknown-node"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数错误"));
    }

    @Test
    void tooDeepConditionTreeShouldReturnBizError() throws Exception {
        mockMvc.perform(post("/api/mdm/dict/global/types/list")
                        .contentType(MediaType.APPLICATION_JSON)
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
                .andExpect(jsonPath("$.code").value(3000011))
                .andExpect(jsonPath("$.msg").value("动态查询条件树深度超限"));
    }

    @Test
    void tooManyNodesShouldReturnBizError() throws Exception {
        String children = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> """
                        {
                          "nodeType": "text",
                          "field": "dictTypeCode",
                          "op": "EQ",
                          "value": "code-%d"
                        }
                        """.formatted(index))
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/mdm/dict/global/types/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "compose",
                                    "logic": "AND",
                                    "children": [%s]
                                  }
                                }
                                """.formatted(children)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000012))
                .andExpect(jsonPath("$.msg").value("动态查询条件节点数量超限"));
    }

    @Test
    void oversizedPageSizeShouldReturnBizError() throws Exception {
        mockMvc.perform(post("/api/mdm/export/my/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                   "pageSize": 2001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000010))
                .andExpect(jsonPath("$.msg").value("动态查询分页大小超限"));
    }

    @Test
    void oversizedInValuesShouldReturnBizError() throws Exception {
        String values = IntStream.rangeClosed(1, 201)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/mdm/export/my/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "enum",
                                    "field": "status",
                                    "op": "IN",
                                    "values": [%s]
                                  }
                                }
                                """.formatted(values)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000009))
                .andExpect(jsonPath("$.msg").value("动态查询 IN 集合过大"));
    }

    @Test
    void eqWithNullShouldReturnWrappedValidationError() throws Exception {
        mockMvc.perform(post("/api/mdm/dict/global/types/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "text",
                                    "field": "dictTypeCode",
                                    "op": "EQ",
                                    "value": null
                                  }
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("文本条件 value/values 不匹配操作符"));
    }

    @Test
    void logicalDeletedRecordsShouldNotBeVisibleThroughDynamicQuery() throws Exception {
        insertGlobalDictType(801L, "user_status", "用户状态", 0L, LocalDateTime.of(2026, 6, 1, 8, 0, 0));
        insertGlobalDictType(802L, "user_status_deleted", "已删除用户状态", 1L, LocalDateTime.of(2026, 6, 2, 8, 0, 0));

        mockMvc.perform(post("/api/mdm/dict/global/types/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "text",
                                    "field": "dictTypeCode",
                                    "op": "CONTAINS",
                                    "value": "status"
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

    private void cleanTables() {
        jdbcTemplate.update("delete from sys_export_record_global");
        jdbcTemplate.update("delete from sys_dict_item_global");
        jdbcTemplate.update("delete from sys_dict_type_global");
    }

    private void insertGlobalDictType(
            Long id,
            String dictTypeCode,
            String dictTypeName,
            Long deleted,
            LocalDateTime createTime
    ) {
        jdbcTemplate.update("""
                insert into sys_dict_type_global (
                  id, dict_type_code, dict_type_name, create_time, update_time, create_by, update_by, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                dictTypeCode,
                dictTypeName,
                Timestamp.valueOf(createTime),
                Timestamp.valueOf(createTime),
                0L,
                0L,
                deleted
        );
    }
}
