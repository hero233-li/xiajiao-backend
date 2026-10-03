package cn.xuexizhitu;
import cn.xuexizhitu.common.BusinessException;
import cn.xuexizhitu.identity.api.LoginRequest;
import cn.xuexizhitu.identity.api.RefreshRequest;
import cn.xuexizhitu.identity.api.RegisterRequest;
import cn.xuexizhitu.identity.api.UserDto;
import cn.xuexizhitu.identity.domain.Role;
import cn.xuexizhitu.identity.infrastructure.LoginIdentifierRepository;
import cn.xuexizhitu.identity.infrastructure.UserRepository;
import cn.xuexizhitu.identity.application.AccountService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"app.assessments.timeout-enabled=false","app.files.cleanup-enabled=false"}) @AutoConfigureMockMvc @Testcontainers
class BackendMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("xuexizhitu");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.files.root",()->System.getProperty("java.io.tmpdir")+"/xuexizhitu-it-BackendMySqlIT-"+UUID.randomUUID());
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.bootstrap.enabled",() -> false);r.add("app.auth.registration-mode",() -> "DISABLED");
        r.add("app.jwt.secret-base64",() -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;@Autowired AccountService accounts;
    @Autowired UserRepository users;@Autowired PasswordEncoder encoder;@Autowired LoginIdentifierRepository identifiers;
    @Test void flywayCreatesAllApprovedTablesAndRejectsInvalidRole() {
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name<>'flyway_schema_history'",Integer.class);
        assertThat(count).isEqualTo(65);assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='1'",Boolean.class)).isTrue();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO app_user(id,username,email,password_hash,role) VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),"invalid","invalid@example.com","hash","OTHER")).isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("ck_app_user_1");
    }
    @Test void loginRefreshReplayAndLogoutAreEnforcedOverHttp() throws Exception {
        String name="u"+UUID.randomUUID();String email=name+"@example.com";String password="本次测试密码123";
        UserDto user=accounts.create(new RegisterRequest(name,email,password),Role.USER);
        assertThat(encoder.matches(password,users.findById(user.id()).orElseThrow().getPasswordHash())).isTrue();
        JsonNode login=login(email,password);String oldAccess=login.path("accessToken").asText();String oldRefresh=login.path("refreshToken").asText();
        login(name,password);
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+oldAccess)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(user.id()));
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+oldRefresh)).andExpect(status().isUnauthorized());
        JsonNode changed=mapper.readTree(mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(new RefreshRequest(oldRefresh))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+oldAccess)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(new RefreshRequest(oldRefresh))))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
        String access=changed.path("accessToken").asText();
        mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+access)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(new RefreshRequest(changed.path("refreshToken").asText())))).andExpect(status().isUnauthorized());
    }
    @Test void identifierNamespaceConflictRollsBackAccount() {
        String name="conflict"+UUID.randomUUID()+"@example.com";accounts.create(new RegisterRequest(name,"original"+UUID.randomUUID()+"@example.com","12345678"),Role.USER);
        long before=users.count();
        assertThatThrownBy(() -> accounts.create(new RegisterRequest("new"+UUID.randomUUID(),name,"12345678"),Role.USER)).isInstanceOf(cn.xuexizhitu.common.BusinessException.class);
        assertThat(users.count()).isEqualTo(before);
    }
    @Test void healthWorksOnRealContext() throws Exception {mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("UP"));}
    @Test void registrationIsClosedUntilConfirmed() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(mapper.writeValueAsString(new RegisterRequest("someone","someone@example.com","12345678"))))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40301));
    }
    private JsonNode login(String id,String password) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(mapper.writeValueAsString(new LoginRequest(id,password))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    }
}
