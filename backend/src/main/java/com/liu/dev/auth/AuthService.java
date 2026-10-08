package com.liu.dev.auth;

import com.liu.dev.common.BusinessException;
import com.liu.dev.security.JwtService;
import com.liu.dev.sysparam.SysParamService;
import com.liu.dev.user.UserAccount;
import com.liu.dev.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final SysParamService sysParamService;

    public AuthService(AuthenticationManager authManager, JwtService jwtService, UserService userService,
                       SysParamService sysParamService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.sysParamService = sysParamService;
    }

    public LoginResult login(LoginRequest req) {
        log.info("使用者 {} 嘗試登入", req.username());
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (DisabledException e) {
            log.warn("登入失敗：帳號 {} 已停用", req.username());
            throw new BusinessException("此帳號已停用，請聯絡管理員");
        } catch (BadCredentialsException e) {
            log.warn("登入失敗：帳號 {} 帳密錯誤", req.username());
            throw new BusinessException("帳號或密碼錯誤");
        }
        UserAccount u = userService.findByUsername(req.username())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));
        log.info("使用者 {} 登入成功（角色 {}）", u.getUsername(), u.getRole());
        return new LoginResult(jwtService.generate(u.getUsername()), jwtService.expireSeconds(), toInfo(u));
    }

    public boolean isRegisterEnabled() {
        return sysParamService.getBoolean(SysParamService.REGISTER_ENABLED, false);
    }

    public UserInfo register(RegisterRequest req) {
        log.info("使用者 {} 嘗試註冊", req.username());
        if (!isRegisterEnabled()) {
            log.warn("註冊失敗：系統未開放註冊（{}）", req.username());
            throw new BusinessException("目前未開放註冊帳號");
        }
        return toInfo(userService.registerUser(req.username().trim(), req.password(), req.nickname()));
    }

    public UserInfo me(String username) {
        return userService.findByUsername(username).map(this::toInfo)
                .orElseThrow(() -> new BusinessException("使用者不存在"));
    }

    private UserInfo toInfo(UserAccount u) {
        return new UserInfo(u.getUsername(), u.getNickname(), u.getRole(), u.getCreatedAt());
    }
}
