package com.example.admin.core.operator;

import org.springframework.lang.Nullable;

public final class CacheUserHolder {

    private static volatile CacheUserService service;

    private CacheUserHolder() {
    }

    public static void setService(CacheUserService service) {
        CacheUserHolder.service = service;
    }

    @Nullable
    public static String getUserName(Long userId) {
        CacheUserService s = service;
        if (s == null) {
            return null;
        }
        return s.getUserName(userId);
    }

    @Nullable
    public static String getRealName(Long userId) {
        CacheUserService s = service;
        if (s == null) {
            return null;
        }
        return s.getRealName(userId);
    }
}
