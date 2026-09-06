package com.oigit.admin.iam.infra.config;

import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import com.oigit.admin.iam.domain.service.IamDeptService;
import com.oigit.admin.iam.domain.service.IamMenuService;
import com.oigit.admin.iam.domain.service.IamRoleService;
import com.oigit.admin.iam.domain.service.IamStaffService;
import com.oigit.admin.iam.domain.service.PasswordPolicyService;
import com.oigit.admin.iam.domain.service.PermissionSnapshotService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IamDomainConfiguration {
    @Bean
    public IamStaffService iamStaffService(
            StaffRepository staff, DeptRepository dept, RoleRepository role) {
        return new IamStaffService(staff, dept, role);
    }

    @Bean
    public IamDeptService iamDeptService(DeptRepository dept, StaffRepository staff) {
        return new IamDeptService(dept, staff);
    }

    @Bean
    public IamRoleService iamRoleService(
            RoleRepository role, MenuRepository menu, DeptRepository dept) {
        return new IamRoleService(role, menu, dept);
    }

    @Bean
    public IamMenuService iamMenuService(MenuRepository menu) {
        return new IamMenuService(menu);
    }

    @Bean
    public PermissionSnapshotService permissionSnapshotService(
            StaffRepository staff, DeptRepository dept, RoleRepository role, MenuRepository menu) {
        return new PermissionSnapshotService(staff, dept, role, menu);
    }

    @Bean
    public PasswordPolicyService passwordPolicyService() {
        return new PasswordPolicyService();
    }
}
