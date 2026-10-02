package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.config.*;
import cn.xuexizhitu.dto.LoginRequest;
import cn.xuexizhitu.repository.UserRepository;
import cn.xuexizhitu.security.JwtUserConverter;
import cn.xuexizhitu.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest({HealthController.class,AuthController.class})
@Import({SecurityConfiguration.class,JwtConfiguration.class,JwtUserConverter.class})
@TestPropertySource(properties="app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class HealthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;
    @MockitoBean AuthService auth;
    @MockitoBean cn.xuexizhitu.service.RegistrationService registration;
    @Test void anonymousHealthUsesEnvelope() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.status").value("UP")).andExpect(jsonPath("$.message").value("ok"));
    }
    @Test void currentUserRequiresLogin() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101))
            .andExpect(content().json("{\"code\":40101,\"data\":null,\"message\":\"请先登录或令牌已失效\"}"));
    }
    @Test void badTokenUsesJsonInsteadOfHtml() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer invalid"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
    }
    @Test void missingPasswordIsValidationError() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"identifier\":\"owner\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001)).andExpect(jsonPath("$.message").isNotEmpty());
    }
    @Test void clientCannotInjectUserId() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"identifier\":\"owner\",\"password\":\"12345678\",\"userId\":\"other\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
    }
    @Test void businessErrorsKeepEnvelope() throws Exception {
        when(auth.login(any(LoginRequest.class))).thenThrow(new BusinessException(ErrorCode.BAD_CREDENTIALS));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"identifier\":\"owner\",\"password\":\"12345678\"}"))
            .andExpect(status().isUnauthorized()).andExpect(content().json("{\"code\":40102,\"data\":null,\"message\":\"用户名、邮箱或密码不正确\"}"));
    }
}
