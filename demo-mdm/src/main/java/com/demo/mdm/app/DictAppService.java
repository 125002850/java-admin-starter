package com.demo.mdm.app;

import com.demo.mdm.controller.dto.DictItemRspDTO;
import com.demo.mdm.controller.dto.DictListReqDTO;
import com.demo.mdm.infra.entity.DictItemEntity;
import com.demo.mdm.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictAppService {

    private final DictService dictService;

    public DictAppService(DictService dictService) {
        this.dictService = dictService;
    }

    @Transactional(readOnly = true)
    public List<DictItemRspDTO> listItemsByType(DictListReqDTO reqDTO) {
        List<DictItemEntity> items = dictService.listItemsByType(reqDTO.getTenantId(), reqDTO.getDictTypeCode());
        return items.stream()
                .map(item -> new DictItemRspDTO(
                        item.getId(),
                        item.getDictTypeCode(),
                        item.getDictItemCode(),
                        item.getDictItemName()
                ))
                .collect(Collectors.toList());
    }
}
