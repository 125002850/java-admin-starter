package com.oigit.admin.workcalendar.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarVersionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkCalendarVersionMapper extends BaseMapper<WorkCalendarVersionEntity> {
}
