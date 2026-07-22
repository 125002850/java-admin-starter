package com.oigit.admin.iam.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.iam.infra.entity.IamMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IamMenuMapper extends BaseMapper<IamMenuEntity> {

    @Update("update sys_menu set deleted = id, update_time = current_timestamp, update_by = #{updateBy} where id = #{id} and deleted = 0")
    int softDeleteById(@Param("id") Long id, @Param("updateBy") Long updateBy);
}
