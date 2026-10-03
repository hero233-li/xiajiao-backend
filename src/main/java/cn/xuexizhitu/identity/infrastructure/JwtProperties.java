package cn.xuexizhitu.identity.infrastructure;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
@Validated @ConfigurationProperties("app.jwt") public record JwtProperties(@NotBlank String secretBase64, @NotBlank String issuer,     @NotBlank String audience, @NotNull Duration accessTtl, @NotNull Duration refreshTtl) {
    @AssertTrue(message="令牌有效期必须为正数且刷新期限长于访问期限")     public boolean isTtlValid() {
        return accessTtl!=null && refreshTtl!=null && !accessTtl.isNegative() && !accessTtl.isZero()         && refreshTtl.compareTo(accessTtl)>0;
    }
}
