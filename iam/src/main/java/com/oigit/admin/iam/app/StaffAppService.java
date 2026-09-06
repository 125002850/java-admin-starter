package com.oigit.admin.iam.app;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.annotation.OperationLog;
import com.oigit.admin.iam.domain.gateway.CurrentUserGateway;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.service.IamStaffService;
import com.oigit.admin.iam.domain.service.PasswordPolicyService;
import com.oigit.admin.iam.dto.req.StaffCreateReqDTO;
import com.oigit.admin.iam.dto.req.StaffPageReqDTO;
import com.oigit.admin.iam.dto.req.StaffPasswordResetReqDTO;
import com.oigit.admin.iam.dto.req.StaffRolesAssignReqDTO;
import com.oigit.admin.iam.dto.req.StaffStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.req.StaffUpdateReqDTO;
import com.oigit.admin.iam.dto.rsp.DeptSummaryRspDTO;
import com.oigit.admin.iam.dto.rsp.RoleSummaryRspDTO;
import com.oigit.admin.iam.dto.rsp.StaffRspDTO;
import com.oigit.admin.iam.enums.IamStatus;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class StaffAppService {

    private final CurrentUserGateway currentUserGateway;
    private final IamStaffService staffService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final RefreshTokenAppService refreshTokenService;

    public StaffAppService(
            IamStaffService staffService,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            RefreshTokenAppService refreshTokenService,
            CurrentUserGateway currentUserGateway) {
        this.currentUserGateway = currentUserGateway;
        this.staffService = staffService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public PageResult<StaffRspDTO> page(StaffPageReqDTO reqDTO) {
        PermissionSnapshot snapshot =
                currentUserGateway
                        .current()
                        .orElseThrow(
                                () ->
                                        new AuthenticationCredentialsNotFoundException(
                                                "not authenticated"));
        PageSlice<IamStaff> page = staffService.page(IamQueryMapper.toQuery(reqDTO), snapshot);
        return assemblePageResult(page);
    }

    PageResult<StaffRspDTO> assemblePageResult(PageSlice<IamStaff> page) {
        Map<Long, IamDept> deptsById =
                staffService.findDepts(
                        page.getRecords().stream().map(IamStaff::getDeptId).toList());
        Map<Long, List<IamRole>> rolesByStaffId =
                staffService.listRolesByStaffIds(
                        page.getRecords().stream().map(IamStaff::getId).toList());
        List<StaffRspDTO> records =
                page.getRecords().stream()
                        .map(
                                entity ->
                                        toRsp(
                                                entity,
                                                deptsById.get(entity.getDeptId()),
                                                rolesByStaffId.getOrDefault(
                                                        entity.getId(), List.of())))
                        .toList();
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(readOnly = true)
    public StaffRspDTO detail(Long staffId) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(staffId, snapshot);
        IamStaff entity = staffService.requireById(staffId);
        return toRsp(
                entity,
                staffService.findDept(entity.getDeptId()),
                staffService.listRoles(entity.getId()));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.CREATE)
    public void create(StaffCreateReqDTO reqDTO) {
        passwordPolicyService.validate(reqDTO.getPassword());
        staffService.create(
                IamRequestMapper.toStaff(reqDTO),
                reqDTO.getRoleIds(),
                passwordEncoder.encode(reqDTO.getPassword()));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.UPDATE)
    public void update(StaffUpdateReqDTO reqDTO) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(reqDTO.getStaffId(), snapshot);
        IamStaff existing = staffService.requireById(reqDTO.getStaffId());
        IamStatus oldStatus = existing.getStatus();
        staffService.update(IamRequestMapper.toStaff(reqDTO));
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
        staffService.updatePassword(
                reqDTO.getStaffId(), passwordEncoder.encode(reqDTO.getNewPassword()), true);
        refreshTokenService.revokeAllByStaffId(reqDTO.getStaffId(), "PASSWORD_RESET");
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_STAFF, action = OperationLogAction.ASSIGN)
    public void assignRoles(StaffRolesAssignReqDTO reqDTO) {
        PermissionSnapshot snapshot = currentSnapshot();
        staffService.assertInDataScope(reqDTO.getStaffId(), snapshot);
        staffService.assignRoles(reqDTO.getStaffId(), reqDTO.getRoleIds());
    }

    private StaffRspDTO toRsp(IamStaff entity, IamDept dept, List<IamRole> roles) {
        StaffRspDTO dto = new StaffRspDTO();
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
        dto.setStatus(entity.getStatus());
        dto.setMustChangePassword(Boolean.TRUE.equals(entity.getMustChangePassword()));
        dto.setRemark(entity.getRemark());
        dto.setRoles(roleSummaries(roles));
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateById(entity.getCreateBy());
        dto.setUpdateById(entity.getUpdateBy());
        return dto;
    }

    private PermissionSnapshot currentSnapshot() {
        return currentUserGateway
                .current()
                .orElseThrow(
                        () ->
                                new org.springframework.security.authentication
                                        .AuthenticationCredentialsNotFoundException(
                                        "not authenticated"));
    }

    private List<RoleSummaryRspDTO> roleSummaries(List<IamRole> roles) {
        return roles.stream().map(PermissionSnapshotMapper::toRoleSummary).toList();
    }
}
