package com.oigit.admin.dict.infra.persistence.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.dict.enums.DictErrorCode;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictTypeEntity;
import com.oigit.admin.dict.infra.persistence.mapper.GlobalDictTypeMapper;
import com.oigit.admin.dict.infra.persistence.service.GlobalDictTypePersistenceService;
import com.oigit.admin.dict.infra.query.GlobalDictTypeSceneQueryDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalDictTypePersistenceServiceImpl
        extends ServiceImpl<GlobalDictTypeMapper, GlobalDictTypeEntity>
        implements GlobalDictTypePersistenceService {

    private final MybatisPlusQueryExecutor queryExecutor;
    private final GlobalDictTypeSceneQueryDefinition queryDefinition;

    public GlobalDictTypePersistenceServiceImpl(
            MybatisPlusQueryExecutor queryExecutor,
            GlobalDictTypeSceneQueryDefinition queryDefinition
    ) {
        this.queryExecutor = queryExecutor;
        this.queryDefinition = queryDefinition;
    }

    @Override
    public Page<GlobalDictTypeEntity> pageBy(QueryAst queryAst) {
        return queryExecutor.selectPage(getBaseMapper(), queryAst, queryDefinition);
    }

    @Override
    public List<GlobalDictTypeEntity> listForExport(QueryAst queryAst) {
        QueryAst exportQueryAst = new QueryAst();
        exportQueryAst.setRoot(queryAst.getRoot());
        exportQueryAst.setSorts(queryAst.getSorts());
        exportQueryAst.setPageNo(1L);
        exportQueryAst.setPageSize((long) queryDefinition.maxExportRows() + 1L);
        Page<GlobalDictTypeEntity> page = queryExecutor.selectPage(getBaseMapper(), exportQueryAst, queryDefinition);
        if (page.getTotal() > queryDefinition.maxExportRows()) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_EXPORT_ROW_LIMIT_EXCEEDED);
        }
        return page.getRecords();
    }

    @Override
    public int maxQueryComplexityScore() {
        return queryDefinition.maxComplexityScore();
    }
}
