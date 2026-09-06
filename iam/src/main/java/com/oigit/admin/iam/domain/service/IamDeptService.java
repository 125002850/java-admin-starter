package com.oigit.admin.iam.domain.service;

import static com.oigit.admin.iam.domain.service.IamDomainRules.hasText;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.IamStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IamDeptService {
    private final DeptRepository deptRepository;
    private final StaffRepository staffRepository;

    public IamDeptService(DeptRepository deptRepository, StaffRepository staffRepository) {
        this.deptRepository = deptRepository;
        this.staffRepository = staffRepository;
    }

    public IamDept requireById(Long deptId) {
        IamDept dept = deptRepository.findById(deptId);
        if (dept == null) {
            throw new BizException(IamErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    public List<IamDept> listAll(String keyword) {
        return deptRepository.listAll(keyword);
    }

    public IamDept create(IamDept dept) {
        validateParent(null, dept.getParentId());
        assertUnique(dept, null);
        normalize(dept);
        dept.setFullPath(resolveFullPath(dept.getParentId(), dept.getDeptName()));
        deptRepository.save(dept);
        return dept;
    }

    public void update(IamDept changes) {
        IamDept dept = requireById(changes.getId());
        validateParent(dept.getId(), changes.getParentId());
        assertUnique(changes, dept.getId());
        normalize(changes);
        dept.setParentId(changes.getParentId());
        dept.setDeptCode(changes.getDeptCode());
        dept.setDeptName(changes.getDeptName());
        dept.setSortOrder(changes.getSortOrder());
        dept.setStatus(changes.getStatus());
        dept.setRemark(changes.getRemark());
        dept.setFullPath(resolveFullPath(dept.getParentId(), dept.getDeptName()));
        deptRepository.save(dept);
        Map<Long, List<IamDept>> children =
                deptRepository.listAll(null).stream()
                        .filter(item -> item.getParentId() != null)
                        .collect(Collectors.groupingBy(IamDept::getParentId));
        refreshChildrenFullPath(dept, children, new LinkedHashSet<>());
    }

    public void updateStatus(Long deptId, IamStatus status) {
        IamDept dept = requireById(deptId);
        dept.setStatus(status);
        deptRepository.save(dept);
    }

    public void delete(Long deptId) {
        requireById(deptId);
        if (deptRepository.hasChildren(deptId)) {
            throw new BizException(IamErrorCode.DEPT_HAS_CHILDREN);
        }
        if (staffRepository.hasStaffInDept(deptId)) {
            throw new BizException(IamErrorCode.DEPT_HAS_STAFF);
        }
        deptRepository.delete(deptId);
    }

    private void assertUnique(IamDept dept, Long excludeId) {
        if (deptRepository.codeExists(dept.getParentId(), dept.getDeptCode(), excludeId)) {
            throw new BizException(IamErrorCode.DEPT_CODE_DUPLICATED);
        }
        if (deptRepository.nameExists(dept.getParentId(), dept.getDeptName(), excludeId)) {
            throw new BizException(IamErrorCode.DEPT_NAME_DUPLICATED);
        }
    }

    private void validateParent(Long deptId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(deptId)) {
            throw new BizException(IamErrorCode.DEPT_PARENT_INVALID);
        }
        IamDept parent = requireById(parentId);
        if (parent.getStatus() != IamStatus.ENABLED) {
            throw new BizException(IamErrorCode.DEPT_DISABLED);
        }
        if (deptId == null) {
            return;
        }
        Map<Long, IamDept> all =
                deptRepository.listAll(null).stream()
                        .collect(Collectors.toMap(IamDept::getId, item -> item));
        Set<Long> visited = new LinkedHashSet<>();
        Long current = parent.getParentId();
        while (current != null) {
            if (current.equals(deptId) || !visited.add(current)) {
                throw new BizException(IamErrorCode.DEPT_PARENT_INVALID);
            }
            IamDept ancestor = all.get(current);
            if (ancestor == null) {
                throw new BizException(IamErrorCode.DEPT_NOT_FOUND);
            }
            current = ancestor.getParentId();
        }
    }

    private void refreshChildrenFullPath(
            IamDept parent, Map<Long, List<IamDept>> children, Set<Long> visited) {
        if (parent.getId() == null || !visited.add(parent.getId())) {
            return;
        }
        String parentPath =
                hasText(parent.getFullPath()) ? parent.getFullPath() : parent.getDeptName();
        for (IamDept child : children.getOrDefault(parent.getId(), List.of())) {
            child.setFullPath(parentPath + "/" + child.getDeptName());
            deptRepository.save(child);
            refreshChildrenFullPath(child, children, visited);
        }
    }

    private void normalize(IamDept dept) {
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(IamStatus.ENABLED);
        }
    }

    private String resolveFullPath(Long parentId, String name) {
        if (parentId == null) {
            return name;
        }
        IamDept parent = requireById(parentId);
        return (hasText(parent.getFullPath()) ? parent.getFullPath() : parent.getDeptName())
                + "/"
                + name;
    }
}
