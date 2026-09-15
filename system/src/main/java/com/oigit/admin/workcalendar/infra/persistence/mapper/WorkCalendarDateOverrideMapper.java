package com.oigit.admin.workcalendar.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarDateOverrideEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkCalendarDateOverrideMapper extends BaseMapper<WorkCalendarDateOverrideEntity> {

    int physicalDeleteByVersionId(@Param("calendarVersionId") Long calendarVersionId);
}
