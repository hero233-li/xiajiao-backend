package cn.xuexizhitu.config;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;
@Configuration public class OpenApiConfiguration {
    @Bean OpenAPI implementedApi() {
        return new OpenAPI().info(new Info().title("学习知途后端接口").version("0.0.1")             .description("已实现认证、学习内容、练习检测、真题成绩、计划看板、管理审核及历史接口；完整评审契约见docs/openapi.yaml。"))             .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
