package com.demo.iam.app;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.exception.BizException;
import com.demo.core.web.PageResult;
import com.demo.iam.dto.IamLogDTO.LogIdReqDTO;
import com.demo.iam.dto.IamLogDTO.LoginLogPageReqDTO;
import com.demo.iam.dto.IamLogDTO.LoginLogRspDTO;
import com.demo.iam.dto.IamLogDTO.OperationLogPageReqDTO;
import com.demo.iam.dto.IamLogDTO.OperationLogRspDTO;
import com.demo.iam.enums.LoginResult;
import com.demo.iam.infra.entity.IamLoginLogEntity;
import com.demo.iam.infra.entity.IamOperationLogEntity;
import com.demo.iam.infra.mapper.IamLoginLogMapper;
import com.demo.iam.infra.mapper.IamOperationLogMapper;
import com.demo.core.exception.CommonErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LogAppService {

    private final IamLoginLogMapper loginLogMapper;
    private final IamOperationLogMapper operationLogMapper;

    public LogAppService(IamLoginLogMapper loginLogMapper, IamOperationLogMapper operationLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<LoginLogRspDTO> pageLoginLogs(LoginLogPageReqDTO reqDTO) {
        var query = Wrappers.<IamLoginLogEntity>lambdaQuery()
                .orderByDesc(IamLoginLogEntity::getOperationTime)
                .orderByDesc(IamLoginLogEntity::getId);
        if (StringUtils.hasText(reqDTO.username)) {
            query.like(IamLoginLogEntity::getUsername, reqDTO.username);
        }
        if (StringUtils.hasText(reqDTO.result)) {
            query.eq(IamLoginLogEntity::getResult, LoginResult.valueOf(reqDTO.result));
        }
        Page<IamLoginLogEntity> page = loginLogMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        return new PageResult<>(page.getRecords().stream().map(this::toLoginLogRsp).toList(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public LoginLogRspDTO loginLogDetail(LogIdReqDTO reqDTO) {
        IamLoginLogEntity entity = loginLogMapper.selectById(reqDTO.logId);
        if (entity == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
        return toLoginLogRsp(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<OperationLogRspDTO> pageOperationLogs(OperationLogPageReqDTO reqDTO) {
        var query = Wrappers.<IamOperationLogEntity>lambdaQuery()
                .orderByDesc(IamOperationLogEntity::getOperationTime)
                .orderByDesc(IamOperationLogEntity::getId);
        if (reqDTO.operatorId != null) {
            query.eq(IamOperationLogEntity::getOperatorId, reqDTO.operatorId);
        }
        if (StringUtils.hasText(reqDTO.module)) {
            query.eq(IamOperationLogEntity::getModule, reqDTO.module);
        }
        if (StringUtils.hasText(reqDTO.action)) {
            query.eq(IamOperationLogEntity::getAction, reqDTO.action);
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

    private LoginLogRspDTO toLoginLogRsp(IamLoginLogEntity entity) {
        LoginLogRspDTO dto = new LoginLogRspDTO();
        dto.logId = entity.getId();
        dto.staffId = entity.getStaffId();
        dto.username = entity.getUsername();
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
