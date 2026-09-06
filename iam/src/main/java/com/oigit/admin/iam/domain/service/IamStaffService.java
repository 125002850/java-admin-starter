package com.oigit.admin.iam.domain.service;

import static com.oigit.admin.iam.domain.service.IamDomainRules.descendants;
import static com.oigit.admin.iam.domain.service.IamDomainRules.normalizeIds;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.query.StaffQuery;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.IamStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IamStaffService {
    private final StaffRepository staffRepository;
    private final DeptRepository deptRepository;
    private final RoleRepository roleRepository;

    public IamStaffService(
            StaffRepository staffRepository,
            DeptRepository deptRepository,
            RoleRepository roleRepository) {
        this.staffRepository = staffRepository;
        this.deptRepository = deptRepository;
        this.roleRepository = roleRepository;
    }

    public PageSlice<IamStaff> page(StaffQuery query, PermissionSnapshot snapshot) {
        Collection<Long> roots =
                query.getDeptIds() != null && !query.getDeptIds().isEmpty()
                        ? query.getDeptIds()
                        : query.getDeptId() == null ? List.of() : List.of(query.getDeptId());
        if (roots.isEmpty()) {
            return staffRepository.page(query, snapshot, null);
        }
        Set<Long> deptIds = normalizeIds(roots);
        if (!deptIds.isEmpty() && !Boolean.FALSE.equals(query.getIncludeDescendants())) {
            deptIds = descendants(deptIds, deptRepository.listAll(null));
        }
        return staffRepository.page(query, snapshot, deptIds);
    }

    public IamStaff findByUsername(String username) {
        return staffRepository.findByUsername(username);
    }

    public IamStaff requireById(Long staffId) {
        IamStaff staff = staffRepository.findById(staffId);
        if (staff == null) {
            throw new BizException(IamErrorCode.STAFF_NOT_FOUND);
        }
        return staff;
    }

    public boolean isEnabled(IamStaff staff) {
        return staff != null && staff.getStatus() == IamStatus.ENABLED;
    }

    public void updatePassword(Long staffId, String passwordHash, boolean mustChangePassword) {
        IamStaff staff = requireById(staffId);
        staff.setPasswordHash(passwordHash);
        staff.setMustChangePassword(mustChangePassword);
        staff.setPasswordUpdatedTime(LocalDateTime.now());
        staffRepository.save(staff);
    }

    public IamStaff create(IamStaff staff, List<Long> roleIds, String passwordHash) {
        assertSuperAdminRoleNotRequested(roleIds, staffRepository.superAdminRoleId());
        if (staffRepository.usernameExists(staff.getUsername(), null)) {
            throw new BizException(IamErrorCode.STAFF_USERNAME_DUPLICATED);
        }
        assertStaffCodeAvailable(staff.getStaffCode(), null);
        requireAssignableDept(staff.getDeptId());
        staff.setPasswordHash(passwordHash);
        staff.setStatus(staff.getStatus() == null ? IamStatus.ENABLED : staff.getStatus());
        staff.setMustChangePassword(true);
        staff.setPasswordUpdatedTime(LocalDateTime.now());
        staffRepository.save(staff);
        assignRoles(staff.getId(), roleIds);
        return staff;
    }

    public void update(IamStaff changes) {
        IamStaff staff = requireById(changes.getId());
        assertStaffCodeAvailable(changes.getStaffCode(), changes.getId());
        requireAssignableDept(changes.getDeptId());
        IamStatus newStatus = changes.getStatus() == null ? staff.getStatus() : changes.getStatus();
        if (newStatus == IamStatus.DISABLED && staff.getStatus() != IamStatus.DISABLED) {
            assertCanRemoveSuperAdminCapability(staff.getId());
        }
        staff.setStaffCode(changes.getStaffCode());
        staff.setStaffName(changes.getStaffName());
        staff.setDeptId(changes.getDeptId());
        staff.setPhone(changes.getPhone());
        staff.setEmail(changes.getEmail());
        staff.setAvatar(changes.getAvatar());
        staff.setStatus(newStatus);
        staff.setRemark(changes.getRemark());
        staffRepository.save(staff);
    }

    public void updateStatus(Long staffId, IamStatus status) {
        IamStaff staff = requireById(staffId);
        if (status == IamStatus.DISABLED) {
            assertCanRemoveSuperAdminCapability(staffId);
        }
        staff.setStatus(status);
        staffRepository.save(staff);
    }

    public void delete(Long staffId) {
        requireById(staffId);
        assertCanRemoveSuperAdminCapability(staffId);
        staffRepository.delete(staffId);
    }

    public void assignRoles(Long staffId, List<Long> roleIds) {
        requireById(staffId);
        Long superAdminRoleId = staffRepository.superAdminRoleId();
        if (staffRepository.hasRole(staffId, superAdminRoleId)) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_PROTECTED);
        }
        assertSuperAdminRoleNotRequested(roleIds, superAdminRoleId);
        Set<Long> ids = roleIds == null ? Set.of() : new LinkedHashSet<>(roleIds);
        if (!roleRepository.findByIds(ids).keySet().containsAll(ids)) {
            throw new BizException(IamErrorCode.ROLE_NOT_FOUND);
        }
        staffRepository.replaceRoles(staffId, ids);
    }

    public List<IamRole> listRoles(Long staffId) {
        return staffRepository.listRoles(staffId);
    }

    public Map<Long, List<IamRole>> listRolesByStaffIds(Collection<Long> staffIds) {
        return staffRepository.listRolesByStaffIds(staffIds);
    }

    public IamDept findDept(Long deptId) {
        return deptId == null ? null : deptRepository.findById(deptId);
    }

    public Map<Long, IamDept> findDepts(Collection<Long> deptIds) {
        return deptRepository.findByIds(deptIds);
    }

    public void assertInDataScope(Long targetStaffId, PermissionSnapshot snapshot) {
        if (snapshot == null
                || snapshot.isSuperAdmin()
                || DataScopeType.ALL
                        .getCode()
                        .equals(snapshot.getDataScopeSummary().getEffectiveType())) {
            return;
        }
        if (snapshot.getStaffId().equals(targetStaffId)
                && snapshot.getDataScopeSummary().isIncludeSelf()) {
            return;
        }
        IamStaff target = requireById(targetStaffId);
        if (target.getDeptId() != null
                && snapshot.getDataScopeSummary().getDeptIds().contains(target.getDeptId())) {
            return;
        }
        throw new BizException(IamErrorCode.STAFF_OUT_OF_DATA_SCOPE);
    }

    private void assertStaffCodeAvailable(String code, Long excludeId) {
        if (staffRepository.staffCodeExists(code, excludeId)) {
            throw new BizException(IamErrorCode.STAFF_CODE_DUPLICATED);
        }
    }

    private void requireAssignableDept(Long deptId) {
        IamDept dept = deptRepository.findById(deptId);
        if (dept == null) {
            throw new BizException(IamErrorCode.DEPT_NOT_FOUND);
        }
        if (dept.getStatus() == IamStatus.DISABLED) {
            throw new BizException(IamErrorCode.DEPT_DISABLED);
        }
    }

    private void assertSuperAdminRoleNotRequested(List<Long> ids, Long roleId) {
        if (roleId != null && ids != null && ids.contains(roleId)) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_PROTECTED);
        }
    }

    private void assertCanRemoveSuperAdminCapability(Long staffId) {
        Long roleId = staffRepository.superAdminRoleId();
        if (staffRepository.hasRole(staffId, roleId)
                && staffRepository.countOtherEnabledStaffWithRole(staffId, roleId) == 0) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_REQUIRED);
        }
    }
}
