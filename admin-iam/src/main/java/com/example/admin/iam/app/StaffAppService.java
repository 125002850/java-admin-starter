package com.example.admin.iam.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.core.web.PageResult;
import com.example.admin.iam.annotation.OperationLog;
import com.example.admin.iam.dto.IamAuthDTO.DeptSummaryRspDTO;
import com.example.admin.iam.dto.IamAuthDTO.RoleSummaryRspDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffCreateReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffPageReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffPasswordResetReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffRolesAssignReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffRspDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffStatusUpdateReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffUpdateReqDTO;
import com.example.admin.iam.enums.IamStatus;
import com.example.admin.iam.enums.OperationLogAction;
import com.example.admin.iam.enums.OperationLogModule;
import com.example.admin.iam.infra.entity.IamDeptEntity;
import com.example.admin.iam.infra.entity.IamRoleEntity;
import com.example.admin.iam.infra.entity.IamStaffEntity;
import com.example.admin.iam.security.CurrentIam;
import com.example.admin.iam.service.IamStaffService;
import com.example.admin.iam.service.PasswordPolicyService;
import com.example.admin.iam.service.PermissionSnapshot;
import com.example.admin.iam.service.PermissionSnapshotMapper;
import com.example.admin.iam.service.RefreshTokenService;
import java.util.List;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAppService {

    private final IamStaffService staffService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final RefreshTokenService refreshTokenService;

    public StaffAppService(
            IamStaffService staffService,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            RefreshTokenService refreshTokenService
    ) {
        this.staffService = staffService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public PageResult<StaffRspDTO> page(StaffPageReqDTO reqDTO) {
        PermissionSnapshot snapshot = CurrentIam.principal()
                .map(principal -> principal.getSnapshot())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("not authenticated"));
        Page<IamStaffEntity> page = staffService.page(reqDTO, snapshot);
        return new PageResult<>(page.getRecords().stream().map(this::toRsp).toList(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public StaffRspDTO detail(Long staffId) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(staffId, snapshot);
        return toRsp(staffService.requireById(staffId));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.CREATE)
    public void create(StaffCreateReqDTO reqDTO) {
        passwordPolicyService.validate(reqDTO.getPassword());
        staffService.create(reqDTO, passwordEncoder.encode(reqDTO.getPassword()));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.UPDATE)
    public void update(StaffUpdateReqDTO reqDTO) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(reqDTO.getStaffId(), snapshot);
        IamStaffEntity existing = staffService.requireById(reqDTO.getStaffId());
        IamStatus oldStatus = existing.getStatus();
        staffService.update(reqDTO);
        if (reqDTO.getStatus() == IamStatus.DISABLED && oldStatus != IamStatus.DISABLED) {
            refreshTokenService.revokeAllByStaffId(reqDTO.getStaffId(), "STAFF_DISABLED");
        }
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.DELETE)
    public void delete(Long staffId) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(staffId, snapshot);
        staffService.delete(staffId);
        refreshTokenService.revokeAllByStaffId(staffId, "STAFF_DELETED");
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.STATUS_UPDATE)
    public void updateStatus(StaffStatusUpdateReqDTO reqDTO) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(reqDTO.getStaffId(), snapshot);
        staffService.updateStatus(reqDTO.getStaffId(), reqDTO.getStatus());
        if (reqDTO.getStatus() != null && "DISABLED".equals(reqDTO.getStatus().getCode())) {
            refreshTokenService.revokeAllByStaffId(reqDTO.getStaffId(), "STAFF_DISABLED");
        }
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.RESET_PASSWORD)
    public void resetPassword(StaffPasswordResetReqDTO reqDTO) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(reqDTO.getStaffId(), snapshot);
        passwordPolicyService.validate(reqDTO.getNewPassword());
        staffService.updatePassword(reqDTO.getStaffId(), passwordEncoder.encode(reqDTO.getNewPassword()), true);
        refreshTokenService.revokeAllByStaffId(reqDTO.getStaffId(), "PASSWORD_RESET");
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.ASSIGN)
    public void assignRoles(StaffRolesAssignReqDTO reqDTO) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(reqDTO.getStaffId(), snapshot);
        staffService.assignRoles(reqDTO.getStaffId(), reqDTO.getRoleIds());
    }

    private StaffRspDTO toRsp(IamStaffEntity entity) {
        StaffRspDTO dto = new StaffRspDTO();
        IamDeptEntity dept = staffService.findDept(entity.getDeptId());
        DeptSummaryRspDTO deptSummary = PermissionSnapshotMapper.toDeptSummary(dept);
        dto.setStaffId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setStaffCode(entity.getStaffCode());
        dto.setStaffName(entity.getStaffName());
        dto.setDeptId(entity.getDeptId());
        dto.setDeptName(deptSummary == null ? null : deptSummary.getDeptName());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setAvatar(entity.getAvatar());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().getCode());
        dto.setMustChangePassword(Boolean.TRUE.equals(entity.getMustChangePassword()));
        dto.setRemark(entity.getRemark());
        dto.setRoles(roleSummaries(entity.getId()));
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        return dto;
    }

    private PermissionSnapshot currentSnapshot() {
        return CurrentIam.principal()
                .map(p -> p.getSnapshot())
                .orElseThrow(() -> new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("not authenticated"));
    }

    private List<RoleSummaryRspDTO> roleSummaries(Long staffId) {
        List<IamRoleEntity> roles = staffService.listRoles(staffId);
        return roles.stream().map(PermissionSnapshotMapper::toRoleSummary).toList();
    }
}
