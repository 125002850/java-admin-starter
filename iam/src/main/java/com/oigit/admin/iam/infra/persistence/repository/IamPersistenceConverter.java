package com.oigit.admin.iam.infra.persistence.repository;

import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamLoginLog;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.model.IamOperationLog;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.infra.persistence.entity.IamDeptEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamLoginLogEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamMenuEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamOperationLogEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamRefreshTokenEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamRoleEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamStaffEntity;

final class IamPersistenceConverter {
    private IamPersistenceConverter() {}

    public static IamStaff toDomain(IamStaffEntity source) {
        if (source == null) {
            return null;
        }
        IamStaff target = new IamStaff();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setUsername(source.getUsername());
        target.setPasswordHash(source.getPasswordHash());
        target.setStaffCode(source.getStaffCode());
        target.setStaffName(source.getStaffName());
        target.setDeptId(source.getDeptId());
        target.setPhone(source.getPhone());
        target.setEmail(source.getEmail());
        target.setAvatar(source.getAvatar());
        target.setStatus(source.getStatus());
        target.setMustChangePassword(source.getMustChangePassword());
        target.setPasswordUpdatedTime(source.getPasswordUpdatedTime());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        return target;
    }

    public static IamStaffEntity toEntity(IamStaff source) {
        if (source == null) {
            return null;
        }
        IamStaffEntity target = new IamStaffEntity();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setUsername(source.getUsername());
        target.setPasswordHash(source.getPasswordHash());
        target.setStaffCode(source.getStaffCode());
        target.setStaffName(source.getStaffName());
        target.setDeptId(source.getDeptId());
        target.setPhone(source.getPhone());
        target.setEmail(source.getEmail());
        target.setAvatar(source.getAvatar());
        target.setStatus(source.getStatus());
        target.setMustChangePassword(source.getMustChangePassword());
        target.setPasswordUpdatedTime(source.getPasswordUpdatedTime());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        if (source.getId() == null) {
            target.setDeleted(0L);
        }
        return target;
    }

    public static IamDept toDomain(IamDeptEntity source) {
        if (source == null) {
            return null;
        }
        IamDept target = new IamDept();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setParentId(source.getParentId());
        target.setDeptCode(source.getDeptCode());
        target.setDeptName(source.getDeptName());
        target.setFullPath(source.getFullPath());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        return target;
    }

    public static IamDeptEntity toEntity(IamDept source) {
        if (source == null) {
            return null;
        }
        IamDeptEntity target = new IamDeptEntity();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setParentId(source.getParentId());
        target.setDeptCode(source.getDeptCode());
        target.setDeptName(source.getDeptName());
        target.setFullPath(source.getFullPath());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        if (source.getId() == null) {
            target.setDeleted(0L);
        }
        return target;
    }

    public static IamRole toDomain(IamRoleEntity source) {
        if (source == null) {
            return null;
        }
        IamRole target = new IamRole();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setRoleCode(source.getRoleCode());
        target.setRoleName(source.getRoleName());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setDataScopeType(source.getDataScopeType());
        target.setSystemBuiltIn(source.getSystemBuiltIn());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        return target;
    }

    public static IamRoleEntity toEntity(IamRole source) {
        if (source == null) {
            return null;
        }
        IamRoleEntity target = new IamRoleEntity();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setRoleCode(source.getRoleCode());
        target.setRoleName(source.getRoleName());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setDataScopeType(source.getDataScopeType());
        target.setSystemBuiltIn(source.getSystemBuiltIn());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        if (source.getId() == null) {
            target.setDeleted(0L);
        }
        return target;
    }

    public static IamMenu toDomain(IamMenuEntity source) {
        if (source == null) {
            return null;
        }
        IamMenu target = new IamMenu();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setParentId(source.getParentId());
        target.setMenuCode(source.getMenuCode());
        target.setMenuName(source.getMenuName());
        target.setMenuType(source.getMenuType());
        target.setRoutePath(source.getRoutePath());
        target.setComponentPath(source.getComponentPath());
        target.setIcon(source.getIcon());
        target.setSortOrder(source.getSortOrder());
        target.setHidden(source.getHidden());
        target.setCached(source.getCached());
        target.setStatus(source.getStatus());
        target.setPermissionCode(source.getPermissionCode());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        return target;
    }

