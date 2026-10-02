package cn.xuexizhitu.config;
import cn.xuexizhitu.dto.RegisterRequest;
import cn.xuexizhitu.entity.Role;
import cn.xuexizhitu.repository.UserRepository;
import cn.xuexizhitu.service.AccountService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
@Slf4j @Component @RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {
    private final BootstrapProperties properties;private final AccountService accounts;private final UserRepository users;private final Validator validator;
    @Override public void run(ApplicationArguments args) {
        if (!properties.enabled()) return;
        var request=new RegisterRequest(properties.username(),properties.email(),properties.password());
        if (!validator.validate(request).isEmpty()) throw new IllegalStateException("初始化管理员参数不合法，请检查用户名、邮箱和密码");
        var existing=users.findByIdentifier(properties.username());
        if (existing.isPresent()) {
            var user=existing.get();
            if (!user.getEmail().equals(properties.email()) || user.getRole()!=Role.ADMIN || !user.isEnabled())
                throw new IllegalStateException("已有账号与初始化管理员配置冲突");
            log.info("初始化管理员已存在，未改写密码");return;
        }
        if (users.count()!=0) throw new IllegalStateException("初始化仅允许在无账号的数据库创建首个管理员");
        accounts.create(request,Role.ADMIN);log.info("初始化管理员账号已创建");
    }
}
