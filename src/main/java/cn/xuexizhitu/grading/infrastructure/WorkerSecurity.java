package cn.xuexizhitu.grading.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.grading.application.GradingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
@Configuration public class WorkerSecurity {
    @Bean @Order(1) SecurityFilterChain workerChain(HttpSecurity http,GradingService service,ObjectMapper json)throws Exception {
        return http.securityMatcher("/api/v1/grading-worker/**").csrf(c->c.disable()).sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).requestCache(c->c.disable())             .addFilterBefore(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
                try {
                    var p=service.authenticate(req.getHeader("X-Worker-Token")); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of()));
                }
                catch(BusinessException e) {
                    res.setStatus(401); res.setContentType("application/json"); json.writeValue(res.getOutputStream(),ApiResponse.failure(ErrorCode.UNAUTHORIZED)); return;
                }
                res.setHeader("Cache-Control","no-store"); chain.doFilter(req,res);
            }
        }
        ,UsernamePasswordAuthenticationFilter.class).authorizeHttpRequests(c->c.anyRequest().authenticated()).build();
    }
}
