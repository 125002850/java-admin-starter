package com.oigit.admin.dict.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictTypeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GlobalDictTypeMapper extends BaseMapper<GlobalDictTypeEntity> {
}
