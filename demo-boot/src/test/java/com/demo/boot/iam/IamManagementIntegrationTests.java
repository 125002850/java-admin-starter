package com.demo.boot.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class IamManagementIntegrationTests {

    private static final String DEFAULT_ADMIN_PASSWORD_HASH =
            "$2a$10$WbuU43YMwePH06bezaQJBO3NG8OvpJBpN/kq13BLpS.25GE6gfwf2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDefaultAdmin() {
        jdbcTemplate.update("delete from sys_refresh_token");
        jdbcTemplate.update("""
                update sys_staff
                   set password_hash = ?,
                       must_change_password = 0,
                       status = 'ENABLED',
                       deleted = 0
                 where username = 'admin'
                """, DEFAULT_ADMIN_PASSWORD_HASH);
    }

    @Test
    void staffUpdateShouldRejectDisabledDept() throws Exception {
        String token = adminAccessToken();
        String suffix = suffix();
        String enabledDeptCode = "EN_" + suffix;
        String disabledDeptCode = "DIS_" + suffix;
        createDept(token, 1L, enabledDeptCode, "启用部门" + suffix, "ENABLED");
        createDept(token, 1L, disabledDeptCode, "禁用部门" + suffix, "DISABLED");
        Long enabledDeptId = deptId(enabledDeptCode);
        Long disabledDeptId = deptId(disabledDeptCode);

        String username = "staff_" + suffix;
        String staffCode = "S_" + suffix;
        createStaff(token, username, staffCode, enabledDeptId);
        Long staffId = staffId(username);

        JsonNode response = postJson("/api/iam/staff/update", """
                {
                  "staffId": %d,
                  "staffCode": "%s",
                  "staffName": "员工更新%s",
                  "deptId": %d,
                  "status": "ENABLED"
                }
                """.formatted(staffId, staffCode, suffix, disabledDeptId), token, 200);

        assertThat(response.path("code").asInt()).isEqualTo(2003006);
    }

    @Test
    void deptManagementShouldRejectDuplicateRootAndTreeCycles() throws Exception {
        String token = adminAccessToken();
        String suffix = suffix();
        String rootCode = "ROOT_" + suffix;
        createDept(token, null, rootCode, "根部门" + suffix, "ENABLED");

        JsonNode duplicateRoot = postJson("/api/iam/dept/create", """
                {
                  "deptCode": "%s",
                  "deptName": "根部门重复%s",
                  "status": "ENABLED"
                }
                """.formatted(rootCode, suffix), token, 200);
        assertThat(duplicateRoot.path("code").asInt()).isEqualTo(2003002);

        String parentCode = "DP_" + suffix;
        String childCode = "DC_" + suffix;
        createDept(token, 1L, parentCode, "父部门" + suffix, "ENABLED");
        Long parentId = deptId(parentCode);
        createDept(token, parentId, childCode, "子部门" + suffix, "ENABLED");
        Long childId = deptId(childCode);

        JsonNode selfParent = postJson("/api/iam/dept/update", """
                {
                  "deptId": %d,
                  "parentId": %d,
                  "deptCode": "%s",
                  "deptName": "子部门%s",
                  "status": "ENABLED"
                }
                """.formatted(childId, childId, childCode, suffix), token, 200);
        assertThat(selfParent.path("code").asInt()).isEqualTo(2003007);

        JsonNode descendantParent = postJson("/api/iam/dept/update", """
                {
                  "deptId": %d,
                  "parentId": %d,
                  "deptCode": "%s",
                  "deptName": "父部门%s",
                  "status": "ENABLED"
                }
                """.formatted(parentId, childId, parentCode, suffix), token, 200);
        assertThat(descendantParent.path("code").asInt()).isEqualTo(2003007);
    }

    @Test
    void menuManagementShouldRejectTreeCycles() throws Exception {
        String token = adminAccessToken();
        String suffix = suffix();
        String parentCode = "menu_parent_" + suffix;
        String childCode = "menu_child_" + suffix;
        createMenu(token, 1000L, parentCode, "父菜单" + suffix);
        Long parentId = menuId(parentCode);
        createMenu(token, parentId, childCode, "子菜单" + suffix);
        Long childId = menuId(childCode);

        JsonNode selfParent = postJson("/api/iam/menu/update", """
                {
                  "menuId": %d,
                  "parentId": %d,
                  "menuCode": "%s",
                  "menuName": "子菜单%s",
                  "menuType": "MENU",
                  "status": "ENABLED"
                }
                """.formatted(childId, childId, childCode, suffix), token, 200);
        assertThat(selfParent.path("code").asInt()).isEqualTo(2005006);

        JsonNode descendantParent = postJson("/api/iam/menu/update", """
                {
                  "menuId": %d,
                  "parentId": %d,
                  "menuCode": "%s",
                  "menuName": "父菜单%s",
                  "menuType": "MENU",
                  "status": "ENABLED"
                }
                """.formatted(parentId, childId, parentCode, suffix), token, 200);
        assertThat(descendantParent.path("code").asInt()).isEqualTo(2005006);
    }

    @Test
    void pageFiltersShouldSupportPrdFields() throws Exception {
        String token = adminAccessToken();
        String suffix = suffix();
        String deptCode = "PF_" + suffix;
        createDept(token, 1L, deptCode, "筛选部门" + suffix, "ENABLED");
        Long deptId = deptId(deptCode);

        String username = "filter_" + suffix;
        String staffCode = "F_" + suffix;
        String staffName = "筛选员工" + suffix;
        createStaff(token, username, staffCode, staffName, deptId);
        Long staffId = staffId(username);

        JsonNode staffPage = postJson("/api/iam/staff/page", """
                {
                  "pageNo": 1,
                  "pageSize": 10,
                  "staffCode": "%s",
                  "username": "%s",
                  "staffName": "%s",
                  "createTimeRange": {
                    "startTime": "2000-01-01 00:00:00",
                    "endTime": "2099-01-01 00:00:00"
                  }
                }
                """.formatted(staffCode, username, staffName), token, 200);
        assertThat(staffPage.path("code").asInt()).isEqualTo(200);
        assertThat(staffPage.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(staffPage.path("data").path("list").get(0).path("username").asText()).isEqualTo(username);

        String loginIp = "10.88.0.1";
        jdbcTemplate.update("""
                insert into sys_login_log
                  (staff_id, username, event_type, result, ip, operation_time, create_by, update_by, deleted)
                values (?, ?, 'LOGIN', 'SUCCESS', ?, '2026-07-08 10:00:00', 0, 0, 0)
                """, staffId, username, loginIp);

        JsonNode loginLogPage = postJson("/api/iam/log/login/page", """
                {
                  "pageNo": 1,
                  "pageSize": 10,
                  "username": "%s",
                  "staffName": "%s",
                  "result": "SUCCESS",
                  "ip": "%s",
                  "operationTimeRange": {
                    "startTime": "2026-07-08 00:00:00",
                    "endTime": "2026-07-08 23:59:59"
                  }
                }
                """.formatted(username, staffName, loginIp), token, 200);
        assertThat(loginLogPage.path("code").asInt()).isEqualTo(200);
        assertThat(loginLogPage.path("data").path("total").asLong()).isEqualTo(1);

        String requestPath = "/api/iam/staff/filter-" + suffix;
        jdbcTemplate.update("""
                insert into sys_operation_log
                  (operator_id, operator_username, operator_staff_name, module, action, request_path, http_method,
                   success, ip, user_agent, cost_millis, operation_time, create_by, update_by, deleted)
                values (?, ?, ?, 'IAM_STAFF', 'CREATE', ?, 'POST', 1, '10.88.0.2', 'JUnit', 12,
                        '2026-07-08 11:00:00', 0, 0, 0)
                """, staffId, username, staffName, requestPath);

        JsonNode operationLogPage = postJson("/api/iam/log/operation/page", """
                {
                  "pageNo": 1,
                  "pageSize": 10,
                  "operatorUsername": "%s",
                  "operatorStaffName": "%s",
                  "success": true,
                  "requestPath": "%s",
                  "operationTimeRange": {
                    "startTime": "2026-07-08 00:00:00",
                    "endTime": "2026-07-08 23:59:59"
                  }
                }
                """.formatted(username, staffName, requestPath), token, 200);
        assertThat(operationLogPage.path("code").asInt()).isEqualTo(200);
        assertThat(operationLogPage.path("data").path("total").asLong()).isEqualTo(1);
    }

    private String adminAccessToken() throws Exception {
        JsonNode login = postJson("/api/iam/auth/login", """
                {"username":"admin","password":"Admin@123456"}
                """, null, 200);
        assertThat(login.path("code").asInt()).isEqualTo(200);
        return login.path("data").path("accessToken").asText();
    }

    private void createDept(String token, Long parentId, String deptCode, String deptName, String status) throws Exception {
        String parentFragment = parentId == null ? "" : "\"parentId\": %d,".formatted(parentId);
        JsonNode response = postJson("/api/iam/dept/create", """
                {
                  %s
                  "deptCode": "%s",
                  "deptName": "%s",
                  "status": "%s"
                }
                """.formatted(parentFragment, deptCode, deptName, status), token, 200);
        assertThat(response.path("code").asInt()).isEqualTo(200);
    }

    private void createStaff(String token, String username, String staffCode, Long deptId) throws Exception {
        createStaff(token, username, staffCode, "测试员工", deptId);
    }

    private void createStaff(String token, String username, String staffCode, String staffName, Long deptId) throws Exception {
        JsonNode response = postJson("/api/iam/staff/create", """
                {
                  "username": "%s",
                  "staffCode": "%s",
                  "staffName": "%s",
                  "deptId": %d,
                  "password": "Staff@123456",
                  "status": "ENABLED"
                }
                """.formatted(username, staffCode, staffName, deptId), token, 200);
        assertThat(response.path("code").asInt()).isEqualTo(200);
    }

    private void createMenu(String token, Long parentId, String menuCode, String menuName) throws Exception {
        JsonNode response = postJson("/api/iam/menu/create", """
                {
                  "parentId": %d,
                  "menuCode": "%s",
                  "menuName": "%s",
                  "menuType": "MENU",
                  "status": "ENABLED"
                }
                """.formatted(parentId, menuCode, menuName), token, 200);
        assertThat(response.path("code").asInt()).isEqualTo(200);
    }

    private Long deptId(String deptCode) {
        return jdbcTemplate.queryForObject(
                "select id from sys_dept where dept_code = ? and deleted = 0",
                Long.class,
                deptCode
        );
    }

    private Long staffId(String username) {
        return jdbcTemplate.queryForObject(
                "select id from sys_staff where username = ? and deleted = 0",
                Long.class,
                username
        );
    }

    private Long menuId(String menuCode) {
        return jdbcTemplate.queryForObject(
                "select id from sys_menu where menu_code = ? and deleted = 0",
                Long.class,
                menuCode
        );
    }

    private JsonNode postJson(String path, String body, String accessToken, int expectedStatus) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "{}" : body);
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        String content = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private String suffix() {
        return Long.toString(System.nanoTime(), 36);
    }
}
