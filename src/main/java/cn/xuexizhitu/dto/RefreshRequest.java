package cn.xuexizhitu.dto;
import jakarta.validation.constraints.*;
public record RefreshRequest(@NotBlank(message="刷新令牌不能为空") @Size(max=4096,message="刷新令牌过长") String refreshToken) {}
