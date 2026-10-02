package cn.xuexizhitu.security;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
public record CurrentUser(String id,String username,Role role,int tokenVersion) {
    public static CurrentUser require() {
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        if (a==null || !a.isAuthenticated() || !(a.getPrincipal() instanceof CurrentUser user)) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user;
    }
    public static String idOrThrow() { return require().id(); }
}
