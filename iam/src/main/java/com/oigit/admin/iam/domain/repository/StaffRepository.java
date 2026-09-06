package com.oigit.admin.iam.domain.repository;

import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.query.StaffQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface StaffRepository {
    IamStaff findById(Long staffId);

    IamStaff findByUsername(String username);

    void save(IamStaff staff);

    void delete(Long staffId);

    boolean usernameExists(String username, Long excludeId);

    boolean staffCodeExists(String staffCode, Long excludeId);

    PageSlice<IamStaff> page(
            StaffQuery query, PermissionSnapshot snapshot, Collection<Long> deptIds);

    List<IamRole> listRoles(Long staffId);

    Map<Long, List<IamRole>> listRolesByStaffIds(Collection<Long> staffIds);

    void replaceRoles(Long staffId, Collection<Long> roleIds);

    Long superAdminRoleId();

    boolean hasRole(Long staffId, Long roleId);

    long countOtherEnabledStaffWithRole(Long staffId, Long roleId);

    boolean hasStaffInDept(Long deptId);
}
