package com.oigit.admin.operationaudit.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.operationaudit.infra.persistence.entity.OperationAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationAuditLogMapper extends BaseMapper<OperationAuditLogEntity> {
}
