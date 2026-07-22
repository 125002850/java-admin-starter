package com.oigit.admin.iam.app;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.dto.IamLogDTO.LogIdReqDTO;
import com.oigit.admin.iam.dto.IamLogDTO.LoginLogPageReqDTO;
import com.oigit.admin.iam.dto.IamLogDTO.LoginLogRspDTO;
import com.oigit.admin.iam.dto.IamLogDTO.OperationLogPageReqDTO;
import com.oigit.admin.iam.dto.IamLogDTO.OperationLogRspDTO;
import com.oigit.admin.iam.enums.LoginResult;
import com.oigit.admin.iam.infra.entity.IamLoginLogEntity;
import com.oigit.admin.iam.infra.entity.IamOperationLogEntity;
import com.oigit.admin.iam.infra.entity.IamStaffEntity;
import com.oigit.admin.iam.infra.mapper.IamLoginLogMapper;
import com.oigit.admin.iam.infra.mapper.IamOperationLogMapper;
import com.oigit.admin.iam.infra.mapper.IamStaffMapper;
import com.oigit.admin.core.exception.CommonErrorCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LogAppService {

    private final IamLoginLogMapper loginLogMapper;
    private final IamOperationLogMapper operationLogMapper;
    private final IamStaffMapper staffMapper;

    public LogAppService(
            IamLoginLogMapper loginLogMapper,
            IamOperationLogMapper operationLogMapper,
            IamStaffMapper staffMapper
    ) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.staffMapper = staffMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<LoginLogRspDTO> pageLoginLogs(LoginLogPageReqDTO reqDTO) {
        var query = Wrappers.<IamLoginLogEntity>lambdaQuery()
                .orderByDesc(IamLoginLogEntity::getOperationTime)
                .orderByDesc(IamLoginLogEntity::getId);
        if (StringUtils.hasText(reqDTO.username)) {
            query.like(IamLoginLogEntity::getUsername, reqDTO.username);
        }
        if (StringUtils.hasText(reqDTO.staffName)) {
            List<Long> staffIds = staffMapper.selectList(
                            Wrappers.<IamStaffEntity>lambdaQuery()
                                    .like(IamStaffEntity::getStaffName, reqDTO.staffName)
                    ).stream()
                    .map(IamStaffEntity::getId)
                    .toList();
            if (staffIds.isEmpty()) {
                query.eq(IamLoginLogEntity::getStaffId, -1L);
            } else {
                query.in(IamLoginLogEntity::getStaffId, staffIds);
            }
        }
        if (StringUtils.hasText(reqDTO.result)) {
            query.eq(IamLoginLogEntity::getResult, LoginResult.valueOf(reqDTO.result));
        }
        if (StringUtils.hasText(reqDTO.ip)) {
            query.like(IamLoginLogEntity::getIp, reqDTO.ip);
        }
        if (reqDTO.operationTimeRange != null) {
            if (reqDTO.operationTimeRange.getStartTime() != null) {
                query.ge(IamLoginLogEntity::getOperationTime, reqDTO.operationTimeRange.getStartTime());
            }
            if (reqDTO.operationTimeRange.getEndTime() != null) {
                query.le(IamLoginLogEntity::getOperationTime, reqDTO.operationTimeRange.getEndTime());
            }
        }
        Page<IamLoginLogEntity> page = loginLogMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        Map<Long, String> staffNameMap = loadStaffNames(page.getRecords());
        return new PageResult<>(
                page.getRecords().stream().map(entity -> toLoginLogRsp(entity, staffNameMap)).toList(),
                page.getTotal()
        );
    }

    @Transactional(readOnly = true)
    public LoginLogRspDTO loginLogDetail(LogIdReqDTO reqDTO) {
        IamLoginLogEntity entity = loginLogMapper.selectById(reqDTO.logId);
        if (entity == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
        return toLoginLogRsp(entity, loadStaffNames(List.of(entity)));
    }

    @Transactional(readOnly = true)
    public PageResult<OperationLogRspDTO> pageOperationLogs(OperationLogPageReqDTO reqDTO) {
        var query = Wrappers.<IamOperationLogEntity>lambdaQuery()
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
        if (StringUtils.hasText(reqDTO.module)) {
            query.eq(IamOperationLogEntity::getModule, reqDTO.module);
        }
        if (StringUtils.hasText(reqDTO.action)) {
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
                query.ge(IamOperationLogEntity::getOperationTime, reqDTO.operationTimeRange.getStartTime());
            }
            if (reqDTO.operationTimeRange.getEndTime() != null) {
                query.le(IamOperationLogEntity::getOperationTime, reqDTO.operationTimeRange.getEndTime());
            }
        }
        Page<IamOperationLogEntity> page = operationLogMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        return new PageResult<>(page.getRecords().stream().map(this::toOperationLogRsp).toList(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public OperationLogRspDTO operationLogDetail(LogIdReqDTO reqDTO) {
        IamOperationLogEntity entity = operationLogMapper.selectById(reqDTO.logId);
        if (entity == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
        return toOperationLogRsp(entity);
    }

    private Map<Long, String> loadStaffNames(List<IamLoginLogEntity> loginLogs) {
        List<Long> staffIds = loginLogs.stream()
                .map(IamLoginLogEntity::getStaffId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (staffIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> staffNameMap = new HashMap<>();
        staffMapper.selectList(Wrappers.<IamStaffEntity>lambdaQuery().in(IamStaffEntity::getId, staffIds))
                .forEach(staff -> staffNameMap.put(staff.getId(), staff.getStaffName()));
        return staffNameMap;
    }

    private LoginLogRspDTO toLoginLogRsp(IamLoginLogEntity entity, Map<Long, String> staffNameMap) {
        LoginLogRspDTO dto = new LoginLogRspDTO();
        dto.logId = entity.getId();
        dto.staffId = entity.getStaffId();
        dto.username = entity.getUsername();
        dto.staffName = entity.getStaffId() == null ? null : staffNameMap.get(entity.getStaffId());
        dto.eventType = entity.getEventType() == null ? null : entity.getEventType().name();
        dto.result = entity.getResult() == null ? null : entity.getResult().name();
        dto.failureReason = entity.getFailureReason();
        dto.ip = entity.getIp();
        dto.userAgent = entity.getUserAgent();
        dto.tokenId = entity.getTokenId();
        dto.operationTime = entity.getOperationTime();
        return dto;
    }

    private OperationLogRspDTO toOperationLogRsp(IamOperationLogEntity entity) {
        OperationLogRspDTO dto = new OperationLogRspDTO();
        dto.logId = entity.getId();
        dto.operatorId = entity.getOperatorId();
        dto.operatorUsername = entity.getOperatorUsername();
        dto.operatorStaffName = entity.getOperatorStaffName();
        dto.module = entity.getModule() == null ? null : entity.getModule().name();
        dto.action = entity.getAction() == null ? null : entity.getAction().name();
        dto.requestPath = entity.getRequestPath();
        dto.httpMethod = entity.getHttpMethod();
        dto.requestSummary = entity.getRequestSummary();
        dto.responseSummary = entity.getResponseSummary();
        dto.success = entity.getSuccess();
        dto.errorMessage = entity.getErrorMessage();
        dto.ip = entity.getIp();
        dto.userAgent = entity.getUserAgent();
        dto.costMillis = entity.getCostMillis();
        dto.operationTime = entity.getOperationTime();
        return dto;
    }
}
