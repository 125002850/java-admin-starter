package com.oigit.admin.staff.infra.translation;

import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.core.translation.TranslationKey;
import com.oigit.admin.core.translation.TranslationProvider;
import com.oigit.admin.core.translation.TranslationTypes;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Batch adapter from the generic translation engine to the SSO user cache. */
@Component
public class UserNameTranslationProvider implements TranslationProvider {

    private final OperatorUsernameResolver operatorUsernameResolver;

    public UserNameTranslationProvider(OperatorUsernameResolver operatorUsernameResolver) {
        this.operatorUsernameResolver = operatorUsernameResolver;
    }

    @Override
    public String type() {
        return TranslationTypes.USER_NAME;
    }

    @Override
    public Map<TranslationKey, String> translate(Set<TranslationKey> keys) {
        Map<TranslationKey, Long> idsByKey = new LinkedHashMap<>();
        for (TranslationKey key : keys) {
            try {
                idsByKey.put(key, Long.valueOf(key.value()));
            } catch (NumberFormatException ignored) {
                // Invalid user IDs are left untranslated instead of causing the response to fail.
            }
        }
        Map<Long, String> usernames = operatorUsernameResolver.resolveUsernames(idsByKey.values());
        Map<TranslationKey, String> result = new LinkedHashMap<>();
        idsByKey.forEach((key, id) -> {
            String username = usernames.get(id);
            if (username != null) {
                result.put(key, username);
            }
        });
        return result;
    }
}
