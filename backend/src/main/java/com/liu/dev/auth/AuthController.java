package com.liu.dev.auth;

import com.liu.dev.common.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登入（公開） */
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest req) {
        log.debug("*****AuthController.login*****");
        return ApiResponse.ok("登入成功", authService.login(req));
    }

    /** 是否開放註冊（公開）；前端註冊頁用來顯示提示 */
    @GetMapping("/register-enabled")
    public ApiResponse<Boolean> registerEnabled() {
        log.debug("*****AuthController.registerEnabled*****");
        return ApiResponse.ok(authService.isRegisterEnabled());
    }

    /** 註冊（公開）；是否開放由系統參數 REGISTER_ENABLED 控制，註冊者一律為一般使用者 */
    @PostMapping("/register")
    public ApiResponse<UserInfo> register(@Valid @RequestBody RegisterRequest req) {
        log.debug("*****AuthController.register*****");
        return ApiResponse.ok("註冊成功，請登入", authService.register(req));
    }

    /** 取得目前登入者資訊（需登入）；前端可用來驗證 token 是否仍有效 */
    @GetMapping("/me")
    public ApiResponse<UserInfo> me(Authentication authentication) {
        log.debug("*****AuthController.me*****");
        return ApiResponse.ok(authService.me(authentication.getName()));
    }
}
