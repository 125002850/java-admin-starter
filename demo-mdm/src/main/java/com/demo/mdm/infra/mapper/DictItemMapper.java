package com.demo.mdm.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.mdm.infra.entity.DictItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictItemMapper extends BaseMapper<DictItemEntity> {
}
