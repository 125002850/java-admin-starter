package com.oigit.admin.iam.domain.service;

import static com.oigit.admin.iam.domain.service.IamDomainRules.hasText;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.IamStatus;
import com.oigit.admin.iam.enums.MenuType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IamMenuService {
    private final MenuRepository menuRepository;

    public IamMenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public IamMenu requireById(Long menuId) {
        IamMenu menu = menuRepository.findById(menuId);
        if (menu == null) {
            throw new BizException(IamErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    public List<IamMenu> listAll(String keyword) {
        return menuRepository.listAll(keyword);
    }

    public IamMenu create(IamMenu menu) {
        if (menu.getParentId() != null) {
            requireById(menu.getParentId());
        }
        validate(menu);
        assertUnique(menu, null);
        normalize(menu);
        menuRepository.save(menu);
        return menu;
    }

    public void update(IamMenu changes) {
        IamMenu menu = requireById(changes.getId());
        validateParent(menu.getId(), changes.getParentId());
        validate(changes);
        assertUnique(changes, menu.getId());
        normalize(changes);
        menu.setParentId(changes.getParentId());
        menu.setMenuCode(changes.getMenuCode());
        menu.setMenuName(changes.getMenuName());
        menu.setMenuType(changes.getMenuType());
        menu.setRoutePath(changes.getRoutePath());
        menu.setComponentPath(changes.getComponentPath());
        menu.setIcon(changes.getIcon());
        menu.setSortOrder(changes.getSortOrder());
        menu.setHidden(changes.getHidden());
        menu.setCached(changes.getCached());
        menu.setStatus(changes.getStatus());
        menu.setPermissionCode(changes.getPermissionCode());
        menu.setRemark(changes.getRemark());
        menuRepository.save(menu);
    }

    public void updateStatus(Long id, IamStatus status) {
        IamMenu menu = requireById(id);
        menu.setStatus(status);
        menuRepository.save(menu);
    }

    public void delete(Long id) {
        requireById(id);
        if (menuRepository.hasChildren(id)) {
            throw new BizException(IamErrorCode.MENU_HAS_CHILDREN);
        }
        menuRepository.delete(id);
    }

    private void validate(IamMenu menu) {
        if (menu.getMenuType() == MenuType.BUTTON && !hasText(menu.getPermissionCode())) {
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
        IamMenu parent = requireById(parentId);
        Map<Long, IamMenu> all =
                menuRepository.listAll(null).stream()
                        .collect(Collectors.toMap(IamMenu::getId, item -> item));
        Set<Long> visited = new LinkedHashSet<>();
        Long current = parent.getParentId();
        while (current != null) {
            if (current.equals(menuId) || !visited.add(current)) {
                throw new BizException(IamErrorCode.MENU_PARENT_INVALID);
            }
            IamMenu ancestor = all.get(current);
            if (ancestor == null) {
                throw new BizException(IamErrorCode.MENU_NOT_FOUND);
            }
            current = ancestor.getParentId();
        }
    }

    private void assertUnique(IamMenu menu, Long excludeId) {
        if (menuRepository.codeExists(menu.getMenuCode(), excludeId)) {
            throw new BizException(IamErrorCode.MENU_CODE_DUPLICATED);
        }
        if (hasText(menu.getPermissionCode())
                && menuRepository.permissionExists(menu.getPermissionCode(), excludeId)) {
            throw new BizException(IamErrorCode.MENU_PERMISSION_DUPLICATED);
        }
    }

    private void normalize(IamMenu menu) {
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(IamStatus.ENABLED);
        }
        menu.setHidden(Boolean.TRUE.equals(menu.getHidden()));
        menu.setCached(Boolean.TRUE.equals(menu.getCached()));
        if (!hasText(menu.getPermissionCode())) {
            menu.setPermissionCode(null);
        }
    }
}
