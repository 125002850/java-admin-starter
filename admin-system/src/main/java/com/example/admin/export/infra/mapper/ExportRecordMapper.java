package com.example.admin.export.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.export.infra.entity.ExportRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExportRecordMapper extends BaseMapper<ExportRecordEntity> {
}
