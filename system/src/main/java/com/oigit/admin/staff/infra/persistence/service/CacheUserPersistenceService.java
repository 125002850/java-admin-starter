package com.oigit.admin.staff.infra.persistence.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oigit.admin.core.operator.OperatorUserCacheWriter;
import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.staff.infra.persistence.entity.CacheUserEntity;

public interface CacheUserPersistenceService extends IService<CacheUserEntity>,
        OperatorUserCacheWriter, OperatorUsernameResolver {
}
