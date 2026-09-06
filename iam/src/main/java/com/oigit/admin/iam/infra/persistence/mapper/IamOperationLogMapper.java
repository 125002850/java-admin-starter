package com.oigit.admin.iam.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.iam.infra.persistence.entity.IamOperationLogEntity;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamOperationLogMapper extends BaseMapper<IamOperationLogEntity> {}
