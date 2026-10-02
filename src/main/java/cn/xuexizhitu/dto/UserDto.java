package cn.xuexizhitu.dto;
import cn.xuexizhitu.entity.AppUser;
import cn.xuexizhitu.entity.Role;
public record UserDto(String id,String username,String email,Role role) {
    public static UserDto from(AppUser u) {return new UserDto(u.getId(),u.getUsername(),u.getEmail(),u.getRole());}
}
