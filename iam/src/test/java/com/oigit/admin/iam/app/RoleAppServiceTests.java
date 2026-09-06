package com.oigit.admin.iam.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.service.IamRoleService;
import com.oigit.admin.iam.dto.req.RolePageReqDTO;
import com.oigit.admin.iam.dto.rsp.RoleRspDTO;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class RoleAppServiceTests {

    @Test
    void rolePageLoadsMenuAndDepartmentAssignmentsInBatches() {
        IamRoleService roles = mock(IamRoleService.class);
        IamRole first = new IamRole();
        first.setId(1L);
        IamRole second = new IamRole();
        second.setId(2L);
        when(roles.page(any())).thenReturn(new PageSlice<>(List.of(first, second), 2));
        when(roles.listMenuIdsByRoleIds(List.of(1L, 2L))).thenReturn(Map.of(1L, List.of(100L)));
        when(roles.listDataScopeDeptIdsByRoleIds(List.of(1L, 2L)))
                .thenReturn(Map.of(2L, List.of(200L)));

        PageResult<RoleRspDTO> result = new RoleAppService(roles).page(new RolePageReqDTO());

        assertThat(result.getList().get(0).menuIds).containsExactly(100L);
        assertThat(result.getList().get(1).dataScopeDeptIds).containsExactly(200L);
        verify(roles).listMenuIdsByRoleIds(List.of(1L, 2L));
        verify(roles).listDataScopeDeptIdsByRoleIds(List.of(1L, 2L));
        verify(roles, never()).listMenuIds(any());
        verify(roles, never()).listDataScopeDeptIds(any());
    }
}
