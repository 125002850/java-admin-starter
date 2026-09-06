package com.oigit.admin.iam.app;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.annotation.OperationLog;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.service.IamRoleService;
import com.oigit.admin.iam.dto.req.RoleCreateReqDTO;
import com.oigit.admin.iam.dto.req.RoleDataScopeAssignReqDTO;
import com.oigit.admin.iam.dto.req.RoleMenusAssignReqDTO;
import com.oigit.admin.iam.dto.req.RolePageReqDTO;
import com.oigit.admin.iam.dto.req.RoleStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.req.RoleUpdateReqDTO;
import com.oigit.admin.iam.dto.rsp.RoleRspDTO;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RoleAppService {

    private final IamRoleService roleService;

    public RoleAppService(IamRoleService roleService) {
        this.roleService = roleService;
    }

    @Transactional(readOnly = true)
    public PageResult<RoleRspDTO> page(RolePageReqDTO reqDTO) {
        PageSlice<IamRole> page = roleService.page(IamQueryMapper.toQuery(reqDTO));
        List<Long> roleIds = page.getRecords().stream().map(IamRole::getId).toList();
        Map<Long, List<Long>> menus = roleService.listMenuIdsByRoleIds(roleIds);
        Map<Long, List<Long>> depts = roleService.listDataScopeDeptIdsByRoleIds(roleIds);
        List<RoleRspDTO> records =
                page.getRecords().stream()
                        .map(
                                role ->
                                        toRsp(
                                                role,
                                                menus.getOrDefault(role.getId(), List.of()),
                                                depts.getOrDefault(role.getId(), List.of())))
                        .toList();
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(readOnly = true)
    public RoleRspDTO detail(Long roleId) {
        IamRole entity = roleService.requireById(roleId);
        return toRsp(
                entity, roleService.listMenuIds(roleId), roleService.listDataScopeDeptIds(roleId));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.CREATE)
    public void create(RoleCreateReqDTO reqDTO) {
        roleService.create(IamRequestMapper.toRole(reqDTO));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.UPDATE)
    public void update(RoleUpdateReqDTO reqDTO) {
        roleService.update(IamRequestMapper.toRole(reqDTO));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.DELETE)
    public void delete(Long roleId) {
        roleService.delete(roleId);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.STATUS_UPDATE)
    public void updateStatus(RoleStatusUpdateReqDTO reqDTO) {
        roleService.updateStatus(reqDTO.roleId, reqDTO.status);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.ASSIGN)
    public void assignMenus(RoleMenusAssignReqDTO reqDTO) {
        roleService.assignMenus(reqDTO.roleId, reqDTO.menuIds);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.ASSIGN)
    public void assignDataScope(RoleDataScopeAssignReqDTO reqDTO) {
        roleService.assignDataScope(reqDTO.roleId, reqDTO.dataScopeType, reqDTO.deptIds);
    }

    private RoleRspDTO toRsp(IamRole entity, List<Long> menuIds, List<Long> deptIds) {
        RoleRspDTO dto = new RoleRspDTO();
        dto.roleId = entity.getId();
        dto.roleCode = entity.getRoleCode();
        dto.roleName = entity.getRoleName();
        dto.sortOrder = entity.getSortOrder();
        dto.status = entity.getStatus();
        dto.dataScopeType = entity.getDataScopeType();
        dto.systemBuiltIn = entity.getSystemBuiltIn();
        dto.remark = entity.getRemark();
        dto.menuIds = menuIds;
        dto.dataScopeDeptIds = deptIds;
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateById(entity.getCreateBy());
        dto.setUpdateById(entity.getUpdateBy());
        return dto;
    }
}
