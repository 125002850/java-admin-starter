package com.oigit.admin.iam.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.iam.infra.persistence.entity.IamRefreshTokenEntity;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamRefreshTokenMapper extends BaseMapper<IamRefreshTokenEntity> {}
