package com.oigit.admin.boot.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.boot.iam.IamTestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformModulesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    private String token;

    @BeforeEach void login() throws Exception { token = IamTestAuth.adminAccessToken(mvc, json); }

    @Test void scheduleLifecyclePersistsSuccessAndFailureAudit() throws Exception {
        String code = "verify-" + UUID.randomUUID();
        var request = Map.of("jobName", "验证任务", "jobCode", code, "defaultCron", "0 0 0 1 1 *",
                "invokeRoute", "bean://verificationJob.run", "status", "disable");
        JsonNode created = ok("/api/system/schedule/job/create", request);
        long id = created.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(created.path("status").asText()).isEqualTo("disable");
        assertThat(call("/api/system/schedule/job/create", request).path("code").asInt()).isEqualTo(3005002);
        assertThat(ok("/api/system/schedule/job/next-executions", Map.of("cronExpression", "0 0 9 * * MON-FRI", "count", 3)).size()).isEqualTo(3);
        var invalid = new java.util.HashMap<String, Object>(request);
        invalid.put("jobCode", code + "-invalid"); invalid.put("defaultCron", "invalid");
        assertThat(call("/api/system/schedule/job/create", invalid).path("code").asInt()).isEqualTo(3005003);
        ok("/api/system/schedule/job/enable", Map.of("id", id));
        assertThat(call("/api/system/schedule/job/delete", Map.of("id", id)).path("code").asInt()).isEqualTo(3005007);
        ok("/api/system/schedule/job/disable", Map.of("id", id));
        ok("/api/system/schedule/job/delete", Map.of("id", id));
        var logs = jdbc.queryForList("select result_status, operator_id from sys_operation_audit_log where request_path = ? and request_params like ?",
                "/api/system/schedule/job/create", "%" + code + "%");
        assertThat(logs).anySatisfy(log -> {
            assertThat(log.get("result_status")).isEqualTo("success");
            assertThat(log.get("operator_id")).isNotNull();
        }).anySatisfy(log -> assertThat(log.get("result_status")).isEqualTo("failed"));
    }

    @Test void calendarPublishesOnlyThePrecheckedUnchangedDraft() throws Exception {
        int year = 2098;
        // The dedicated test year is not shared with other platform fixtures.
        jdbc.update("delete from sys_work_calendar_date_override where calendar_version_id in (select id from sys_work_calendar_version where calendar_year = ?)", year);
        jdbc.update("delete from sys_work_calendar_version where calendar_year = ?", year);
        jdbc.update("delete from sys_work_calendar_year where calendar_year = ?", year);
        var request = Map.of("year", year, "standardPeriods", List.of(Map.of("start", "09:00", "end", "18:00")),
                "dateOverrides", List.of(Map.of("date", "2098-01-01", "type", "public_holiday", "name", "测试假日")));
        var saved = ok("/api/system/work-calendar/draft/save", request);
        var draft = saved.path("draftVersion");
        long id = draft.path("versionId").asLong();
        int version = draft.path("lockVersion").asInt();
        assertThat(id).isPositive();
        var prepared = ok("/api/system/work-calendar/publish/prepare", Map.of("draftVersionId", id, "expectedLockVersion", version));
        assertThat(prepared.path("publishable").asBoolean()).isTrue();
        assertThat(call("/api/system/work-calendar/publish", Map.of("draftVersionId", id, "expectedLockVersion", version + 1, "contentHash", prepared.path("contentHash").asText())).path("code").asInt()).isNotEqualTo(200);
        ok("/api/system/work-calendar/publish", Map.of("draftVersionId", id, "expectedLockVersion", version, "contentHash", prepared.path("contentHash").asText()));
        var classified = ok("/api/system/work-calendar/classify", Map.of("localDateTime", "2098-01-01 10:00:00"));
        assertThat(classified.path("effectiveDayKind").asText()).isEqualTo("public_holiday");
        assertThat(classified.path("basis").asText()).isEqualTo("published_snapshot");
        assertThat(classified.path("working").asBoolean()).isFalse();
    }

    @Test void listAllDictionaryItemsDoesNotApplyPageLimit() throws Exception {
        String type = "VERIFY_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("insert into sys_dict_type_global(dict_type_code, dict_type_name) values (?, ?)", type, "全量验证");
        try {
            for (int i = 0; i < 1005; i++) jdbc.update("insert into sys_dict_item_global(dict_type_code, dict_item_code, dict_item_name, sort_order) values (?, ?, ?, ?)", type, "v" + i, "项目" + i, i);
            assertThat(ok("/api/system/dict/global/items/list-all", Map.of("dictTypeCode", type)).size()).isEqualTo(1005);
        } finally {
            jdbc.update("delete from sys_dict_item_global where dict_type_code = ?", type);
            jdbc.update("delete from sys_dict_type_global where dict_type_code = ?", type);
        }
    }

    @Test void moduleMenusBelongToTheSystemManagementDirectory() {
        var menus = jdbc.queryForList("select child.menu_code from sys_menu child join sys_menu parent on child.parent_id=parent.id where parent.menu_code='system_management' and child.menu_code in ('operation-audit','schedule-center','work-calendar') and child.deleted=0", String.class);
        assertThat(menus).containsExactlyInAnyOrder("operation-audit", "schedule-center", "work-calendar");
    }

    @Test void newModulesRequireAuthentication() throws Exception {
        for (String path : List.of("/api/system/schedule/job/page", "/api/system/work-calendar/year/detail", "/api/system/operation-audit/page")) {
            mvc.perform(post(path).contentType(APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        }
    }

    private JsonNode ok(String path, Object payload) throws Exception {
        var result = call(path, payload);
        assertThat(result.path("code").asInt()).as(path + " " + result).isEqualTo(200);
        return result.path("data");
    }
    private JsonNode call(String path, Object payload) throws Exception {
        return json.readTree(mvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(payload))).andReturn().getResponse().getContentAsString());
    }
}
