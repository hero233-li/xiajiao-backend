package cn.xuexizhitu.service;
import cn.xuexizhitu.identity.application.TokenService;
import cn.xuexizhitu.common.BusinessException;
import cn.xuexizhitu.identity.infrastructure.JwtConfiguration;
import cn.xuexizhitu.identity.infrastructure.JwtProperties;
import cn.xuexizhitu.identity.domain.AppUser;
import cn.xuexizhitu.identity.domain.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(classes=TokenServiceTest.TestConfig.class,properties={"app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=","app.jwt.issuer=xuexizhitu","app.jwt.audience=xuexizhitu-api","app.jwt.access-ttl=15m","app.jwt.refresh-ttl=7d"})
class TokenServiceTest {
    @org.springframework.context.annotation.Configuration
    @org.springframework.boot.context.properties.EnableConfigurationProperties(JwtProperties.class)
    @Import({JwtConfiguration.class,TokenService.class}) static class TestConfig {}
    @Autowired TokenService tokens;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Test void signedTokensAreSeparatedAndHaveIdentity() {
        var user=new AppUser(UUID.randomUUID().toString(),"owner","owner@example.com","hash",Role.ADMIN);
        var result=tokens.issue(user);var jwt=decoder.decode(result.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(user.getId());assertThat(jwt.getClaimAsString("token_type")).isEqualTo("access");
        assertThat(tokens.readRefresh(result.refreshToken()).getClaimAsString("token_type")).isEqualTo("refresh");
        assertThat(result.refreshExpiresAt()).isAfter(result.expiresAt());
        assertThatThrownBy(() -> tokens.readRefresh(result.accessToken())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> decoder.decode(result.accessToken()+"tampered")).isInstanceOf(JwtException.class);
    }
    @Test void rejectsExpiredWrongIssuerAndWrongAudience() {
        java.time.Instant now=java.time.Instant.now();
        for (var claims: java.util.List.of(claims("another","xuexizhitu-api",now.plusSeconds(100)),
            claims("xuexizhitu","another",now.plusSeconds(100)),claims("xuexizhitu","xuexizhitu-api",now.minusSeconds(1)))) {
            String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
        }
    }
    private JwtClaimsSet claims(String issuer,String audience,java.time.Instant expiry) {
        return JwtClaimsSet.builder().issuer(issuer).audience(java.util.List.of(audience)).subject(UUID.randomUUID().toString())
            .expiresAt(expiry).claim("token_type","access").claim("token_version",0).build();
    }
}
