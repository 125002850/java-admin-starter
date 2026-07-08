package com.demo.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.exception.BizException;
import com.demo.core.operator.OperatorContext;
import com.demo.iam.dto.IamDeptDTO.DeptCreateReqDTO;
import com.demo.iam.dto.IamDeptDTO.DeptUpdateReqDTO;
import com.demo.iam.enums.IamErrorCode;
import com.demo.iam.enums.IamStatus;
import com.demo.iam.infra.entity.IamDeptEntity;
import com.demo.iam.infra.entity.IamStaffEntity;
import com.demo.iam.infra.mapper.IamDeptMapper;
import com.demo.iam.infra.mapper.IamStaffMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IamDeptService {

    private final IamDeptMapper deptMapper;
    private final IamStaffMapper staffMapper;

    public IamDeptService(IamDeptMapper deptMapper, IamStaffMapper staffMapper) {
        this.deptMapper = deptMapper;
        this.staffMapper = staffMapper;
    }

    public IamDeptEntity requireById(Long deptId) {
        IamDeptEntity dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BizException(IamErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    public List<IamDeptEntity> listAll(String keyword) {
        var query = Wrappers.<IamDeptEntity>lambdaQuery()
                .orderByAsc(IamDeptEntity::getSortOrder)
                .orderByAsc(IamDeptEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(IamDeptEntity::getDeptCode, keyword)
                    .or()
                    .like(IamDeptEntity::getDeptName, keyword));
        }
        return deptMapper.selectList(query);
    }

    public IamDeptEntity create(DeptCreateReqDTO reqDTO) {
        validateParent(null, reqDTO.parentId);
        assertUnique(reqDTO.parentId, reqDTO.deptCode, reqDTO.deptName, null);
        IamDeptEntity entity = new IamDeptEntity();
        fill(entity, reqDTO.parentId, reqDTO.deptCode, reqDTO.deptName, reqDTO.sortOrder, reqDTO.status, reqDTO.remark);
        entity.setFullPath(resolveFullPath(reqDTO.parentId, reqDTO.deptName));
        entity.setDeleted(0L);
        deptMapper.insert(entity);
        return entity;
    }

    public void update(DeptUpdateReqDTO reqDTO) {
        IamDeptEntity entity = requireById(reqDTO.deptId);
        validateParent(reqDTO.deptId, reqDTO.parentId);
        assertUnique(reqDTO.parentId, reqDTO.deptCode, reqDTO.deptName, reqDTO.deptId);
        fill(entity, reqDTO.parentId, reqDTO.deptCode, reqDTO.deptName, reqDTO.sortOrder, reqDTO.status, reqDTO.remark);
        entity.setFullPath(resolveFullPath(reqDTO.parentId, reqDTO.deptName));
        deptMapper.updateById(entity);
        refreshChildrenFullPath(entity, new LinkedHashSet<>());
    }

    public void updateStatus(Long deptId, IamStatus status) {
        IamDeptEntity entity = requireById(deptId);
        entity.setStatus(status);
        deptMapper.updateById(entity);
    }

    public void delete(Long deptId) {
        requireById(deptId);
        Long childCount = deptMapper.selectCount(
                Wrappers.<IamDeptEntity>lambdaQuery().eq(IamDeptEntity::getParentId, deptId)
        );
        if (childCount != null && childCount > 0L) {
            throw new BizException(IamErrorCode.DEPT_HAS_CHILDREN);
        }
        Long staffCount = staffMapper.selectCount(
                Wrappers.<IamStaffEntity>lambdaQuery().eq(IamStaffEntity::getDeptId, deptId)
        );
        if (staffCount != null && staffCount > 0L) {
            throw new BizException(IamErrorCode.DEPT_HAS_STAFF);
        }
        deptMapper.softDeleteById(deptId, operatorId());
    }

    private void assertUnique(Long parentId, String deptCode, String deptName, Long excludeId) {
        var codeQuery = sameParentQuery(parentId).eq(IamDeptEntity::getDeptCode, deptCode);
        var nameQuery = sameParentQuery(parentId).eq(IamDeptEntity::getDeptName, deptName);
        if (excludeId != null) {
            codeQuery.ne(IamDeptEntity::getId, excludeId);
            nameQuery.ne(IamDeptEntity::getId, excludeId);
        }
        Long codeCount = deptMapper.selectCount(codeQuery);
        if (codeCount != null && codeCount > 0L) {
            throw new BizException(IamErrorCode.DEPT_CODE_DUPLICATED);
        }
        Long nameCount = deptMapper.selectCount(nameQuery);
        if (nameCount != null && nameCount > 0L) {
            throw new BizException(IamErrorCode.DEPT_NAME_DUPLICATED);
        }
    }

    private LambdaQueryWrapper<IamDeptEntity> sameParentQuery(Long parentId) {
        LambdaQueryWrapper<IamDeptEntity> query = Wrappers.lambdaQuery();
        if (parentId == null) {
            query.isNull(IamDeptEntity::getParentId);
        } else {
            query.eq(IamDeptEntity::getParentId, parentId);
        }
        return query;
    }

    private void validateParent(Long deptId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(deptId)) {
            throw new BizException(IamErrorCode.DEPT_PARENT_INVALID);
        }
        IamDeptEntity parent = requireById(parentId);
        if (parent.getStatus() != IamStatus.ENABLED) {
            throw new BizException(IamErrorCode.DEPT_DISABLED);
        }
        if (deptId == null) {
            return;
        }
        Set<Long> visited = new LinkedHashSet<>();
        Long currentParentId = parent.getParentId();
        while (currentParentId != null) {
            if (currentParentId.equals(deptId) || !visited.add(currentParentId)) {
                throw new BizException(IamErrorCode.DEPT_PARENT_INVALID);
            }
            currentParentId = requireById(currentParentId).getParentId();
        }
    }

    private void refreshChildrenFullPath(IamDeptEntity parent, Set<Long> visited) {
        if (parent.getId() == null || !visited.add(parent.getId())) {
            return;
        }
        List<IamDeptEntity> children = deptMapper.selectList(
                Wrappers.<IamDeptEntity>lambdaQuery().eq(IamDeptEntity::getParentId, parent.getId())
        );
        String parentPath = StringUtils.hasText(parent.getFullPath()) ? parent.getFullPath() : parent.getDeptName();
        for (IamDeptEntity child : children) {
            child.setFullPath(parentPath + "/" + child.getDeptName());
            deptMapper.updateById(child);
            refreshChildrenFullPath(child, visited);
        }
    }

    private void fill(IamDeptEntity entity, Long parentId, String deptCode, String deptName, Integer sortOrder, IamStatus status, String remark) {
        entity.setParentId(parentId);
        entity.setDeptCode(deptCode);
        entity.setDeptName(deptName);
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setStatus(status == null ? IamStatus.ENABLED : status);
        entity.setRemark(remark);
    }

    private String resolveFullPath(Long parentId, String deptName) {
        if (parentId == null) {
            return deptName;
        }
        IamDeptEntity parent = requireById(parentId);
        return (StringUtils.hasText(parent.getFullPath()) ? parent.getFullPath() : parent.getDeptName()) + "/" + deptName;
    }

    private Long operatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? 0L : operatorId;
    }
}
