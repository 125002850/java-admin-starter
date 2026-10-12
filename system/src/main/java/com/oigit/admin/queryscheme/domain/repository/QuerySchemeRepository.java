package com.oigit.admin.queryscheme.domain.repository;

import com.oigit.admin.queryscheme.domain.model.QueryScheme;
import java.util.List;

public interface QuerySchemeRepository {
    List<QueryScheme> list(Long userId, String sceneCode);
    QueryScheme require(Long id, Long userId);
    void lockScene(Long userId, String sceneCode);
    Long create(Long userId, String sceneCode, String name, String content);
    void update(Long userId, Long id, String name, String content, Integer version);
    void delete(Long userId, Long id, Integer version);
    void replaceDefault(Long userId, String sceneCode, Long id);
}
