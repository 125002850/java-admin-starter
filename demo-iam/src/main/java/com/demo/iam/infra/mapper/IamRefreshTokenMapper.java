package com.demo.iam.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.iam.infra.entity.IamRefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamRefreshTokenMapper extends BaseMapper<IamRefreshTokenEntity> {
}
