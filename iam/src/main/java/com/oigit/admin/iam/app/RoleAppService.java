package com.oigit.admin.iam.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.annotation.OperationLog;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleCreateReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleDataScopeAssignReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleMenusAssignReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RolePageReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleRspDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleUpdateReqDTO;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;
import com.oigit.admin.iam.infra.entity.IamRoleEntity;
import com.oigit.admin.iam.service.IamRoleService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleAppService {

    private final IamRoleService roleService;

    public RoleAppService(IamRoleService roleService) {
        this.roleService = roleService;
    }

    @Transactional(readOnly = true)
    public PageResult<RoleRspDTO> page(RolePageReqDTO reqDTO) {
        Page<IamRoleEntity> page = roleService.page(reqDTO);
        List<RoleRspDTO> records = page.getRecords().stream()
                .map(this::toRsp)
                .toList();
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(readOnly = true)
    public RoleRspDTO detail(Long roleId) {
        IamRoleEntity entity = roleService.requireById(roleId);
        return toRsp(entity);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.CREATE)
    public void create(RoleCreateReqDTO reqDTO) {
        roleService.create(reqDTO);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.UPDATE)
    public void update(RoleUpdateReqDTO reqDTO) {
        roleService.update(reqDTO);
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
        roleService.assignMenus(reqDTO);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_ROLE, action = OperationLogAction.ASSIGN)
    public void assignDataScope(RoleDataScopeAssignReqDTO reqDTO) {
        roleService.assignDataScope(reqDTO);
    }

    private RoleRspDTO toRsp(IamRoleEntity entity) {
        RoleRspDTO dto = new RoleRspDTO();
        dto.roleId = entity.getId();
        dto.roleCode = entity.getRoleCode();
        dto.roleName = entity.getRoleName();
        dto.sortOrder = entity.getSortOrder();
        dto.status = entity.getStatus();
        dto.dataScopeType = entity.getDataScopeType();
        dto.systemBuiltIn = entity.getSystemBuiltIn();
        dto.remark = entity.getRemark();
        dto.menuIds = roleService.listMenuIds(entity.getId());
        dto.dataScopeDeptIds = roleService.listDataScopeDeptIds(entity.getId());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateById(entity.getCreateBy());
        dto.setUpdateById(entity.getUpdateBy());
        return dto;
    }
}
