package cn.xuexizhitu.identity.infrastructure;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.bootstrap") public record BootstrapProperties(boolean enabled,String username,String email,String password) {
}
