package com.oigit.admin.iam.app;

import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.dto.req.DeptCreateReqDTO;
import com.oigit.admin.iam.dto.req.DeptUpdateReqDTO;
import com.oigit.admin.iam.dto.req.MenuCreateReqDTO;
import com.oigit.admin.iam.dto.req.MenuUpdateReqDTO;
import com.oigit.admin.iam.dto.req.RoleCreateReqDTO;
import com.oigit.admin.iam.dto.req.RoleUpdateReqDTO;
import com.oigit.admin.iam.dto.req.StaffCreateReqDTO;
import com.oigit.admin.iam.dto.req.StaffUpdateReqDTO;

final class IamRequestMapper {
    private IamRequestMapper() {}

    static IamStaff toStaff(StaffCreateReqDTO dto) {
        IamStaff model = new IamStaff();
        model.setUsername(dto.getUsername());
        model.setStaffCode(dto.getStaffCode());
        model.setStaffName(dto.getStaffName());
        model.setDeptId(dto.getDeptId());
        model.setPhone(dto.getPhone());
        model.setEmail(dto.getEmail());
        model.setAvatar(dto.getAvatar());
        model.setStatus(dto.getStatus());
        model.setRemark(dto.getRemark());
        return model;
    }

    static IamStaff toStaff(StaffUpdateReqDTO dto) {
        IamStaff model = new IamStaff();
        model.setId(dto.getStaffId());
        model.setStaffCode(dto.getStaffCode());
        model.setStaffName(dto.getStaffName());
        model.setDeptId(dto.getDeptId());
        model.setPhone(dto.getPhone());
        model.setEmail(dto.getEmail());
        model.setAvatar(dto.getAvatar());
        model.setStatus(dto.getStatus());
        model.setRemark(dto.getRemark());
        return model;
    }

    static IamDept toDept(DeptCreateReqDTO dto) {
        IamDept model = new IamDept();
        model.setParentId(dto.parentId);
        model.setDeptCode(dto.deptCode);
        model.setDeptName(dto.deptName);
        model.setSortOrder(dto.sortOrder);
        model.setStatus(dto.status);
        model.setRemark(dto.remark);
        return model;
    }

    static IamDept toDept(DeptUpdateReqDTO dto) {
        IamDept model = new IamDept();
        model.setId(dto.deptId);
        model.setParentId(dto.parentId);
        model.setDeptCode(dto.deptCode);
        model.setDeptName(dto.deptName);
        model.setSortOrder(dto.sortOrder);
        model.setStatus(dto.status);
        model.setRemark(dto.remark);
        return model;
    }

    static IamRole toRole(RoleCreateReqDTO dto) {
        IamRole model = new IamRole();
        model.setRoleCode(dto.roleCode);
        model.setRoleName(dto.roleName);
        model.setSortOrder(dto.sortOrder);
        model.setStatus(dto.status);
        model.setDataScopeType(dto.dataScopeType);
        model.setRemark(dto.remark);
        return model;
    }

    static IamRole toRole(RoleUpdateReqDTO dto) {
        IamRole model = new IamRole();
        model.setId(dto.roleId);
        model.setRoleCode(dto.roleCode);
        model.setRoleName(dto.roleName);
        model.setSortOrder(dto.sortOrder);
        model.setStatus(dto.status);
        model.setDataScopeType(dto.dataScopeType);
        model.setRemark(dto.remark);
        return model;
    }

    static IamMenu toMenu(MenuCreateReqDTO dto) {
        IamMenu model = new IamMenu();
        model.setParentId(dto.parentId);
        model.setMenuCode(dto.menuCode);
        model.setMenuName(dto.menuName);
        model.setMenuType(dto.menuType);
        model.setRoutePath(dto.routePath);
        model.setComponentPath(dto.componentPath);
        model.setIcon(dto.icon);
        model.setSortOrder(dto.sortOrder);
        model.setHidden(dto.hidden);
        model.setCached(dto.cached);
        model.setStatus(dto.status);
        model.setPermissionCode(dto.permissionCode);
        model.setRemark(dto.remark);
        return model;
    }

    static IamMenu toMenu(MenuUpdateReqDTO dto) {
        IamMenu model = new IamMenu();
        model.setId(dto.menuId);
        model.setParentId(dto.parentId);
        model.setMenuCode(dto.menuCode);
        model.setMenuName(dto.menuName);
        model.setMenuType(dto.menuType);
        model.setRoutePath(dto.routePath);
        model.setComponentPath(dto.componentPath);
        model.setIcon(dto.icon);
        model.setSortOrder(dto.sortOrder);
        model.setHidden(dto.hidden);
        model.setCached(dto.cached);
        model.setStatus(dto.status);
        model.setPermissionCode(dto.permissionCode);
        model.setRemark(dto.remark);
        return model;
    }
}
