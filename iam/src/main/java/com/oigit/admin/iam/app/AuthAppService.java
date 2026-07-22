package com.oigit.admin.iam.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.annotation.OperationLog;
import com.oigit.admin.iam.config.IamProperties;
import com.oigit.admin.iam.dto.IamAuthDTO.ChangePasswordReqDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.ChangePasswordRspDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.LoginReqDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.LoginRspDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.LogoutReqDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.MeRspDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.RefreshReqDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.TokenRspDTO;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.LoginEventType;
import com.oigit.admin.iam.enums.LoginFailureReason;
import com.oigit.admin.iam.enums.LoginResult;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;
import com.oigit.admin.iam.infra.entity.IamRefreshTokenEntity;
import com.oigit.admin.iam.infra.entity.IamStaffEntity;
import com.oigit.admin.iam.security.CurrentIam;
import com.oigit.admin.iam.security.JwtService;
import com.oigit.admin.iam.security.TokenPair;
import com.oigit.admin.iam.service.IamStaffService;
import com.oigit.admin.iam.service.LoginLogService;
import com.oigit.admin.iam.service.PasswordPolicyService;
import com.oigit.admin.iam.service.PermissionSnapshot;
import com.oigit.admin.iam.service.PermissionSnapshotService;
import com.oigit.admin.iam.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAppService {

    private final IamStaffService staffService;
    private final PermissionSnapshotService permissionSnapshotService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final LoginLogService loginLogService;
    private final IamProperties iamProperties;

    public AuthAppService(
            IamStaffService staffService,
            PermissionSnapshotService permissionSnapshotService,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            LoginLogService loginLogService,
            IamProperties iamProperties
    ) {
        this.staffService = staffService;
        this.permissionSnapshotService = permissionSnapshotService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.loginLogService = loginLogService;
        this.iamProperties = iamProperties;
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_AUTH, action = OperationLogAction.LOGIN)
    public LoginRspDTO login(LoginReqDTO reqDTO) {
        IamStaffEntity staff = staffService.findByUsername(reqDTO.getUsername());
        if (staff == null || !passwordEncoder.matches(reqDTO.getPassword(), staff.getPasswordHash())) {
            loginLogService.record(
                    LoginEventType.LOGIN,
                    LoginResult.FAIL,
                    null,
                    reqDTO.getUsername(),
                    LoginFailureReason.BAD_CREDENTIALS,
                    null
            );
            delayFailure();
            throw new BizException(IamErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!staffService.isEnabled(staff)) {
            loginLogService.record(
                    LoginEventType.LOGIN,
                    LoginResult.FAIL,
                    staff.getId(),
                    staff.getUsername(),
                    LoginFailureReason.STAFF_DISABLED,
                    null
            );
            delayFailure();
            throw new BizException(IamErrorCode.AUTH_STAFF_DISABLED);
        }
        TokenPair tokenPair = issueTokenPair(staff.getId());
        PermissionSnapshot snapshot = permissionSnapshotService.loadByStaffId(staff.getId());
        loginLogService.record(LoginEventType.LOGIN, LoginResult.SUCCESS, staff.getId(), staff.getUsername(), null, tokenPair.accessTokenId());
        LoginRspDTO rspDTO = new LoginRspDTO();
        fillToken(rspDTO, tokenPair);
        MeRspDTO me = snapshot.toMeRspDTO();
        rspDTO.setStaff(me.getStaff());
        rspDTO.setMustChangePassword(me.isMustChangePassword());
        rspDTO.setRoles(me.getRoles());
        rspDTO.setPermissions(me.getPermissions());
        rspDTO.setMenus(me.getMenus());
        rspDTO.setDataScopeSummary(me.getDataScopeSummary());
        rspDTO.setPermissionFingerprint(me.getPermissionFingerprint());
        return rspDTO;
    }

    @Transactional
    public TokenRspDTO refresh(RefreshReqDTO reqDTO) {
        try {
            IamRefreshTokenEntity oldToken = refreshTokenService.validateForRefresh(reqDTO.getRefreshToken());
            IamStaffEntity staff = staffService.requireById(oldToken.getStaffId());
            if (!staffService.isEnabled(staff)) {
                refreshTokenService.revokeAllByStaffId(staff.getId(), "STAFF_DISABLED");
                throw new AuthenticationCredentialsNotFoundException("staff disabled");
            }
            RefreshTokenService.IssuedRefreshToken newRefreshToken = refreshTokenService.rotate(oldToken);
            JwtService.AccessToken accessToken = jwtService.issueAccessToken(staff.getId());
            loginLogService.record(LoginEventType.REFRESH, LoginResult.SUCCESS, staff.getId(), staff.getUsername(), null, accessToken.jwtId());
            TokenRspDTO rspDTO = new TokenRspDTO();
            rspDTO.setAccessToken(accessToken.value());
            rspDTO.setRefreshToken(newRefreshToken.plainToken());
            rspDTO.setAccessTokenExpiresAt(accessToken.expiresAt());
            return rspDTO;
        } catch (BizException ex) {
            loginLogService.record(
                    LoginEventType.REFRESH,
                    LoginResult.FAIL,
                    null,
                    null,
                    refreshFailureReason(ex),
                    null
            );
            delayFailure();
            throw new AuthenticationCredentialsNotFoundException("refresh token invalid");
        }
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_AUTH, action = OperationLogAction.LOGOUT)
    public void logout(LogoutReqDTO reqDTO) {
        if (reqDTO != null) {
            refreshTokenService.revokeCurrent(reqDTO.getRefreshToken(), "LOGOUT");
        }
        CurrentIam.principal().ifPresent(principal ->
                loginLogService.record(LoginEventType.LOGOUT, LoginResult.SUCCESS, principal.getStaffId(), principal.getUsername(), null, null)
        );
    }

    @Transactional(readOnly = true)
    public MeRspDTO me() {
        return CurrentIam.principal()
                .map(principal -> principal.getSnapshot().toMeRspDTO())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("not authenticated"));
    }

    @Transactional
    @OperationLog(module = OperationLogModule.IAM_AUTH, action = OperationLogAction.CHANGE_PASSWORD)
    public ChangePasswordRspDTO changePassword(ChangePasswordReqDTO reqDTO) {
        Long staffId = CurrentIam.staffIdOrNull();
        if (staffId == null) {
            throw new AuthenticationCredentialsNotFoundException("not authenticated");
        }
        IamStaffEntity staff = staffService.requireById(staffId);
        if (!passwordEncoder.matches(reqDTO.getOldPassword(), staff.getPasswordHash())) {
            throw new BizException(IamErrorCode.AUTH_OLD_PASSWORD_INVALID);
        }
        passwordPolicyService.validate(reqDTO.getNewPassword());
        staffService.updatePassword(staffId, passwordEncoder.encode(reqDTO.getNewPassword()), false);
        refreshTokenService.revokeAllByStaffId(staffId, "PASSWORD_CHANGED");
        TokenPair tokenPair = issueTokenPair(staffId);
        ChangePasswordRspDTO rspDTO = new ChangePasswordRspDTO();
        fillToken(rspDTO, tokenPair);
        rspDTO.setMustChangePassword(false);
        return rspDTO;
    }

    private TokenPair issueTokenPair(Long staffId) {
        JwtService.AccessToken accessToken = jwtService.issueAccessToken(staffId);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(staffId);
        return new TokenPair(accessToken.value(), refreshToken.plainToken(), accessToken.expiresAt(), accessToken.jwtId());
    }

    private void fillToken(TokenRspDTO dto, TokenPair tokenPair) {
        dto.setAccessToken(tokenPair.accessToken());
        dto.setRefreshToken(tokenPair.refreshToken());
        dto.setAccessTokenExpiresAt(tokenPair.accessTokenExpiresAt());
        dto.setTokenType("Bearer");
    }

    private void delayFailure() {
        long delay = Math.max(0, iamProperties.getFailureDelayMillis());
        if (delay == 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private LoginFailureReason refreshFailureReason(BizException exception) {
        if (exception.getErrorCode().getCode() == IamErrorCode.AUTH_REFRESH_TOKEN_EXPIRED.getCode()) {
            return LoginFailureReason.REFRESH_TOKEN_EXPIRED;
        }
        return LoginFailureReason.REFRESH_TOKEN_INVALID;
    }
}
