package com.oigit.admin.core.operator;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CacheUserMapper extends BaseMapper<CacheUserEntity> {

    @Insert("INSERT INTO sys_user_cache (user_id, user_name, user_phone, real_name, user_code, create_time, update_time) "
            + "VALUES (#{userId}, #{userName}, #{userPhone}, #{realName}, #{userCode}, #{createTime}, #{updateTime}) "
            + "ON DUPLICATE KEY UPDATE user_name = COALESCE(#{userName}, user_name), "
            + "user_phone = COALESCE(#{userPhone}, user_phone), "
            + "real_name = COALESCE(#{realName}, real_name), "
            + "user_code = COALESCE(#{userCode}, user_code), update_time = #{updateTime}")
    int upsert(CacheUserEntity entity);
}
