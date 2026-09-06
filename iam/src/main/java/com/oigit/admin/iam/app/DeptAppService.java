package com.oigit.admin.iam.app;

import com.oigit.admin.iam.annotation.OperationLog;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.service.IamDeptService;
import com.oigit.admin.iam.dto.req.DeptCreateReqDTO;
import com.oigit.admin.iam.dto.req.DeptStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.req.DeptTreeReqDTO;
import com.oigit.admin.iam.dto.req.DeptUpdateReqDTO;
import com.oigit.admin.iam.dto.rsp.DeptRspDTO;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeptAppService {

    private final IamDeptService deptService;

    public DeptAppService(IamDeptService deptService) {
        this.deptService = deptService;
    }

    @Transactional(readOnly = true)
    public List<DeptRspDTO> tree(DeptTreeReqDTO reqDTO) {
        List<IamDept> depts = deptService.listAll(reqDTO == null ? null : reqDTO.keyword);
        return buildTree(depts);
    }

    @Transactional(readOnly = true)
    public DeptRspDTO detail(Long deptId) {
        IamDept entity = deptService.requireById(deptId);
        return toRsp(entity);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_DEPT, action = OperationLogAction.CREATE)
    public void create(DeptCreateReqDTO reqDTO) {
        deptService.create(IamRequestMapper.toDept(reqDTO));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_DEPT, action = OperationLogAction.UPDATE)
    public void update(DeptUpdateReqDTO reqDTO) {
        deptService.update(IamRequestMapper.toDept(reqDTO));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_DEPT, action = OperationLogAction.DELETE)
    public void delete(Long deptId) {
        deptService.delete(deptId);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_DEPT, action = OperationLogAction.STATUS_UPDATE)
    public void updateStatus(DeptStatusUpdateReqDTO reqDTO) {
        deptService.updateStatus(reqDTO.deptId, reqDTO.status);
    }

    private List<DeptRspDTO> buildTree(List<IamDept> depts) {
        Map<Long, DeptRspDTO> byId = new LinkedHashMap<>();
        depts.stream()
                .sorted(
                        Comparator.comparing(
                                        (IamDept item) ->
                                                item.getSortOrder() == null
                                                        ? 0
                                                        : item.getSortOrder())
                                .thenComparing(IamDept::getId))
                .forEach(dept -> byId.put(dept.getId(), toRsp(dept)));
        List<DeptRspDTO> roots = new ArrayList<>();
        for (IamDept dept : depts) {
            DeptRspDTO node = byId.get(dept.getId());
            if (dept.getParentId() != null && byId.containsKey(dept.getParentId())) {
                byId.get(dept.getParentId()).children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private DeptRspDTO toRsp(IamDept entity) {
        DeptRspDTO dto = new DeptRspDTO();
        dto.deptId = entity.getId();
        dto.parentId = entity.getParentId();
        dto.deptCode = entity.getDeptCode();
        dto.deptName = entity.getDeptName();
        dto.fullPath = entity.getFullPath();
        dto.sortOrder = entity.getSortOrder();
        dto.status = entity.getStatus();
        dto.remark = entity.getRemark();
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateById(entity.getCreateBy());
        dto.setUpdateById(entity.getUpdateBy());
        return dto;
    }
}
