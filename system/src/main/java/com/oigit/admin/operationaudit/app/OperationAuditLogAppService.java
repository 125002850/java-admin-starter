package com.oigit.admin.operationaudit.app;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.support.DynamicQueryGuard;
import com.oigit.admin.operationaudit.app.query.OperationAuditLogSceneQueryMapper;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLog;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLogPage;
import com.oigit.admin.operationaudit.domain.repository.OperationAuditLogRepository;
import com.oigit.admin.operationaudit.dto.req.query.OperationAuditLogDynamicPageReqDTO;
import com.oigit.admin.operationaudit.dto.rsp.OperationAuditLogDetailRspDTO;
import com.oigit.admin.operationaudit.dto.rsp.OperationAuditLogRspDTO;
import com.oigit.admin.operationaudit.enums.OperationAuditErrorCode;
import com.oigit.admin.core.operator.OperatorUsernameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OperationAuditLogAppService {

    private final OperationAuditLogRepository repository;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final OperationAuditLogSceneQueryMapper queryMapper;
    private final OperatorUsernameResolver operatorUsernameResolver;

    public OperationAuditLogAppService(
            OperationAuditLogRepository repository,
            DynamicQueryGuard dynamicQueryGuard,
            OperationAuditLogSceneQueryMapper queryMapper,
            OperatorUsernameResolver operatorUsernameResolver
    ) {
        this.repository = repository;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.queryMapper = queryMapper;
        this.operatorUsernameResolver = operatorUsernameResolver;
    }

    @Transactional(readOnly = true)
    public PageResult<OperationAuditLogRspDTO> page(OperationAuditLogDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = queryMapper.map(reqDTO);
        dynamicQueryGuard.validate(queryAst, repository.maxQueryComplexityScore());
        OperationAuditLogPage page = repository.page(queryAst);
        Map<Long, String> profiles = loadProfiles(page.records());
        List<OperationAuditLogRspDTO> records = page.records().stream()
                .map(log -> toRsp(log, profileFor(log, profiles)))
                .toList();
        return new PageResult<>(records, page.total());
    }

    @Transactional(readOnly = true)
    public OperationAuditLogDetailRspDTO detail(Long logId) {
        return repository.findActiveById(logId)
                .map(this::toDetailRsp)
                .orElseThrow(() -> new BizException(OperationAuditErrorCode.LOG_NOT_FOUND));
    }

    private Map<Long, String> loadProfiles(List<OperationAuditLog> records) {
        List<Long> operatorIds = records.stream()
                .map(OperationAuditLog::operatorId)
                .filter(java.util.Objects::nonNull)
                .toList();
        return operatorUsernameResolver.resolveUsernames(operatorIds);
    }

    private String profileFor(
            OperationAuditLog log,
            Map<Long, String> profiles
    ) {
        Long operatorId = log.operatorId();
        return operatorId == null ? null : profiles.get(operatorId);
    }

    private OperationAuditLogRspDTO toRsp(
            OperationAuditLog log,
            String profile
    ) {
        OperationAuditLogRspDTO rspDTO = new OperationAuditLogRspDTO();
        copyCommon(log, rspDTO);
        rspDTO.setOperatorName(resolveOperatorDisplayName(log, profile));
        return rspDTO;
    }

    private OperationAuditLogDetailRspDTO toDetailRsp(OperationAuditLog log) {
        OperationAuditLogDetailRspDTO rspDTO = new OperationAuditLogDetailRspDTO();
        copyCommon(log, rspDTO);
        String profile = log.operatorId() == null
                ? null
                : operatorUsernameResolver.resolveUsernames(List.of(log.operatorId())).get(log.operatorId());
        rspDTO.setOperatorUsername(resolveOperatorAccount(log, profile));
        rspDTO.setOperatorRealName(resolveOperatorRealName(log, profile));
        rspDTO.setOperatorName(resolveOperatorDisplayName(log, profile));
        rspDTO.setRequestMethod(log.requestMethod());
        rspDTO.setRequestPath(log.requestPath());
        rspDTO.setTraceId(log.traceId());
        rspDTO.setRequestParams(log.requestParams());
        rspDTO.setHttpStatus(log.httpStatus());
        rspDTO.setResultCode(log.resultCode());
        rspDTO.setErrorMessage(log.errorMessage());
        return rspDTO;
    }

    private void copyCommon(OperationAuditLog log, OperationAuditLogRspDTO rspDTO) {
        rspDTO.setLogId(log.id());
        rspDTO.setModuleCode(log.moduleCode());
        rspDTO.setModuleName(log.moduleName());
        rspDTO.setAction(log.action());
        rspDTO.setDescription(log.description());
        rspDTO.setOperatorId(log.operatorId());
        rspDTO.setOperatorName(log.operatorName());
        rspDTO.setClientIp(log.clientIp());
        rspDTO.setResultStatus(log.resultStatus());
        rspDTO.setDurationMs(log.durationMs());
        rspDTO.setOperationTime(log.operationTime());
    }

    private static String resolveOperatorAccount(
            OperationAuditLog log,
            String profile
    ) {
        return firstNonBlank(
                log.operatorUsername(),
                profile == null ? null : profile
        );
    }

    private static String resolveOperatorRealName(
            OperationAuditLog log,
            String profile
    ) {
        return firstNonBlank(
                log.operatorRealName(),
                profile == null ? null : null
        );
    }

    /** 展示名优先级：真实姓名 → 账号 → 原始记录名（跳过仅由 operatorId 兜底的无意义值）。 */
    private static String resolveOperatorDisplayName(
            OperationAuditLog log,
            String profile
    ) {
        String realName = resolveOperatorRealName(log, profile);
        if (realName != null) {
            return realName;
        }
        String username = resolveOperatorAccount(log, profile);
        if (username != null) {
            return username;
        }
        if (isOperatorIdFallback(log)) {
            return null;
        }
        return log.operatorName();
    }

    private static boolean isOperatorIdFallback(OperationAuditLog log) {
        Long operatorId = log.operatorId();
        if (operatorId == null || !StringUtils.hasText(log.operatorName())) {
            return false;
        }
        return log.operatorName().trim().equals(String.valueOf(operatorId));
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return StringUtils.hasText(secondary) ? secondary.trim() : null;
    }
}
