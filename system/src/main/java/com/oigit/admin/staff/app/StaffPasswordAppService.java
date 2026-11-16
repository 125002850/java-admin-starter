package com.oigit.admin.staff.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.staff.dto.req.StaffPasswordChangeReqDTO;
import com.oigit.admin.staff.enums.StaffErrorCode;
import com.oigit.admin.staff.domain.gateway.StaffPasswordGateway;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StaffPasswordAppService {

    private final StaffPasswordGateway provider;

    public StaffPasswordAppService(StaffPasswordGateway provider) {
        this.provider = provider;
    }

    public void change(StaffPasswordChangeReqDTO request) {
        if (OperatorContext.getOperatorId() == null) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        String account = OperatorContext.getOperatorUserCode();
        if (!StringUtils.hasText(account)) {
            throw new BizException(StaffErrorCode.STAFF_PASSWORD_IDENTITY_UNAVAILABLE);
        }
        if (!StringUtils.hasText(request.oldPassword()) || !StringUtils.hasText(request.newPassword())
                || !StringUtils.hasText(request.repeatPassword())) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        if (!request.newPassword().equals(request.repeatPassword())) {
            throw new BizException(StaffErrorCode.STAFF_PASSWORD_CONFIRMATION_MISMATCH);
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BizException(StaffErrorCode.STAFF_PASSWORD_UNCHANGED);
        }
        provider.change(account, request.oldPassword(), request.newPassword(), request.repeatPassword());
    }
}
