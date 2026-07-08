package com.demo.iam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.exception.BizException;
import com.demo.core.operator.OperatorContext;
import com.demo.iam.dto.IamMenuDTO.MenuCreateReqDTO;
import com.demo.iam.dto.IamMenuDTO.MenuUpdateReqDTO;
import com.demo.iam.enums.IamErrorCode;
import com.demo.iam.enums.IamStatus;
import com.demo.iam.enums.MenuType;
import com.demo.iam.infra.entity.IamMenuEntity;
import com.demo.iam.infra.mapper.IamMenuMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IamMenuService {

    private final IamMenuMapper menuMapper;

    public IamMenuService(IamMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public IamMenuEntity requireById(Long menuId) {
        IamMenuEntity menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new BizException(IamErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    public List<IamMenuEntity> listAll(String keyword) {
        var query = Wrappers.<IamMenuEntity>lambdaQuery()
                .orderByAsc(IamMenuEntity::getSortOrder)
                .orderByAsc(IamMenuEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(IamMenuEntity::getMenuCode, keyword)
                    .or()
                    .like(IamMenuEntity::getMenuName, keyword)
                    .or()
                    .like(IamMenuEntity::getPermissionCode, keyword));
        }
        return menuMapper.selectList(query);
    }

    public IamMenuEntity create(MenuCreateReqDTO reqDTO) {
        if (reqDTO.parentId != null) {
            requireById(reqDTO.parentId);
        }
        validate(reqDTO.menuType, reqDTO.permissionCode);
        assertUnique(reqDTO.menuCode, reqDTO.permissionCode, null);
        IamMenuEntity entity = new IamMenuEntity();
        fill(entity, reqDTO);
        entity.setDeleted(0L);
        menuMapper.insert(entity);
        return entity;
    }

    public void update(MenuUpdateReqDTO reqDTO) {
        IamMenuEntity entity = requireById(reqDTO.menuId);
        validateParent(reqDTO.menuId, reqDTO.parentId);
        validate(reqDTO.menuType, reqDTO.permissionCode);
        assertUnique(reqDTO.menuCode, reqDTO.permissionCode, reqDTO.menuId);
        fill(entity, reqDTO);
        menuMapper.updateById(entity);
    }

    public void updateStatus(Long menuId, IamStatus status) {
        IamMenuEntity entity = requireById(menuId);
        entity.setStatus(status);
        menuMapper.updateById(entity);
    }

    public void delete(Long menuId) {
        requireById(menuId);
        Long childCount = menuMapper.selectCount(
                Wrappers.<IamMenuEntity>lambdaQuery().eq(IamMenuEntity::getParentId, menuId)
        );
        if (childCount != null && childCount > 0L) {
            throw new BizException(IamErrorCode.MENU_HAS_CHILDREN);
        }
        menuMapper.softDeleteById(menuId, operatorId());
    }

    private void validate(MenuType menuType, String permissionCode) {
        if (menuType == MenuType.BUTTON && !StringUtils.hasText(permissionCode)) {
            throw new BizException(IamErrorCode.MENU_BUTTON_PERMISSION_REQUIRED);
        }
    }

    private void validateParent(Long menuId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(menuId)) {
            throw new BizException(IamErrorCode.MENU_PARENT_INVALID);
        }
        IamMenuEntity parent = requireById(parentId);
        Set<Long> visited = new LinkedHashSet<>();
        Long currentParentId = parent.getParentId();
        while (currentParentId != null) {
            if (currentParentId.equals(menuId) || !visited.add(currentParentId)) {
                throw new BizException(IamErrorCode.MENU_PARENT_INVALID);
            }
            currentParentId = requireById(currentParentId).getParentId();
        }
    }

    private void assertUnique(String menuCode, String permissionCode, Long excludeId) {
        var codeQuery = Wrappers.<IamMenuEntity>lambdaQuery().eq(IamMenuEntity::getMenuCode, menuCode);
        if (excludeId != null) {
            codeQuery.ne(IamMenuEntity::getId, excludeId);
        }
        Long codeCount = menuMapper.selectCount(codeQuery);
        if (codeCount != null && codeCount > 0L) {
            throw new BizException(IamErrorCode.MENU_CODE_DUPLICATED);
        }
        if (!StringUtils.hasText(permissionCode)) {
            return;
        }
        var permissionQuery = Wrappers.<IamMenuEntity>lambdaQuery().eq(IamMenuEntity::getPermissionCode, permissionCode);
        if (excludeId != null) {
            permissionQuery.ne(IamMenuEntity::getId, excludeId);
        }
        Long permissionCount = menuMapper.selectCount(permissionQuery);
        if (permissionCount != null && permissionCount > 0L) {
            throw new BizException(IamErrorCode.MENU_PERMISSION_DUPLICATED);
        }
    }

    private void fill(IamMenuEntity entity, MenuCreateReqDTO reqDTO) {
        entity.setParentId(reqDTO.parentId);
        entity.setMenuCode(reqDTO.menuCode);
        entity.setMenuName(reqDTO.menuName);
        entity.setMenuType(reqDTO.menuType);
        entity.setRoutePath(reqDTO.routePath);
        entity.setComponentPath(reqDTO.componentPath);
        entity.setIcon(reqDTO.icon);
        entity.setSortOrder(reqDTO.sortOrder == null ? 0 : reqDTO.sortOrder);
        entity.setHidden(Boolean.TRUE.equals(reqDTO.hidden));
        entity.setCached(Boolean.TRUE.equals(reqDTO.cached));
        entity.setStatus(reqDTO.status == null ? IamStatus.ENABLED : reqDTO.status);
        entity.setPermissionCode(StringUtils.hasText(reqDTO.permissionCode) ? reqDTO.permissionCode : null);
        entity.setRemark(reqDTO.remark);
    }

    private Long operatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? 0L : operatorId;
    }
}
