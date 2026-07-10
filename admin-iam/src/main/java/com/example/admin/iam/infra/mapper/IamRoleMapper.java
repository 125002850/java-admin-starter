package com.example.admin.iam.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.iam.infra.entity.IamRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IamRoleMapper extends BaseMapper<IamRoleEntity> {

    @Update("update sys_role set deleted = id, update_time = current_timestamp, update_by = #{updateBy} where id = #{id} and deleted = 0")
    int softDeleteById(@Param("id") Long id, @Param("updateBy") Long updateBy);
}
