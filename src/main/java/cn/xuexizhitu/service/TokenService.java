package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.config.JwtProperties;
import cn.xuexizhitu.dto.*;
import cn.xuexizhitu.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class TokenService {
    private final JwtEncoder encoder; private final JwtDecoder decoder; private final JwtProperties properties; private final Clock clock;
    public TokenResponse issue(AppUser user) {
        Instant now=clock.instant();Instant accessEnd=now.plus(properties.accessTtl());Instant refreshEnd=now.plus(properties.refreshTtl());
        return new TokenResponse(encode(user,"access",now,accessEnd),encode(user,"refresh",now,refreshEnd),"Bearer",accessEnd,refreshEnd,UserDto.from(user));
    }
    private String encode(AppUser user,String type,Instant now,Instant end) {
        var claims=JwtClaimsSet.builder().issuer(properties.issuer()).subject(user.getId()).audience(List.of(properties.audience()))
            .issuedAt(now).notBefore(now).expiresAt(end).id(UUID.randomUUID().toString())
            .claim("token_type",type).claim("token_version",user.getTokenVersion()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    public Jwt readRefresh(String token) {
        try {
            Jwt jwt=decoder.decode(token);Number version=jwt.getClaim("token_version");
            if (!"refresh".equals(jwt.getClaimAsString("token_type")) || version==null || jwt.getExpiresAt()==null
                || !jwt.getExpiresAt().isAfter(clock.instant())) throw new BusinessException(ErrorCode.UNAUTHORIZED);
            return jwt;
        } catch (JwtException | IllegalArgumentException e) {throw new BusinessException(ErrorCode.UNAUTHORIZED);}
    }
}
