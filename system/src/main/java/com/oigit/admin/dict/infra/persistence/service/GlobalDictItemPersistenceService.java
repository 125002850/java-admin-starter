package com.oigit.admin.dict.infra.persistence.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictItemEntity;

public interface GlobalDictItemPersistenceService extends IService<GlobalDictItemEntity> {

    Page<GlobalDictItemEntity> pageBy(QueryAst queryAst);

    int maxQueryComplexityScore();
}
