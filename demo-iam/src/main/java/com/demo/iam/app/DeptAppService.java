package com.demo.iam.app;

import com.demo.iam.annotation.OperationLog;
import com.demo.iam.dto.IamDeptDTO.DeptCreateReqDTO;
import com.demo.iam.dto.IamDeptDTO.DeptRspDTO;
import com.demo.iam.dto.IamDeptDTO.DeptStatusUpdateReqDTO;
import com.demo.iam.dto.IamDeptDTO.DeptTreeReqDTO;
import com.demo.iam.dto.IamDeptDTO.DeptUpdateReqDTO;
import com.demo.iam.enums.OperationLogAction;
import com.demo.iam.enums.OperationLogModule;
import com.demo.iam.infra.entity.IamDeptEntity;
import com.demo.iam.service.IamDeptService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeptAppService {

    private final IamDeptService deptService;

    public DeptAppService(IamDeptService deptService) {
        this.deptService = deptService;
    }

    @Transactional(readOnly = true)
    public List<DeptRspDTO> tree(DeptTreeReqDTO reqDTO) {
        return buildTree(deptService.listAll(reqDTO == null ? null : reqDTO.keyword));
    }

    @Transactional(readOnly = true)
    public DeptRspDTO detail(Long deptId) {
        return toRsp(deptService.requireById(deptId));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_DEPT, action = OperationLogAction.CREATE)
    public void create(DeptCreateReqDTO reqDTO) {
        deptService.create(reqDTO);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_DEPT, action = OperationLogAction.UPDATE)
    public void update(DeptUpdateReqDTO reqDTO) {
        deptService.update(reqDTO);
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

    private List<DeptRspDTO> buildTree(List<IamDeptEntity> depts) {
        Map<Long, DeptRspDTO> byId = new LinkedHashMap<>();
        depts.stream()
                .sorted(Comparator.comparing((IamDeptEntity item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(IamDeptEntity::getId))
                .forEach(dept -> byId.put(dept.getId(), toRsp(dept)));
        List<DeptRspDTO> roots = new ArrayList<>();
        for (IamDeptEntity dept : depts) {
            DeptRspDTO node = byId.get(dept.getId());
            if (dept.getParentId() != null && byId.containsKey(dept.getParentId())) {
                byId.get(dept.getParentId()).children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private DeptRspDTO toRsp(IamDeptEntity entity) {
        DeptRspDTO dto = new DeptRspDTO();
        dto.deptId = entity.getId();
        dto.parentId = entity.getParentId();
        dto.deptCode = entity.getDeptCode();
        dto.deptName = entity.getDeptName();
        dto.fullPath = entity.getFullPath();
        dto.sortOrder = entity.getSortOrder();
        dto.status = entity.getStatus() == null ? null : entity.getStatus().getCode();
        dto.remark = entity.getRemark();
        dto.createTime = entity.getCreateTime();
        dto.updateTime = entity.getUpdateTime();
        dto.createBy = entity.getCreateBy();
        dto.updateBy = entity.getUpdateBy();
        return dto;
    }
}
