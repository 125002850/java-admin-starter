package com.oigit.admin.queryscheme.infra.persistence.repository;

import com.oigit.admin.queryscheme.domain.model.QueryScheme;
import com.oigit.admin.queryscheme.domain.repository.QuerySchemeRepository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.queryscheme.enums.QuerySchemeErrorCode;
import com.oigit.admin.queryscheme.infra.persistence.entity.QuerySchemeEntity;
import com.oigit.admin.queryscheme.infra.persistence.mapper.QuerySchemeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class MybatisQuerySchemeRepository implements QuerySchemeRepository {
    private final QuerySchemeMapper mapper;
    public MybatisQuerySchemeRepository(QuerySchemeMapper mapper) { this.mapper = mapper; }

    public List<QueryScheme> list(Long userId, String sceneCode) {
        return mapper.selectList(Wrappers.<QuerySchemeEntity>lambdaQuery()
                .eq(QuerySchemeEntity::getUserId, userId).eq(QuerySchemeEntity::getSceneCode, sceneCode)
                .orderByDesc(QuerySchemeEntity::getDefaultScheme).orderByDesc(QuerySchemeEntity::getUpdateTime)
                .orderByDesc(QuerySchemeEntity::getId)).stream().map(this::toDomain).toList();
    }

    public QueryScheme require(Long id, Long userId) {
        var entity = mapper.selectOne(Wrappers.<QuerySchemeEntity>lambdaQuery()
                .eq(QuerySchemeEntity::getId, id).eq(QuerySchemeEntity::getUserId, userId));
        if (entity == null) throw new BizException(QuerySchemeErrorCode.SCHEME_NOT_FOUND);
        return toDomain(entity);
    }

    public void lockScene(Long userId, String sceneCode) {
        mapper.ensureSceneLock(userId, sceneCode);
        mapper.lockScene(userId, sceneCode);
    }

    public Long create(Long userId, String sceneCode, String name, String content) {
        if (mapper.selectCount(Wrappers.<QuerySchemeEntity>lambdaQuery()
                .eq(QuerySchemeEntity::getUserId, userId).eq(QuerySchemeEntity::getSceneCode, sceneCode)) >= 20) {
            throw new BizException(QuerySchemeErrorCode.LIMIT_EXCEEDED);
        }
        var entity = new QuerySchemeEntity();
        entity.setUserId(userId); entity.setSceneCode(sceneCode); entity.setSchemeName(name.trim());
        entity.setContentJson(content); entity.setSchemaVersion(1); entity.setDefaultScheme(false);
        entity.setDeleted(0L); entity.setVersion(0);
        try { mapper.insert(entity); }
        catch (DuplicateKeyException ex) { throw new BizException(QuerySchemeErrorCode.NAME_DUPLICATED); }
        return entity.getId();
    }

    public void update(Long userId, Long id, String name, String content, Integer version) {
        var entity = new QuerySchemeEntity();
        entity.setSchemeName(name.trim()); entity.setContentJson(content); entity.setSchemaVersion(1);
        entity.setVersion(version);
        try {
            int changed = mapper.update(entity, Wrappers.<QuerySchemeEntity>lambdaUpdate()
                    .eq(QuerySchemeEntity::getId, id).eq(QuerySchemeEntity::getUserId, userId));
            if (changed != 1) throw new BizException(QuerySchemeErrorCode.DATA_CHANGED);
        } catch (DuplicateKeyException ex) { throw new BizException(QuerySchemeErrorCode.NAME_DUPLICATED); }
    }

    public void delete(Long userId, Long id, Integer version) {
        // 使用主键作为删除标记，避免同名方案在同一秒内重复创建/删除导致唯一键冲突。
        int changed = mapper.deleteOwned(userId, id, version);
        if (changed != 1) throw new BizException(QuerySchemeErrorCode.DATA_CHANGED);
    }

    public void replaceDefault(Long userId, String sceneCode, Long id) {
        mapper.update(null, Wrappers.<QuerySchemeEntity>lambdaUpdate()
                .eq(QuerySchemeEntity::getUserId, userId).eq(QuerySchemeEntity::getSceneCode, sceneCode)
                .eq(QuerySchemeEntity::getDefaultScheme, true).set(QuerySchemeEntity::getDefaultScheme, false));
        if (id != null) mapper.update(null, Wrappers.<QuerySchemeEntity>lambdaUpdate()
                .eq(QuerySchemeEntity::getId, id).eq(QuerySchemeEntity::getUserId, userId)
                .eq(QuerySchemeEntity::getSceneCode, sceneCode).set(QuerySchemeEntity::getDefaultScheme, true));
    }
    private QueryScheme toDomain(QuerySchemeEntity e) {
        return new QueryScheme(e.getId(), e.getSceneCode(), e.getSchemeName(), e.getContentJson(),
                e.getSchemaVersion(), e.getDefaultScheme(), e.getVersion(), e.getCreateTime(),
                e.getUpdateTime(), e.getCreateBy(), e.getUpdateBy());
    }
}
