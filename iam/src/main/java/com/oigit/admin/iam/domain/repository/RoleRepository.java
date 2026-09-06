package com.oigit.admin.iam.domain.repository;

import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.query.RoleQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RoleRepository {
    IamRole findById(Long roleId);

    Map<Long, IamRole> findByIds(Collection<Long> roleIds);

    PageSlice<IamRole> page(RoleQuery query);

    void save(IamRole role);

    void delete(Long roleId);

    boolean codeExists(String roleCode, Long excludeId);

    boolean nameExists(String roleName, Long excludeId);

    boolean hasStaff(Long roleId);

    void replaceMenus(Long roleId, Collection<Long> menuIds);

    void replaceDataScopeDepts(Long roleId, Collection<Long> deptIds);

    Map<Long, List<Long>> listMenuIdsByRoleIds(Collection<Long> roleIds);

    Map<Long, List<Long>> listDataScopeDeptIdsByRoleIds(Collection<Long> roleIds);
}
