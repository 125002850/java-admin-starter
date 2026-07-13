package com.example.admin.iam.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.core.web.PageResult;
import com.example.admin.iam.annotation.OperationLog;
import com.example.admin.iam.dto.IamRoleDTO.RoleCreateReqDTO;
import com.example.admin.iam.dto.IamRoleDTO.RoleDataScopeAssignReqDTO;
import com.example.admin.iam.dto.IamRoleDTO.RoleMenusAssignReqDTO;
import com.example.admin.iam.dto.IamRoleDTO.RolePageReqDTO;
import com.example.admin.iam.dto.IamRoleDTO.RoleRspDTO;
import com.example.admin.iam.dto.IamRoleDTO.RoleStatusUpdateReqDTO;
import com.example.admin.iam.dto.IamRoleDTO.RoleUpdateReqDTO;
import com.example.admin.iam.enums.OperationLogAction;
import com.example.admin.iam.enums.OperationLogModule;
import com.example.admin.iam.infra.entity.IamRoleEntity;
import com.example.admin.iam.service.IamRoleService;
import com.example.admin.iam.service.IamStaffService;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleAppService {

    private final IamRoleService roleService;
    private final IamStaffService staffService;

    public RoleAppService(IamRoleService roleService, IamStaffService staffService) {
        this.roleService = roleService;
        this.staffService = staffService;
    }

    @Transactional(readOnly = true)
    public PageResult<RoleRspDTO> page(RolePageReqDTO reqDTO) {
        Page<IamRoleEntity> page = roleService.page(reqDTO);
        Map<Long, String> usernames = auditUsernames(page.getRecords());
        List<RoleRspDTO> records = page.getRecords().stream()
                .map(entity -> toRsp(entity, usernames))
                .toList();
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(readOnly = true)
    public RoleRspDTO detail(Long roleId) {
        IamRoleEntity entity = roleService.requireById(roleId);
        return toRsp(entity, auditUsernames(List.of(entity)));
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

    private RoleRspDTO toRsp(IamRoleEntity entity, Map<Long, String> usernames) {
        RoleRspDTO dto = new RoleRspDTO();
        dto.roleId = entity.getId();
        dto.roleCode = entity.getRoleCode();
        dto.roleName = entity.getRoleName();
        dto.sortOrder = entity.getSortOrder();
        dto.status = entity.getStatus() == null ? null : entity.getStatus().getCode();
        dto.dataScopeType = entity.getDataScopeType() == null ? null : entity.getDataScopeType().getCode();
        dto.systemBuiltIn = entity.getSystemBuiltIn();
        dto.remark = entity.getRemark();
        dto.menuIds = roleService.listMenuIds(entity.getId());
        dto.dataScopeDeptIds = roleService.listDataScopeDeptIds(entity.getId());
        dto.createTime = entity.getCreateTime();
        dto.updateTime = entity.getUpdateTime();
        dto.createBy = auditUsername(usernames, entity.getCreateBy());
        dto.updateBy = auditUsername(usernames, entity.getUpdateBy());
        return dto;
    }

    private Map<Long, String> auditUsernames(List<IamRoleEntity> roles) {
        List<Long> staffIds = roles.stream()
                .flatMap(role -> Stream.of(role.getCreateBy(), role.getUpdateBy()))
                .toList();
        return staffService.resolveUsernames(staffIds);
    }

    private String auditUsername(Map<Long, String> usernames, Long staffId) {
        return staffId == null ? null : usernames.get(staffId);
    }
}
