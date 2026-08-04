package com.oigit.admin.dict.domain.repository;

import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.dict.domain.model.DictPage;
import com.oigit.admin.dict.domain.model.GlobalDictItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GlobalDictItemRepository {

    boolean existsByTypeAndCode(String dictTypeCode, String dictItemCode, Long excludeId);

    Optional<GlobalDictItem> findById(Long id);

    List<GlobalDictItem> findByIds(Collection<Long> ids);

    long countByTypeCode(String dictTypeCode);

    void create(GlobalDictItem dictItem);

    void update(GlobalDictItem dictItem);

    boolean existsAnyByIds(List<Long> ids);

    void deleteByIds(List<Long> ids);

    void changeTypeCode(String oldTypeCode, String newTypeCode);

    DictPage<GlobalDictItem> page(QueryAst queryAst);

    List<GlobalDictItem> listByTypeCodes(Collection<String> dictTypeCodes);

    Map<String, Map<String, String>> findNamesByTypeCodes(Collection<String> dictTypeCodes);

    int maxQueryComplexityScore();
}
