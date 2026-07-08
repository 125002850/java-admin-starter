package com.demo.iam.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.web.PageResult;
import com.demo.iam.annotation.OperationLog;
import com.demo.iam.dto.IamAuthDTO.DeptSummaryRspDTO;
import com.demo.iam.dto.IamAuthDTO.RoleSummaryRspDTO;
import com.demo.iam.dto.IamStaffDTO.StaffCreateReqDTO;
import com.demo.iam.dto.IamStaffDTO.StaffPageReqDTO;
import com.demo.iam.dto.IamStaffDTO.StaffPasswordResetReqDTO;
import com.demo.iam.dto.IamStaffDTO.StaffRolesAssignReqDTO;
import com.demo.iam.dto.IamStaffDTO.StaffRspDTO;
import com.demo.iam.dto.IamStaffDTO.StaffStatusUpdateReqDTO;
import com.demo.iam.dto.IamStaffDTO.StaffUpdateReqDTO;
import com.demo.iam.enums.OperationLogAction;
import com.demo.iam.enums.OperationLogModule;
import com.demo.iam.infra.entity.IamDeptEntity;
import com.demo.iam.infra.entity.IamRoleEntity;
import com.demo.iam.infra.entity.IamStaffEntity;
import com.demo.iam.security.CurrentIam;
import com.demo.iam.service.IamStaffService;
import com.demo.iam.service.PasswordPolicyService;
import com.demo.iam.service.PermissionSnapshot;
import com.demo.iam.service.PermissionSnapshotMapper;
import com.demo.iam.service.RefreshTokenService;
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
        staffService.update(reqDTO);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.DELETE)
    public void delete(Long staffId) {
        staffService.delete(staffId);
        refreshTokenService.revokeAllByStaffId(staffId, "STAFF_DELETED");
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.STATUS_UPDATE)
    public void updateStatus(StaffStatusUpdateReqDTO reqDTO) {
        staffService.updateStatus(reqDTO.getStaffId(), reqDTO.getStatus());
        if (reqDTO.getStatus() != null && "DISABLED".equals(reqDTO.getStatus().getCode())) {
            refreshTokenService.revokeAllByStaffId(reqDTO.getStaffId(), "STAFF_DISABLED");
        }
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.RESET_PASSWORD)
    public void resetPassword(StaffPasswordResetReqDTO reqDTO) {
        passwordPolicyService.validate(reqDTO.getNewPassword());
        staffService.updatePassword(reqDTO.getStaffId(), passwordEncoder.encode(reqDTO.getNewPassword()), true);
        refreshTokenService.revokeAllByStaffId(reqDTO.getStaffId(), "PASSWORD_RESET");
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.ASSIGN)
    public void assignRoles(StaffRolesAssignReqDTO reqDTO) {
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

    private List<RoleSummaryRspDTO> roleSummaries(Long staffId) {
        List<IamRoleEntity> roles = staffService.listRoles(staffId);
        return roles.stream().map(PermissionSnapshotMapper::toRoleSummary).toList();
    }
}
