package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.config.RegistrationProperties;
import cn.xuexizhitu.dto.*;
import cn.xuexizhitu.entity.Role;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationProperties properties;private final AccountService accounts;
    public UserDto register(RegisterRequest request) {
        switch (properties.registrationMode()) {
            case DISABLED -> throw new BusinessException(ErrorCode.FORBIDDEN,"当前未开放注册");
            case ADMIN_ONLY -> {
                if (CurrentUser.require().role()!=Role.ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            case PUBLIC -> { /* 仅在用户明确确认开放后配置；默认关闭。 */ }
        }
        // 请求DTO没有role字段，注册永远不能自授ADMIN。
        return accounts.create(request,Role.USER);
    }
}
