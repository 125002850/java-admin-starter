package com.oigit.admin.schedule.infra.query;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;

@Component
public class ScheduleJobSceneQueryDefinition implements SceneQueryDefinition<ScheduleJobEntity> {

    @Override
    public String sceneCode() {
        return "system.schedule.job.page";
    }

    @Override
    public Map<String, SFunction<ScheduleJobEntity, String>> textFields() {
        return Map.of(
                "jobName", ScheduleJobEntity::getJobName,
                "jobCode", ScheduleJobEntity::getJobCode,
                "invokeRoute", ScheduleJobEntity::getInvokeRoute,
                "groupName", ScheduleJobEntity::getGroupName);
    }

    @Override
    public Map<String, SFunction<ScheduleJobEntity, LocalDateTime>> dateTimeFields() {
        return Map.of(
                "createTime", ScheduleJobEntity::getCreateTime,
                "updateTime", ScheduleJobEntity::getUpdateTime);
    }

    @Override
    public Map<String, SFunction<ScheduleJobEntity, ?>> enumFields() {
        return Map.of("status", ScheduleJobEntity::getStatus);
    }

    @Override
    public Map<String, SFunction<ScheduleJobEntity, ?>> sortFields() {
        return Map.of(
                "id", ScheduleJobEntity::getId,
                "jobName", ScheduleJobEntity::getJobName,
                "jobCode", ScheduleJobEntity::getJobCode,
                "groupName", ScheduleJobEntity::getGroupName,
                "cronExpression", ScheduleJobEntity::getCronExpression,
                "invokeRoute", ScheduleJobEntity::getInvokeRoute,
                "remark", ScheduleJobEntity::getRemark,
                "createTime", ScheduleJobEntity::getCreateTime,
                "updateTime", ScheduleJobEntity::getUpdateTime);
    }

    @Override
    public Map<String, String> fieldLabels() {
        return Map.of(
                "id", "ID",
                "jobName", "任务名称",
                "jobCode", "任务编码",
                "invokeRoute", "调用路由",
                "groupName", "任务分组",
                "cronExpression", "Cron 表达式",
                "remark", "备注",
                "status", "启用状态",
                "createTime", "创建时间",
                "updateTime", "更新时间");
    }

    @Override
    public Set<QueryOperator> allowedOperators(String fieldKey) {
        return switch (fieldKey) {
            case "jobName", "jobCode", "invokeRoute", "groupName" -> Set.of(
                    QueryOperator.EQ,
                    QueryOperator.CONTAINS,
                    QueryOperator.STARTS_WITH,
                    QueryOperator.ENDS_WITH,
                    QueryOperator.IN,
                    QueryOperator.IS_NULL,
                    QueryOperator.IS_NOT_NULL);
            case "status" -> Set.of(
                    QueryOperator.EQ,
                    QueryOperator.IN);
            case "createTime", "updateTime" -> Set.of(
                    QueryOperator.GT,
                    QueryOperator.GTE,
                    QueryOperator.LT,
                    QueryOperator.LTE,
                    QueryOperator.BETWEEN,
                    QueryOperator.IS_NULL,
                    QueryOperator.IS_NOT_NULL);
            default -> Set.of();
        };
    }

}
