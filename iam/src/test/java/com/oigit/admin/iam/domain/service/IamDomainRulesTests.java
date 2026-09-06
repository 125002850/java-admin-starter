package com.oigit.admin.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.DataScopeSummary;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.IamStatus;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

class IamDomainRulesTests {

    private final StaffRepository staffs = mock(StaffRepository.class);
    private final DeptRepository depts = mock(DeptRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final MenuRepository menus = mock(MenuRepository.class);

    @Test
    void lastEnabledSuperAdminCannotBeDisabled() {
        IamStaff staff = staff(1L, 10L);
        when(staffs.findById(1L)).thenReturn(staff);
        when(staffs.superAdminRoleId()).thenReturn(100L);
        when(staffs.hasRole(1L, 100L)).thenReturn(true);
        when(staffs.countOtherEnabledStaffWithRole(1L, 100L)).thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                new IamStaffService(staffs, depts, roles)
                                        .updateStatus(1L, IamStatus.DISABLED))
                .isInstanceOfSatisfying(
                        BizException.class,
                        ex ->
                                assertThat(ex.getErrorCode())
                                        .isEqualTo(IamErrorCode.STAFF_SUPER_ADMIN_REQUIRED));
        assertThat(staff.getStatus()).isEqualTo(IamStatus.ENABLED);
        verify(staffs, never()).save(any());
    }

    @Test
    void creatingStaffCannotGrantTheProtectedSuperAdminRole() {
        when(staffs.superAdminRoleId()).thenReturn(100L);

        assertThatThrownBy(
                        () ->
                                new IamStaffService(staffs, depts, roles)
                                        .create(staff(null, 10L), List.of(100L), "encoded"))
                .isInstanceOfSatisfying(
                        BizException.class,
                        ex ->
                                assertThat(ex.getErrorCode())
                                        .isEqualTo(IamErrorCode.STAFF_SUPER_ADMIN_PROTECTED));
        verify(staffs, never()).save(any());
    }

    @Test
    void aDepartmentCannotBeMovedBelowItsOwnDescendant() {
        IamDept parent = dept(10L, null, "总部");
        IamDept child = dept(11L, 10L, "研发");
        IamDept changes = dept(10L, 11L, "总部");
        when(depts.findById(10L)).thenReturn(parent);
        when(depts.findById(11L)).thenReturn(child);
        when(depts.listAll(null)).thenReturn(List.of(parent, child));

        assertThatThrownBy(() -> new IamDeptService(depts, staffs).update(changes))
                .isInstanceOfSatisfying(
                        BizException.class,
                        ex ->
                                assertThat(ex.getErrorCode())
                                        .isEqualTo(IamErrorCode.DEPT_PARENT_INVALID));
        verify(depts, never()).save(any());
    }

    @Test
    void permissionSnapshotCombinesRoleScopesAndRejectsMenusBelowDisabledAncestors() {
        IamStaff staff = staff(1L, 10L);
        IamRole descendantRole = role(100L, "DEPT", DataScopeType.DEPT_AND_CHILD);
        IamRole customRole = role(101L, "CUSTOM", DataScopeType.CUSTOM_DEPT);
        IamRole selfRole = role(102L, "SELF", DataScopeType.SELF);
        when(staffs.findById(1L)).thenReturn(staff);
        when(staffs.listRoles(1L)).thenReturn(List.of(descendantRole, customRole, selfRole));
        when(depts.listAll(null))
                .thenReturn(
                        List.of(
                                dept(10L, null, "总部"),
                                dept(11L, 10L, "研发"),
                                dept(20L, null, "分部")));
        when(roles.listDataScopeDeptIdsByRoleIds(List.of(100L, 101L, 102L)))
                .thenReturn(Map.of(101L, List.of(20L)));
        when(roles.listMenuIdsByRoleIds(List.of(100L, 101L, 102L)))
                .thenReturn(Map.of(100L, List.of(201L, 301L)));
        when(menus.listAll(null))
                .thenReturn(
                        List.of(
                                menu(200L, null, IamStatus.ENABLED, null),
                                menu(201L, 200L, IamStatus.ENABLED, "staff:read"),
                                menu(300L, null, IamStatus.DISABLED, null),
                                menu(301L, 300L, IamStatus.ENABLED, "staff:delete")));

        PermissionSnapshot snapshot =
                new PermissionSnapshotService(staffs, depts, roles, menus).loadByStaffId(1L);

        assertThat(snapshot.getPermissions()).containsExactly("staff:read");
        assertThat(snapshot.getMenus()).extracting(IamMenu::getId).containsExactly(200L, 201L);
        assertThat(snapshot.getDataScopeSummary().getEffectiveType())
                .isEqualTo(DataScopeType.MIXED.getCode());
        assertThat(snapshot.getDataScopeSummary().getDeptIds()).containsExactly(10L, 11L, 20L);
        assertThat(snapshot.getDataScopeSummary().isIncludeSelf()).isTrue();
        assertThat(snapshot.getDataScopeSummary().getRoleScopes()).hasSize(3);
        verify(depts).listAll(null);
        verify(roles).listDataScopeDeptIdsByRoleIds(List.of(100L, 101L, 102L));
    }

    @Test
    void staffDetailsOutsideTheCurrentScopeAreRejected() {
        IamStaff current = staff(1L, 10L);
        DataScopeSummary summary = new DataScopeSummary();
        summary.setEffectiveType(DataScopeType.DEPT_ONLY.getCode());
        summary.setDeptIds(List.of(10L));
        PermissionSnapshot snapshot =
                new PermissionSnapshot(
                        current,
                        null,
                        List.of(),
                        Set.of(),
                        List.of(),
                        summary,
                        false,
                        "fingerprint");
        when(staffs.findById(2L)).thenReturn(staff(2L, 20L));

        assertThatThrownBy(
                        () ->
                                new IamStaffService(staffs, depts, roles)
                                        .assertInDataScope(2L, snapshot))
                .isInstanceOfSatisfying(
                        BizException.class,
                        ex ->
                                assertThat(ex.getErrorCode())
                                        .isEqualTo(IamErrorCode.STAFF_OUT_OF_DATA_SCOPE));
    }

    @Test
    void refreshTokenExpiresAtTheExactExpiryInstant() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 5, 12, 0);
        IamRefreshToken token = new IamRefreshToken();
        token.setExpireTime(now);

        assertThatThrownBy(() -> RefreshTokenPolicy.validate(token, now))
                .isInstanceOfSatisfying(
                        BizException.class,
                        ex ->
                                assertThat(ex.getErrorCode())
                                        .isEqualTo(IamErrorCode.AUTH_REFRESH_TOKEN_EXPIRED));
    }

    private IamStaff staff(Long id, Long deptId) {
        IamStaff staff = new IamStaff();
        staff.setId(id);
        staff.setDeptId(deptId);
        staff.setStatus(IamStatus.ENABLED);
        return staff;
    }

    private IamDept dept(Long id, Long parentId, String name) {
        IamDept dept = new IamDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setStatus(IamStatus.ENABLED);
        return dept;
    }

    private IamRole role(Long id, String code, DataScopeType type) {
        IamRole role = new IamRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setDataScopeType(type);
        role.setStatus(IamStatus.ENABLED);
        role.setSortOrder(0);
        return role;
    }

    private IamMenu menu(Long id, Long parentId, IamStatus status, String permission) {
        IamMenu menu = new IamMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setStatus(status);
        menu.setPermissionCode(permission);
        return menu;
    }
}
