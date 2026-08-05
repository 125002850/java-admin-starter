package com.oigit.admin.dict.domain.repository;

import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.dict.domain.model.DictPage;
import com.oigit.admin.dict.domain.model.GlobalDictType;

import java.util.List;
import java.util.Optional;

public interface GlobalDictTypeRepository {

    boolean existsByCode(String dictTypeCode, Long excludeId);

    Optional<GlobalDictType> findById(Long id);

    void create(GlobalDictType dictType);

    void update(GlobalDictType dictType);

    void deleteById(Long id);

    List<GlobalDictType> listAll(String keyword);

    DictPage<GlobalDictType> page(QueryAst queryAst);

    List<GlobalDictType> listForExport(QueryAst queryAst);

    int maxQueryComplexityScore();
}
