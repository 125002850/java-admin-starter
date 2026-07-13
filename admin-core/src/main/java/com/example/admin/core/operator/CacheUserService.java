package com.example.admin.core.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CacheUserService implements OperatorUsernameResolver {

    private static final Logger log = LoggerFactory.getLogger(CacheUserService.class);

    private final CacheUserMapper cacheUserMapper;

    public CacheUserService(CacheUserMapper cacheUserMapper) {
        this.cacheUserMapper = cacheUserMapper;
    }

    public void upsert(Long userId, String userName, String userPhone, String realName, String userCode) {
        if (userId == null) {
            return;
        }
        String normalizedUserName = normalize(userName);
        String normalizedUserPhone = normalize(userPhone);
        String normalizedRealName = normalize(realName);
        String normalizedUserCode = normalize(userCode);
        if (normalizedUserName == null
                && normalizedUserPhone == null
                && normalizedRealName == null
                && normalizedUserCode == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CacheUserEntity entity = new CacheUserEntity();
        entity.setUserId(userId);
        entity.setUserName(normalizedUserName);
        entity.setUserPhone(normalizedUserPhone);
        entity.setRealName(normalizedRealName);
        entity.setUserCode(normalizedUserCode);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        cacheUserMapper.upsert(entity);
        log.debug("Upserted sys_user_cache: userId={}, userName={}, realName={}, userCode={}",
                userId, normalizedUserName, normalizedRealName, normalizedUserCode);
    }

    @Override
    public Map<Long, String> resolveUsernames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id != 0L)
                .collect(Collectors.toSet());
        if (normalizedUserIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> usernames = cacheUserMapper.selectBatchIds(normalizedUserIds).stream()
                .filter(entity -> StringUtils.hasText(entity.getUserName()))
                .collect(Collectors.toMap(
                        CacheUserEntity::getUserId,
                        entity -> entity.getUserName().trim(),
                        (left, ignored) -> left,
                        LinkedHashMap::new
                ));
        Long currentOperatorId = OperatorContext.getOperatorId();
        String currentUsername = normalize(OperatorContext.getOperatorName());
        if (currentUsername != null && normalizedUserIds.contains(currentOperatorId)) {
            usernames.put(currentOperatorId, currentUsername);
        }
        return usernames;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
