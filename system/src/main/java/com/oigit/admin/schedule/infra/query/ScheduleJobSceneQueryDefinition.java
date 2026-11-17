package com.oigit.admin.schedule.infra.query;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.scene.QueryFieldSpec;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;

@Component
public class ScheduleJobSceneQueryDefinition implements SceneQueryDefinition<ScheduleJobEntity> {

    @Override
    public String sceneCode() {
        return "system.schedule.job.page";
    }

    @Override
    public Map<String, QueryFieldSpec<ScheduleJobEntity>> fields() {
        return Map.ofEntries(
                Map.entry("jobName", QueryFieldSpec.text(ScheduleJobEntity::getJobName, "任务名称").sortable()),
                Map.entry("jobCode", QueryFieldSpec.text(ScheduleJobEntity::getJobCode, "任务编码").sortable()),
                Map.entry("invokeRoute", QueryFieldSpec.text(ScheduleJobEntity::getInvokeRoute, "调用路由").sortable()),
                Map.entry("groupName", QueryFieldSpec.text(ScheduleJobEntity::getGroupName, "任务分组").sortable()),
                Map.entry("createTime", QueryFieldSpec.dateTime(ScheduleJobEntity::getCreateTime, "创建时间").sortable()),
                Map.entry("updateTime", QueryFieldSpec.dateTime(ScheduleJobEntity::getUpdateTime, "更新时间").sortable()),
                Map.entry("status", QueryFieldSpec.enumeration(ScheduleJobEntity::getStatus, "启用状态", QueryOperator.EQ, QueryOperator.NE, QueryOperator.IN, QueryOperator.NOT_IN).withValueLabels(java.util.Arrays.stream(com.oigit.admin.core.enums.EnableStatusEnum.values())
                .collect(java.util.stream.Collectors.toMap(com.oigit.admin.core.enums.EnableStatusEnum::getCode,
                        com.oigit.admin.core.enums.EnableStatusEnum::getDesc)))),
                Map.entry("id", QueryFieldSpec.sortOnly(ScheduleJobEntity::getId, "ID")),
                Map.entry("cronExpression", QueryFieldSpec.sortOnly(ScheduleJobEntity::getCronExpression, "Cron 表达式")),
                Map.entry("remark", QueryFieldSpec.sortOnly(ScheduleJobEntity::getRemark, "备注"))
        );
    }
}
