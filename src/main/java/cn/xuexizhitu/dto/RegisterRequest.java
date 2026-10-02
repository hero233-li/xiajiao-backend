package cn.xuexizhitu.dto;
import cn.xuexizhitu.validation.ValidPassword;
import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank(message="请输入用户名") @Size(max=100,message="用户名不得超过100字符")
    @Pattern(regexp="[^\\s]+",message="用户名不能包含空白") String username,
    @NotBlank(message="请输入邮箱") @Email(message="邮箱格式不合法") @Size(max=254,message="邮箱过长")
    @Pattern(regexp="[^\\s]+",message="邮箱不能包含空白") String email,
    @io.swagger.v3.oas.annotations.media.Schema(format="password",accessMode=io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY) @ValidPassword(minLength=8,message="密码至少8个字符，且UTF-8长度不得超过72字节") String password) {}
