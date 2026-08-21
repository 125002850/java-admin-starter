package com.oigit.admin.iam.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.dto.IamStaffDTO.StaffRspDTO;
import com.oigit.admin.iam.infra.entity.IamDeptEntity;
import com.oigit.admin.iam.infra.entity.IamRoleEntity;
import com.oigit.admin.iam.infra.entity.IamStaffEntity;
import com.oigit.admin.iam.service.IamStaffService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StaffAppServiceTests {

    @Test
    void pageResultShouldLoadDepartmentsAndRolesInBatches() {
        IamStaffService staffService = mock(IamStaffService.class);
        StaffAppService appService = new StaffAppService(staffService, null, null, null);
        IamStaffEntity first = staff(11L, 101L, "张三");
        IamStaffEntity second = staff(12L, 102L, "李四");
        Page<IamStaffEntity> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(first, second));

        IamDeptEntity firstDept = dept(101L, "研发部");
        IamDeptEntity secondDept = dept(102L, "产品部");
        IamRoleEntity role = role(201L, "管理员");
        when(staffService.findDepts(anyCollection())).thenReturn(Map.of(101L, firstDept, 102L, secondDept));
        when(staffService.listRolesByStaffIds(anyCollection())).thenReturn(Map.of(11L, List.of(role)));

        PageResult<StaffRspDTO> result = appService.assemblePageResult(page);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList())
                .extracting(StaffRspDTO::getDeptName)
                .containsExactly("研发部", "产品部");
        assertThat(result.getList().get(0).getRoles())
                .extracting(roleSummary -> roleSummary.getRoleName())
                .containsExactly("管理员");
        assertThat(result.getList().get(1).getRoles()).isEmpty();
        verify(staffService).findDepts(anyCollection());
        verify(staffService).listRolesByStaffIds(anyCollection());
        verify(staffService, never()).findDept(101L);
        verify(staffService, never()).listRoles(11L);
    }

    private IamStaffEntity staff(Long id, Long deptId, String staffName) {
        IamStaffEntity entity = new IamStaffEntity();
        entity.setId(id);
        entity.setDeptId(deptId);
        entity.setStaffName(staffName);
        return entity;
    }

    private IamDeptEntity dept(Long id, String deptName) {
        IamDeptEntity entity = new IamDeptEntity();
        entity.setId(id);
        entity.setDeptName(deptName);
        return entity;
    }

    private IamRoleEntity role(Long id, String roleName) {
        IamRoleEntity entity = new IamRoleEntity();
        entity.setId(id);
        entity.setRoleName(roleName);
        return entity;
    }
}
