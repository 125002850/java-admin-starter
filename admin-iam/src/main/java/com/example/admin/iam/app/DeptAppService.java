package com.example.admin.iam.app;

import com.example.admin.iam.annotation.OperationLog;
import com.example.admin.iam.dto.IamDeptDTO.DeptCreateReqDTO;
import com.example.admin.iam.dto.IamDeptDTO.DeptRspDTO;
import com.example.admin.iam.dto.IamDeptDTO.DeptStatusUpdateReqDTO;
import com.example.admin.iam.dto.IamDeptDTO.DeptTreeReqDTO;
import com.example.admin.iam.dto.IamDeptDTO.DeptUpdateReqDTO;
import com.example.admin.iam.enums.OperationLogAction;
import com.example.admin.iam.enums.OperationLogModule;
import com.example.admin.iam.infra.entity.IamDeptEntity;
import com.example.admin.iam.service.IamDeptService;
import com.example.admin.iam.service.IamStaffService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeptAppService {

    private final IamDeptService deptService;
    private final IamStaffService staffService;

    public DeptAppService(IamDeptService deptService, IamStaffService staffService) {
        this.deptService = deptService;
        this.staffService = staffService;
    }

    @Transactional(readOnly = true)
    public List<DeptRspDTO> tree(DeptTreeReqDTO reqDTO) {
        List<IamDeptEntity> depts = deptService.listAll(reqDTO == null ? null : reqDTO.keyword);
        return buildTree(depts, auditUsernames(depts));
    }

    @Transactional(readOnly = true)
    public DeptRspDTO detail(Long deptId) {
        IamDeptEntity entity = deptService.requireById(deptId);
        return toRsp(entity, auditUsernames(List.of(entity)));
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

    private List<DeptRspDTO> buildTree(List<IamDeptEntity> depts, Map<Long, String> usernames) {
        Map<Long, DeptRspDTO> byId = new LinkedHashMap<>();
        depts.stream()
                .sorted(Comparator.comparing((IamDeptEntity item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(IamDeptEntity::getId))
                .forEach(dept -> byId.put(dept.getId(), toRsp(dept, usernames)));
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

    private DeptRspDTO toRsp(IamDeptEntity entity, Map<Long, String> usernames) {
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
        dto.createBy = auditUsername(usernames, entity.getCreateBy());
        dto.updateBy = auditUsername(usernames, entity.getUpdateBy());
        return dto;
    }

    private Map<Long, String> auditUsernames(List<IamDeptEntity> depts) {
        List<Long> staffIds = depts.stream()
                .flatMap(dept -> Stream.of(dept.getCreateBy(), dept.getUpdateBy()))
                .toList();
        return staffService.resolveUsernames(staffIds);
    }

    private String auditUsername(Map<Long, String> usernames, Long staffId) {
        return staffId == null ? null : usernames.get(staffId);
    }
}
