package com.liu.dev.auth;

import com.liu.dev.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登入（公開） */
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok("登入成功", authService.login(req));
    }

    /** 取得目前登入者資訊（需登入）；前端可用來驗證 token 是否仍有效 */
    @GetMapping("/me")
    public ApiResponse<UserInfo> me(Authentication authentication) {
        return ApiResponse.ok(authService.me(authentication.getName()));
    }
}
