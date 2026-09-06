package com.oigit.admin.iam.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.iam.domain.model.IamLoginLog;
import com.oigit.admin.iam.domain.model.IamOperationLog;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.repository.LogRepository;
import com.oigit.admin.iam.dto.req.LogIdReqDTO;
import com.oigit.admin.iam.dto.req.LoginLogPageReqDTO;
import com.oigit.admin.iam.dto.req.OperationLogPageReqDTO;
import com.oigit.admin.iam.dto.rsp.LoginLogRspDTO;
import com.oigit.admin.iam.dto.rsp.OperationLogRspDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogAppService {
    private final LogRepository logRepository;

    public LogAppService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<LoginLogRspDTO> pageLoginLogs(LoginLogPageReqDTO dto) {
        PageSlice<IamLoginLog> page = logRepository.pageLoginLogs(IamQueryMapper.toQuery(dto));
        return new PageResult<>(
                page.getRecords().stream().map(this::toLoginLogRsp).toList(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public LoginLogRspDTO loginLogDetail(LogIdReqDTO dto) {
        IamLoginLog log = logRepository.findLoginLog(dto.logId);
        if (log == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
        return toLoginLogRsp(log);
    }

    private LoginLogRspDTO toLoginLogRsp(IamLoginLog log) {
        LoginLogRspDTO dto = new LoginLogRspDTO();
        dto.logId = log.id();
        dto.staffId = log.staffId();
        dto.username = log.username();
        dto.eventType = log.eventType();
        dto.result = log.result();
        dto.failureReason = log.failureReason();
        dto.ip = log.ip();
        dto.userAgent = log.userAgent();
        dto.tokenId = log.tokenId();
        dto.operationTime = log.operationTime();
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResult<OperationLogRspDTO> pageOperationLogs(OperationLogPageReqDTO dto) {
        PageSlice<IamOperationLog> page =
                logRepository.pageOperationLogs(IamQueryMapper.toQuery(dto));
        return new PageResult<>(
                page.getRecords().stream().map(this::toOperationLogRsp).toList(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public OperationLogRspDTO operationLogDetail(LogIdReqDTO dto) {
        IamOperationLog log = logRepository.findOperationLog(dto.logId);
        if (log == null) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
        return toOperationLogRsp(log);
    }

    private OperationLogRspDTO toOperationLogRsp(IamOperationLog log) {
        OperationLogRspDTO dto = new OperationLogRspDTO();
        dto.logId = log.id();
        dto.operatorId = log.operatorId();
        dto.operatorUsername = log.operatorUsername();
        dto.operatorStaffName = log.operatorStaffName();
        dto.module = log.module();
        dto.action = log.action();
        dto.requestPath = log.requestPath();
        dto.httpMethod = log.httpMethod();
        dto.requestSummary = log.requestSummary();
        dto.responseSummary = log.responseSummary();
        dto.success = log.success();
        dto.errorMessage = log.errorMessage();
        dto.ip = log.ip();
        dto.userAgent = log.userAgent();
        dto.costMillis = log.costMillis();
        dto.operationTime = log.operationTime();
        return dto;
    }
}
