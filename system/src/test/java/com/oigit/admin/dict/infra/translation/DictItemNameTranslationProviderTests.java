package com.oigit.admin.dict.infra.translation;

import com.oigit.admin.core.translation.TranslationKey;
import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictItemNameTranslationProviderTests {

    @Test
    void should_load_all_dictionary_types_with_one_repository_batch() {
        GlobalDictItemRepository repository = mock(GlobalDictItemRepository.class);
        when(repository.findNamesByTypeCodes(argThat(typeCodes ->
                typeCodes.contains("ENABLE_STATUS") && typeCodes.contains("YES_NO") && typeCodes.size() == 2
        ))).thenReturn(Map.of(
                "ENABLE_STATUS", Map.of("enable", "启用", "disable", "禁用"),
                "YES_NO", Map.of("yes", "是")
        ));
        DictItemNameTranslationProvider provider = new DictItemNameTranslationProvider(repository);
        Set<TranslationKey> keys = Set.of(
                new TranslationKey("ENABLE_STATUS", "enable"),
                new TranslationKey("ENABLE_STATUS", "disable"),
                new TranslationKey("YES_NO", "yes")
        );

        Map<TranslationKey, String> result = provider.translate(keys);

        verify(repository, times(1)).findNamesByTypeCodes(argThat(typeCodes -> typeCodes.size() == 2));
        assertThat(result)
                .containsEntry(new TranslationKey("ENABLE_STATUS", "enable"), "启用")
                .containsEntry(new TranslationKey("ENABLE_STATUS", "disable"), "禁用")
                .containsEntry(new TranslationKey("YES_NO", "yes"), "是");
    }
}
