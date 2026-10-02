package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.*;
import cn.xuexizhitu.entity.*;
import cn.xuexizhitu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor @org.springframework.validation.annotation.Validated
public class AccountService {
    private final UserRepository users;private final LoginIdentifierRepository identifiers;private final PasswordEncoder encoder;
    @Transactional
    public UserDto create(@jakarta.validation.Valid RegisterRequest request,Role role) {
        if (identifiers.existsById(request.username()) || identifiers.existsById(request.email())) throw conflict();
        try {
            AppUser user=new AppUser(UUID.randomUUID().toString(),request.username(),request.email(),encoder.encode(request.password()),role);
            users.saveAndFlush(user);
            if (request.username().equals(request.email())) identifiers.save(new LoginIdentifier(request.username(),user.getId(),"BOTH"));
            else {identifiers.save(new LoginIdentifier(request.username(),user.getId(),"USERNAME"));identifiers.save(new LoginIdentifier(request.email(),user.getId(),"EMAIL"));}
            identifiers.flush();return UserDto.from(user);
        } catch (DataIntegrityViolationException e) {throw conflict();}
    }
    private BusinessException conflict() {return new BusinessException(ErrorCode.CONFLICT,"用户名或邮箱已被占用");}
}
