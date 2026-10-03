package cn.xuexizhitu.identity.api;
import cn.xuexizhitu.identity.domain.AppUser;
import cn.xuexizhitu.identity.domain.Role;
public record UserDto(String id,String username,String email,Role role) {
    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(),u.getUsername(),u.getEmail(),u.getRole());
    }
}
