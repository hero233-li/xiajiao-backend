package cn.xuexizhitu.config;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@Validated @ConfigurationProperties("app.auth")
public record RegistrationProperties(@NotNull Mode registrationMode) {
    public enum Mode { DISABLED, ADMIN_ONLY, PUBLIC }
}
