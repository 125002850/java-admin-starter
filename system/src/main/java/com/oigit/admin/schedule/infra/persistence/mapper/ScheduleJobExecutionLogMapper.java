package com.oigit.admin.schedule.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobExecutionLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduleJobExecutionLogMapper extends BaseMapper<ScheduleJobExecutionLogEntity> {
    java.util.List<ScheduleJobExecutionLogEntity> selectLatestByJobIds(
            @org.apache.ibatis.annotations.Param("jobIds") java.util.List<Long> jobIds);
}
