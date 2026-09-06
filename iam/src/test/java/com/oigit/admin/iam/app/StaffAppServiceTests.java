package com.oigit.admin.iam.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.service.IamStaffService;
import com.oigit.admin.iam.dto.rsp.StaffRspDTO;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class StaffAppServiceTests {

    @Test
    void pageResultShouldLoadDepartmentsAndRolesInBatches() {
        IamStaffService staffService = mock(IamStaffService.class);
        StaffAppService appService = new StaffAppService(staffService, null, null, null, null);
        IamStaff first = staff(11L, 101L, "张三");
        IamStaff second = staff(12L, 102L, "李四");
        PageSlice<IamStaff> page = new PageSlice<>(List.of(first, second), 2);

        IamDept firstDept = dept(101L, "研发部");
        IamDept secondDept = dept(102L, "产品部");
        IamRole role = role(201L, "管理员");
        when(staffService.findDepts(anyCollection()))
                .thenReturn(Map.of(101L, firstDept, 102L, secondDept));
        when(staffService.listRolesByStaffIds(anyCollection()))
                .thenReturn(Map.of(11L, List.of(role)));

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

    private IamStaff staff(Long id, Long deptId, String staffName) {
        IamStaff entity = new IamStaff();
        entity.setId(id);
        entity.setDeptId(deptId);
        entity.setStaffName(staffName);
        return entity;
    }

    private IamDept dept(Long id, String deptName) {
        IamDept entity = new IamDept();
        entity.setId(id);
        entity.setDeptName(deptName);
        return entity;
    }

    private IamRole role(Long id, String roleName) {
        IamRole entity = new IamRole();
        entity.setId(id);
        entity.setRoleName(roleName);
        return entity;
    }
}
