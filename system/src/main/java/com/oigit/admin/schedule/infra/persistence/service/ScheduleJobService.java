package com.oigit.admin.schedule.infra.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.schedule.enums.ScheduleErrorCode;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobMapper;
import com.oigit.admin.schedule.infra.query.ScheduleJobSceneQueryDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleJobService {

    private final ScheduleJobMapper scheduleJobMapper;
    private final MybatisPlusQueryExecutor mybatisPlusQueryExecutor;

    public ScheduleJobService(
            ScheduleJobMapper scheduleJobMapper,
            MybatisPlusQueryExecutor mybatisPlusQueryExecutor) {
        this.scheduleJobMapper = scheduleJobMapper;
        this.mybatisPlusQueryExecutor = mybatisPlusQueryExecutor;
    }

    public ScheduleJobEntity getById(Long id) {
        ScheduleJobEntity entity = scheduleJobMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ScheduleErrorCode.JOB_NOT_FOUND);
        }
        return entity;
    }

    @Transactional
    public ScheduleJobEntity create(ScheduleJobEntity entity) {
        long count = scheduleJobMapper.selectCount(
                new LambdaQueryWrapper<ScheduleJobEntity>()
                        .eq(ScheduleJobEntity::getJobCode, entity.getJobCode())
        );
        if (count > 0) {
            throw new BizException(ScheduleErrorCode.JOB_CODE_DUPLICATE);
        }
        scheduleJobMapper.insert(entity);
        return entity;
    }

    @Transactional
    public ScheduleJobEntity update(ScheduleJobEntity entity) {
        ScheduleJobEntity existing = getById(entity.getId());
        long count = scheduleJobMapper.selectCount(
                new LambdaQueryWrapper<ScheduleJobEntity>()
                        .eq(ScheduleJobEntity::getJobCode, entity.getJobCode())
                        .ne(ScheduleJobEntity::getId, entity.getId())
        );
        if (count > 0) {
            throw new BizException(ScheduleErrorCode.JOB_CODE_DUPLICATE);
        }
        entity.setStatus(existing.getStatus());
        entity.setCreateTime(existing.getCreateTime());
        entity.setCreateBy(existing.getCreateBy());
        if (scheduleJobMapper.updateById(entity) != 1) {
            throw new BizException(ScheduleErrorCode.JOB_VERSION_CONFLICT);
        }
        return entity;
    }

    @Transactional
    public void delete(Long id) {
        ScheduleJobEntity entity = getById(id);
        scheduleJobMapper.deleteById(entity.getId());
    }

    @Transactional
    public ScheduleJobEntity enable(Long id) {
        ScheduleJobEntity entity = getById(id);
        entity.setStatus(EnableStatusEnum.ENABLE);
        if (scheduleJobMapper.updateById(entity) != 1) {
            throw new BizException(ScheduleErrorCode.JOB_VERSION_CONFLICT);
        }
        return entity;
    }

    @Transactional
    public ScheduleJobEntity disable(Long id) {
        ScheduleJobEntity entity = getById(id);
        entity.setStatus(EnableStatusEnum.DISABLE);
        if (scheduleJobMapper.updateById(entity) != 1) {
            throw new BizException(ScheduleErrorCode.JOB_VERSION_CONFLICT);
        }
        return entity;
    }

    public Page<ScheduleJobEntity> page(QueryAst queryAst, ScheduleJobSceneQueryDefinition queryDefinition) {
        return mybatisPlusQueryExecutor.selectPage(scheduleJobMapper, queryAst, queryDefinition);
    }
}
