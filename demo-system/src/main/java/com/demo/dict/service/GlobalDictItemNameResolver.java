package com.demo.dict.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.dict.DictItemNameResolver;
import com.demo.dict.infra.entity.GlobalDictItemEntity;
import com.demo.dict.infra.mapper.GlobalDictItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalDictItemNameResolver implements DictItemNameResolver {

    private final GlobalDictItemMapper globalDictItemMapper;

    public GlobalDictItemNameResolver(GlobalDictItemMapper globalDictItemMapper) {
        this.globalDictItemMapper = globalDictItemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> resolveItemNames(Collection<String> dictTypeCodes) {
        List<String> normalizedTypeCodes = normalize(dictTypeCodes);
        if (normalizedTypeCodes.isEmpty()) {
            return Map.of();
        }

        List<GlobalDictItemEntity> items = globalDictItemMapper.selectList(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .in(GlobalDictItemEntity::getDictTypeCode, normalizedTypeCodes)
                        .orderByAsc(GlobalDictItemEntity::getDictTypeCode)
                        .orderByAsc(GlobalDictItemEntity::getSortOrder)
                        .orderByAsc(GlobalDictItemEntity::getId)
        );

        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (GlobalDictItemEntity item : items) {
            if (!StringUtils.hasText(item.getDictTypeCode()) || !StringUtils.hasText(item.getDictItemCode())) {
                continue;
            }
            result.computeIfAbsent(item.getDictTypeCode(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(item.getDictItemCode(), item.getDictItemName());
        }
        return result;
    }

    private List<String> normalize(Collection<String> dictTypeCodes) {
        if (dictTypeCodes == null || dictTypeCodes.isEmpty()) {
            return List.of();
        }
        return dictTypeCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
