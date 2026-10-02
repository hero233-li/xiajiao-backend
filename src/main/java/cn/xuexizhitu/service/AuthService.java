package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.*;
import cn.xuexizhitu.entity.AppUser;
import cn.xuexizhitu.repository.UserRepository;
import cn.xuexizhitu.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
    private final UserRepository users;private final PasswordEncoder encoder;private final TokenService tokens;private final String dummyHash;
    public AuthService(UserRepository users,PasswordEncoder encoder,TokenService tokens) {
        this.users=users;this.encoder=encoder;this.tokens=tokens;this.dummyHash=encoder.encode("仅用于未知账号耗时对齐的密码");
    }
    @Transactional(readOnly=true)
    public TokenResponse login(LoginRequest request) {
        AppUser user=users.findByIdentifier(request.identifier()).orElse(null);
        boolean matches=encoder.matches(request.password(),user==null?dummyHash:user.getPasswordHash());
        if (user==null || !matches || !user.isEnabled()) throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        return tokens.issue(user);
    }
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        var jwt=tokens.readRefresh(request.refreshToken());
        AppUser user=users.findLockedById(jwt.getSubject()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Number version=jwt.getClaim("token_version");
        if (!user.isEnabled() || version.longValue()!=user.getTokenVersion()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        // 现有设计只有用户级token_version：轮换会撤销该账号所有旧访问/刷新令牌。
        user.rotateTokenVersion();users.flush();return tokens.issue(user);
    }
    @Transactional(readOnly=true)
    public UserDto me() {return UserDto.from(users.findById(CurrentUser.idOrThrow()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED)));}
    @Transactional
    public void logout() {
        CurrentUser principal=CurrentUser.require();
        AppUser user=users.findLockedById(principal.id()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getTokenVersion()!=principal.tokenVersion()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        user.rotateTokenVersion();users.flush();
    }
}
