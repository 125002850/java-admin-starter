package com.oigit.admin.queryscheme.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeCreateReqDTO;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeDefaultUpdateReqDTO;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeUpdateReqDTO;
import com.oigit.admin.queryscheme.dto.rsp.QuerySchemeRspDTO;
import com.oigit.admin.queryscheme.enums.QuerySchemeErrorCode;
import com.oigit.admin.queryscheme.domain.model.QueryScheme;
import com.oigit.admin.queryscheme.domain.repository.QuerySchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class QuerySchemeAppService {
    private final QuerySchemeRepository repository;
    private final QuerySchemeContentValidator validator;
    public QuerySchemeAppService(QuerySchemeRepository repository, QuerySchemeContentValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }
    public List<QuerySchemeRspDTO> list(String sceneCode) {
        Long userId = currentUserId();
        validator.validateScene(sceneCode);
        return repository.list(userId, sceneCode).stream().map(this::toRsp).toList();
    }
    @Transactional
    public Long create(QuerySchemeCreateReqDTO req) {
        Long userId = currentUserId();
        validator.validate(req.getSceneCode(), req.getContentJson());
        repository.lockScene(userId, req.getSceneCode());
        Long id = repository.create(userId, req.getSceneCode(), req.getSchemeName(), req.getContentJson());
        if (req.isDefaultScheme()) repository.replaceDefault(userId, req.getSceneCode(), id);
        return id;
    }
    @Transactional
    public void update(QuerySchemeUpdateReqDTO req) {
        Long userId = currentUserId();
        var current = repository.require(req.getId(), userId);
        validator.validate(current.sceneCode(), req.getContentJson());
        repository.lockScene(userId, current.sceneCode());
        repository.update(userId, req.getId(), req.getSchemeName(), req.getContentJson(), req.getVersion());
    }
    @Transactional
    public void delete(Long id, Integer version) {
        Long userId = currentUserId();
        var current = repository.require(id, userId);
        repository.lockScene(userId, current.sceneCode());
        repository.delete(userId, id, version);
    }
    @Transactional
    public void updateDefault(QuerySchemeDefaultUpdateReqDTO req) {
        Long userId = currentUserId();
        validator.validateScene(req.getSceneCode());
        repository.lockScene(userId, req.getSceneCode());
        if (req.getSchemeId() != null) {
            var scheme = repository.require(req.getSchemeId(), userId);
            if (!scheme.sceneCode().equals(req.getSceneCode())) throw new BizException(QuerySchemeErrorCode.SCHEME_NOT_FOUND);
        }
        repository.replaceDefault(userId, req.getSceneCode(), req.getSchemeId());
    }
    private Long currentUserId() {
        Long id = OperatorContext.getOperatorId();
        if (id == null || id <= 0) throw new BizException(QuerySchemeErrorCode.USER_REQUIRED);
        return id;
    }
    private QuerySchemeRspDTO toRsp(QueryScheme e) {
        var rsp = new QuerySchemeRspDTO();
        rsp.setId(e.id());
        rsp.setSceneCode(e.sceneCode());
        rsp.setSchemeName(e.schemeName());
        rsp.setContentJson(e.contentJson());
        rsp.setSchemaVersion(e.schemaVersion());
        rsp.setDefaultScheme(Boolean.TRUE.equals(e.defaultScheme()));
        rsp.setVersion(e.version());
        rsp.setCreateTime(e.createTime());
        rsp.setUpdateTime(e.updateTime());
        rsp.setCreateById(e.createBy());
        rsp.setUpdateById(e.updateBy());
        return rsp;
    }
}
