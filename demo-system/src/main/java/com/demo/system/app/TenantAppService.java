package com.demo.system.app;

import com.demo.system.controller.dto.TenantCreateReqDTO;
import com.demo.system.controller.dto.TenantCreateRspDTO;
import com.demo.system.controller.dto.TenantDeleteReqDTO;
import com.demo.system.controller.dto.TenantUpdateReqDTO;
import com.demo.system.infra.entity.SysTenantGlobalEntity;
import com.demo.system.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAppService {

    private final TenantService tenantService;

    public TenantAppService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Transactional
    public TenantCreateRspDTO create(TenantCreateReqDTO reqDTO) {
        SysTenantGlobalEntity entity = tenantService.create(reqDTO.getTenantName());
        return new TenantCreateRspDTO(entity.getId());
    }

    @Transactional
    public void update(TenantUpdateReqDTO reqDTO) {
        tenantService.update(reqDTO.getId(), reqDTO.getTenantName());
    }

    @Transactional
    public void delete(TenantDeleteReqDTO reqDTO) {
        tenantService.delete(reqDTO.getId());
    }
}
