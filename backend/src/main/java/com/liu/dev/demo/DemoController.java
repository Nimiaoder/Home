package com.liu.dev.demo;

import com.liu.dev.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 範例 Controller：展示 detail 可為 Map / List / String，以及權限控管。
 * 所有 /api/** 預設都需登入（見 SecurityConfig），不用每支 Controller 另外處理。
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/hello")
    public ApiResponse<Map<String, Object>> hello(Authentication auth) {
        return ApiResponse.ok(Map.of("hello", auth.getName(), "roles", auth.getAuthorities().toString()));
    }

    @PostMapping("/list")
    public ApiResponse<List<String>> list(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(List.of("A", "B", "C"));
    }

    @GetMapping("/text")
    public ApiResponse<String> text() {
        return ApiResponse.ok("操作成功", "just a string");
    }

    /** 只有 ADMIN 可呼叫 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ApiResponse<String> admin() {
        return ApiResponse.ok("你是管理員");
    }
}
