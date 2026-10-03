package cn.xuexizhitu.identity.infrastructure;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
@Configuration public class JwtConfiguration {
    @Bean Clock clock() {
        return Clock.systemUTC();
    }
    @Bean SecretKey jwtSecret(JwtProperties p) {
        byte[] bytes;
        try {
            bytes=Base64.getDecoder().decode(p.secretBase64());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT密钥必须是有效Base64");
        }
        if (bytes.length<32) throw new IllegalStateException("JWT密钥至少需要32个随机字节");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
    @Bean JwtDecoder jwtDecoder(SecretKey key,JwtProperties p) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var audience=new JwtClaimValidator<List<String>>("aud", a -> a!=null && a.contains(p.audience()));
        var expiry=new JwtClaimValidator<Object>("exp", value -> value!=null);
        var subject=new JwtClaimValidator<String>("sub", value -> value!=null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
        var version=new JwtClaimValidator<Object>("token_version", value -> value instanceof Number n && n.doubleValue()>=0 && n.doubleValue()<=Integer.MAX_VALUE && n.doubleValue()==n.intValue());
        var type=new JwtClaimValidator<String>("token_type", value -> "access".equals(value) || "refresh".equals(value));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(java.time.Duration.ZERO),new JwtIssuerValidator(p.issuer()),audience,expiry,subject,version,type));
        return decoder;
    }
}
