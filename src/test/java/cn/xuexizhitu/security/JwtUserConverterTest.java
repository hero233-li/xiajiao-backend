package cn.xuexizhitu.security;
import cn.xuexizhitu.entity.*;
import cn.xuexizhitu.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class JwtUserConverterTest {
    @Test void rejectsRefreshAndRevokedAccess() {
        var repo=mock(UserRepository.class);var u=new AppUser(UUID.randomUUID().toString(),"u","u@example.com","hash",Role.USER);
        when(repo.findById(u.getId())).thenReturn(Optional.of(u));var converter=new JwtUserConverter(repo);
        Jwt access=Jwt.withTokenValue("example").header("alg","HS256").subject(u.getId()).claim("token_type","access").claim("token_version",0).build();
        assertThat(((CurrentUser)converter.convert(access).getPrincipal()).id()).isEqualTo(u.getId());
        u.rotateTokenVersion();assertThatThrownBy(() -> converter.convert(access)).isInstanceOf(InvalidBearerTokenException.class);
        Jwt refresh=Jwt.withTokenValue("example").header("alg","HS256").subject(u.getId()).claim("token_type","refresh").claim("token_version",1).build();
        assertThatThrownBy(() -> converter.convert(refresh)).isInstanceOf(InvalidBearerTokenException.class);
    }
    @Test void noCurrentUserFailsClosed() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertThatThrownBy(CurrentUser::require).isInstanceOf(cn.xuexizhitu.common.BusinessException.class);
    }
}
