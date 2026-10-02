package cn.xuexizhitu.dto;
import java.time.Instant;
public record TokenResponse(String accessToken,String refreshToken,String tokenType,Instant expiresAt,Instant refreshExpiresAt,UserDto user) {}
