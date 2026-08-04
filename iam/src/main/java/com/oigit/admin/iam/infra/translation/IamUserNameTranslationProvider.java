package com.oigit.admin.iam.infra.translation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.core.translation.TranslationKey;
import com.oigit.admin.core.translation.TranslationProvider;
import com.oigit.admin.core.translation.TranslationTypes;
import com.oigit.admin.iam.infra.entity.IamStaffEntity;
import com.oigit.admin.iam.infra.mapper.IamStaffMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Resolves audit user IDs from the local IAM staff table in one batch query. */
@Component
public class IamUserNameTranslationProvider implements TranslationProvider {

    private final IamStaffMapper iamStaffMapper;

    public IamUserNameTranslationProvider(IamStaffMapper iamStaffMapper) {
        this.iamStaffMapper = iamStaffMapper;
    }

    @Override
    public String type() {
        return TranslationTypes.USER_NAME;
    }

    @Override
    public Map<TranslationKey, String> translate(Set<TranslationKey> keys) {
        Map<TranslationKey, Long> idsByKey = new LinkedHashMap<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (TranslationKey key : keys) {
            try {
                Long userId = Long.valueOf(key.value());
                idsByKey.put(key, userId);
                userIds.add(userId);
            } catch (NumberFormatException ignored) {
                // Invalid IDs stay untranslated and never fail the whole response.
            }
        }
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> namesById = new LinkedHashMap<>();
        iamStaffMapper.selectList(Wrappers.<IamStaffEntity>lambdaQuery()
                        .select(IamStaffEntity::getId, IamStaffEntity::getStaffName, IamStaffEntity::getUsername)
                        .in(IamStaffEntity::getId, userIds))
                .forEach(staff -> namesById.put(staff.getId(), displayName(staff)));

        Map<TranslationKey, String> result = new LinkedHashMap<>();
        idsByKey.forEach((key, id) -> {
            String name = namesById.get(id);
            if (StringUtils.hasText(name)) {
                result.put(key, name);
            }
        });
        return result;
    }

    private static String displayName(IamStaffEntity staff) {
        return StringUtils.hasText(staff.getStaffName()) ? staff.getStaffName() : staff.getUsername();
    }
}
