package cn.xuexizhitu;
import cn.xuexizhitu.config.BootstrapProperties;
import cn.xuexizhitu.config.JwtProperties;
import cn.xuexizhitu.config.RegistrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, BootstrapProperties.class, RegistrationProperties.class})
public class XuexiZhituApplication {
    public static void main(String[] args) { SpringApplication.run(XuexiZhituApplication.class, args); }
}
