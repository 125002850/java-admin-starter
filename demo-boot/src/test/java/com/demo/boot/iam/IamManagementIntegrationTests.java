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
        JsonNode response = postJson("/api/iam/staff/create", """
                {
                  "username": "%s",
                  "staffCode": "%s",
                  "staffName": "测试员工",
                  "deptId": %d,
                  "password": "Staff@123456",
                  "status": "ENABLED"
                }
                """.formatted(username, staffCode, deptId), token, 200);
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
