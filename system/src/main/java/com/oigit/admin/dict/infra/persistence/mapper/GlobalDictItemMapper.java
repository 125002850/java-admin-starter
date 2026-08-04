package com.oigit.admin.dict.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GlobalDictItemMapper extends BaseMapper<GlobalDictItemEntity> {
}
