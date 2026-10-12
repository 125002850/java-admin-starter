package com.oigit.admin.queryscheme.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.queryscheme.infra.persistence.entity.QuerySchemeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface QuerySchemeMapper extends BaseMapper<QuerySchemeEntity> {
    @Update("update sys_user_query_scheme set deleted = id, version = version + 1, "
            + "update_time = current_timestamp, update_by = #{userId} "
            + "where id = #{id} and user_id = #{userId} and version = #{version} and deleted = 0")
    int deleteOwned(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version);

    @Insert("insert into sys_user_query_scheme_lock (user_id, scene_code) values (#{userId}, #{sceneCode}) "
            + "on duplicate key update user_id = values(user_id)")
    void ensureSceneLock(@Param("userId") Long userId, @Param("sceneCode") String sceneCode);

    @Select("select user_id from sys_user_query_scheme_lock "
            + "where user_id = #{userId} and scene_code = #{sceneCode} for update")
    Long lockScene(@Param("userId") Long userId, @Param("sceneCode") String sceneCode);
}
