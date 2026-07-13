package com.example.admin.core.operator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CacheUserServiceTests {

    private CacheUserMapper cacheUserMapper;
    private CacheUserService cacheUserService;

    @BeforeEach
    void setUp() {
        cacheUserMapper = Mockito.mock(CacheUserMapper.class);
        cacheUserService = new CacheUserService(cacheUserMapper);
    }

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
    }

    @Test
    void should_skip_upsert_when_all_cache_fields_are_blank() {
        cacheUserService.upsert(100L, " ", null, "\t", "");

        verifyNoInteractions(cacheUserMapper);
    }

    @Test
    void should_normalize_values_before_upsert() {
        cacheUserService.upsert(100L, " test-user ", " 13800138000 ", " 张三 ", " U100 ");

        ArgumentCaptor<CacheUserEntity> captor = ArgumentCaptor.forClass(CacheUserEntity.class);
        verify(cacheUserMapper).upsert(captor.capture());

        CacheUserEntity entity = captor.getValue();
        assertThat(entity.getUserId()).isEqualTo(100L);
        assertThat(entity.getUserName()).isEqualTo("test-user");
        assertThat(entity.getUserPhone()).isEqualTo("13800138000");
        assertThat(entity.getRealName()).isEqualTo("张三");
        assertThat(entity.getUserCode()).isEqualTo("U100");
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
    }

    @Test
    void should_resolve_usernames_for_non_empty_cached_users() {
        CacheUserEntity user100 = new CacheUserEntity();
        user100.setUserId(100L);
        user100.setUserName(" test-user ");
        user100.setRealName("张三");
        CacheUserEntity user101 = new CacheUserEntity();
        user101.setUserId(101L);
        user101.setUserName(" ");
        user101.setRealName("李四");
        when(cacheUserMapper.selectBatchIds(argThat(ids -> ids.contains(100L) && ids.contains(101L) && ids.size() == 2)))
                .thenReturn(List.of(user100, user101));

        Map<Long, String> result = cacheUserService.resolveUsernames(List.of(100L, 101L, 0L));

        assertThat(result).containsEntry(100L, "test-user");
        assertThat(result).doesNotContainKey(101L);
    }

    @Test
    void should_prefer_current_operator_username_over_stale_cache() {
        CacheUserEntity cachedUser = new CacheUserEntity();
        cachedUser.setUserId(100L);
        cachedUser.setUserName("old-user");
        when(cacheUserMapper.selectBatchIds(argThat(ids -> ids.contains(100L))))
                .thenReturn(List.of(cachedUser));
        OperatorContext.set(100L, " current-user ", null);

        Map<Long, String> result = cacheUserService.resolveUsernames(List.of(100L));

        assertThat(result).containsEntry(100L, "current-user");
    }
}
