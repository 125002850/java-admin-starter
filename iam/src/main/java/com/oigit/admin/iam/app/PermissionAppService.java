package com.oigit.admin.iam.app;

import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.service.PermissionSnapshotService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionAppService {
    private final PermissionSnapshotService permissionSnapshotService;

    public PermissionAppService(PermissionSnapshotService permissionSnapshotService) {
        this.permissionSnapshotService = permissionSnapshotService;
    }

    @Transactional(readOnly = true)
    public PermissionSnapshot loadByStaffId(Long staffId) {
        return permissionSnapshotService.loadByStaffId(staffId);
    }
}
