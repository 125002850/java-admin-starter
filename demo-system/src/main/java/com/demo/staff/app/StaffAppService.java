package com.demo.staff.app;

import com.oigit.appcik.CIClient;
import com.oigit.appcik.core.service.sso.model.StaffInfoPageQueryReq;
import com.oigit.appcik.core.service.sso.model.StaffInfoRsp;
import com.oigit.common.page.PageInfo;
import com.demo.staff.controller.dto.StaffInfoRspDTO;
import com.demo.staff.controller.dto.query.StaffListAllReqDTO;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "platform.sso-staff", name = "enabled", havingValue = "true")
public class StaffAppService {

    private static final int CIK_PAGE_SIZE = 500;

    private final CIClient ciClient;

    public StaffAppService(CIClient ciClient) {
        this.ciClient = ciClient;
    }

    public List<StaffInfoRspDTO> listAll(StaffListAllReqDTO req) {
        List<StaffInfoRsp> cikRows = fetchAllFromCik(req);
        if (cikRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<StaffInfoRspDTO> list = cikRows.stream()
                .map(this::toRspDTO)
                .collect(Collectors.toMap(
                        StaffInfoRspDTO::getStaffCode,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values().stream()
                .collect(Collectors.toList());

        if (StringUtils.hasText(req.getKeyword())) {
            String kw = req.getKeyword().toLowerCase();
            list = list.stream()
                    .filter(dto -> matchesKeyword(dto, kw))
                    .collect(Collectors.toList());
        }
        if (StringUtils.hasText(req.getSex())) {
            list = list.stream()
                    .filter(dto -> req.getSex().equals(dto.getSex()))
                    .collect(Collectors.toList());
        }

        return list;
    }

    private List<StaffInfoRsp> fetchAllFromCik(StaffListAllReqDTO req) {
        List<StaffInfoRsp> rows = new ArrayList<>();
        long total = -1L;
        int page = 1;

        while (true) {
            StaffInfoPageQueryReq cikReq = buildCikReq(req, page);
            PageInfo<StaffInfoRsp> cikResult = ciClient.sso().staff().pageQuery(cikReq);
            if (cikResult == null || cikResult.getList() == null || cikResult.getList().isEmpty()) {
                break;
            }

            List<StaffInfoRsp> pageRows = cikResult.getList();
            rows.addAll(pageRows);
            if (cikResult.getTotal() > 0) {
                total = cikResult.getTotal();
            }
            if ((total > 0 && rows.size() >= total) || pageRows.size() < CIK_PAGE_SIZE) {
                break;
            }
            page++;
        }

        return rows;
    }

    private StaffInfoPageQueryReq buildCikReq(StaffListAllReqDTO req, int page) {
        StaffInfoPageQueryReq cikReq = new StaffInfoPageQueryReq();
        cikReq.setPageSize(CIK_PAGE_SIZE);
        cikReq.setPage(page);
        cikReq.setStatus(1);
        cikReq.setStaffStatus(0);

        if (StringUtils.hasText(req.getKeyword())) {
            String kw = req.getKeyword();
            cikReq.setStaffCode(kw);
        } else {
            if (StringUtils.hasText(req.getStaffCode())) {
                cikReq.setStaffCode(req.getStaffCode());
            }
            if (StringUtils.hasText(req.getUserName())) {
                cikReq.setName(req.getUserName());
            }
            if (StringUtils.hasText(req.getAccount())) {
                cikReq.setAccount(req.getAccount());
            }
        }
        if (StringUtils.hasText(req.getMobile())) {
            cikReq.setMobile(req.getMobile());
        }

        return cikReq;
    }

    private boolean matchesKeyword(StaffInfoRspDTO dto, String kw) {
        return containsIgnoreCase(dto.getStaffCode(), kw)
                || containsIgnoreCase(dto.getUserName(), kw)
                || containsIgnoreCase(dto.getAccount(), kw)
                || containsIgnoreCase(dto.getMobile(), kw);
    }

    private boolean containsIgnoreCase(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }

    private StaffInfoRspDTO toRspDTO(StaffInfoRsp cikRsp) {
        StaffInfoRspDTO dto = new StaffInfoRspDTO();
        dto.setStaffCode(cikRsp.getStaffCode());
        dto.setSsoAccountId(cikRsp.getSsoAccountId());
        dto.setUserName(cikRsp.getUserName());
        dto.setSex(cikRsp.getSex());
        dto.setAccount(cikRsp.getAccount());
        dto.setMobile(cikRsp.getMobile());
        return dto;
    }
}
