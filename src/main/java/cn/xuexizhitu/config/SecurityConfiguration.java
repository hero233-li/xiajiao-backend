package cn.xuexizhitu.config;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.security.JwtUserConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableMethodSecurity
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
    @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtUserConverter converter,ObjectMapper mapper) throws Exception {
        return http.headers(h -> h.cacheControl(c -> c.disable()).addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Cache-Control","no-store"))).csrf(c -> c.disable()).sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable())
            .authorizeHttpRequests(c -> c
                .requestMatchers("/api/v1/health","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/register").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(c -> c.authenticationEntryPoint((request,response,e) -> write(response,mapper,ErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request,response,e) -> write(response,mapper,ErrorCode.FORBIDDEN)))
            .oauth2ResourceServer(c -> c.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((request,response,e) -> write(response,mapper,ErrorCode.UNAUTHORIZED)))
            .build();
    }
    private void write(HttpServletResponse response,ObjectMapper mapper,ErrorCode error) throws java.io.IOException {
        response.setStatus(error.status().value());response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(),ApiResponse.failure(error));
    }
}
