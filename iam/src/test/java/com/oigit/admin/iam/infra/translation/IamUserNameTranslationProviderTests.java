package com.oigit.admin.iam.infra.translation;

import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.oigit.admin.core.translation.TranslationKey;
import com.oigit.admin.iam.infra.entity.IamStaffEntity;
import com.oigit.admin.iam.infra.mapper.IamStaffMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IamUserNameTranslationProviderTests {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MybatisMapperBuilderAssistant assistant = new MybatisMapperBuilderAssistant(
                configuration,
                "iam-user-name-translation"
        );
        assistant.setCurrentNamespace(IamStaffMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, IamStaffEntity.class);
    }

    @Test
    void shouldResolveEveryUniqueUserIdWithOneMapperQuery() {
        IamStaffMapper mapper = mock(IamStaffMapper.class);
        IamStaffEntity admin = staff(1L, "admin", "超级管理员");
        IamStaffEntity fallback = staff(2L, "operator", null);
        when(mapper.selectList(any())).thenReturn(List.of(admin, fallback));

        IamUserNameTranslationProvider provider = new IamUserNameTranslationProvider(mapper);
        TranslationKey adminKey = new TranslationKey("", "1");
        TranslationKey fallbackKey = new TranslationKey("", "2");
        TranslationKey invalidKey = new TranslationKey("", "invalid");

        Map<TranslationKey, String> translated = provider.translate(
                Set.of(adminKey, fallbackKey, invalidKey)
        );

        assertThat(translated)
                .containsEntry(adminKey, "超级管理员")
                .containsEntry(fallbackKey, "operator")
                .doesNotContainKey(invalidKey);
        verify(mapper, times(1)).selectList(any());
    }

    private static IamStaffEntity staff(Long id, String username, String staffName) {
        IamStaffEntity entity = new IamStaffEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setStaffName(staffName);
        return entity;
    }
}
