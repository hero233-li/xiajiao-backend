package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.config.RegistrationProperties;
import cn.xuexizhitu.config.RegistrationProperties.Mode;
import cn.xuexizhitu.dto.*;
import cn.xuexizhitu.entity.Role;
import cn.xuexizhitu.security.CurrentUser;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class RegistrationServiceTest {
    final AccountService accounts=mock(AccountService.class);
    final RegisterRequest request=new RegisterRequest("newuser","new@example.com","12345678");
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    @Test void disabledRejectsEvenAdmin() {
        principal(Role.ADMIN);
        assertThatThrownBy(() -> service(Mode.DISABLED).register(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(accounts);
    }
    @Test void adminModeRejectsAnonymousAndUser() {
        assertThatThrownBy(() -> service(Mode.ADMIN_ONLY).register(request)).isInstanceOf(BusinessException.class);
        principal(Role.USER);assertThatThrownBy(() -> service(Mode.ADMIN_ONLY).register(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(accounts);
    }
    @Test void adminModeCreatesOnlyUser() {principal(Role.ADMIN);service(Mode.ADMIN_ONLY).register(request);verify(accounts).create(request,Role.USER);}
    @Test void explicitlyConfiguredPublicModeCreatesOnlyUser() {service(Mode.PUBLIC).register(request);verify(accounts).create(request,Role.USER);}
    RegistrationService service(Mode mode) {return new RegistrationService(new RegistrationProperties(mode),accounts);}
    void principal(Role role) {SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser("id","name",role,0),null,List.of()));}
}
