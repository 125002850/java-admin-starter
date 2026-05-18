package com.demo.system.infra.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.system.infra.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            select id, tenant_id, username, password, status, display_name, mobile, email, create_time, update_time, create_by, update_by, deleted
            from sys_user
            where tenant_id = #{tenantId}
              and username = #{username}
              and deleted = 0
            limit 2
            """)
    List<SysUserEntity> selectForLogin(@Param("tenantId") Long tenantId, @Param("username") String username);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            select count(*)
            from sys_user
            where tenant_id = #{tenantId}
              and deleted = 0
            """)
    long countByTenantId(@Param("tenantId") Long tenantId);
}
