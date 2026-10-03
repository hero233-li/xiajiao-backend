package cn.xuexizhitu.identity.api;
import cn.xuexizhitu.validation.ValidPassword;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank(message="请输入用户名或邮箱") @Size(max=254,message="登录标识过长") String identifier,     @io.swagger.v3.oas.annotations.media.Schema(format="password",accessMode=io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY) @ValidPassword String password) {
}
