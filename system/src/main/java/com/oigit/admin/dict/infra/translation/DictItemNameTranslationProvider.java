package com.oigit.admin.dict.infra.translation;

import com.oigit.admin.core.translation.TranslationKey;
import com.oigit.admin.core.translation.TranslationProvider;
import com.oigit.admin.core.translation.TranslationTypes;
import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Resolves all requested dictionary types in one repository batch. */
@Component
public class DictItemNameTranslationProvider implements TranslationProvider {

    private final GlobalDictItemRepository globalDictItemRepository;

    public DictItemNameTranslationProvider(GlobalDictItemRepository globalDictItemRepository) {
        this.globalDictItemRepository = globalDictItemRepository;
    }

    @Override
    public String type() {
        return TranslationTypes.DICT_ITEM_NAME;
    }

    @Override
    public Map<TranslationKey, String> translate(Set<TranslationKey> keys) {
        Map<String, Map<String, String>> namesByType = globalDictItemRepository.findNamesByTypeCodes(
                keys.stream().map(TranslationKey::qualifier).filter(value -> !value.isBlank()).distinct().toList()
        );
        Map<TranslationKey, String> result = new LinkedHashMap<>();
        for (TranslationKey key : keys) {
            String name = namesByType.getOrDefault(key.qualifier(), Map.of()).get(key.value());
            if (name != null) {
                result.put(key, name);
            }
        }
        return result;
    }
}
