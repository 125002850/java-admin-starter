package com.oigit.admin.iam.domain.repository;

import com.oigit.admin.iam.domain.model.IamDept;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DeptRepository {
    IamDept findById(Long deptId);

    List<IamDept> listAll(String keyword);

    Map<Long, IamDept> findByIds(Collection<Long> deptIds);

    void save(IamDept dept);

    void delete(Long deptId);

    boolean codeExists(Long parentId, String deptCode, Long excludeId);

    boolean nameExists(Long parentId, String deptName, Long excludeId);

    boolean hasChildren(Long deptId);
}
