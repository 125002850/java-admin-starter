package com.oigit.admin.export.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.export.infra.persistence.entity.ExportRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExportRecordMapper extends BaseMapper<ExportRecordEntity> {
}
