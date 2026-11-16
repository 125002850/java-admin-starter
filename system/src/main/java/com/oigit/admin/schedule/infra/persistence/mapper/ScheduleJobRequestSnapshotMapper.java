package com.oigit.admin.schedule.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobRequestSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduleJobRequestSnapshotMapper extends BaseMapper<ScheduleJobRequestSnapshotEntity> {
    void replaceLatest(@Param("jobId") Long jobId, @Param("executionId") Long executionId,
            @Param("snapshotJson") String snapshotJson);
}
