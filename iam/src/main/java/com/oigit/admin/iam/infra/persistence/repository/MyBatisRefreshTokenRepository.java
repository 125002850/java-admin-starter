package com.oigit.admin.iam.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.domain.repository.RefreshTokenRepository;
import com.oigit.admin.iam.infra.persistence.entity.IamRefreshTokenEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamRefreshTokenMapper;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class MyBatisRefreshTokenRepository implements RefreshTokenRepository {
    private final IamRefreshTokenMapper mapper;

    public MyBatisRefreshTokenRepository(IamRefreshTokenMapper mapper) {
        this.mapper = mapper;
    }

    public IamRefreshToken findByHash(String hash) {
        return IamPersistenceConverter.toDomain(
                mapper.selectOne(
                        Wrappers.<IamRefreshTokenEntity>lambdaQuery()
                                .eq(IamRefreshTokenEntity::getTokenHash, hash)
                                .last("limit 1")));
    }

    public void save(IamRefreshToken token) {
        IamRefreshTokenEntity entity = IamPersistenceConverter.toEntity(token);
        if (entity.getId() == null) {
            mapper.insert(entity);
            token.setId(entity.getId());
        } else {
            mapper.updateById(entity);
        }
        token.setVersion(entity.getVersion());
    }

    public boolean revokeIfActive(Long id, LocalDateTime now, String reason) {
        return mapper.update(
                        Wrappers.<IamRefreshTokenEntity>lambdaUpdate()
                                .eq(IamRefreshTokenEntity::getId, id)
                                .isNull(IamRefreshTokenEntity::getRevokedTime)
                                .set(IamRefreshTokenEntity::getRevokedTime, now)
                                .set(IamRefreshTokenEntity::getRevokeReason, reason))
                > 0;
    }

    public void revokeAllByStaffId(Long staffId, LocalDateTime now, String reason) {
        mapper.update(
                Wrappers.<IamRefreshTokenEntity>lambdaUpdate()
                        .eq(IamRefreshTokenEntity::getStaffId, staffId)
                        .isNull(IamRefreshTokenEntity::getRevokedTime)
                        .set(IamRefreshTokenEntity::getRevokedTime, now)
                        .set(IamRefreshTokenEntity::getRevokeReason, reason));
    }
}
