package com.oigit.admin.workcalendar.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarYearEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkCalendarYearMapper extends BaseMapper<WorkCalendarYearEntity> {

    int insertIgnore(@Param("calendarYear") Integer calendarYear, @Param("operatorId") Long operatorId);

    WorkCalendarYearEntity selectByCalendarYearForUpdate(@Param("calendarYear") Integer calendarYear);

    int activatePublishedVersion(
            @Param("yearId") Long yearId,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("draftVersionId") Long draftVersionId,
            @Param("operatorId") Long operatorId
    );
}
