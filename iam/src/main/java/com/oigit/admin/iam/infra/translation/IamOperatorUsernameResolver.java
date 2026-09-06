package com.oigit.admin.iam.infra.translation;

import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.iam.infra.persistence.entity.IamStaffEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamStaffMapper;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IamOperatorUsernameResolver implements OperatorUsernameResolver {
    private final IamStaffMapper staffMapper;

    public IamOperatorUsernameResolver(IamStaffMapper staffMapper) {
        this.staffMapper = staffMapper;
    }

    @Override
    public Map<Long, String> resolveUsernames(Collection<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> normalizedIds =
                staffIds.stream()
                        .filter(Objects::nonNull)
                        .filter(staffId -> staffId > 0L)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        return staffMapper.selectBatchIds(normalizedIds).stream()
                .filter(staff -> StringUtils.hasText(staff.getUsername()))
                .collect(
                        Collectors.toMap(
                                IamStaffEntity::getId,
                                staff -> staff.getUsername().trim(),
                                (left, ignored) -> left,
                                LinkedHashMap::new));
    }
}
