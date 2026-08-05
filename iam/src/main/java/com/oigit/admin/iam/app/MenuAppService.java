package com.oigit.admin.iam.app;

import com.oigit.admin.iam.annotation.OperationLog;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuCreateReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuRspDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuTreeReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuUpdateReqDTO;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;
import com.oigit.admin.iam.infra.entity.IamMenuEntity;
import com.oigit.admin.iam.service.IamMenuService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuAppService {

    private final IamMenuService menuService;

    public MenuAppService(IamMenuService menuService) {
        this.menuService = menuService;
    }

    @Transactional(readOnly = true)
    public List<MenuRspDTO> tree(MenuTreeReqDTO reqDTO) {
        List<IamMenuEntity> menus = menuService.listAll(reqDTO == null ? null : reqDTO.keyword);
        return buildTree(menus);
    }

    @Transactional(readOnly = true)
    public MenuRspDTO detail(Long menuId) {
        IamMenuEntity menu = menuService.requireById(menuId);
        return toRsp(menu);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_MENU, action = OperationLogAction.CREATE)
    public void create(MenuCreateReqDTO reqDTO) {
        menuService.create(reqDTO);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_MENU, action = OperationLogAction.UPDATE)
    public void update(MenuUpdateReqDTO reqDTO) {
        menuService.update(reqDTO);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_MENU, action = OperationLogAction.DELETE)
    public void delete(Long menuId) {
        menuService.delete(menuId);
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_MENU, action = OperationLogAction.STATUS_UPDATE)
    public void updateStatus(MenuStatusUpdateReqDTO reqDTO) {
        menuService.updateStatus(reqDTO.menuId, reqDTO.status);
    }

    private List<MenuRspDTO> buildTree(List<IamMenuEntity> menus) {
        Map<Long, MenuRspDTO> byId = new LinkedHashMap<>();
        menus.stream()
                .sorted(Comparator.comparing((IamMenuEntity item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(IamMenuEntity::getId))
                .forEach(menu -> byId.put(menu.getId(), toRsp(menu)));
        List<MenuRspDTO> roots = new ArrayList<>();
        for (IamMenuEntity menu : menus) {
            MenuRspDTO node = byId.get(menu.getId());
            if (menu.getParentId() != null && byId.containsKey(menu.getParentId())) {
                byId.get(menu.getParentId()).children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private MenuRspDTO toRsp(IamMenuEntity entity) {
        MenuRspDTO dto = new MenuRspDTO();
        dto.menuId = entity.getId();
        dto.parentId = entity.getParentId();
        dto.menuCode = entity.getMenuCode();
        dto.menuKey = entity.getMenuCode();
        dto.menuName = entity.getMenuName();
        dto.menuType = entity.getMenuType();
        dto.routePath = entity.getRoutePath();
        dto.componentPath = entity.getComponentPath();
        dto.icon = entity.getIcon();
        dto.sortOrder = entity.getSortOrder();
        dto.hidden = entity.getHidden();
        dto.cached = entity.getCached();
        dto.status = entity.getStatus();
        dto.permissionCode = entity.getPermissionCode();
        dto.remark = entity.getRemark();
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateById(entity.getCreateBy());
        dto.setUpdateById(entity.getUpdateBy());
        return dto;
    }
}
