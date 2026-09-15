package com.oigit.admin.schedule.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobExecutionLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduleJobExecutionLogMapper extends BaseMapper<ScheduleJobExecutionLogEntity> {
}
