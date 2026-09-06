package com.oigit.admin.iam.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.iam.domain.model.IamLoginLog;
import com.oigit.admin.iam.domain.model.IamOperationLog;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.query.LoginLogQuery;
import com.oigit.admin.iam.domain.query.OperationLogQuery;
import com.oigit.admin.iam.domain.repository.LogRepository;
import com.oigit.admin.iam.infra.persistence.entity.IamLoginLogEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamOperationLogEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamStaffEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamLoginLogMapper;
import com.oigit.admin.iam.infra.persistence.mapper.IamOperationLogMapper;
import com.oigit.admin.iam.infra.persistence.mapper.IamStaffMapper;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class MyBatisLogRepository implements LogRepository {
    private final IamLoginLogMapper loginLogMapper;
    private final IamOperationLogMapper operationLogMapper;
    private final IamStaffMapper staffMapper;

    public MyBatisLogRepository(
            IamLoginLogMapper loginLogMapper,
            IamOperationLogMapper operationLogMapper,
            IamStaffMapper staffMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.staffMapper = staffMapper;
    }

    public IamLoginLog findLoginLog(Long id) {
        return IamPersistenceConverter.toDomain(loginLogMapper.selectById(id));
    }

    public IamOperationLog findOperationLog(Long id) {
        return IamPersistenceConverter.toDomain(operationLogMapper.selectById(id));
    }

    public void saveLoginLog(IamLoginLog log) {
        loginLogMapper.insert(IamPersistenceConverter.toEntity(log));
    }

    public PageSlice<IamLoginLog> pageLoginLogs(LoginLogQuery reqDTO) {
        var query =
                Wrappers.<IamLoginLogEntity>lambdaQuery()
                        .orderByDesc(IamLoginLogEntity::getOperationTime)
                        .orderByDesc(IamLoginLogEntity::getId);
        if (StringUtils.hasText(reqDTO.username)) {
            query.like(IamLoginLogEntity::getUsername, reqDTO.username);
        }
        if (StringUtils.hasText(reqDTO.staffName)) {
            List<Long> staffIds =
                    staffMapper
                            .selectList(
                                    Wrappers.<IamStaffEntity>lambdaQuery()
                                            .like(IamStaffEntity::getStaffName, reqDTO.staffName))
                            .stream()
                            .map(IamStaffEntity::getId)
                            .toList();
            if (staffIds.isEmpty()) {
                query.eq(IamLoginLogEntity::getStaffId, -1L);
            } else {
                query.in(IamLoginLogEntity::getStaffId, staffIds);
            }
        }
        if (reqDTO.result != null) {
            query.eq(IamLoginLogEntity::getResult, reqDTO.result);
        }
        if (StringUtils.hasText(reqDTO.ip)) {
            query.like(IamLoginLogEntity::getIp, reqDTO.ip);
        }
        if (reqDTO.operationTimeRange != null) {
            if (reqDTO.operationTimeRange.getStartTime() != null) {
                query.ge(
                        IamLoginLogEntity::getOperationTime,
                        reqDTO.operationTimeRange.getStartTime());
            }
            if (reqDTO.operationTimeRange.getEndTime() != null) {
                query.le(
                        IamLoginLogEntity::getOperationTime,
                        reqDTO.operationTimeRange.getEndTime());
            }
        }
        Page<IamLoginLogEntity> page =
                loginLogMapper.selectPage(
                        new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        return new PageSlice<>(
                page.getRecords().stream().map(IamPersistenceConverter::toDomain).toList(),
                page.getTotal());
    }

    public PageSlice<IamOperationLog> pageOperationLogs(OperationLogQuery reqDTO) {
        var query =
                Wrappers.<IamOperationLogEntity>lambdaQuery()
                        .orderByDesc(IamOperationLogEntity::getOperationTime)
                        .orderByDesc(IamOperationLogEntity::getId);
        if (reqDTO.operatorId != null) {
            query.eq(IamOperationLogEntity::getOperatorId, reqDTO.operatorId);
        }
        if (StringUtils.hasText(reqDTO.operatorUsername)) {
            query.like(IamOperationLogEntity::getOperatorUsername, reqDTO.operatorUsername);
        }
        if (StringUtils.hasText(reqDTO.operatorStaffName)) {
            query.like(IamOperationLogEntity::getOperatorStaffName, reqDTO.operatorStaffName);
        }
        if (reqDTO.module != null) {
            query.eq(IamOperationLogEntity::getModule, reqDTO.module);
        }
        if (reqDTO.action != null) {
            query.eq(IamOperationLogEntity::getAction, reqDTO.action);
        }
        if (reqDTO.success != null) {
            query.eq(IamOperationLogEntity::getSuccess, reqDTO.success);
        }
        if (StringUtils.hasText(reqDTO.requestPath)) {
            query.like(IamOperationLogEntity::getRequestPath, reqDTO.requestPath);
        }
        if (reqDTO.operationTimeRange != null) {
            if (reqDTO.operationTimeRange.getStartTime() != null) {
                query.ge(
                        IamOperationLogEntity::getOperationTime,
                        reqDTO.operationTimeRange.getStartTime());
            }
            if (reqDTO.operationTimeRange.getEndTime() != null) {
                query.le(
                        IamOperationLogEntity::getOperationTime,
                        reqDTO.operationTimeRange.getEndTime());
            }
        }
        Page<IamOperationLogEntity> page =
                operationLogMapper.selectPage(
                        new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        return new PageSlice<>(
                page.getRecords().stream().map(IamPersistenceConverter::toDomain).toList(),
                page.getTotal());
    }
}
