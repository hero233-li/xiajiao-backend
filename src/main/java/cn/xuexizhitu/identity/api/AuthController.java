package cn.xuexizhitu.identity.api;
import cn.xuexizhitu.identity.application.RegistrationService;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.identity.api.LoginRequest;
import cn.xuexizhitu.identity.api.RefreshRequest;
import cn.xuexizhitu.identity.api.RegisterRequest;
import cn.xuexizhitu.identity.api.TokenResponse;
import cn.xuexizhitu.identity.api.UserDto;
import cn.xuexizhitu.identity.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor @Tag(name="auth",description="账号认证") public class AuthController {
    private final AuthService auth;
    private final cn.xuexizhitu.identity.application.RegistrationService registration;
    @PostMapping("/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="创建USER账号",description="默认关闭；按部署配置允许ADMIN创建或公开注册，范围待用户确认")     public ApiResponse<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(registration.register(request));
    }
    @PostMapping("/login") @Operation(summary="用户名或邮箱登录")     public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(auth.login(request));
    }
    @PostMapping("/refresh") @Operation(summary="轮换刷新令牌，同时撤销该账号旧令牌")     public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(auth.refresh(request));
    }
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")     @GetMapping("/me") @Operation(summary="获取当前用户")     public ApiResponse<UserDto> me() {
        return ApiResponse.ok(auth.me());
    }
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")     @PostMapping("/logout") @Operation(summary="退出并撤销账号全部令牌")     public ApiResponse<Void> logout() {
        auth.logout();
        return ApiResponse.ok(null);
    }
}
