package cn.xuexizhitu;
import cn.xuexizhitu.identity.infrastructure.BootstrapProperties;
import cn.xuexizhitu.identity.infrastructure.JwtProperties;
import cn.xuexizhitu.identity.infrastructure.RegistrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
@SpringBootApplication @EnableConfigurationProperties( {
    JwtProperties.class, BootstrapProperties.class, RegistrationProperties.class
}
) public class XuexiZhituApplication {
    public static void main(String[] args) {
        SpringApplication.run(XuexiZhituApplication.class, args);
    }
}
