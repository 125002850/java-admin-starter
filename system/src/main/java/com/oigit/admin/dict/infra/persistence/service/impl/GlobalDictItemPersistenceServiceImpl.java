package com.oigit.admin.dict.infra.persistence.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictItemEntity;
import com.oigit.admin.dict.infra.persistence.mapper.GlobalDictItemMapper;
import com.oigit.admin.dict.infra.persistence.service.GlobalDictItemPersistenceService;
import com.oigit.admin.dict.infra.query.GlobalDictItemSceneQueryDefinition;
import org.springframework.stereotype.Service;

@Service
public class GlobalDictItemPersistenceServiceImpl
        extends ServiceImpl<GlobalDictItemMapper, GlobalDictItemEntity>
        implements GlobalDictItemPersistenceService {

    private final MybatisPlusQueryExecutor queryExecutor;
    private final GlobalDictItemSceneQueryDefinition queryDefinition;

    public GlobalDictItemPersistenceServiceImpl(
            MybatisPlusQueryExecutor queryExecutor,
            GlobalDictItemSceneQueryDefinition queryDefinition
    ) {
        this.queryExecutor = queryExecutor;
        this.queryDefinition = queryDefinition;
    }

    @Override
    public Page<GlobalDictItemEntity> pageBy(QueryAst queryAst) {
        return queryExecutor.selectPage(getBaseMapper(), queryAst, queryDefinition);
    }

    @Override
    public int maxQueryComplexityScore() {
        return queryDefinition.maxComplexityScore();
    }
}