    public static IamMenuEntity toEntity(IamMenu source) {
        if (source == null) {
            return null;
        }
        IamMenuEntity target = new IamMenuEntity();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setParentId(source.getParentId());
        target.setMenuCode(source.getMenuCode());
        target.setMenuName(source.getMenuName());
        target.setMenuType(source.getMenuType());
        target.setRoutePath(source.getRoutePath());
        target.setComponentPath(source.getComponentPath());
        target.setIcon(source.getIcon());
        target.setSortOrder(source.getSortOrder());
        target.setHidden(source.getHidden());
        target.setCached(source.getCached());
        target.setStatus(source.getStatus());
        target.setPermissionCode(source.getPermissionCode());
        target.setRemark(source.getRemark());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        if (source.getId() == null) {
            target.setDeleted(0L);
        }
        return target;
    }

    public static IamRefreshToken toDomain(IamRefreshTokenEntity source) {
        if (source == null) {
            return null;
        }
        IamRefreshToken target = new IamRefreshToken();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setStaffId(source.getStaffId());
        target.setTokenHash(source.getTokenHash());
        target.setSessionId(source.getSessionId());
        target.setDeviceId(source.getDeviceId());
        target.setIp(source.getIp());
        target.setUserAgent(source.getUserAgent());
        target.setIssuedTime(source.getIssuedTime());
        target.setExpireTime(source.getExpireTime());
        target.setLastUsedTime(source.getLastUsedTime());
        target.setRevokedTime(source.getRevokedTime());
        target.setRevokeReason(source.getRevokeReason());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        return target;
    }

    public static IamRefreshTokenEntity toEntity(IamRefreshToken source) {
        if (source == null) {
            return null;
        }
        IamRefreshTokenEntity target = new IamRefreshTokenEntity();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setStaffId(source.getStaffId());
        target.setTokenHash(source.getTokenHash());
        target.setSessionId(source.getSessionId());
        target.setDeviceId(source.getDeviceId());
        target.setIp(source.getIp());
        target.setUserAgent(source.getUserAgent());
        target.setIssuedTime(source.getIssuedTime());
        target.setExpireTime(source.getExpireTime());
        target.setLastUsedTime(source.getLastUsedTime());
        target.setRevokedTime(source.getRevokedTime());
        target.setRevokeReason(source.getRevokeReason());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        if (source.getId() == null) {
            target.setDeleted(0L);
        }
        return target;
    }

    public static IamLoginLog toDomain(IamLoginLogEntity source) {
        if (source == null) {
            return null;
        }
        return new IamLoginLog(
                source.getId(),
                source.getStaffId(),
                source.getUsername(),
                source.getEventType(),
                source.getResult(),
                source.getFailureReason(),
                source.getIp(),
                source.getUserAgent(),
                source.getTokenId(),
                source.getOperationTime());
    }

    public static IamLoginLogEntity toEntity(IamLoginLog source) {
        if (source == null) {
            return null;
        }
        IamLoginLogEntity target = new IamLoginLogEntity();
        target.setId(source.id());
        target.setStaffId(source.staffId());
        target.setUsername(source.username());
        target.setEventType(source.eventType());
        target.setResult(source.result());
        target.setFailureReason(source.failureReason());
        target.setIp(source.ip());
        target.setUserAgent(source.userAgent());
        target.setTokenId(source.tokenId());
        target.setOperationTime(source.operationTime());
        if (source.id() == null) {
            target.setDeleted(0L);
        }
        return target;
    }

    public static IamOperationLog toDomain(IamOperationLogEntity source) {
        if (source == null) {
            return null;
        }
        return new IamOperationLog(
                source.getId(),
                source.getOperatorId(),
                source.getOperatorUsername(),
                source.getOperatorStaffName(),
                source.getModule(),
                source.getAction(),
                source.getRequestPath(),
                source.getHttpMethod(),
                source.getRequestSummary(),
                source.getResponseSummary(),
                source.getSuccess(),
                source.getErrorMessage(),
                source.getIp(),
                source.getUserAgent(),
                source.getCostMillis(),
                source.getOperationTime());
    }
}
