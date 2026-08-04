package com.oigit.admin.staff.infra.translation;

import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.core.translation.TranslationKey;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserNameTranslationProviderTests {

    @Test
    void should_resolve_all_valid_user_ids_with_one_batch_call() {
        OperatorUsernameResolver resolver = mock(OperatorUsernameResolver.class);
        when(resolver.resolveUsernames(argThat(ids -> ids.contains(7L) && ids.contains(8L) && ids.size() == 2)))
                .thenReturn(Map.of(7L, "alice", 8L, "bob"));
        UserNameTranslationProvider provider = new UserNameTranslationProvider(resolver);

        Map<TranslationKey, String> result = provider.translate(Set.of(
                new TranslationKey("", "7"),
                new TranslationKey("", "8"),
                new TranslationKey("", "not-a-number")
        ));

        verify(resolver, times(1)).resolveUsernames(argThat(ids -> ids.size() == 2));
        assertThat(result)
                .containsEntry(new TranslationKey("", "7"), "alice")
                .containsEntry(new TranslationKey("", "8"), "bob")
                .doesNotContainKey(new TranslationKey("", "not-a-number"));
    }
}
