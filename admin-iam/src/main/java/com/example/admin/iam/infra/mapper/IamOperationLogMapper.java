package com.example.admin.iam.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.iam.infra.entity.IamOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamOperationLogMapper extends BaseMapper<IamOperationLogEntity> {
}
