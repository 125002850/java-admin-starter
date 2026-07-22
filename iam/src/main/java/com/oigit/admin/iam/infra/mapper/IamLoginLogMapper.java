package com.oigit.admin.iam.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.iam.infra.entity.IamLoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamLoginLogMapper extends BaseMapper<IamLoginLogEntity> {
}
