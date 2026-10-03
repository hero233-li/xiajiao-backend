package cn.xuexizhitu.identity.infrastructure;
import cn.xuexizhitu.identity.domain.AppUser;
import cn.xuexizhitu.identity.infrastructure.UserRepository;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import java.util.List;
@Component @RequiredArgsConstructor public class JwtUserConverter implements Converter<Jwt,AbstractAuthenticationToken> {
    private final UserRepository users;
    @Override public AbstractAuthenticationToken convert(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("token_type"))) throw new InvalidBearerTokenException("无效访问令牌");
        AppUser u=users.findById(jwt.getSubject()).orElseThrow(() -> new InvalidBearerTokenException("无效访问令牌"));
        Object rawVersion=jwt.getClaim("token_version");
        if (!u.isEnabled() || !(rawVersion instanceof Number version) || version.doubleValue()!=u.getTokenVersion()) throw new InvalidBearerTokenException("令牌已失效");
        CurrentUser principal=new CurrentUser(u.getId(),u.getUsername(),u.getRole(),u.getTokenVersion());
        return new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name())));
    }
}
