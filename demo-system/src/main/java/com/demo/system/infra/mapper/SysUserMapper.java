package com.demo.system.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.system.infra.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}
