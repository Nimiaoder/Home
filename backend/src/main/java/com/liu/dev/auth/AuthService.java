package com.liu.dev.auth;

import com.liu.dev.common.BusinessException;
import com.liu.dev.security.JwtService;
import com.liu.dev.user.UserAccount;
import com.liu.dev.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(AuthenticationManager authManager, JwtService jwtService, UserService userService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public LoginResult login(LoginRequest req) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (DisabledException e) {
            throw new BusinessException("此帳號已停用，請聯絡管理員");
        } catch (BadCredentialsException e) {
            throw new BusinessException("帳號或密碼錯誤");
        }
        UserAccount u = userService.findByUsername(req.username())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));
        return new LoginResult(jwtService.generate(u.getUsername()), jwtService.expireSeconds(), toInfo(u));
    }

    public UserInfo me(String username) {
        return userService.findByUsername(username).map(this::toInfo)
                .orElseThrow(() -> new BusinessException("使用者不存在"));
    }

    private UserInfo toInfo(UserAccount u) {
        return new UserInfo(u.getUsername(), u.getNickname(), u.getRole(), u.getCreatedAt());
    }
}
