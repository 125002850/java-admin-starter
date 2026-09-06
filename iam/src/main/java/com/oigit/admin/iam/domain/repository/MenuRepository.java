package com.oigit.admin.iam.domain.repository;

import com.oigit.admin.iam.domain.model.IamMenu;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MenuRepository {
    IamMenu findById(Long menuId);

    Map<Long, IamMenu> findByIds(Collection<Long> menuIds);

    List<IamMenu> listAll(String keyword);

    void save(IamMenu menu);

    void delete(Long menuId);

    boolean codeExists(String menuCode, Long excludeId);

    boolean permissionExists(String permissionCode, Long excludeId);

    boolean hasChildren(Long menuId);
}
