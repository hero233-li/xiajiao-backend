package cn.xuexizhitu.identity.api;
import java.time.Instant;
public record TokenResponse(String accessToken,String refreshToken,String tokenType,Instant expiresAt,Instant refreshExpiresAt,UserDto user) {
}
