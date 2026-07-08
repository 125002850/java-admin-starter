package com.demo.iam.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.iam.infra.entity.IamDeptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IamDeptMapper extends BaseMapper<IamDeptEntity> {

    @Update("update sys_dept set deleted = id, update_time = current_timestamp, update_by = #{updateBy} where id = #{id} and deleted = 0")
    int softDeleteById(@Param("id") Long id, @Param("updateBy") Long updateBy);
}
