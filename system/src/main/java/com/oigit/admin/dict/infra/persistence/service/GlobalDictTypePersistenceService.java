package com.oigit.admin.dict.infra.persistence.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictTypeEntity;

import java.util.List;

public interface GlobalDictTypePersistenceService extends IService<GlobalDictTypeEntity> {

    Page<GlobalDictTypeEntity> pageBy(QueryAst queryAst);

    List<GlobalDictTypeEntity> listForExport(QueryAst queryAst);

    int maxQueryComplexityScore();
}
