package com.oigit.admin.staff.app;

import com.oigit.admin.staff.domain.gateway.StaffDirectoryGateway;
import com.oigit.admin.staff.domain.model.StaffDirectoryQuery;
import com.oigit.admin.staff.domain.model.StaffInfo;
import com.oigit.admin.staff.dto.req.query.StaffListAllReqDTO;
import com.oigit.admin.staff.dto.rsp.StaffInfoRspDTO;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "platform.sso-staff", name = "enabled", havingValue = "true")
public class StaffAppService {

    private final StaffDirectoryGateway staffDirectoryGateway;

    public StaffAppService(StaffDirectoryGateway staffDirectoryGateway) {
        this.staffDirectoryGateway = staffDirectoryGateway;
    }

    public List<StaffInfoRspDTO> listAll(StaffListAllReqDTO req) {
        StaffDirectoryQuery query = new StaffDirectoryQuery(
                req.getKeyword(),
                req.getStaffCode(),
                req.getUserName(),
                req.getAccount(),
                req.getMobile(),
                req.getSex()
        );
        List<StaffInfoRspDTO> list = staffDirectoryGateway.listAll(query).stream()
                .map(this::toRspDTO)
                .collect(Collectors.toMap(
                        StaffInfoRspDTO::getStaffCode,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values().stream()
                .toList();

        if (hasText(req.getKeyword())) {
            String keyword = req.getKeyword().toLowerCase();
            list = list.stream().filter(dto -> matchesKeyword(dto, keyword)).toList();
        }
        if (hasText(req.getSex())) {
            list = list.stream().filter(dto -> req.getSex().equals(dto.getSex())).toList();
        }
        return list;
    }

    private boolean matchesKeyword(StaffInfoRspDTO dto, String keyword) {
        return containsIgnoreCase(dto.getStaffCode(), keyword)
                || containsIgnoreCase(dto.getUserName(), keyword)
                || containsIgnoreCase(dto.getAccount(), keyword)
                || containsIgnoreCase(dto.getMobile(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private StaffInfoRspDTO toRspDTO(StaffInfo staffInfo) {
        StaffInfoRspDTO dto = new StaffInfoRspDTO();
        dto.setStaffCode(staffInfo.staffCode());
        dto.setSsoAccountId(staffInfo.ssoAccountId());
        dto.setUserName(staffInfo.userName());
        dto.setSex(staffInfo.sex());
        dto.setAccount(staffInfo.account());
        dto.setMobile(staffInfo.mobile());
        return dto;
    }
}
